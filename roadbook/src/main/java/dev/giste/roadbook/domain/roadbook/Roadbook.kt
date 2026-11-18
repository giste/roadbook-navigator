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

/**
 * A roadbook: the route to navigate, as the list of its waypoints in order. It is the aggregate
 * root that owns its [Waypoint]s.
 *
 * It is immutable. Its invariants are checked on creation, so every instance is a roadbook that
 * can be navigated:
 *
 * - It has at least one waypoint. A file without waypoints is not a roadbook, so importing it
 *   fails instead of producing an empty one.
 * - The waypoints are numbered 1, 2, 3, … in order, with no gaps.
 * - The first waypoint has no distance from a previous one.
 *
 * The distances themselves are not checked against each other: they come from the route, where
 * waypoints that are not shown still add distance.
 *
 * @property name the name of the roadbook, as given in the file. May be empty.
 * @property description a description of the roadbook, as given in the file. May be empty.
 * @property startLocation where the route starts, as given in the file. May be empty.
 * @property endLocation where the route ends, as given in the file. May be empty.
 * @property waypoints the waypoints, in route order. Pass a list that is not modified afterwards:
 * the roadbook does not copy it.
 * @throws IllegalArgumentException if an invariant is broken. This is a programming error: the
 * code that builds a roadbook (e.g. the `.rn2` parser) must produce a valid one, or report a
 * failure instead.
 */
internal data class Roadbook(
    val name: String = "",
    val description: String = "",
    val startLocation: String = "",
    val endLocation: String = "",
    val waypoints: List<Waypoint>,
) {
    init {
        require(waypoints.isNotEmpty()) { "A roadbook must have at least one waypoint" }
        waypoints.forEachIndexed { index, waypoint ->
            require(waypoint.number.value == index + 1) {
                "Waypoints must be numbered 1, 2, 3, … in order, but waypoint ${index + 1} " +
                    "is numbered ${waypoint.number}"
            }
        }
        require(waypoints.first().distanceFromPrevious == Distance.ZERO) {
            "The first waypoint has no previous one, but its distance from the previous one is " +
                "${waypoints.first().distanceFromPrevious}"
        }
    }

    /** The number of the last waypoint, which is also how many waypoints there are. */
    val lastWaypointNumber: WaypointNumber
        get() = waypoints.last().number

    /**
     * Keeps [number] within the waypoints of this roadbook.
     *
     * A position may come from storage or from a previous roadbook, so it may be past the last
     * waypoint (e.g. after importing a shorter roadbook). It can never be before the first one,
     * because waypoint numbers start at 1.
     *
     * @param number a waypoint number, possibly past the last waypoint.
     * @return [number], or [lastWaypointNumber] when [number] is past it.
     */
    fun coerceWaypointNumber(number: WaypointNumber): WaypointNumber =
        minOf(number, lastWaypointNumber)

    /**
     * The waypoint after [number], or the last one when there is none.
     *
     * @param number the current waypoint. It is first kept within the roadbook with
     * [coerceWaypointNumber].
     * @return the number of the next waypoint, or [lastWaypointNumber] at the end.
     */
    fun waypointNumberAfter(number: WaypointNumber): WaypointNumber =
        coerceWaypointNumber(WaypointNumber(coerceWaypointNumber(number).value + 1))

    /**
     * The waypoint before [number], or the first one when there is none.
     *
     * @param number the current waypoint. It is first kept within the roadbook with
     * [coerceWaypointNumber].
     * @return the number of the previous waypoint, or [WaypointNumber.FIRST] at the start.
     */
    fun waypointNumberBefore(number: WaypointNumber): WaypointNumber {
        val current = coerceWaypointNumber(number)
        return if (current == WaypointNumber.FIRST) current else WaypointNumber(current.value - 1)
    }
}
