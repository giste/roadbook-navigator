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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapTiltTest {

    @Test
    fun limits_areZeroToSixtyFiveWithDefaultSixty() {
        assertEquals(0.0, MapTilt.MIN.inDegrees, 0.0)
        assertEquals(65.0, MapTilt.MAX.inDegrees, 0.0)
        assertEquals(60.0, MapTilt.DEFAULT.inDegrees, 0.0)
    }

    @Test
    fun of_withinRange_keepsTheValue() {
        assertEquals(0.0, MapTilt.of(0.0).inDegrees, 0.0)
        assertEquals(65.0, MapTilt.of(65.0).inDegrees, 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun of_belowRange_isRejected() {
        MapTilt.of(-0.1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun of_aboveRange_isRejected() {
        MapTilt.of(65.1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun of_nan_isRejected() {
        MapTilt.of(Double.NaN)
    }

    @Test
    fun coerce_withinRange_keepsTheValue() {
        assertEquals(MapTilt.of(30.0), MapTilt.coerce(30.0))
    }

    @Test
    fun coerce_belowRange_isMin() {
        assertEquals(MapTilt.MIN, MapTilt.coerce(-10.0))
    }

    @Test
    fun coerce_aboveRange_isMax() {
        assertEquals(MapTilt.MAX, MapTilt.coerce(Double.POSITIVE_INFINITY))
    }

    @Test
    fun coerce_nan_isDefault() {
        assertEquals(MapTilt.DEFAULT, MapTilt.coerce(Double.NaN))
    }

    @Test
    fun compareTo_ordersByDegrees() {
        assertTrue(MapTilt.of(10.0) < MapTilt.of(20.0))
        assertTrue(MapTilt.of(20.0) > MapTilt.of(10.0))
    }

    @Test
    fun toString_showsDegrees() {
        assertEquals("60.0°", MapTilt.DEFAULT.toString())
    }
}
