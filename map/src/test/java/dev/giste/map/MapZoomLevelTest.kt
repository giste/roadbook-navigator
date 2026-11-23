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

class MapZoomLevelTest {

    @Test
    fun limits_areTwoToTwentyWithDefaultEighteen() {
        assertEquals(2, MapZoomLevel.MIN.value)
        assertEquals(20, MapZoomLevel.MAX.value)
        assertEquals(18, MapZoomLevel.DEFAULT.value)
    }

    @Test
    fun of_withinRange_keepsTheValue() {
        assertEquals(2, MapZoomLevel.of(2).value)
        assertEquals(20, MapZoomLevel.of(20).value)
    }

    @Test(expected = IllegalArgumentException::class)
    fun of_belowRange_isRejected() {
        MapZoomLevel.of(1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun of_aboveRange_isRejected() {
        MapZoomLevel.of(21)
    }

    @Test
    fun coerce_withinRange_keepsTheValue() {
        assertEquals(MapZoomLevel.of(14), MapZoomLevel.coerce(14))
    }

    @Test
    fun coerce_belowRange_isMin() {
        assertEquals(MapZoomLevel.MIN, MapZoomLevel.coerce(-5))
    }

    @Test
    fun coerce_aboveRange_isMax() {
        assertEquals(MapZoomLevel.MAX, MapZoomLevel.coerce(99))
    }

    @Test
    fun compareTo_ordersByLevel() {
        assertTrue(MapZoomLevel.of(10) < MapZoomLevel.of(11))
        assertTrue(MapZoomLevel.of(11) > MapZoomLevel.of(10))
    }

    @Test
    fun toString_showsTheLevel() {
        assertEquals("z18", MapZoomLevel.DEFAULT.toString())
    }
}
