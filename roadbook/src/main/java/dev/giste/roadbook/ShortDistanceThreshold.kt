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

package dev.giste.roadbook

import dev.giste.geo.Distance

/**
 * The distance between two waypoints below which the roadbook highlights the second one, to warn
 * the user that it comes soon after the previous one.
 *
 * It is always within [RANGE], from [MIN] to [MAX], a reasonable range for a roadbook. Create it
 * with [of] for a value that must be valid (e.g. from a settings slider bounded by [RANGE]), or
 * with [coerce] for a value that may be out of range (e.g. one read from storage).
 *
 * Thresholds are ordered by their distance, so they can be compared with `<`, `>`, etc.
 */
@JvmInline
public value class ShortDistanceThreshold private constructor(
    /** The threshold distance. Always within [RANGE]. */
    public val distance: Distance,
) : Comparable<ShortDistanceThreshold> {

    /**
     * Compares the distances of this threshold and [other].
     *
     * @param other the threshold to compare with.
     * @return a negative number, zero or a positive number when this threshold is shorter than,
     * as long as, or longer than [other].
     */
    override fun compareTo(other: ShortDistanceThreshold): Int = distance.compareTo(other.distance)

    /** Returns the threshold distance with its unit, e.g. `250.0 m`. Meant for logs, not for display. */
    override fun toString(): String = distance.toString()

    /** Factories, limits and default of [ShortDistanceThreshold]. */
    public companion object {

        /** The shortest threshold allowed: 50 m. */
        public val MIN_DISTANCE: Distance = Distance.meters(50.0)

        /** The longest threshold allowed: 1 km. */
        public val MAX_DISTANCE: Distance = Distance.meters(1_000.0)

        /** The distances a threshold may have, from [MIN_DISTANCE] to [MAX_DISTANCE], both included. */
        public val RANGE: ClosedRange<Distance> = MIN_DISTANCE..MAX_DISTANCE

        /** The shortest threshold allowed: [MIN_DISTANCE]. */
        public val MIN: ShortDistanceThreshold = ShortDistanceThreshold(MIN_DISTANCE)

        /** The longest threshold allowed: [MAX_DISTANCE]. */
        public val MAX: ShortDistanceThreshold = ShortDistanceThreshold(MAX_DISTANCE)

        /** The default threshold: 250 m. */
        public val DEFAULT: ShortDistanceThreshold = ShortDistanceThreshold(Distance.meters(250.0))

        /**
         * Creates a threshold of [distance].
         *
         * @param distance the threshold distance. Must be within [RANGE].
         * @return the threshold.
         * @throws IllegalArgumentException if [distance] is outside [RANGE]. This is a programming
         * error: values that may be out of range must go through [coerce].
         */
        public fun of(distance: Distance): ShortDistanceThreshold {
            require(distance in RANGE) {
                "A short distance threshold must be within $RANGE, but was $distance"
            }
            return ShortDistanceThreshold(distance)
        }

        /**
         * Creates a threshold of [distance], moved into [RANGE] if it is outside it.
         *
         * Use it for values that may be out of range, e.g. one persisted by an older version of
         * the app, so they never crash.
         *
         * @param distance the wanted threshold distance.
         * @return a threshold of [distance] when it is within [RANGE], [MIN] when it is shorter,
         * or [MAX] when it is longer.
         */
        public fun coerce(distance: Distance): ShortDistanceThreshold =
            ShortDistanceThreshold(distance.coerceIn(RANGE))
    }
}
