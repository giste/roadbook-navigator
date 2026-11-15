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

package dev.giste.roadbook.domain.drawing

import dev.giste.geo.GeoPoint
import kotlin.math.cos
import kotlin.math.sin

/**
 * Margin between the tulip's edge and the end of an exit road, in canvas units. The tulip canvas
 * spans 200 by 135 units, from 100 left to 100 right of the center, and from 85 above to 50
 * below it.
 */
private const val EXIT_MARGIN: Double = 25.0

/** Left limit of an exit road: 100 left of the center, minus the margin. */
private const val EXIT_LEFT: Double = -100.0 + EXIT_MARGIN

/** Right limit of an exit road: 100 right of the center, minus the margin. */
private const val EXIT_RIGHT: Double = 100.0 - EXIT_MARGIN

/** Top limit of an exit road: 85 above the center, minus the margin. */
private const val EXIT_TOP: Double = -85.0 + EXIT_MARGIN

/** Bottom limit of an exit road: 50 below the center, minus the margin. */
private const val EXIT_BOTTOM: Double = 50.0 - EXIT_MARGIN

/**
 * Where the road out of a tulip ends, when the roadbook does not draw it: the direction from
 * [current] to [next], relative to the direction of arrival, projected onto the edge of the
 * tulip.
 *
 * A tulip is drawn as the rider sees it: they arrive from the bottom, so straight on is up, a
 * right turn points right and a U-turn points down. The direction of arrival is the bearing from
 * [previous] to [current], and the direction out is the bearing from [current] to [next]. The
 * difference between both is the turn, which is drawn as a ray from the center of the canvas
 * until it reaches a rectangle [EXIT_MARGIN] inside the tulip's edges. For example, going
 * straight on ends at (0, -60), and a right angle to the right at (75, 0).
 *
 * @param previous the point the rider comes from, or `null` at the start of the route. Without
 * it, the rider is taken to arrive heading to [next], so the road goes straight on.
 * @param current the point the tulip is drawn for.
 * @param next the point the rider heads to.
 * @return the end of the road out, relative to the center of the canvas.
 */
internal fun tulipExitPoint(previous: GeoPoint?, current: GeoPoint, next: GeoPoint): CanvasPoint {
    // A point at the same position as its neighbor gives no direction. Going straight on is the
    // only drawing that does not invent a turn.
    val bearingOut = current.bearingTo(next) ?: return straightOn()
    val bearingIn = previous?.bearingTo(current) ?: bearingOut

    val turn = Math.toRadians(bearingOut.inDegrees - bearingIn.inDegrees)
    // On the canvas, angle 0 points right and angles grow clockwise, because y grows downwards.
    // Straight on (turn 0) points up, which is -90°.
    val angle = turn - Math.PI / 2
    return rayToExitLimits(cos(angle), sin(angle))
}

/** The end of a road that goes straight on: the top limit, above the center. */
private fun straightOn(): CanvasPoint = rayToExitLimits(dx = 0.0, dy = -1.0)

/**
 * Follows the ray from the center of the canvas in the direction ([dx], [dy]) until it reaches
 * the first exit limit, and returns that point.
 *
 * For each axis the ray moves along, the ray reaches the limit on that side at distance
 * `limit / d`. The nearest of those is where the ray leaves the rectangle.
 */
private fun rayToExitLimits(dx: Double, dy: Double): CanvasPoint {
    var distance = Double.MAX_VALUE
    if (dx > 0) distance = minOf(distance, EXIT_RIGHT / dx)
    if (dx < 0) distance = minOf(distance, EXIT_LEFT / dx)
    if (dy > 0) distance = minOf(distance, EXIT_BOTTOM / dy)
    if (dy < 0) distance = minOf(distance, EXIT_TOP / dy)
    return CanvasPoint(dx * distance, dy * distance)
}
