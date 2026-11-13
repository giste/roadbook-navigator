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

/**
 * A direction on the ground, measured clockwise from true north, in degrees in `[0, 360)`.
 *
 * `0` is north, `90` east, `180` south and `270` west. Create it with [Bearing.degrees], which
 * accepts any finite angle and wraps it into range, so `360` and `-360` both become `0`, and
 * `-90` becomes `270`.
 *
 * Bearings are not [Comparable]: directions are circular, so `350` is not "more" than `10`.
 */
@JvmInline
public value class Bearing private constructor(
    /** The direction in degrees, clockwise from true north. Always in `[0, 360)`. */
    public val inDegrees: Double,
) {

    /** Returns the direction in degrees with its unit, e.g. `90.0°`. Meant for logs, not for display. */
    override fun toString(): String = "$inDegrees°"

    /** Factories and constants for [Bearing]. */
    public companion object {

        private const val FULL_TURN_DEGREES: Double = 360.0

        /** North: `0` degrees. */
        public val NORTH: Bearing = Bearing(0.0)

        /**
         * Creates a bearing from an angle in degrees, clockwise from true north.
         *
         * @param value any finite angle in degrees. It is wrapped into `[0, 360)`.
         * @return the bearing.
         * @throws IllegalArgumentException if [value] is `NaN` or infinite.
         */
        public fun degrees(value: Double): Bearing {
            require(value.isFinite()) { "A bearing must be finite, but was $value°" }
            return Bearing(normalize(value))
        }

        /**
         * Wraps a finite angle into `[0, 360)`.
         *
         * `%` keeps the sign of [value], so negative remainders get a full turn added. A tiny
         * negative remainder (e.g. `-1e-15`) plus 360 rounds to exactly `360.0`, which is out of
         * range, so that case becomes `0.0`. Finally, adding `0.0` turns `-0.0` into `0.0`, so
         * north always has the same representation and bearings compare equal.
         */
        private fun normalize(value: Double): Double {
            val remainder = value % FULL_TURN_DEGREES
            val wrapped = if (remainder < 0.0) remainder + FULL_TURN_DEGREES else remainder
            return if (wrapped >= FULL_TURN_DEGREES) 0.0 else wrapped + 0.0
        }
    }
}
