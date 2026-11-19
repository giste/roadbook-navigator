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

/**
 * The number printed on a waypoint: its position in the roadbook, counting only the waypoints
 * that are shown, from 1.
 *
 * It identifies the waypoint within its [Roadbook], which keeps the numbers consecutive.
 * Numbers are ordered, so they can be compared with `<`, `>`, etc.
 *
 * @property value the number. At least 1.
 * @throws IllegalArgumentException if [value] is less than 1.
 */
@JvmInline
internal value class WaypointNumber(val value: Int) : Comparable<WaypointNumber> {

    init {
        require(value >= 1) { "A waypoint number must be at least 1, but was $value" }
    }

    /**
     * Compares the positions of this number and [other] in the roadbook.
     *
     * @param other the number to compare with.
     * @return a negative number, zero or a positive number when this waypoint comes before, is the
     * same as, or comes after [other].
     */
    override fun compareTo(other: WaypointNumber): Int = value.compareTo(other.value)

    /** Returns the number alone, e.g. `12`. Meant for logs, not for display. */
    override fun toString(): String = value.toString()

    /** Constants for [WaypointNumber]. */
    companion object {

        /** The number of the first waypoint: 1. */
        val FIRST: WaypointNumber = WaypointNumber(1)
    }
}
