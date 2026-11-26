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

package dev.giste.roadbooknavigator.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenShareTest {

    @Test
    fun of_validPercent_keepsIt() {
        assertEquals(30, ScreenShare.of(30).percent)
    }

    @Test
    fun of_limits_areValid() {
        assertEquals(1, ScreenShare.of(ScreenShare.MIN_PERCENT).percent)
        assertEquals(99, ScreenShare.of(ScreenShare.MAX_PERCENT).percent)
    }

    @Test(expected = IllegalArgumentException::class)
    fun of_zero_isRejected() {
        ScreenShare.of(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun of_wholeScreen_isRejected() {
        ScreenShare.of(100)
    }

    @Test
    fun isValid_insideAndOutsideTheLimits_tellsThemApart() {
        assertTrue(ScreenShare.isValid(1))
        assertTrue(ScreenShare.isValid(99))
        assertFalse(ScreenShare.isValid(0))
        assertFalse(ScreenShare.isValid(100))
        assertFalse(ScreenShare.isValid(-5))
    }

    @Test
    fun fraction_isThePercentOverAHundred() {
        assertEquals(0.08f, ScreenShare.of(8).fraction, 0.0001f)
        assertEquals(0.3f, ScreenShare.of(30).fraction, 0.0001f)
    }
}
