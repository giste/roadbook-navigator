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

package dev.giste.roadbooknavigator.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class RiderTypographyTest {

    private val material = Typography()

    /** Every style of [typography], by name, so failures name the style. */
    private fun styles(typography: Typography): Map<String, TextStyle> = with(typography) {
        mapOf(
            "displayLarge" to displayLarge, "displayMedium" to displayMedium,
            "displaySmall" to displaySmall, "headlineLarge" to headlineLarge,
            "headlineMedium" to headlineMedium, "headlineSmall" to headlineSmall,
            "titleLarge" to titleLarge, "titleMedium" to titleMedium, "titleSmall" to titleSmall,
            "bodyLarge" to bodyLarge, "bodyMedium" to bodyMedium, "bodySmall" to bodySmall,
            "labelLarge" to labelLarge, "labelMedium" to labelMedium, "labelSmall" to labelSmall,
        )
    }

    @Test
    fun riderTypography_everyStyle_isTheMaterialOneScaledBy1Point25() {
        val rider = styles(RiderTypography)

        styles(material).forEach { (name, style) ->
            val scaled = rider.getValue(name)
            assertEquals("$name font size", style.fontSize * 1.25f, scaled.fontSize)
            assertEquals("$name line height", style.lineHeight * 1.25f, scaled.lineHeight)
        }
    }

    @Test
    fun riderTypography_everyStyle_keepsItsWeightAndLetterSpacing() {
        val rider = styles(RiderTypography)

        styles(material).forEach { (name, style) ->
            val scaled = rider.getValue(name)
            assertEquals("$name weight", style.fontWeight, scaled.fontWeight)
            assertEquals("$name letter spacing", style.letterSpacing, scaled.letterSpacing)
        }
    }

    @Test
    fun riderTypography_bodyText_isAtLeast17Sp() {
        assertEquals(17.5.sp, RiderTypography.bodyMedium.fontSize)
        assertEquals(20.sp, RiderTypography.bodyLarge.fontSize)
        assertEquals(17.5.sp, RiderTypography.labelLarge.fontSize)
    }

    @Test
    fun scaled_unspecifiedSizes_stayUnspecified() {
        val style = TextStyle(fontWeight = FontWeight.Bold)

        val scaled = style.scaled(2f)

        assertEquals(TextUnit.Unspecified, scaled.fontSize)
        assertEquals(TextUnit.Unspecified, scaled.lineHeight)
        assertEquals(FontWeight.Bold, scaled.fontWeight)
    }

    @Test
    fun scaled_specifiedSizes_areMultiplied() {
        val scaled = TextStyle(fontSize = 10.sp, lineHeight = 12.sp).scaled(1.5f)

        assertEquals(15.sp, scaled.fontSize)
        assertEquals(18.sp, scaled.lineHeight)
    }
}
