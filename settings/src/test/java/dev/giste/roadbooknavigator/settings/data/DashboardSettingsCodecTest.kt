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
import dev.giste.roadbooknavigator.settings.DashboardSettings
import dev.giste.roadbooknavigator.settings.FakeLogger
import dev.giste.roadbooknavigator.settings.ScreenShare
import dev.giste.roadbooknavigator.settings.data.DashboardSettingsCodec.LANDSCAPE_SIDE_COLUMN_KEY
import dev.giste.roadbooknavigator.settings.data.DashboardSettingsCodec.PORTRAIT_DISTANCES_KEY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardSettingsCodecTest {

    private val logger = FakeLogger()

    @Test
    fun read_nothingStored_isTheDefaults() {
        assertEquals(DashboardSettings(), DashboardSettingsCodec.read(preferencesOf(), logger))
    }

    @Test
    fun read_writtenSettings_isTheSameSettings() {
        val settings = DashboardSettings(
            landscapeSideColumn = ScreenShare.of(40),
            portraitDistances = ScreenShare.of(11),
        )
        val preferences = mutablePreferencesOf()
        DashboardSettingsCodec.write(preferences, settings)

        assertEquals(settings, DashboardSettingsCodec.read(preferences, logger))
    }

    @Test
    fun write_defaults_storesNothing() {
        val preferences = mutablePreferencesOf()

        DashboardSettingsCodec.write(preferences, DashboardSettings())

        assertTrue(preferences.asMap().isEmpty())
    }

    @Test
    fun write_changedValues_storesTheirPercents() {
        val preferences = mutablePreferencesOf()

        DashboardSettingsCodec.write(
            preferences,
            DashboardSettings(
                landscapeSideColumn = ScreenShare.of(25),
                portraitDistances = ScreenShare.of(12),
            ),
        )

        assertEquals(25, preferences[LANDSCAPE_SIDE_COLUMN_KEY])
        assertEquals(12, preferences[PORTRAIT_DISTANCES_KEY])
    }

    @Test
    fun write_backToTheDefaults_removesTheKeys() {
        val preferences = mutablePreferencesOf()
        DashboardSettingsCodec.write(
            preferences,
            DashboardSettings(
                landscapeSideColumn = ScreenShare.of(35),
                portraitDistances = ScreenShare.of(10),
            ),
        )

        DashboardSettingsCodec.write(preferences, DashboardSettings())

        assertTrue(preferences.asMap().isEmpty())
    }

    @Test
    fun read_invalidSideColumn_usesTheDefaultKeepsTheOtherAndWarns() {
        val settings = DashboardSettingsCodec.read(
            preferencesOf(LANDSCAPE_SIDE_COLUMN_KEY to 0, PORTRAIT_DISTANCES_KEY to 10),
            logger,
        )

        assertEquals(DashboardSettings(portraitDistances = ScreenShare.of(10)), settings)
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun read_invalidDistancesRow_usesTheDefaultKeepsTheOtherAndWarns() {
        val settings = DashboardSettingsCodec.read(
            preferencesOf(LANDSCAPE_SIDE_COLUMN_KEY to 35, PORTRAIT_DISTANCES_KEY to 150),
            logger,
        )

        assertEquals(DashboardSettings(landscapeSideColumn = ScreenShare.of(35)), settings)
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun read_validSharesOutsideTheOfferedRanges_areBroughtIntoThem() {
        val settings = DashboardSettingsCodec.read(
            preferencesOf(LANDSCAPE_SIDE_COLUMN_KEY to 50, PORTRAIT_DISTANCES_KEY to 5),
            logger,
        )

        assertEquals(
            DashboardSettings(
                landscapeSideColumn = ScreenShare.of(40),
                portraitDistances = ScreenShare.of(8),
            ),
            settings,
        )
        assertTrue(logger.warnings.isEmpty())
    }
}
