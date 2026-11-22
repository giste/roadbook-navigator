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

package dev.giste.roadbook.ui.drawing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import dev.giste.roadbook.R
import dev.giste.roadbook.domain.drawing.Element
import dev.giste.roadbook.domain.drawing.Icon
import dev.giste.roadbook.domain.drawing.IconType
import dev.giste.roadbook.domain.drawing.NoteElement
import dev.giste.roadbook.domain.drawing.Road
import dev.giste.roadbook.domain.drawing.Text
import dev.giste.roadbook.domain.drawing.Track
import dev.giste.roadbook.domain.drawing.TulipElement
import dev.giste.roadbook.ui.icon.IconColors
import dev.giste.roadbook.ui.icon.rememberIconVector
import dev.giste.roadbook.ui.theme.RoadbookTheme

/**
 * Draws a tulip: the junction diagram of a waypoint, with the mark at its junction, its roads,
 * the track the rider follows (ending in an arrow), its symbols and its texts.
 *
 * It keeps the proportions of the roadbook canvas and fills the width it is given. Colors come
 * from the enclosing `RoadbookTheme`.
 *
 * @param elements what the tulip holds, drawn in order.
 * @param modifier the modifier applied to the canvas.
 */
@Composable
internal fun TulipCanvas(elements: List<TulipElement>, modifier: Modifier = Modifier) {
    ElementCanvas(elements = elements, hasWaypointMark = true, modifier = modifier)
}

/**
 * Draws the notes column of a waypoint: its symbols and texts.
 *
 * It keeps the proportions of the roadbook canvas and fills the width it is given. Colors come
 * from the enclosing `RoadbookTheme`.
 *
 * @param elements what the notes column holds, drawn in order.
 * @param modifier the modifier applied to the canvas.
 */
@Composable
internal fun NotesCanvas(elements: List<NoteElement>, modifier: Modifier = Modifier) {
    ElementCanvas(elements = elements, hasWaypointMark = false, modifier = modifier)
}

/**
 * Draws [elements] in order, on a canvas with the proportions of the roadbook canvas, scaled to
 * the width it is given and clipped to its bounds.
 *
 * @param elements what to draw.
 * @param hasWaypointMark whether to draw the mark at the junction first, as tulips have.
 * @param modifier the modifier applied to the canvas.
 */
@Composable
private fun ElementCanvas(
    elements: List<Element>,
    hasWaypointMark: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = RoadbookTheme.colors
    val textMeasurer = rememberTextMeasurer()
    val unknownSymbol = stringResource(R.string.roadbook_symbol_unknown)
    val painters = rememberIconPainters(elements, colors.iconColors())

    Canvas(modifier = modifier.aspectRatio(CANVAS_WIDTH / CANVAS_HEIGHT)) {
        val scale = size.width / CANVAS_WIDTH
        clipRect {
            if (hasWaypointMark) {
                inCanvasUnits(scale) { drawWaypointMark(colors.onSurface) }
            }
            elements.forEach { element ->
                when (element) {
                    is Track -> inCanvasUnits(scale) {
                        val (track, center) = colors.track to colors.trackSecondary
                        drawRoad(element.roadIn, track, center, RoadEnd.NONE)
                        drawRoad(element.roadOut, track, center, RoadEnd.ARROW)
                    }
                    is Road -> inCanvasUnits(scale) {
                        drawRoad(element, colors.onSurface, colors.roadSecondary, RoadEnd.BAR)
                    }
                    is Icon -> {
                        val painter = painters[element.type]
                        if (painter != null) {
                            drawIcon(element, painter, scale)
                        } else {
                            drawUnknownIcon(
                                icon = element,
                                fallbackText = unknownSymbol,
                                textMeasurer = textMeasurer,
                                color = colors.danger,
                                scale = scale,
                            )
                        }
                    }
                    is Text -> drawCanvasText(element, textMeasurer, colors.onSurface, scale)
                }
            }
        }
    }
}

/** Runs [block] scaled by [scale] pixels per canvas unit, so it can draw in canvas units. */
private fun DrawScope.inCanvasUnits(scale: Float, block: DrawScope.() -> Unit) {
    withTransform(transformBlock = { scale(scale, scale, pivot = Offset.Zero) }, drawBlock = block)
}

/**
 * One painter per type of symbol in [elements], drawn with [colors], kept for as long as both
 * stay the same. Symbols without a vector (`IconType.UNKNOWN`) have none.
 */
@Composable
private fun rememberIconPainters(
    elements: List<Element>,
    colors: IconColors,
): Map<IconType, Painter> {
    val types = remember(elements) {
        elements.filterIsInstance<Icon>().map { it.type }.distinct()
    }
    return types.mapNotNull { type ->
        key(type) {
            rememberIconVector(type, colors)?.let { type to rememberVectorPainter(it) }
        }
    }.toMap()
}
