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

package dev.giste.roadbook.presentation.drawing

import android.graphics.Paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import dev.giste.roadbook.domain.drawing.Icon
import dev.giste.roadbook.domain.drawing.Road
import dev.giste.roadbook.domain.drawing.RoadType
import dev.giste.roadbook.domain.drawing.Text

// Draws the elements of a tulip or notes column on a Canvas, as the previous app did.
//
// Roads are paths, so they are drawn in canvas units under a scale transform. Symbols and texts
// are drawn in pixels instead (their canvas positions and sizes multiplied by the scale): a vector
// painter renders to a bitmap of the size it is drawn at, and text is laid out at its font size,
// so scaling them up afterwards would blur them.

/** How the end of a road is marked. */
internal enum class RoadEnd {

    /** No mark: the road the rider arrives on. */
    NONE,

    /** An arrow head: the road the rider leaves on. */
    ARROW,

    /** A short bar across the road: any other road. */
    BAR,
}

/** Width of an ordinary track. */
private const val TRACK_WIDTH: Float = 6f

/** Width of narrower tracks: small, low visibility and off track. */
private const val THIN_TRACK_WIDTH: Float = 4f

/** Width of the edges of a tarmac road or dual carriageway, and of thin marks. */
private const val EDGE_WIDTH: Float = 2f

/** Total width of a tarmac road, between the outer sides of its edges. */
private const val TARMAC_WIDTH: Float = 6f

/** Total width of a dual carriageway, between the outer sides of its edges. */
private const val DUAL_CARRIAGEWAY_WIDTH: Float = 8f

/** Dash pattern of a low visibility track: long and short dashes. */
private val LOW_VISIBILITY_DASHES: FloatArray = floatArrayOf(15f, 5f, 5f, 5f)

/** Dash pattern of an off track. */
private val OFF_TRACK_DASHES: FloatArray = floatArrayOf(5f, 5f)

/** Length of the mark at the junction of every tulip, down and to the right of it. */
private const val WAYPOINT_MARK_LENGTH: Float = 15f

/** Radius of the dot at the end of the waypoint mark. */
private const val WAYPOINT_MARK_DOT_RADIUS: Float = 3f

/** Size of the font of the name of a symbol the app cannot draw, in canvas units. */
private const val UNKNOWN_SYMBOL_FONT_SIZE: Float = 10f

/** Dash and gap length of the frame of a symbol the app cannot draw, in canvas units. */
private const val UNKNOWN_SYMBOL_DASH: Float = 4f

/**
 * Extra line spacing, over the line height of the file, that the previous app used so lines of
 * bold text do not touch.
 */
private const val TEXT_LINE_SPACING: Float = 1.1f

/**
 * Draws the mark every tulip has at its junction: a short line down and to the right, ending in
 * a dot. In canvas units: call it under the canvas scale.
 *
 * @param color the ink color.
 */
internal fun DrawScope.drawWaypointMark(color: Color) {
    val end = JUNCTION + Offset(WAYPOINT_MARK_LENGTH, WAYPOINT_MARK_LENGTH)
    drawLine(color = color, start = JUNCTION, end = end, strokeWidth = EDGE_WIDTH)
    drawCircle(color = color, radius = WAYPOINT_MARK_DOT_RADIUS, center = end)
}

/**
 * Draws [road] in the style of its type, with the mark [roadEnd] at its end. A road without an
 * end is not drawn. In canvas units: call it under the canvas scale.
 *
 * @param road the road to draw.
 * @param color the color of the road.
 * @param secondaryColor the color of the center line of a dual carriageway.
 * @param roadEnd how to mark its end.
 */
internal fun DrawScope.drawRoad(road: Road, color: Color, secondaryColor: Color, roadEnd: RoadEnd) {
    val points = road.canvasPoints() ?: return
    val path = smoothSegments(points).toPath(start = points.first())

    when (road.type) {
        RoadType.TRACK -> drawPath(path, color, style = plainStroke(TRACK_WIDTH))
        RoadType.SMALL_TRACK -> drawPath(path, color, style = plainStroke(THIN_TRACK_WIDTH))
        RoadType.LOW_VISIBILITY_TRACK ->
            drawPath(path, color, style = plainStroke(THIN_TRACK_WIDTH, LOW_VISIBILITY_DASHES))
        RoadType.OFF_TRACK ->
            drawPath(path, color, style = plainStroke(THIN_TRACK_WIDTH, OFF_TRACK_DASHES))
        RoadType.TARMAC_ROAD ->
            drawPath(path.edges(TARMAC_WIDTH), color, style = plainStroke(EDGE_WIDTH))
        RoadType.DUAL_CARRIAGEWAY -> {
            drawPath(path, secondaryColor, style = plainStroke(EDGE_WIDTH))
            drawPath(path.edges(DUAL_CARRIAGEWAY_WIDTH), color, style = plainStroke(EDGE_WIDTH))
        }
    }

    val end = points.last()
    val angle = endAngle(points)
    when (roadEnd) {
        RoadEnd.NONE -> Unit
        RoadEnd.ARROW -> drawPath(arrowHead(end, angle).toClosedPath(), color)
        RoadEnd.BAR -> {
            val (barStart, barEnd) = endBar(end, angle)
            drawLine(color, barStart, barEnd, strokeWidth = EDGE_WIDTH, cap = StrokeCap.Butt)
        }
    }
}

/**
 * Draws [icon] with [painter], centered on its position, rotated by its angle and scaled by its
 * scales, which mirror it when negative. In pixels: call it without the canvas scale.
 *
 * @param icon the symbol and where it goes.
 * @param painter draws the symbol.
 * @param scale pixels per canvas unit.
 */
internal fun DrawScope.drawIcon(icon: Icon, painter: Painter, scale: Float) {
    val size = Size(icon.width.toFloat() * scale, icon.height.toFloat() * scale)
    withIconTransform(icon, size, scale) {
        with(painter) { draw(size) }
    }
}

/**
 * Draws a symbol the app cannot draw: a dashed frame of its size, with its name from the file
 * inside, or [fallbackText] when the name does not fit or there is none. In pixels: call it
 * without the canvas scale.
 *
 * @param icon the symbol and where it goes.
 * @param fallbackText what to write when the name does not fit, e.g. a question mark.
 * @param textMeasurer lays out the text.
 * @param color the color of the frame and the text.
 * @param scale pixels per canvas unit.
 */
internal fun DrawScope.drawUnknownIcon(
    icon: Icon,
    fallbackText: String,
    textMeasurer: TextMeasurer,
    color: Color,
    scale: Float,
) {
    val size = Size(icon.width.toFloat() * scale, icon.height.toFloat() * scale)
    withIconTransform(icon, size, scale) {
        val dash = UNKNOWN_SYMBOL_DASH * scale
        drawRect(
            color = color,
            size = size,
            style = Stroke(
                width = scale,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
            ),
        )
        val style = TextStyle(
            color = color,
            fontSize = (UNKNOWN_SYMBOL_FONT_SIZE * scale).toSp(),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        val name = icon.originalName ?: icon.originalId
        val constraints =
            Constraints(maxWidth = size.width.toInt(), maxHeight = size.height.toInt())
        val nameLayout = name?.let { textMeasurer.measure(it, style, constraints = constraints) }
        val layout = nameLayout
            ?.takeUnless { it.hasVisualOverflow }
            ?: textMeasurer.measure(fallbackText, style)
        drawText(
            textLayoutResult = layout,
            topLeft = Offset(
                (size.width - layout.size.width) / 2f,
                (size.height - layout.size.height) / 2f,
            ),
        )
    }
}

/**
 * Draws [text] in bold, centered in its box, and clipped to the largest size the box may grow
 * to. In pixels: call it without the canvas scale.
 *
 * @param text the text and where it goes.
 * @param textMeasurer lays out the text.
 * @param color the ink color.
 * @param scale pixels per canvas unit.
 */
internal fun DrawScope.drawCanvasText(
    text: Text,
    textMeasurer: TextMeasurer,
    color: Color,
    scale: Float,
) {
    val center = text.center.toOffset() * scale
    val box = Size(text.maxWidth.toFloat() * scale, text.maxHeight.toFloat() * scale)
    val boxTopLeft = center - Offset(box.width / 2f, box.height / 2f)
    // toSp() also undoes the user's font scale: the text must fit the drawing, like the roads.
    val fontSize = (text.fontSize * scale).toSp()
    val style = TextStyle(
        color = color,
        fontSize = fontSize,
        lineHeight = fontSize * (text.lineHeight.toFloat() * TEXT_LINE_SPACING),
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
    )
    val layout = textMeasurer.measure(
        text = text.text,
        style = style,
        constraints = Constraints(maxWidth = box.width.toInt(), maxHeight = box.height.toInt()),
    )
    clipRect(
        left = boxTopLeft.x,
        top = boxTopLeft.y,
        right = boxTopLeft.x + box.width,
        bottom = boxTopLeft.y + box.height,
    ) {
        drawText(
            textLayoutResult = layout,
            topLeft = boxTopLeft + Offset(
                (box.width - layout.size.width) / 2f,
                (box.height - layout.size.height) / 2f,
            ),
        )
    }
}

/**
 * Runs [block] with the origin at the top-left corner of [icon], of [size] pixels, after moving
 * it to its center, rotating it by its angle and scaling it by its scales around its center.
 */
private fun DrawScope.withIconTransform(
    icon: Icon,
    size: Size,
    scale: Float,
    block: DrawScope.() -> Unit,
) {
    val center = icon.center.toOffset() * scale
    withTransform(
        transformBlock = {
            translate(center.x, center.y)
            rotate(icon.angle.toFloat(), pivot = Offset.Zero)
            scale(icon.scaleX.toFloat(), icon.scaleY.toFloat(), pivot = Offset.Zero)
            translate(-size.width / 2f, -size.height / 2f)
        },
        drawBlock = block,
    )
}

/** A stroke with square ends and corners, optionally dashed with [dashes]. */
private fun plainStroke(width: Float, dashes: FloatArray? = null): Stroke = Stroke(
    width = width,
    cap = StrokeCap.Butt,
    join = StrokeJoin.Miter,
    pathEffect = dashes?.let { PathEffect.dashPathEffect(it) },
)

/** The path of these segments, starting at [start]. */
private fun List<RoadSegment>.toPath(start: Offset): Path = Path().apply {
    moveTo(start.x, start.y)
    forEach { segment ->
        when (segment) {
            is RoadSegment.Line -> lineTo(segment.end.x, segment.end.y)
            is RoadSegment.Quadratic ->
                quadraticTo(segment.control.x, segment.control.y, segment.end.x, segment.end.y)
            is RoadSegment.Cubic -> cubicTo(
                segment.control1.x,
                segment.control1.y,
                segment.control2.x,
                segment.control2.y,
                segment.end.x,
                segment.end.y,
            )
        }
    }
}

/** The closed outline through these points. */
private fun List<Offset>.toClosedPath(): Path = Path().apply {
    moveTo(first().x, first().y)
    drop(1).forEach { lineTo(it.x, it.y) }
    close()
}

/**
 * The outline of this path stroked [width] wide: its two edges, which are drawn to show a road
 * with two sides. Compose has no such operation, so it uses Android's `Paint.getFillPath`.
 */
private fun Path.edges(width: Float): Path {
    val outline = android.graphics.Path()
    Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = width
        strokeCap = Paint.Cap.BUTT
        strokeJoin = Paint.Join.MITER
        isAntiAlias = true
    }.getFillPath(asAndroidPath(), outline)
    return outline.asComposePath()
}
