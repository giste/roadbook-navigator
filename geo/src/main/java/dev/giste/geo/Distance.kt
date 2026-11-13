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

import kotlin.time.Duration
import kotlin.time.DurationUnit

/**
 * A non-negative, finite length, stored in meters.
 *
 * Create it with [Distance.meters] or [Distance.kilometers]. Use it instead of a raw `Double`
 * for every distance in the domain, and convert to other units only for display.
 *
 * It owns the length unit conversions of `:geo`: other types, such as [Speed], convert lengths
 * through it instead of keeping their own factors.
 *
 * Distances are ordered by length, so they can be compared with `<`, `>`, `minOf`, etc.
 */
@JvmInline
public value class Distance private constructor(
    /** The length in meters. Never negative, `NaN` or infinite. */
    public val inMeters: Double,
) : Comparable<Distance> {

    /** The length in kilometers. */
    public val inKilometers: Double
        get() = inMeters / METERS_PER_KILOMETER

    /**
     * Returns the sum of this distance and [other].
     *
     * @param other the distance to add.
     * @return a distance of `this.inMeters + other.inMeters` meters.
     * @throws IllegalArgumentException if the sum overflows to infinity. This is a programming
     * error: no real distance is that long.
     */
    public operator fun plus(other: Distance): Distance = meters(inMeters + other.inMeters)

    /**
     * Returns this distance minus [other].
     *
     * @param other the distance to subtract. Must not be longer than this distance.
     * @return a distance of `this.inMeters - other.inMeters` meters.
     * @throws IllegalArgumentException if [other] is longer than this distance, because a
     * distance cannot be negative. Callers that may subtract a longer distance must compare first.
     */
    public operator fun minus(other: Distance): Distance {
        require(other <= this) { "Cannot subtract $other from $this: the result would be negative" }
        return meters(inMeters - other.inMeters)
    }

    /**
     * Returns the average speed needed to cover this distance in [time].
     *
     * @param time how long it takes to cover this distance. Must be finite and positive.
     * @return a speed of `inMeters` divided by [time] in seconds.
     * @throws IllegalArgumentException if [time] is zero, negative or infinite, or if the
     * result overflows (a huge distance in a tiny time).
     */
    public operator fun div(time: Duration): Speed {
        require(time.isFinite() && time.isPositive()) {
            "A distance can only be divided by a finite, positive duration, but was $time"
        }
        return Speed.metersPerSecond(inMeters / time.toDouble(DurationUnit.SECONDS))
    }

    /**
     * Compares the lengths of this distance and [other].
     *
     * @param other the distance to compare with.
     * @return a negative number, zero or a positive number when this distance is shorter than,
     * as long as, or longer than [other].
     */
    override fun compareTo(other: Distance): Int = inMeters.compareTo(other.inMeters)

    /** Returns the length in meters with its unit, e.g. `12.5 m`. Meant for logs, not for display. */
    override fun toString(): String = "$inMeters m"

    /** Factories and constants for [Distance]. */
    public companion object {

        private const val METERS_PER_KILOMETER: Double = 1_000.0

        /** A distance of zero meters. */
        public val ZERO: Distance = Distance(0.0)

        /**
         * Creates a distance from a length in meters.
         *
         * @param value the length in meters. `-0.0` is treated as `0.0`.
         * @return the distance.
         * @throws IllegalArgumentException if [value] is negative, `NaN` or infinite.
         */
        public fun meters(value: Double): Distance {
            require(value.isFinite() && value >= 0.0) {
                "A distance must be finite and non-negative, but was $value m"
            }
            // Adding 0.0 turns -0.0 into 0.0, so both create equal distances.
            return Distance(value + 0.0)
        }

        /**
         * Creates a distance from a length in kilometers.
         *
         * @param value the length in kilometers. `-0.0` is treated as `0.0`.
         * @return the distance.
         * @throws IllegalArgumentException if [value] is negative, `NaN` or infinite, or too
         * large to be expressed in meters.
         */
        public fun kilometers(value: Double): Distance = meters(value * METERS_PER_KILOMETER)
    }
}
