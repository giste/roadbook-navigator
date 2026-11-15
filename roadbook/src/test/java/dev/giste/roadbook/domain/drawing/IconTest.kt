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
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class IconTest {

    private val invalidSizes = listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY)
    private val invalidScales = listOf(0.0, Double.NaN, Double.NEGATIVE_INFINITY)

    @Test
    fun constructor_fractionalSize_isKept() {
        val icon = icon(width = 35.6748046875, height = 20.34)

        assertEquals(35.6748046875, icon.width, 0.0)
        assertEquals(20.34, icon.height, 0.0)
    }

    @Test
    fun constructor_optionalValuesOmitted_usesDefaults() {
        val icon = Icon(type = IconType.CHURCH, width = 50.0, height = 50.0, center = CanvasPoint.CENTER)

        assertEquals(0, icon.angle)
        assertEquals(1.0, icon.scaleX, 0.0)
        assertEquals(1.0, icon.scaleY, 0.0)
        assertNull(icon.originalId)
        assertNull(icon.originalName)
    }

    @Test
    fun constructor_negativeScale_isAccepted() {
        // A negative scale mirrors the symbol.
        val icon = icon(scaleX = -1.0, scaleY = -0.5)

        assertEquals(-1.0, icon.scaleX, 0.0)
        assertEquals(-0.5, icon.scaleY, 0.0)
    }

    @Test
    fun constructor_invalidWidth_throws() {
        invalidSizes.forEach { width ->
            assertThrows("width $width", IllegalArgumentException::class.java) { icon(width = width) }
        }
    }

    @Test
    fun constructor_invalidHeight_throws() {
        invalidSizes.forEach { height ->
            assertThrows("height $height", IllegalArgumentException::class.java) {
                icon(height = height)
            }
        }
    }

    @Test
    fun constructor_invalidScaleX_throws() {
        invalidScales.forEach { scale ->
            assertThrows("scaleX $scale", IllegalArgumentException::class.java) {
                icon(scaleX = scale)
            }
        }
    }

    @Test
    fun constructor_invalidScaleY_throws() {
        invalidScales.forEach { scale ->
            assertThrows("scaleY $scale", IllegalArgumentException::class.java) {
                icon(scaleY = scale)
            }
        }
    }

    private fun icon(
        width: Double = 50.0,
        height: Double = 50.0,
        scaleX: Double = 1.0,
        scaleY: Double = 1.0,
    ): Icon = Icon(
        type = IconType.DANGER_1,
        width = width,
        height = height,
        center = CanvasPoint.CENTER,
        scaleX = scaleX,
        scaleY = scaleY,
    )
}
