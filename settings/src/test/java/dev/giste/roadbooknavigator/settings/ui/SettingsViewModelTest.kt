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

package dev.giste.roadbooknavigator.settings.ui

import app.cash.turbine.test
import dev.giste.geo.Distance
import dev.giste.roadbook.RoadbookSettings
import dev.giste.roadbook.RoadbookStyle
import dev.giste.roadbook.ShortDistanceThreshold
import dev.giste.roadbooknavigator.settings.AppSettings
import dev.giste.roadbooknavigator.settings.FakeSettingsRepository
import dev.giste.roadbooknavigator.settings.MainDispatcherRule
import dev.giste.roadbooknavigator.settings.ScreenOrientation
import dev.giste.roadbooknavigator.settings.ThemeMode
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val app = FakeSettingsRepository(AppSettings())
    private val roadbook = FakeSettingsRepository(RoadbookSettings())

    // Created in each test, after the rule has replaced Main: viewModelScope takes Main's
    // immediate dispatcher when the ViewModel is created.
    private val viewModel by lazy { SettingsViewModel(app, roadbook) }

    private val defaults = SettingsUiState.Loaded(
        themeMode = ThemeMode.SYSTEM,
        orientation = ScreenOrientation.FOLLOW_SYSTEM,
        roadbookStyle = RoadbookStyle.APP_THEME,
        shortDistanceThreshold = ShortDistanceThreshold.DEFAULT,
        canDecreaseThreshold = true,
        canIncreaseThreshold = true,
        saveFailed = false,
    )

    @Test
    fun uiState_beforeCollecting_isLoading() {
        assertEquals(SettingsUiState.Loading, viewModel.uiState.value)
    }

    @Test
    fun uiState_collected_showsTheStoredSettings() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            assertEquals(defaults, awaitItem())
        }
    }

    @Test
    fun uiState_shortestThreshold_cannotDecrease() = runTest {
        roadbook.settings.value = RoadbookSettings(shortDistanceThreshold = ShortDistanceThreshold.MIN)

        viewModel.uiState.test {
            skipItems(1)
            val state = awaitItem() as SettingsUiState.Loaded

            assertEquals(false, state.canDecreaseThreshold)
            assertEquals(true, state.canIncreaseThreshold)
        }
    }

    @Test
    fun uiState_longestThreshold_cannotIncrease() = runTest {
        roadbook.settings.value = RoadbookSettings(shortDistanceThreshold = ShortDistanceThreshold.MAX)

        viewModel.uiState.test {
            skipItems(1)
            val state = awaitItem() as SettingsUiState.Loaded

            assertEquals(true, state.canDecreaseThreshold)
            assertEquals(false, state.canIncreaseThreshold)
        }
    }

    @Test
    fun setThemeMode_storesIt() = runTest {
        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, app.settings.value.themeMode)
    }

    @Test
    fun uiState_storedOrientation_showsIt() = runTest {
        app.settings.value = AppSettings(orientation = ScreenOrientation.VERTICAL)

        viewModel.uiState.test {
            skipItems(1)

            assertEquals(defaults.copy(orientation = ScreenOrientation.VERTICAL), awaitItem())
        }
    }

    @Test
    fun setOrientation_storesIt() = runTest {
        viewModel.setOrientation(ScreenOrientation.HORIZONTAL)
        advanceUntilIdle()

        assertEquals(ScreenOrientation.HORIZONTAL, app.settings.value.orientation)
    }

    @Test
    fun setRoadbookStyle_storesIt() = runTest {
        viewModel.setRoadbookStyle(RoadbookStyle.FIA)
        advanceUntilIdle()

        assertEquals(RoadbookStyle.FIA, roadbook.settings.value.style)
    }

    @Test
    fun increaseShortDistanceThreshold_storesOneStepLonger() = runTest {
        viewModel.increaseShortDistanceThreshold()
        advanceUntilIdle()

        assertEquals(Distance.meters(300.0), roadbook.settings.value.shortDistanceThreshold.distance)
    }

    @Test
    fun decreaseShortDistanceThreshold_storesOneStepShorter() = runTest {
        viewModel.decreaseShortDistanceThreshold()
        advanceUntilIdle()

        assertEquals(Distance.meters(200.0), roadbook.settings.value.shortDistanceThreshold.distance)
    }

    @Test
    fun increaseShortDistanceThreshold_twiceQuickly_storesTwoSteps() = runTest {
        viewModel.increaseShortDistanceThreshold()
        viewModel.increaseShortDistanceThreshold()
        advanceUntilIdle()

        assertEquals(Distance.meters(350.0), roadbook.settings.value.shortDistanceThreshold.distance)
    }

    @Test
    fun change_notSaved_showsTheSaveError() = runTest {
        app.failUpdates = true

        viewModel.uiState.test {
            skipItems(2)

            viewModel.setThemeMode(ThemeMode.DARK)

            assertEquals(defaults.copy(saveFailed = true), awaitItem())
        }
    }

    @Test
    fun dismissSaveError_hidesIt() = runTest {
        roadbook.failUpdates = true

        viewModel.uiState.test {
            skipItems(2)
            viewModel.setRoadbookStyle(RoadbookStyle.FIA)
            skipItems(1)

            viewModel.dismissSaveError()

            assertEquals(defaults, awaitItem())
        }
    }
}
