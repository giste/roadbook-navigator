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

package dev.giste.map

/**
 * How close the map is shown, as a web map zoom level: at level `n` the whole world is `2ⁿ` tiles
 * wide, so each level up shows the ground twice as large.
 *
 * It is always within [RANGE], from [MIN] (a whole country) to [MAX] (single streets and
 * buildings), the levels the map can draw. Create it with [of] for a value that must be valid
 * (e.g. from a settings slider bounded by [RANGE]), or with [coerce] for a value that may be out
 * of range (e.g. one read from storage).
 *
 * Zoom levels are ordered, so they can be compared with `<`, `>`, etc.
 */
@JvmInline
public value class MapZoomLevel private constructor(
    /** The zoom level. Always within [RANGE]. */
    public val value: Int,
) : Comparable<MapZoomLevel> {

    /**
     * Compares this zoom level with [other].
     *
     * @param other the zoom level to compare with.
     * @return a negative number, zero or a positive number when this level shows the map farther
     * than, as close as, or closer than [other].
     */
    override fun compareTo(other: MapZoomLevel): Int = value.compareTo(other.value)

    /** Returns the zoom level, e.g. `z18`. Meant for logs, not for display. */
    override fun toString(): String = "z$value"

    /** Factories, limits and default of [MapZoomLevel]. */
    public companion object {

        /** The levels a zoom level may have, from 2 to 20, both included. */
        public val RANGE: IntRange = 2..20

        /** The farthest zoom level: 2. */
        public val MIN: MapZoomLevel = MapZoomLevel(RANGE.first)

        /** The closest zoom level: 20. */
        public val MAX: MapZoomLevel = MapZoomLevel(RANGE.last)

        /** The default zoom level: 18, close enough to tell the tracks of a junction apart. */
        public val DEFAULT: MapZoomLevel = MapZoomLevel(18)

        /**
         * Creates the zoom level [value].
         *
         * @param value the zoom level. Must be within [RANGE].
         * @return the zoom level.
         * @throws IllegalArgumentException if [value] is outside [RANGE]. This is a programming
         * error: values that may be out of range must go through [coerce].
         */
        public fun of(value: Int): MapZoomLevel {
            require(value in RANGE) { "A map zoom level must be within $RANGE, but was $value" }
            return MapZoomLevel(value)
        }

        /**
         * Creates the zoom level [value], moved into [RANGE] if it is outside it.
         *
         * Use it for values that may be out of range, e.g. one persisted by an older version of
         * the app, so they never crash.
         *
         * @param value the wanted zoom level.
         * @return the zoom level [value] when it is within [RANGE], [MIN] when it is lower, or
         * [MAX] when it is higher.
         */
        public fun coerce(value: Int): MapZoomLevel = MapZoomLevel(value.coerceIn(RANGE))
    }
}
