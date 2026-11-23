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

import androidx.datastore.preferences.core.preferencesOf
import app.cash.turbine.test
import dev.giste.roadbook.RoadbookSettings
import dev.giste.roadbook.RoadbookStyle
import dev.giste.roadbooknavigator.settings.AppSettings
import dev.giste.roadbooknavigator.settings.FakeLogger
import dev.giste.roadbooknavigator.settings.RemoteModel
import dev.giste.roadbooknavigator.settings.RemoteSettings
import dev.giste.roadbooknavigator.settings.ThemeMode
import dev.giste.roadbooknavigator.settings.domain.SettingsUpdateResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The shared behavior of [DataStoreSettingsRepository], through the roadbook repository, plus a
 * check that the location and odometer repositories read their own settings.
 */
class DataStoreSettingsRepositoryTest {

    private val logger = FakeLogger()
    private val dataStore = FakePreferencesDataStore()
    private val repository = DataStoreRoadbookSettingsRepository(dataStore, logger)

    private val fia = RoadbookSettings(style = RoadbookStyle.FIA)

    @Test
    fun settings_nothingStored_emitsTheDefaults() = runTest {
        assertEquals(RoadbookSettings(), repository.settings.first())
    }

    @Test
    fun settings_storedValues_emitsThem() = runTest {
        dataStore.stored.value = preferencesOf(RoadbookSettingsCodec.STYLE_KEY to "FIA")

        assertEquals(fia, repository.settings.first())
    }

    @Test
    fun settings_readFails_emitsTheDefaultsAndLogs() = runTest {
        dataStore.failReads = true

        assertEquals(RoadbookSettings(), repository.settings.first())
        assertEquals(1, logger.loggedErrors.size)
    }

    @Test
    fun update_saves_returnsSavedAndEmitsTheNewSettings() = runTest {
        repository.settings.test {
            assertEquals(RoadbookSettings(), awaitItem())

            val result = repository.update { it.copy(style = RoadbookStyle.FIA) }

            assertEquals(SettingsUpdateResult.SAVED, result)
            assertEquals(fia, awaitItem())
        }
    }

    @Test
    fun update_sameSettings_emitsNothingNew() = runTest {
        repository.settings.test {
            assertEquals(RoadbookSettings(), awaitItem())

            repository.update { it }

            expectNoEvents()
        }
    }

    @Test
    fun update_writeFails_returnsNotSavedKeepsTheSettingsAndLogs() = runTest {
        dataStore.failWrites = true

        val result = repository.update { it.copy(style = RoadbookStyle.FIA) }

        assertEquals(SettingsUpdateResult.NOT_SAVED, result)
        assertEquals(RoadbookSettings(), repository.settings.first())
        assertEquals(1, logger.loggedErrors.size)
    }

    @Test
    fun update_transformGetsTheStoredSettings() = runTest {
        dataStore.stored.value = preferencesOf(RoadbookSettingsCodec.STYLE_KEY to "FIA")
        var received: RoadbookSettings? = null

        repository.update { current -> current.also { received = it } }

        assertEquals(fia, received)
    }

    @Test
    fun appRepository_storedValues_emitsThem() = runTest {
        val store = FakePreferencesDataStore(preferencesOf(AppSettingsCodec.THEME_MODE_KEY to "DARK"))

        val settings = DataStoreAppSettingsRepository(store, logger).settings.first()

        assertEquals(AppSettings(themeMode = ThemeMode.DARK), settings)
    }

    @Test
    fun remoteRepository_storedValues_emitsThem() = runTest {
        val store = FakePreferencesDataStore(
            preferencesOf(RemoteSettingsCodec.MODEL_KEY to "DND2"),
        )

        val settings = DataStoreRemoteSettingsRepository(store, logger).settings.first()

        assertEquals(RemoteSettings(model = RemoteModel.DND2), settings)
    }
}
