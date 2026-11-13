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
import kotlin.time.Duration.Companion.hours
import kotlin.time.DurationUnit

/**
 * A non-negative, finite ground speed, stored in meters per second.
 *
 * Create it with [Speed.metersPerSecond], [Speed.kilometersPerHour] or by dividing a [Distance]
 * by a [Duration] (`Distance.kilometers(90.0) / 1.hours`). It has no direction: pair it with a
 * [Bearing] when the direction matters.
 *
 * Speed is distance over time, so it holds no unit factors of its own: length conversions come
 * from [Distance] and time conversions from [Duration].
 *
 * Speeds are ordered by magnitude, so they can be compared with `<`, `>`, `maxOf`, etc.
 */
@JvmInline
public value class Speed private constructor(
    /** The speed in meters per second. Never negative, `NaN` or infinite. */
    public val inMetersPerSecond: Double,
) : Comparable<Speed> {

    /**
     * The speed in kilometers per hour: the distance covered in one hour, in kilometers.
     *
     * @throws IllegalArgumentException if one hour at this speed overflows a [Distance] (above
     * about 5e304 m/s). No real speed is that high.
     */
    public val inKilometersPerHour: Double
        get() = (this * 1.hours).inKilometers

    /**
     * Returns the distance covered at this speed during [time].
     *
     * @param time how long the speed is held. Must be finite and not negative.
     * @return a distance of `inMetersPerSecond` times [time] in seconds.
     * @throws IllegalArgumentException if [time] is negative or infinite, or if the result
     * overflows a [Distance].
     */
    public operator fun times(time: Duration): Distance {
        require(time.isFinite() && !time.isNegative()) {
            "A speed can only be multiplied by a finite, non-negative duration, but was $time"
        }
        return Distance.meters(inMetersPerSecond * time.toDouble(DurationUnit.SECONDS))
    }

    /**
     * Compares the magnitudes of this speed and [other].
     *
     * @param other the speed to compare with.
     * @return a negative number, zero or a positive number when this speed is slower than, as
     * fast as, or faster than [other].
     */
    override fun compareTo(other: Speed): Int = inMetersPerSecond.compareTo(other.inMetersPerSecond)

    /** Returns the speed in meters per second with its unit, e.g. `8.5 m/s`. Meant for logs, not for display. */
    override fun toString(): String = "$inMetersPerSecond m/s"

    /** Factories and constants for [Speed]. */
    public companion object {

        /** A speed of zero: standing still. */
        public val ZERO: Speed = Speed(0.0)

        /**
         * Creates a speed from a value in meters per second.
         *
         * @param value the speed in meters per second. `-0.0` is treated as `0.0`.
         * @return the speed.
         * @throws IllegalArgumentException if [value] is negative, `NaN` or infinite.
         */
        public fun metersPerSecond(value: Double): Speed {
            require(value.isFinite() && value >= 0.0) {
                "A speed must be finite and non-negative, but was $value m/s"
            }
            // Adding 0.0 turns -0.0 into 0.0, so both create equal speeds.
            return Speed(value + 0.0)
        }

        /**
         * Creates a speed from a value in kilometers per hour: that many kilometers in one hour.
         *
         * @param value the speed in kilometers per hour. `-0.0` is treated as `0.0`.
         * @return the speed.
         * @throws IllegalArgumentException if [value] is negative, `NaN` or infinite, or too large
         * to be expressed as a [Distance] in meters.
         */
        public fun kilometersPerHour(value: Double): Speed {
            // Checked here too, so the message names the speed instead of an internal distance.
            require(value.isFinite() && value >= 0.0) {
                "A speed must be finite and non-negative, but was $value km/h"
            }
            return Distance.kilometers(value) / 1.hours
        }
    }
}
