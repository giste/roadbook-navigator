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

import androidx.compose.ui.test.junit4.v2.createComposeRule
import dev.giste.roadbooknavigator.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class ThemeModesTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** Whether [mode] shows the dark colors, read inside a composition. */
    private fun isDark(mode: ThemeMode): Boolean {
        var dark: Boolean? = null
        composeRule.setContent { dark = mode.isDarkTheme() }
        composeRule.waitForIdle()
        return checkNotNull(dark)
    }

    @Test
    @Config(qualifiers = "night")
    fun isDarkTheme_lightOnDarkSystem_isLight() {
        assertEquals(false, isDark(ThemeMode.LIGHT))
    }

    @Test
    @Config(qualifiers = "notnight")
    fun isDarkTheme_darkOnLightSystem_isDark() {
        assertEquals(true, isDark(ThemeMode.DARK))
    }

    @Test
    @Config(qualifiers = "night")
    fun isDarkTheme_systemOnDarkSystem_isDark() {
        assertEquals(true, isDark(ThemeMode.SYSTEM))
    }

    @Test
    @Config(qualifiers = "notnight")
    fun isDarkTheme_systemOnLightSystem_isLight() {
        assertEquals(false, isDark(ThemeMode.SYSTEM))
    }
}
