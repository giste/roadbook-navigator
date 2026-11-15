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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CanvasPointTest {

    @Test
    fun constructor_negativeCoordinates_areAccepted() {
        val point = CanvasPoint(x = -80.5, y = -50.9)

        assertEquals(-80.5, point.x, 0.0)
        assertEquals(-50.9, point.y, 0.0)
    }

    @Test
    fun center_isTheOrigin() {
        assertEquals(CanvasPoint(0.0, 0.0), CanvasPoint.CENTER)
    }

    @Test
    fun constructor_xNan_throws() {
        assertThrows(IllegalArgumentException::class.java) { CanvasPoint(Double.NaN, 0.0) }
    }

    @Test
    fun constructor_xInfinite_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            CanvasPoint(Double.NEGATIVE_INFINITY, 0.0)
        }
    }

    @Test
    fun constructor_yNan_throws() {
        assertThrows(IllegalArgumentException::class.java) { CanvasPoint(0.0, Double.NaN) }
    }

    @Test
    fun constructor_yInfinite_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            CanvasPoint(0.0, Double.POSITIVE_INFINITY)
        }
    }
}
