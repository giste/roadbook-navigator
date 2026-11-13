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
import org.junit.Test

class BearingTest {

    @Test
    fun degrees_valueInRange_keepsValue() {
        val bearing = Bearing.degrees(123.4)

        assertEquals(123.4, bearing.inDegrees, 0.0)
    }

    @Test
    fun degrees_zero_isNorth() {
        assertEquals(Bearing.NORTH, Bearing.degrees(0.0))
    }

    @Test
    fun degrees_negativeZero_isNorth() {
        assertEquals(Bearing.NORTH, Bearing.degrees(-0.0))
    }

    @Test
    fun degrees_fullTurn_isNorth() {
        assertEquals(Bearing.NORTH, Bearing.degrees(360.0))
    }

    @Test
    fun degrees_negativeFullTurn_isNorth() {
        assertEquals(Bearing.NORTH, Bearing.degrees(-360.0))
    }

    @Test
    fun degrees_aboveFullTurn_wrapsIntoRange() {
        val bearing = Bearing.degrees(450.0)

        assertEquals(90.0, bearing.inDegrees, 0.0)
    }

    @Test
    fun degrees_negative_wrapsIntoRange() {
        val bearing = Bearing.degrees(-90.0)

        assertEquals(270.0, bearing.inDegrees, 0.0)
    }

    @Test
    fun degrees_severalNegativeTurns_wrapsIntoRange() {
        val bearing = Bearing.degrees(-730.0)

        assertEquals(350.0, bearing.inDegrees, 1e-9)
    }

    @Test
    fun degrees_tinyNegative_isNorthInsteadOfFullTurn() {
        // -1e-15 + 360 rounds to exactly 360.0, which is out of range.
        val bearing = Bearing.degrees(-1e-15)

        assertEquals(Bearing.NORTH, bearing)
    }

    @Test
    fun degrees_nan_throws() {
        assertThrows(IllegalArgumentException::class.java) { Bearing.degrees(Double.NaN) }
    }

    @Test
    fun degrees_infinite_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            Bearing.degrees(Double.NEGATIVE_INFINITY)
        }
    }

    @Test
    fun toString_bearing_showsDegreesWithUnit() {
        assertEquals("90.0°", Bearing.degrees(90.0).toString())
    }
}
