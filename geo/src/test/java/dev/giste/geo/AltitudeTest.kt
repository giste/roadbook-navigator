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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AltitudeTest {

    @Test
    fun meters_positiveValue_keepsMeters() {
        assertEquals(657.0, Altitude.meters(657.0).inMeters, 0.0)
    }

    @Test
    fun meters_negativeValue_isAccepted() {
        assertEquals(-430.5, Altitude.meters(-430.5).inMeters, 0.0)
    }

    @Test
    fun meters_zero_equalsZero() {
        assertEquals(Altitude.ZERO, Altitude.meters(0.0))
    }

    @Test
    fun meters_negativeZero_equalsZero() {
        assertEquals(Altitude.ZERO, Altitude.meters(-0.0))
    }

    @Test
    fun meters_nan_throws() {
        assertThrows(IllegalArgumentException::class.java) { Altitude.meters(Double.NaN) }
    }

    @Test
    fun meters_positiveInfinity_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            Altitude.meters(Double.POSITIVE_INFINITY)
        }
    }

    @Test
    fun meters_negativeInfinity_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            Altitude.meters(Double.NEGATIVE_INFINITY)
        }
    }

    @Test
    fun verticalDistanceTo_higherAltitude_isTheDifference() {
        val distance = Altitude.meters(600.0).verticalDistanceTo(Altitude.meters(630.0))

        assertEquals(Distance.meters(30.0), distance)
    }

    @Test
    fun verticalDistanceTo_lowerAltitude_isPositive() {
        val distance = Altitude.meters(630.0).verticalDistanceTo(Altitude.meters(600.0))

        assertEquals(Distance.meters(30.0), distance)
    }

    @Test
    fun verticalDistanceTo_acrossTheReferenceSurface_addsBothSides() {
        val distance = Altitude.meters(-20.0).verticalDistanceTo(Altitude.meters(15.0))

        assertEquals(Distance.meters(35.0), distance)
    }

    @Test
    fun verticalDistanceTo_sameAltitude_isZero() {
        val altitude = Altitude.meters(600.0)

        assertEquals(Distance.ZERO, altitude.verticalDistanceTo(altitude))
    }

    @Test
    fun verticalDistanceTo_overflow_throws() {
        val high = Altitude.meters(Double.MAX_VALUE)
        val low = Altitude.meters(-Double.MAX_VALUE)

        assertThrows(IllegalArgumentException::class.java) { high.verticalDistanceTo(low) }
    }

    @Test
    fun compareTo_ordersByHeight() {
        assertTrue(Altitude.meters(-10.0) < Altitude.ZERO)
        assertTrue(Altitude.meters(100.0) > Altitude.meters(99.9))
    }
}
