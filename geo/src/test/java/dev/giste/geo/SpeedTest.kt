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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedTest {

    @Test
    fun metersPerSecond_validValue_keepsMetersPerSecond() {
        val speed = Speed.metersPerSecond(8.5)

        assertEquals(8.5, speed.inMetersPerSecond, 0.0)
    }

    @Test
    fun metersPerSecond_zero_equalsZero() {
        assertEquals(Speed.ZERO, Speed.metersPerSecond(0.0))
    }

    @Test
    fun metersPerSecond_negativeZero_equalsZero() {
        assertEquals(Speed.ZERO, Speed.metersPerSecond(-0.0))
    }

    @Test
    fun metersPerSecond_negative_throws() {
        assertThrows(IllegalArgumentException::class.java) { Speed.metersPerSecond(-1.0) }
    }

    @Test
    fun metersPerSecond_nan_throws() {
        assertThrows(IllegalArgumentException::class.java) { Speed.metersPerSecond(Double.NaN) }
    }

    @Test
    fun metersPerSecond_positiveInfinity_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            Speed.metersPerSecond(Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun kilometersPerHour_validValue_convertsToMetersPerSecondExactly() {
        val speed = Speed.kilometersPerHour(90.0)

        assertEquals(25.0, speed.inMetersPerSecond, 0.0)
    }

    @Test
    fun kilometersPerHour_negativeZero_equalsZero() {
        assertEquals(Speed.ZERO, Speed.kilometersPerHour(-0.0))
    }

    @Test
    fun kilometersPerHour_negative_throws() {
        assertThrows(IllegalArgumentException::class.java) { Speed.kilometersPerHour(-0.5) }
    }

    @Test
    fun kilometersPerHour_nan_throws() {
        assertThrows(IllegalArgumentException::class.java) { Speed.kilometersPerHour(Double.NaN) }
    }

    @Test
    fun kilometersPerHour_tooLargeForMeters_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            Speed.kilometersPerHour(Double.MAX_VALUE)
        }
    }

    @Test
    fun inKilometersPerHour_metersPerSecond_convertsToKilometersPerHourExactly() {
        val speed = Speed.metersPerSecond(10.0)

        assertEquals(36.0, speed.inKilometersPerHour, 0.0)
    }

    @Test
    fun inKilometersPerHour_oneHourOverflowsDistance_throws() {
        val speed = Speed.metersPerSecond(Double.MAX_VALUE)

        assertThrows(IllegalArgumentException::class.java) { speed.inKilometersPerHour }
    }

    @Test
    fun times_duration_isDistanceCovered() {
        val distance = Speed.metersPerSecond(12.5) * 2.minutes

        assertEquals(Distance.meters(1_500.0), distance)
    }

    @Test
    fun times_zeroDuration_isZeroDistance() {
        assertEquals(Distance.ZERO, Speed.metersPerSecond(30.0) * Duration.ZERO)
    }

    @Test
    fun times_negativeDuration_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            Speed.metersPerSecond(1.0) * (-1).seconds
        }
    }

    @Test
    fun times_infiniteDuration_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            Speed.metersPerSecond(1.0) * Duration.INFINITE
        }
    }

    @Test
    fun times_divideDistanceByDuration_roundTrips() {
        val time = 90.seconds
        val distance = Distance.kilometers(2.0)

        // 2000 / 90 is a repeating decimal, so the round trip may differ in the last bit.
        assertEquals(distance.inMeters, ((distance / time) * time).inMeters, 1e-9)
    }

    @Test
    fun compareTo_slowerSpeed_isLess() {
        assertTrue(Speed.metersPerSecond(1.0) < Speed.metersPerSecond(1.5))
    }

    @Test
    fun compareTo_fasterSpeed_isGreater() {
        assertTrue(Speed.kilometersPerHour(100.0) > Speed.metersPerSecond(27.0))
    }

    @Test
    fun compareTo_sameSpeed_isZero() {
        assertEquals(0, Speed.metersPerSecond(5.0).compareTo(Speed.metersPerSecond(5.0)))
    }

    @Test
    fun toString_speed_showsMetersPerSecondWithUnit() {
        assertEquals("8.5 m/s", Speed.metersPerSecond(8.5).toString())
    }
}
