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

class TextTest {

    private val invalidBoxSizes = listOf(-1.0, Double.NaN, Double.POSITIVE_INFINITY)

    @Test
    fun constructor_fontSizeOmitted_isEighteen() {
        val text = Text(
            text = "CAP 120",
            lineHeight = 0.8,
            width = 100.0,
            height = 20.0,
            maxWidth = 180.0,
            maxHeight = 100.0,
            center = CanvasPoint.CENTER,
        )

        assertEquals(18, text.fontSize)
    }

    @Test
    fun constructor_emptyTextAndZeroBox_isAccepted() {
        val text = text(text = "", width = 0.0, height = 0.0, maxWidth = 0.0, maxHeight = 0.0)

        assertEquals("", text.text)
        assertEquals(0.0, text.width, 0.0)
    }

    @Test
    fun constructor_zeroFontSize_throws() {
        assertThrows(IllegalArgumentException::class.java) { text(fontSize = 0) }
    }

    @Test
    fun constructor_negativeFontSize_throws() {
        assertThrows(IllegalArgumentException::class.java) { text(fontSize = -18) }
    }

    @Test
    fun constructor_invalidLineHeight_throws() {
        listOf(0.0, -0.8, Double.NaN, Double.POSITIVE_INFINITY).forEach { lineHeight ->
            assertThrows("lineHeight $lineHeight", IllegalArgumentException::class.java) {
                text(lineHeight = lineHeight)
            }
        }
    }

    @Test
    fun constructor_invalidWidth_throws() {
        invalidBoxSizes.forEach { size ->
            assertThrows("width $size", IllegalArgumentException::class.java) { text(width = size) }
        }
    }

    @Test
    fun constructor_invalidHeight_throws() {
        invalidBoxSizes.forEach { size ->
            assertThrows("height $size", IllegalArgumentException::class.java) { text(height = size) }
        }
    }

    @Test
    fun constructor_invalidMaxWidth_throws() {
        invalidBoxSizes.forEach { size ->
            assertThrows("maxWidth $size", IllegalArgumentException::class.java) {
                text(maxWidth = size)
            }
        }
    }

    @Test
    fun constructor_invalidMaxHeight_throws() {
        invalidBoxSizes.forEach { size ->
            assertThrows("maxHeight $size", IllegalArgumentException::class.java) {
                text(maxHeight = size)
            }
        }
    }

    private fun text(
        text: String = "CAP 120",
        fontSize: Int = 18,
        lineHeight: Double = 0.8,
        width: Double = 100.0,
        height: Double = 20.0,
        maxWidth: Double = 180.0,
        maxHeight: Double = 100.0,
    ): Text = Text(
        text = text,
        fontSize = fontSize,
        lineHeight = lineHeight,
        width = width,
        height = height,
        maxWidth = maxWidth,
        maxHeight = maxHeight,
        center = CanvasPoint.CENTER,
    )
}
