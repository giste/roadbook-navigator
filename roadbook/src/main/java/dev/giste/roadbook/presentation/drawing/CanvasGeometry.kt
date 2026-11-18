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

import androidx.compose.ui.geometry.Offset
import dev.giste.roadbook.domain.drawing.CanvasPoint
import dev.giste.roadbook.domain.drawing.Road
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// The geometry of a tulip or notes canvas, kept apart from the drawing so it can be tested
// without Android. Everything here is in canvas units, the units of the roadbook file.

/** Width of a tulip or notes canvas. */
internal const val CANVAS_WIDTH: Float = 200f

/** Height of a tulip or notes canvas. */
internal const val CANVAS_HEIGHT: Float = 135f

/**
 * Where the roads of a tulip meet, from the top-left corner of the canvas. Road points are
 * relative to it.
 */
internal val JUNCTION: Offset = Offset(100f, 85f)

/**
 * How far the control points of a curved road reach towards the neighbors of each point, as a
 * fraction of the distance between them. 0 would draw straight segments.
 */
internal const val SMOOTHNESS: Float = 0.2f

/** Length of each side of the arrow head at the end of the track. */
internal const val ARROW_SIDE: Float = 20f

/** Half the angle at the tip of the arrow head: 30°. */
internal val ARROW_HALF_ANGLE: Float = Math.toRadians(30.0).toFloat()

/** Length of the bar across the end of a road that is not the track. */
internal const val END_BAR_LENGTH: Float = 20f

/** One piece of the outline of a road, from where the previous one ended. */
internal sealed interface RoadSegment {

    /** The end of this segment. */
    val end: Offset

    /** A straight line to [end]. */
    data class Line(override val end: Offset) : RoadSegment

    /** A quadratic curve to [end], bent towards [control]. */
    data class Quadratic(val control: Offset, override val end: Offset) : RoadSegment

    /** A cubic curve to [end], bent towards [control1] and then [control2]. */
    data class Cubic(
        val control1: Offset,
        val control2: Offset,
        override val end: Offset,
    ) : RoadSegment
}

/**
 * The points a road goes through on the canvas, from the top-left corner: its start (the
 * junction when it has none), its handles and its end.
 *
 * @return the points, or `null` when the road has no end, so it is not drawn.
 */
internal fun Road.canvasPoints(): List<Offset>? {
    val roadEnd = end ?: return null
    val roadStart = start?.fromJunction() ?: JUNCTION
    return listOf(roadStart) + handles.map { it.fromJunction() } + roadEnd.fromJunction()
}

/** This road point, which is relative to the junction, from the top-left corner of the canvas. */
private fun CanvasPoint.fromJunction(): Offset = JUNCTION + Offset(x.toFloat(), y.toFloat())

/** This icon or text center, which is already measured from the top-left corner, as an offset. */
internal fun CanvasPoint.toOffset(): Offset = Offset(x.toFloat(), y.toFloat())

/**
 * The segments of a smooth line through [points], after starting at the first one.
 *
 * Two points give a straight line. With more, each point is joined to the next by a curve whose
 * control points follow the direction from the point before to the point after, scaled by
 * [SMOOTHNESS]: so the line passes through every point without corners. The first and last
 * segments have no neighbor on one side, so they are quadratic curves.
 *
 * @param points the points, at least two.
 * @return one segment per point after the first.
 */
internal fun smoothSegments(points: List<Offset>): List<RoadSegment> {
    require(points.size >= 2) { "A road needs at least two points, but had ${points.size}" }
    if (points.size == 2) return listOf(RoadSegment.Line(points[1]))

    val last = points.size - 2
    return (0..last).map { i ->
        val current = points[i]
        val next = points[i + 1]
        when (i) {
            0 -> RoadSegment.Quadratic(next - (points[2] - current) * SMOOTHNESS, next)
            last -> RoadSegment.Quadratic(current + (next - points[i - 1]) * SMOOTHNESS, next)
            else -> RoadSegment.Cubic(
                control1 = current + (next - points[i - 1]) * SMOOTHNESS,
                control2 = next - (points[i + 2] - current) * SMOOTHNESS,
                end = next,
            )
        }
    }
}

/**
 * The direction a road has where it ends, in radians from the x axis (clockwise, as y grows
 * downwards), to draw its end mark aligned with it.
 *
 * With more than two points it is the direction from the control point of the last curve, as
 * drawn by [smoothSegments], to the end.
 *
 * @param points the points of the road, at least two.
 * @return the angle.
 */
internal fun endAngle(points: List<Offset>): Float {
    require(points.size >= 2) { "A road needs at least two points, but had ${points.size}" }
    val end = points.last()
    val from = if (points.size > 2) {
        points[points.size - 2] + (end - points[points.size - 3]) * SMOOTHNESS
    } else {
        points.first()
    }
    return atan2(end.y - from.y, end.x - from.x)
}

/**
 * The three corners of the arrow head at the end of the track: its tip, beyond [end] in the
 * direction [angle], and the ends of its two sides.
 *
 * @param end where the road ends: the middle of the base of the arrow head.
 * @param angle the direction of the road at its end, as [endAngle] gives it.
 * @return the tip and the two other corners.
 */
internal fun arrowHead(end: Offset, angle: Float): List<Offset> {
    val height = ARROW_SIDE * cos(ARROW_HALF_ANGLE)
    val tip = end + Offset(height * cos(angle), height * sin(angle))
    return listOf(
        tip,
        tip - Offset(
            ARROW_SIDE * cos(angle - ARROW_HALF_ANGLE),
            ARROW_SIDE * sin(angle - ARROW_HALF_ANGLE),
        ),
        tip - Offset(
            ARROW_SIDE * cos(angle + ARROW_HALF_ANGLE),
            ARROW_SIDE * sin(angle + ARROW_HALF_ANGLE),
        ),
    )
}

/**
 * The two ends of the bar drawn across the end of a road that is not the track, perpendicular to
 * it and centered on [end].
 *
 * @param end where the road ends.
 * @param angle the direction of the road at its end, as [endAngle] gives it.
 * @return both ends of the bar.
 */
internal fun endBar(end: Offset, angle: Float): Pair<Offset, Offset> {
    val half = Offset(-sin(angle), cos(angle)) * (END_BAR_LENGTH / 2)
    return Pair(end + half, end - half)
}
