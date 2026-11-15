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

package dev.giste.geo

import kotlin.math.abs

/**
 * A height above a reference surface, stored in meters.
 *
 * Unlike [Distance], an altitude can be negative: places below the reference surface, such as
 * the shore of the Dead Sea, have negative altitudes. The reference surface (e.g. the WGS84
 * ellipsoid, or mean sea level) is defined by whoever creates the value, and two altitudes are
 * only comparable when they share it.
 *
 * Create it with [meters].
 *
 * @property inMeters the altitude in meters. Negative below the reference surface.
 */
@JvmInline
public value class Altitude private constructor(
    public val inMeters: Double,
) : Comparable<Altitude> {

    /**
     * The vertical distance between this altitude and [other], whichever is higher.
     *
     * It is a [Distance], so it is never negative. It is the same in both directions.
     *
     * @param other the other altitude, on the same reference surface.
     * @return how far apart both altitudes are vertically.
     * @throws IllegalArgumentException if the difference overflows to infinity, which only
     * happens for values far beyond any real altitude.
     */
    public fun verticalDistanceTo(other: Altitude): Distance =
        Distance.meters(abs(other.inMeters - inMeters))

    override fun compareTo(other: Altitude): Int = inMeters.compareTo(other.inMeters)

    override fun toString(): String = "$inMeters m"

    /** Factories and well-known values of [Altitude]. */
    public companion object {

        /** The altitude of the reference surface itself. */
        public val ZERO: Altitude = Altitude(0.0)

        /**
         * Creates an altitude of [value] meters.
         *
         * @param value the altitude in meters. Negative below the reference surface.
         * @return the altitude.
         * @throws IllegalArgumentException if [value] is `NaN` or infinite.
         */
        public fun meters(value: Double): Altitude {
            require(value.isFinite()) { "An altitude must be finite, but was $value m" }
            // Adding 0.0 turns -0.0 into 0.0, so both create equal altitudes.
            return Altitude(value + 0.0)
        }
    }
}
