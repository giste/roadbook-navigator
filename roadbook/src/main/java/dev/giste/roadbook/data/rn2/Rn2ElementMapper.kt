/*
 * Copyright (C) 2026  Giste
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.giste.roadbook.data.rn2

import dev.giste.core.logging.Logger
import dev.giste.roadbook.domain.drawing.CanvasPoint
import dev.giste.roadbook.domain.drawing.Element
import dev.giste.roadbook.domain.drawing.Icon
import dev.giste.roadbook.domain.drawing.IconType
import dev.giste.roadbook.domain.drawing.Road
import dev.giste.roadbook.domain.drawing.RoadType
import dev.giste.roadbook.domain.drawing.Text
import dev.giste.roadbook.domain.drawing.Track
import javax.inject.Inject
import kotlin.math.roundToInt

/** Where the road in of a track ends when the file does not say: just below the center. */
internal val DEFAULT_ROAD_IN_END: CanvasPoint = CanvasPoint(0.0, 35.0)

/**
 * Where the road out of a track ends when the file does not say and there is no next point to
 * aim at: straight on, near the top.
 */
internal val DEFAULT_ROAD_OUT_END: CanvasPoint = CanvasPoint(0.0, -55.0)

/** Line height of a text when the file does not give one, as a multiple of the font size. */
private const val DEFAULT_LINE_HEIGHT: Double = 1.0

/** Widest a text box may grow when the file does not say. */
private const val DEFAULT_TEXT_MAX_WIDTH: Double = 180.0

/** Tallest a text box may grow when the file does not say. */
private const val DEFAULT_TEXT_MAX_HEIGHT: Double = 100.0

/** Rally Navigator's ids of the kinds of road (`typeId`). */
private val ROAD_TYPES_BY_ID: Map<Int, RoadType> = mapOf(
    15 to RoadType.LOW_VISIBILITY_TRACK,
    16 to RoadType.OFF_TRACK,
    4 to RoadType.SMALL_TRACK,
    17 to RoadType.TRACK,
    18 to RoadType.TARMAC_ROAD,
    12 to RoadType.DUAL_CARRIAGEWAY,
)

/**
 * The elements of a tulip, and the kind of road it leaves on.
 *
 * @property elements the elements to draw.
 * @property roadTypeOut the kind of the road out of the last track, or the inherited kind when
 * the tulip has no track. The next tulip inherits it.
 */
internal data class MappedTulip(val elements: List<Element>, val roadTypeOut: RoadType)

/**
 * Maps the elements of a `.rn2` column to drawing elements, filling in what the file leaves out.
 *
 * Elements whose values cannot be drawn (e.g. an icon of zero width) and elements of unknown type
 * are skipped with a warning, so one odd element does not stop the whole roadbook from loading.
 */
internal class Rn2ElementMapper @Inject constructor(private val logger: Logger) {

    /**
     * Maps the elements of a tulip.
     *
     * Roads of a track without a kind take it from the context: the road in takes the
     * [inheritedRoadType] (the kind the previous tulip left on), and the road out takes the kind
     * of the road in. Each track passes its road out's kind on to the next one.
     *
     * @param elements the elements, in drawing order.
     * @param inheritedRoadType the kind of road the previous tulip left on.
     * @param exitPoint where the road out of a track ends when the file does not say. Only called
     * when needed.
     * @return the elements and the kind of road the tulip leaves on.
     */
    fun mapTulip(
        elements: List<Rn2Element>,
        inheritedRoadType: RoadType,
        exitPoint: () -> CanvasPoint,
    ): MappedTulip {
        var roadType = inheritedRoadType
        val mapped = elements.mapNotNull { element ->
            mapElement(element, roadType, exitPoint)?.also {
                if (it is Track) roadType = it.roadOut.type
            }
        }
        return MappedTulip(mapped, roadType)
    }

    /**
     * Maps the elements of a notes column. A track there has no next point to aim at, so its road
     * out goes straight on unless the file says otherwise.
     *
     * @param elements the elements, in drawing order.
     * @param inheritedRoadType the kind of road for tracks that do not give one.
     * @return the elements.
     */
    fun mapNotes(elements: List<Rn2Element>, inheritedRoadType: RoadType): List<Element> =
        mapTulip(elements, inheritedRoadType) { DEFAULT_ROAD_OUT_END }.elements

    /** Maps one element, or returns `null` (and warns) if it must be skipped. */
    private fun mapElement(
        element: Rn2Element,
        inheritedRoadType: RoadType,
        exitPoint: () -> CanvasPoint,
    ): Element? {
        val mapped = when (element) {
            is Rn2Road -> element.toRoad(defaultType = RoadType.TRACK)
            is Rn2Track -> element.toTrack(inheritedRoadType, exitPoint)
            is Rn2Text -> element.toText()
            is Rn2Icon -> element.toIcon()
            Rn2UnknownElement -> {
                logger.w("Skipping a .rn2 element of unknown type")
                return null
            }
        }
        if (mapped == null) logger.w("Skipping a .rn2 element that cannot be drawn: %s", element)
        return mapped
    }

    private fun Rn2Track.toTrack(
        inheritedRoadType: RoadType,
        exitPoint: () -> CanvasPoint,
    ): Track? {
        val roadInType = roadIn.typeId?.let(ROAD_TYPES_BY_ID::get) ?: inheritedRoadType
        val roadOutType = roadOut.typeId?.let(ROAD_TYPES_BY_ID::get) ?: roadInType
        val mappedIn = roadIn.toRoad(defaultType = roadInType, defaultEnd = { DEFAULT_ROAD_IN_END })
            ?: return null
        val mappedOut = roadOut.toRoad(
            defaultType = roadOutType,
            defaultStart = CanvasPoint.CENTER,
            defaultEnd = exitPoint,
        ) ?: return null
        return Track(roadIn = mappedIn, roadOut = mappedOut)
    }

    /**
     * Maps a road, using the defaults for what the file leaves out, or returns `null` if a point
     * is not finite.
     */
    private fun Rn2Road.toRoad(
        defaultType: RoadType,
        defaultStart: CanvasPoint? = null,
        defaultEnd: () -> CanvasPoint? = { null },
    ): Road? {
        val mappedStart = start?.let { it.toCanvasPoint() ?: return null } ?: defaultStart
        val mappedEnd = end?.let { it.toCanvasPoint() ?: return null } ?: defaultEnd()
        val mappedHandles = handles.map { it.toCanvasPoint() ?: return null }
        return Road(
            start = mappedStart,
            end = mappedEnd,
            type = typeId?.let(ROAD_TYPES_BY_ID::get) ?: defaultType,
            handles = mappedHandles,
        )
    }

    private fun Rn2Text.toText(): Text? {
        val textFontSize = fontSize ?: Text.DEFAULT_FONT_SIZE
        val textLineHeight = lineHeight ?: DEFAULT_LINE_HEIGHT
        val textMaxWidth = maxWidth ?: DEFAULT_TEXT_MAX_WIDTH
        val textMaxHeight = maxHeight ?: DEFAULT_TEXT_MAX_HEIGHT
        val center = canvasPointOrNull(x, y) ?: return null
        val boxIsValid = listOf(width, height, textMaxWidth, textMaxHeight)
            .all { it.isFinite() && it >= 0.0 }
        val lineHeightIsValid = textLineHeight.isFinite() && textLineHeight > 0.0
        if (textFontSize <= 0 || !lineHeightIsValid || !boxIsValid) return null
        return Text(
            text = text,
            fontSize = textFontSize,
            lineHeight = textLineHeight,
            width = width,
            height = height,
            maxWidth = textMaxWidth,
            maxHeight = textMaxHeight,
            center = center,
        )
    }

    /**
     * Maps a symbol. A file gives its size either scaled (a width and height with a scale on
     * each axis) or as a square of side `w` without a scale. Both keep the size before scaling
     * and the scale apart, so the drawn size is the size times the scale.
     */
    private fun Rn2Icon.toIcon(): Icon? {
        val givenScaleX = scaleX
        val givenScaleY = scaleY
        val scaled = givenScaleX != null && givenScaleY != null
        val iconWidth = if (scaled) width ?: DEFAULT_ICON_SIZE else w
        val iconHeight = if (scaled) height ?: DEFAULT_ICON_SIZE else w
        val iconScaleX = if (scaled) givenScaleX else 1.0
        val iconScaleY = if (scaled) givenScaleY else 1.0
        val iconAngle = angle ?: 0.0
        val center = canvasPointOrNull(x, y) ?: return null
        val sizeIsValid = listOf(iconWidth, iconHeight).all { it.isFinite() && it > 0.0 }
        val scaleIsValid = listOf(iconScaleX, iconScaleY).all { it.isFinite() && it != 0.0 }
        if (!sizeIsValid || !scaleIsValid || !iconAngle.isFinite()) return null

        val type = iconType()
        if (type == IconType.UNKNOWN) logger.w("Unknown .rn2 symbol: id %s, name %s", id, name)
        return Icon(
            type = type,
            width = iconWidth,
            height = iconHeight,
            center = center,
            angle = iconAngle.roundToInt(),
            scaleX = iconScaleX,
            scaleY = iconScaleY,
            originalId = id,
            originalName = name,
        )
    }

    private fun Rn2Point.toCanvasPoint(): CanvasPoint? = canvasPointOrNull(x, y)

    /** A canvas point at ([x], [y]), or `null` when a coordinate is not finite. */
    private fun canvasPointOrNull(x: Double, y: Double): CanvasPoint? =
        if (x.isFinite() && y.isFinite()) CanvasPoint(x, y) else null
}
