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

package dev.giste.roadbooknavigator.settings.data

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.preferencesOf
import dev.giste.roadbooknavigator.settings.AppSettings
import dev.giste.roadbooknavigator.settings.FakeLogger
import dev.giste.roadbooknavigator.settings.ScreenOrientation
import dev.giste.roadbooknavigator.settings.ThemeMode
import dev.giste.roadbooknavigator.settings.data.AppSettingsCodec.ORIENTATION_KEY
import dev.giste.roadbooknavigator.settings.data.AppSettingsCodec.THEME_MODE_KEY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsCodecTest {

    private val logger = FakeLogger()

    @Test
    fun read_nothingStored_isTheDefaults() {
        assertEquals(AppSettings(), AppSettingsCodec.read(preferencesOf(), logger))
    }

    @Test
    fun read_writtenSettings_isTheSameSettings() {
        val settings = AppSettings(
            themeMode = ThemeMode.DARK,
            orientation = ScreenOrientation.HORIZONTAL,
        )
        val preferences = mutablePreferencesOf()
        AppSettingsCodec.write(preferences, settings)

        assertEquals(settings, AppSettingsCodec.read(preferences, logger))
    }

    @Test
    fun write_defaults_storesNothing() {
        val preferences = mutablePreferencesOf()

        AppSettingsCodec.write(preferences, AppSettings())

        assertTrue(preferences.asMap().isEmpty())
    }

    @Test
    fun read_unknownThemeMode_usesTheDefaultAndWarns() {
        val settings = AppSettingsCodec.read(preferencesOf(THEME_MODE_KEY to "SEPIA"), logger)

        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun read_unknownOrientation_usesTheDefaultAndKeepsTheThemeMode() {
        val settings = AppSettingsCodec.read(
            preferencesOf(ORIENTATION_KEY to "DIAGONAL", THEME_MODE_KEY to ThemeMode.DARK.name),
            logger,
        )

        assertEquals(AppSettings(themeMode = ThemeMode.DARK), settings)
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun write_orientationBackToTheDefault_removesItsKey() {
        val preferences = mutablePreferencesOf()
        AppSettingsCodec.write(preferences, AppSettings(orientation = ScreenOrientation.VERTICAL))

        AppSettingsCodec.write(preferences, AppSettings())

        assertTrue(preferences.asMap().isEmpty())
    }
}
