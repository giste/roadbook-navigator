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
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DistanceTest {

    @Test
    fun meters_validValue_keepsMeters() {
        val distance = Distance.meters(12.5)

        assertEquals(12.5, distance.inMeters, 0.0)
    }

    @Test
    fun meters_zero_isAccepted() {
        val distance = Distance.meters(0.0)

        assertEquals(Distance.ZERO, distance)
    }

    @Test
    fun meters_negativeZero_equalsZero() {
        val distance = Distance.meters(-0.0)

        assertEquals(Distance.ZERO, distance)
    }

    @Test
    fun meters_negative_throws() {
        assertThrows(IllegalArgumentException::class.java) { Distance.meters(-0.1) }
    }

    @Test
    fun meters_nan_throws() {
        assertThrows(IllegalArgumentException::class.java) { Distance.meters(Double.NaN) }
    }

    @Test
    fun meters_positiveInfinity_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            Distance.meters(Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun kilometers_validValue_convertsToMeters() {
        val distance = Distance.kilometers(1.5)

        assertEquals(1_500.0, distance.inMeters, 0.0)
    }

    @Test
    fun kilometers_negative_throws() {
        assertThrows(IllegalArgumentException::class.java) { Distance.kilometers(-1.0) }
    }

    @Test
    fun kilometers_tooLargeForMeters_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            Distance.kilometers(Double.MAX_VALUE)
        }
    }

    @Test
    fun inKilometers_meters_convertsToKilometers() {
        val distance = Distance.meters(2_345.0)

        assertEquals(2.345, distance.inKilometers, 1e-12)
    }

    @Test
    fun plus_twoDistances_addsLengths() {
        val sum = Distance.meters(100.0) + Distance.meters(25.5)

        assertEquals(Distance.meters(125.5), sum)
    }

    @Test
    fun plus_overflowToInfinity_throws() {
        val longest = Distance.meters(Double.MAX_VALUE)

        assertThrows(IllegalArgumentException::class.java) { longest + longest }
    }

    @Test
    fun minus_shorterDistance_subtractsLengths() {
        val difference = Distance.meters(100.0) - Distance.meters(25.5)

        assertEquals(Distance.meters(74.5), difference)
    }

    @Test
    fun minus_sameDistance_isZero() {
        val distance = Distance.meters(42.0)

        assertEquals(Distance.ZERO, distance - distance)
    }

    @Test
    fun minus_longerDistance_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            Distance.meters(10.0) - Distance.meters(10.5)
        }
    }

    @Test
    fun div_positiveDuration_isAverageSpeed() {
        val speed = Distance.meters(100.0) / 8.seconds

        assertEquals(Speed.metersPerSecond(12.5), speed)
    }

    @Test
    fun div_zeroDistance_isZeroSpeed() {
        assertEquals(Speed.ZERO, Distance.ZERO / 1.minutes)
    }

    @Test
    fun div_zeroDuration_throws() {
        assertThrows(IllegalArgumentException::class.java) { Distance.meters(1.0) / Duration.ZERO }
    }

    @Test
    fun div_negativeDuration_throws() {
        assertThrows(IllegalArgumentException::class.java) { Distance.meters(1.0) / (-1).seconds }
    }

    @Test
    fun div_infiniteDuration_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            Distance.meters(1.0) / Duration.INFINITE
        }
    }

    @Test
    fun div_hugeDistanceInTinyDuration_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            Distance.meters(Double.MAX_VALUE) / 1.nanoseconds
        }
    }

    @Test
    fun compareTo_shorterDistance_isLess() {
        assertTrue(Distance.meters(1.0) < Distance.meters(2.0))
    }

    @Test
    fun compareTo_longerDistance_isGreater() {
        assertTrue(Distance.kilometers(1.0) > Distance.meters(999.0))
    }

    @Test
    fun compareTo_sameLength_isZero() {
        assertEquals(0, Distance.kilometers(1.0).compareTo(Distance.meters(1_000.0)))
    }

    @Test
    fun toString_distance_showsMetersWithUnit() {
        assertEquals("12.5 m", Distance.meters(12.5).toString())
    }
}
