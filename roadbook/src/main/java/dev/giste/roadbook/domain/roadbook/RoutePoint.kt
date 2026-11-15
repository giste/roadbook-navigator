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

package dev.giste.roadbook.domain.roadbook

import dev.giste.geo.Altitude
import dev.giste.geo.Distance
import dev.giste.geo.GeoPoint
import dev.giste.roadbook.domain.drawing.Element

/**
 * One point of a route as a roadbook file describes it, before the distances are measured.
 *
 * A route has more points than the roadbook shows: points that are not [shown] shape the route
 * (and add to its distances) without becoming waypoints. [toWaypoints] turns a route into the
 * waypoints of a roadbook.
 *
 * @property position where the point is.
 * @property altitude the altitude of the point, or `null` when the file does not give it.
 * @property shown whether the point is a waypoint of the roadbook.
 * @property resetsPartial whether the partial starts again from zero at this point. It applies
 * even when the point is not [shown].
 * @property dangerLevel how dangerous the point is. Only used when it is [shown].
 * @property tulip what is drawn in its tulip column. Only used when it is [shown].
 * @property notes what is drawn in its notes column. Only used when it is [shown].
 */
internal data class RoutePoint(
    val position: GeoPoint,
    val altitude: Altitude?,
    val shown: Boolean,
    val resetsPartial: Boolean = false,
    val dangerLevel: DangerLevel = DangerLevel.NONE,
    val tulip: List<Element> = emptyList(),
    val notes: List<Element> = emptyList(),
)

/**
 * Measures this route and returns the points that are [shown][RoutePoint.shown] as waypoints, in
 * order and numbered from 1.
 *
 * Walking the route from its first point, the distance between consecutive points is the
 * geodesic distance between their positions ([GeoPoint.distanceTo]). Every point, shown or not,
 * adds to the distances:
 *
 * - The **partial** at a point is the partial at the previous point plus the distance between
 *   them, or only that distance when the previous point [resets the partial]
 *   [RoutePoint.resetsPartial]. The partial at the first point is zero.
 * - The **distance from the previous waypoint** adds up the distances since the last point that
 *   is shown, so points that are not shown in between are included. The first waypoint has none.
 *
 * Distances are kept unrounded: rounding is a matter of display.
 *
 * @return the waypoints, or an empty list when no point is shown (or the route is empty).
 */
internal fun List<RoutePoint>.toWaypoints(): List<Waypoint> {
    val waypoints = mutableListOf<Waypoint>()
    var partial = Distance.ZERO
    var distanceSinceShown = Distance.ZERO

    forEachIndexed { index, point ->
        if (index > 0) {
            val previous = this[index - 1]
            val step = previous.position.distanceTo(point.position)
            partial = if (previous.resetsPartial) step else partial + step
            distanceSinceShown = if (previous.shown) step else distanceSinceShown + step
        }
        if (point.shown) {
            waypoints += Waypoint(
                number = WaypointNumber(waypoints.size + 1),
                position = point.position,
                altitude = point.altitude,
                partial = partial,
                distanceFromPrevious = if (waypoints.isEmpty()) {
                    Distance.ZERO
                } else {
                    distanceSinceShown
                },
                resetsPartial = point.resetsPartial,
                dangerLevel = point.dangerLevel,
                tulip = point.tulip,
                notes = point.notes,
            )
        }
    }
    return waypoints
}
