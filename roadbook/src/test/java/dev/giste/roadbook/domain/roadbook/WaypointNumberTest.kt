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

package dev.giste.roadbook.domain.roadbook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WaypointNumberTest {

    @Test
    fun constructor_one_isAccepted() {
        assertEquals(1, WaypointNumber(1).value)
    }

    @Test
    fun constructor_largeNumber_isAccepted() {
        assertEquals(1_000, WaypointNumber(1_000).value)
    }

    @Test
    fun constructor_zero_throws() {
        assertThrows(IllegalArgumentException::class.java) { WaypointNumber(0) }
    }

    @Test
    fun constructor_negative_throws() {
        assertThrows(IllegalArgumentException::class.java) { WaypointNumber(-1) }
    }

    @Test
    fun first_isOne() {
        assertEquals(WaypointNumber(1), WaypointNumber.FIRST)
    }

    @Test
    fun compareTo_orderedByValue() {
        assertTrue(WaypointNumber(2) < WaypointNumber(3))
        assertTrue(WaypointNumber(3) > WaypointNumber(2))
        assertEquals(0, WaypointNumber(2).compareTo(WaypointNumber(2)))
    }

    @Test
    fun toString_returnsTheNumber() {
        assertEquals("12", WaypointNumber(12).toString())
    }
}
