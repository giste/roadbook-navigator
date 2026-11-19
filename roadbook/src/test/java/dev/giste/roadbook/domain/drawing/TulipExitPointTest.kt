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

package dev.giste.roadbook.domain.drawing

import dev.giste.geo.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Test

// On the equator, bearings between these points are exactly the cardinal directions. The rider
// always arrives heading east, from WEST to CENTER.
class TulipExitPointTest {

    @Test
    fun tulipExitPoint_straightOn_endsAtTheTop() {
        val exit = tulipExitPoint(previous = WEST, current = CENTER, next = EAST)

        assertPoint(0.0, -60.0, exit)
    }

    @Test
    fun tulipExitPoint_rightAngleRight_endsAtTheRight() {
        val exit = tulipExitPoint(previous = WEST, current = CENTER, next = SOUTH)

        assertPoint(75.0, 0.0, exit)
    }

    @Test
    fun tulipExitPoint_rightAngleLeft_endsAtTheLeft() {
        val exit = tulipExitPoint(previous = WEST, current = CENTER, next = NORTH)

        assertPoint(-75.0, 0.0, exit)
    }

    @Test
    fun tulipExitPoint_uTurn_endsAtTheBottom() {
        val exit = tulipExitPoint(previous = WEST, current = CENTER, next = WEST)

        assertPoint(0.0, 25.0, exit)
    }

    @Test
    fun tulipExitPoint_noPrevious_goesStraightOn() {
        val exit = tulipExitPoint(previous = null, current = CENTER, next = NORTH)

        assertPoint(0.0, -60.0, exit)
    }

    @Test
    fun tulipExitPoint_nextAtTheSamePosition_goesStraightOn() {
        val exit = tulipExitPoint(previous = WEST, current = CENTER, next = CENTER)

        assertPoint(0.0, -60.0, exit)
    }

    @Test
    fun tulipExitPoint_previousAtTheSamePosition_goesStraightOn() {
        // Without a direction of arrival, the rider is taken to arrive heading to the next point.
        val exit = tulipExitPoint(previous = CENTER, current = CENTER, next = NORTH)

        assertPoint(0.0, -60.0, exit)
    }

    private fun assertPoint(expectedX: Double, expectedY: Double, actual: CanvasPoint) {
        assertEquals("x", expectedX, actual.x, 1e-6)
        assertEquals("y", expectedY, actual.y, 1e-6)
    }

    private companion object {
        val CENTER = GeoPoint(0.0, 0.0)
        val WEST = GeoPoint(0.0, -0.001)
        val EAST = GeoPoint(0.0, 0.001)
        val NORTH = GeoPoint(0.001, 0.0)
        val SOUTH = GeoPoint(-0.001, 0.0)
    }
}
