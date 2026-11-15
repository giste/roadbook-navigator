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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortDistanceThresholdTest {

    @Test
    fun limits_areFiftyMetersToOneKilometer() {
        assertEquals(Distance.meters(50.0), ShortDistanceThreshold.MIN_DISTANCE)
        assertEquals(Distance.meters(1_000.0), ShortDistanceThreshold.MAX_DISTANCE)
        assertEquals(Distance.meters(50.0), ShortDistanceThreshold.RANGE.start)
        assertEquals(Distance.meters(1_000.0), ShortDistanceThreshold.RANGE.endInclusive)
        assertEquals(ShortDistanceThreshold.MIN_DISTANCE, ShortDistanceThreshold.MIN.distance)
        assertEquals(ShortDistanceThreshold.MAX_DISTANCE, ShortDistanceThreshold.MAX.distance)
    }

    @Test
    fun default_isTwoHundredFiftyMeters() {
        assertEquals(Distance.meters(250.0), ShortDistanceThreshold.DEFAULT.distance)
    }

    @Test
    fun of_distanceWithinRange_keepsDistance() {
        val threshold = ShortDistanceThreshold.of(Distance.meters(400.0))

        assertEquals(Distance.meters(400.0), threshold.distance)
    }

    @Test
    fun of_minDistance_isAccepted() {
        val threshold = ShortDistanceThreshold.of(Distance.meters(50.0))

        assertEquals(ShortDistanceThreshold.MIN, threshold)
    }

    @Test
    fun of_maxDistance_isAccepted() {
        val threshold = ShortDistanceThreshold.of(Distance.meters(1_000.0))

        assertEquals(ShortDistanceThreshold.MAX, threshold)
    }

    @Test
    fun of_distanceBelowMin_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            ShortDistanceThreshold.of(Distance.meters(49.9))
        }
    }

    @Test
    fun of_distanceAboveMax_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            ShortDistanceThreshold.of(Distance.meters(1_000.1))
        }
    }

    @Test
    fun coerce_distanceWithinRange_keepsDistance() {
        val threshold = ShortDistanceThreshold.coerce(Distance.meters(400.0))

        assertEquals(Distance.meters(400.0), threshold.distance)
    }

    @Test
    fun coerce_distanceBelowMin_returnsMin() {
        val threshold = ShortDistanceThreshold.coerce(Distance.ZERO)

        assertEquals(ShortDistanceThreshold.MIN, threshold)
    }

    @Test
    fun coerce_distanceAboveMax_returnsMax() {
        val threshold = ShortDistanceThreshold.coerce(Distance.kilometers(5.0))

        assertEquals(ShortDistanceThreshold.MAX, threshold)
    }

    @Test
    fun compareTo_shorterThreshold_isLess() {
        assertTrue(ShortDistanceThreshold.MIN < ShortDistanceThreshold.DEFAULT)
        assertTrue(ShortDistanceThreshold.MAX > ShortDistanceThreshold.DEFAULT)
    }

    @Test
    fun compareTo_sameDistance_isEqual() {
        val threshold = ShortDistanceThreshold.of(Distance.meters(250.0))

        assertEquals(0, threshold.compareTo(ShortDistanceThreshold.DEFAULT))
    }

    @Test
    fun toString_returnsDistanceWithUnit() {
        assertEquals("250.0 m", ShortDistanceThreshold.DEFAULT.toString())
    }
}
