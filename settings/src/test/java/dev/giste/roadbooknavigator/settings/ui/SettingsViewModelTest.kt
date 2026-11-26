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

import android.view.KeyEvent
import app.cash.turbine.test
import dev.giste.geo.Distance
import dev.giste.map.MapSettings
import dev.giste.map.MapTilt
import dev.giste.map.MapZoomLevel
import dev.giste.roadbook.RoadbookSettings
import dev.giste.roadbook.RoadbookStyle
import dev.giste.roadbook.ShortDistanceThreshold
import dev.giste.roadbooknavigator.settings.AppSettings
import dev.giste.roadbooknavigator.settings.FakeSettingsRepository
import dev.giste.roadbooknavigator.settings.MainDispatcherRule
import dev.giste.roadbooknavigator.settings.RemoteAction
import dev.giste.roadbooknavigator.settings.RemoteKey
import dev.giste.roadbooknavigator.settings.RemoteKeys
import dev.giste.roadbooknavigator.settings.RemoteModel
import dev.giste.roadbooknavigator.settings.RemoteSettings
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
    private val remote = FakeSettingsRepository(RemoteSettings())
    private val roadbook = FakeSettingsRepository(RoadbookSettings())
    private val map = FakeSettingsRepository(MapSettings())

    // Created in each test, after the rule has replaced Main: viewModelScope takes Main's
    // immediate dispatcher when the ViewModel is created.
    private val viewModel by lazy { SettingsViewModel(app, remote, roadbook, map) }

    private val defaults = SettingsUiState.Loaded(
        themeMode = ThemeMode.SYSTEM,
        orientation = ScreenOrientation.FOLLOW_SYSTEM,
        remoteModel = RemoteModel.TERRA_PIRATA,
        remoteKeys = RemoteKeys.TERRA_PIRATA,
        roadbookStyle = RoadbookStyle.APP_THEME,
        shortDistanceThreshold = ShortDistanceThreshold.DEFAULT,
        canDecreaseShortDistanceThreshold = true,
        canIncreaseShortDistanceThreshold = true,
        mapZoomLevel = MapZoomLevel.DEFAULT,
        canDecreaseMapZoom = true,
        canIncreaseMapZoom = true,
        mapTilt = MapTilt.DEFAULT,
        canDecreaseMapTilt = true,
        canIncreaseMapTilt = false,
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

            assertEquals(false, state.canDecreaseShortDistanceThreshold)
            assertEquals(true, state.canIncreaseShortDistanceThreshold)
        }
    }

    @Test
    fun uiState_longestThreshold_cannotIncrease() = runTest {
        roadbook.settings.value = RoadbookSettings(shortDistanceThreshold = ShortDistanceThreshold.MAX)

        viewModel.uiState.test {
            skipItems(1)
            val state = awaitItem() as SettingsUiState.Loaded

            assertEquals(true, state.canDecreaseShortDistanceThreshold)
            assertEquals(false, state.canIncreaseShortDistanceThreshold)
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
    fun uiState_dnd2_showsItsKeys() = runTest {
        remote.settings.value = RemoteSettings(model = RemoteModel.DND2)

        viewModel.uiState.test {
            skipItems(1)

            assertEquals(
                defaults.copy(
                    remoteModel = RemoteModel.DND2,
                    remoteKeys = RemoteKeys.DND2,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun uiState_custom_showsTheCustomKeys() = runTest {
        val custom = RemoteKeys(emptyMap())
        remote.settings.value = RemoteSettings(model = RemoteModel.CUSTOM, customKeys = custom)

        viewModel.uiState.test {
            skipItems(1)

            assertEquals(
                defaults.copy(remoteModel = RemoteModel.CUSTOM, remoteKeys = custom),
                awaitItem(),
            )
        }
    }

    @Test
    fun setRemoteModel_storesItAndKeepsTheCustomKeys() = runTest {
        val custom = RemoteKeys(emptyMap())
        remote.settings.value = RemoteSettings(model = RemoteModel.CUSTOM, customKeys = custom)

        viewModel.setRemoteModel(RemoteModel.TERRA_PIRATA)
        advanceUntilIdle()

        assertEquals(RemoteSettings(RemoteModel.TERRA_PIRATA, custom), remote.settings.value)
    }

    @Test
    fun setRemoteModel_notSaved_showsTheSaveError() = runTest {
        remote.failUpdates = true

        viewModel.uiState.test {
            skipItems(2)

            viewModel.setRemoteModel(RemoteModel.CUSTOM)

            assertEquals(defaults.copy(saveFailed = true), awaitItem())
        }
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

    // --- Map ---

    @Test
    fun uiState_farthestOfferedZoom_cannotDecrease() = runTest {
        map.settings.value = MapSettings(zoomLevel = MapZoomLevel.of(15))

        viewModel.uiState.test {
            skipItems(1)
            val state = awaitItem() as SettingsUiState.Loaded

            assertEquals(MapZoomLevel.of(15), state.mapZoomLevel)
            assertEquals(false, state.canDecreaseMapZoom)
            assertEquals(true, state.canIncreaseMapZoom)
        }
    }

    @Test
    fun uiState_closestOfferedZoom_cannotIncrease() = runTest {
        map.settings.value = MapSettings(zoomLevel = MapZoomLevel.of(20))

        viewModel.uiState.test {
            skipItems(1)
            val state = awaitItem() as SettingsUiState.Loaded

            assertEquals(true, state.canDecreaseMapZoom)
            assertEquals(false, state.canIncreaseMapZoom)
        }
    }

    @Test
    fun uiState_flatMap_cannotDecreaseTilt() = runTest {
        map.settings.value = MapSettings(tilt = MapTilt.of(0.0))

        viewModel.uiState.test {
            skipItems(1)
            val state = awaitItem() as SettingsUiState.Loaded

            assertEquals(MapTilt.of(0.0), state.mapTilt)
            assertEquals(false, state.canDecreaseMapTilt)
            assertEquals(true, state.canIncreaseMapTilt)
        }
    }

    @Test
    fun uiState_mapSettingsChange_showsThem() = runTest {
        viewModel.uiState.test {
            skipItems(2)

            map.settings.value = MapSettings(zoomLevel = MapZoomLevel.of(16), tilt = MapTilt.of(30.0))

            assertEquals(
                defaults.copy(mapZoomLevel = MapZoomLevel.of(16), mapTilt = MapTilt.of(30.0), canIncreaseMapTilt = true),
                awaitItem(),
            )
        }
    }

    @Test
    fun increaseMapZoom_storesOneLevelCloser() = runTest {
        viewModel.increaseMapZoom()
        advanceUntilIdle()

        assertEquals(MapZoomLevel.of(19), map.settings.value.zoomLevel)
    }

    @Test
    fun decreaseMapZoom_storesOneLevelFarther() = runTest {
        viewModel.decreaseMapZoom()
        advanceUntilIdle()

        assertEquals(MapZoomLevel.of(17), map.settings.value.zoomLevel)
    }

    @Test
    fun increaseMapZoom_twiceQuickly_storesTwoLevels() = runTest {
        viewModel.increaseMapZoom()
        viewModel.increaseMapZoom()
        advanceUntilIdle()

        assertEquals(MapZoomLevel.of(20), map.settings.value.zoomLevel)
    }

    @Test
    fun decreaseMapTilt_storesOneStepFlatter() = runTest {
        viewModel.decreaseMapTilt()
        advanceUntilIdle()

        assertEquals(MapTilt.of(55.0), map.settings.value.tilt)
    }

    @Test
    fun increaseMapTilt_storesOneStepMore() = runTest {
        map.settings.value = MapSettings(tilt = MapTilt.of(30.0))

        viewModel.increaseMapTilt()
        advanceUntilIdle()

        assertEquals(MapTilt.of(35.0), map.settings.value.tilt)
    }

    @Test
    fun increaseMapTilt_atTheMost_keepsIt() = runTest {
        viewModel.increaseMapTilt()
        advanceUntilIdle()

        assertEquals(MapTilt.of(60.0), map.settings.value.tilt)
    }

    @Test
    fun mapChange_notSaved_showsTheSaveError() = runTest {
        map.failUpdates = true

        viewModel.uiState.test {
            skipItems(2)

            viewModel.increaseMapZoom()

            assertEquals(defaults.copy(saveFailed = true), awaitItem())
        }
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

    private val f7 = RemoteKey(KeyEvent.KEYCODE_F7)
    private val nextTrack = RemoteKey(KeyEvent.KEYCODE_MEDIA_NEXT)

    /** Selects the custom remote, with Terra Pirata's keys, as it starts. */
    private fun useCustomRemote() {
        remote.settings.value = RemoteSettings(model = RemoteModel.CUSTOM)
    }

    @Test
    fun assignCustomKey_newKey_storesItAsTheActionsOnlyKey() = runTest {
        useCustomRemote()

        viewModel.assignCustomKey(RemoteAction.RESET_PARTIAL, f7)
        advanceUntilIdle()

        assertEquals(setOf(f7), remote.settings.value.customKeys.keysFor(RemoteAction.RESET_PARTIAL))
    }

    @Test
    fun assignCustomKey_newKey_showsNoMove() = runTest {
        useCustomRemote()

        viewModel.uiState.test {
            skipItems(2)

            viewModel.assignCustomKey(RemoteAction.RESET_PARTIAL, f7)

            assertEquals(null, (awaitItem() as SettingsUiState.Loaded).keyMoved)
        }
    }

    @Test
    fun assignCustomKey_keyOfAnotherAction_movesItAndTellsWhich() = runTest {
        useCustomRemote()

        viewModel.uiState.test {
            skipItems(2)

            viewModel.assignCustomKey(RemoteAction.RESET_PARTIAL, nextTrack)

            advanceUntilIdle()
            val state = expectMostRecentItem() as SettingsUiState.Loaded
            assertEquals(KeyMove(nextTrack, RemoteAction.NEXT_WAYPOINT, RemoteAction.RESET_PARTIAL), state.keyMoved)
            assertEquals(emptySet<RemoteKey>(), state.remoteKeys.keysFor(RemoteAction.NEXT_WAYPOINT))
            assertEquals(setOf(nextTrack), state.remoteKeys.keysFor(RemoteAction.RESET_PARTIAL))
        }
    }

    @Test
    fun assignCustomKey_itsOwnKey_showsNoMove() = runTest {
        useCustomRemote()

        viewModel.assignCustomKey(RemoteAction.NEXT_WAYPOINT, nextTrack)
        advanceUntilIdle()

        viewModel.uiState.test {
            skipItems(1)
            assertEquals(null, (awaitItem() as SettingsUiState.Loaded).keyMoved)
        }
        assertEquals(RemoteKeys.TERRA_PIRATA, remote.settings.value.customKeys)
    }

    @Test
    fun dismissKeyMoved_hidesTheMove() = runTest {
        useCustomRemote()

        viewModel.uiState.test {
            skipItems(2)
            viewModel.assignCustomKey(RemoteAction.RESET_PARTIAL, nextTrack)
            advanceUntilIdle()
            expectMostRecentItem()

            viewModel.dismissKeyMoved()

            assertEquals(null, (awaitItem() as SettingsUiState.Loaded).keyMoved)
        }
    }

    @Test
    fun assignCustomKey_presetInUse_changesNothing() = runTest {
        viewModel.assignCustomKey(RemoteAction.RESET_PARTIAL, nextTrack)
        advanceUntilIdle()

        assertEquals(RemoteSettings(), remote.settings.value)
    }

    @Test
    fun assignCustomKey_notSaved_showsTheSaveErrorAndNoMove() = runTest {
        useCustomRemote()
        remote.failUpdates = true

        viewModel.uiState.test {
            skipItems(2)

            viewModel.assignCustomKey(RemoteAction.RESET_PARTIAL, nextTrack)

            val state = awaitItem() as SettingsUiState.Loaded
            assertEquals(true, state.saveFailed)
            assertEquals(null, state.keyMoved)
        }
        assertEquals(RemoteKeys.TERRA_PIRATA, remote.settings.value.customKeys)
    }
}
