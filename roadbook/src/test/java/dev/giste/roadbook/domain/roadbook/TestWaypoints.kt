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

import dev.giste.geo.Distance
import dev.giste.geo.GeoPoint

/**
 * A waypoint numbered [number], with the given distances and everything else at a neutral value.
 *
 * The first waypoint has no distance from a previous one unless the test says otherwise, so a
 * list of `testWaypoint(1)`, `testWaypoint(2)`, … is a valid roadbook.
 */
internal fun testWaypoint(
    number: Int,
    partial: Distance = Distance.meters(100.0 * (number - 1)),
    distanceFromPrevious: Distance = if (number == 1) Distance.ZERO else Distance.meters(100.0),
    resetsPartial: Boolean = false,
): Waypoint = Waypoint(
    number = WaypointNumber(number),
    position = GeoPoint(latitude = 40.0, longitude = -3.0),
    altitude = null,
    partial = partial,
    distanceFromPrevious = distanceFromPrevious,
    resetsPartial = resetsPartial,
)
