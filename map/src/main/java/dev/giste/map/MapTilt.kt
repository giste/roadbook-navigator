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
 * How much the map is tilted towards the horizon, in degrees: `0` looks straight down, and larger
 * values show more of the road ahead, in perspective.
 *
 * It is always within [RANGE], from [MIN] to [MAX], the tilts the map can draw. Create it with
 * [of] for a value that must be valid (e.g. from a settings slider bounded by [RANGE]), or with
 * [coerce] for a value that may be out of range or not finite (e.g. one read from storage).
 *
 * Tilts are ordered, so they can be compared with `<`, `>`, etc.
 */
@JvmInline
public value class MapTilt private constructor(
    /** The tilt in degrees. Always within [RANGE]. */
    public val inDegrees: Double,
) : Comparable<MapTilt> {

    /**
     * Compares this tilt with [other].
     *
     * @param other the tilt to compare with.
     * @return a negative number, zero or a positive number when this tilt is smaller than, equal
     * to, or larger than [other].
     */
    override fun compareTo(other: MapTilt): Int = inDegrees.compareTo(other.inDegrees)

    /** Returns the tilt with its unit, e.g. `60.0°`. Meant for logs, not for display. */
    override fun toString(): String = "$inDegrees°"

    /** Factories, limits and default of [MapTilt]. */
    public companion object {

        /** The tilts allowed, in degrees: from 0 to 65, both included. */
        public val RANGE: ClosedFloatingPointRange<Double> = 0.0..65.0

        /** No tilt: the map is seen from straight above. */
        public val MIN: MapTilt = MapTilt(RANGE.start)

        /** The largest tilt: 65°. */
        public val MAX: MapTilt = MapTilt(RANGE.endInclusive)

        /** The default tilt: 60°, which shows the road ahead in perspective. */
        public val DEFAULT: MapTilt = MapTilt(60.0)

        /**
         * Creates a tilt of [degrees].
         *
         * @param degrees the tilt in degrees. Must be within [RANGE].
         * @return the tilt.
         * @throws IllegalArgumentException if [degrees] is outside [RANGE] or `NaN`. This is a
         * programming error: values that may be out of range must go through [coerce].
         */
        public fun of(degrees: Double): MapTilt {
            // NaN fails the range check, because every comparison with NaN is false.
            require(degrees in RANGE) { "A map tilt must be within $RANGE, but was $degrees°" }
            return MapTilt(degrees)
        }

        /**
         * Creates a tilt of [degrees], moved into [RANGE] if it is outside it.
         *
         * Use it for values that may be out of range, e.g. one persisted by an older version of
         * the app, so they never crash.
         *
         * @param degrees the wanted tilt in degrees.
         * @return a tilt of [degrees] when it is within [RANGE], [MIN] when it is smaller, [MAX]
         * when it is larger, or [DEFAULT] when it is `NaN`.
         */
        public fun coerce(degrees: Double): MapTilt =
            if (degrees.isNaN()) DEFAULT else MapTilt(degrees.coerceIn(RANGE))
    }
}
