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

package dev.giste.roadbooknavigator.ui.dashboard

import android.view.KeyEvent
import app.cash.turbine.test
import dev.giste.geo.Distance
import dev.giste.location.MotionState
import dev.giste.odometer.OdometerState
import dev.giste.roadbooknavigator.FakeLocationSource
import dev.giste.roadbooknavigator.FakeOdometerController
import dev.giste.roadbooknavigator.FakeRemoteSettingsSource
import dev.giste.roadbooknavigator.FakeRoadbookController
import dev.giste.roadbooknavigator.MainDispatcherRule
import dev.giste.roadbooknavigator.domain.KeyPress
import dev.giste.roadbooknavigator.domain.KeyPressType
import dev.giste.roadbooknavigator.domain.RESET_HOLD_MILLIS
import dev.giste.roadbooknavigator.settings.RemoteKey
import dev.giste.roadbooknavigator.settings.RemoteModel
import dev.giste.roadbooknavigator.settings.RemoteSettings
import dev.giste.roadbooknavigator.settings.RemoteSettingsSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val odometer = FakeOdometerController()
    private val roadbook = FakeRoadbookController()
    private val locationSource = FakeLocationSource()
    private val remoteSettings = FakeRemoteSettingsSource()
    // Created in each test, after the rule has replaced Main: viewModelScope takes Main's
    // immediate dispatcher when the ViewModel is created.
    private val viewModel by lazy { DashboardViewModel(odometer, roadbook, locationSource, remoteSettings) }

    private val distances = OdometerState(trip = Distance.meters(1_200.0), total = Distance.meters(9_000.0))

    @Test
    fun uiState_beforeAnything_isLoadingAndShowsImport() = runTest {
        viewModel.uiState.test {
            assertEquals(DashboardUiState.INITIAL, awaitItem())
        }
    }

    @Test
    fun uiState_odometerEmits_showsTripAsPartial() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            odometer.emit(distances)

            assertEquals(
                OdometerUiState.Available(partial = distances.trip, total = distances.total),
                awaitItem().odometer,
            )
        }
    }

    @Test
    fun uiState_moving_hidesImport() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            locationSource.motion.value = MotionState.MOVING

            assertEquals(DashboardUiState(OdometerUiState.Loading, showStoppedActions = false), awaitItem())
        }
    }

    @Test
    fun uiState_stoppedAfterMoving_showsImportAgain() = runTest {
        viewModel.uiState.test {
            skipItems(1)
            locationSource.motion.value = MotionState.MOVING
            skipItems(1)

            locationSource.motion.value = MotionState.STOPPED

            assertEquals(true, awaitItem().showStoppedActions)
        }
    }

    @Test
    fun setPartial_setsTheOdometerTrip() = runTest {
        viewModel.setPartial(Distance.meters(12_350.0))
        advanceUntilIdle()

        assertEquals(listOf("setTrip 12350.0 m"), odometer.calls)
    }

    @Test
    fun resetAll_resetsTheOdometer() = runTest {
        viewModel.resetAll()
        advanceUntilIdle()

        assertEquals(listOf("resetAll"), odometer.calls)
    }

    /** A [KeyPressType.DOWN] of [keyCode], after the ViewModel has read the remote settings. */
    private fun TestScope.pressKey(keyCode: Int, repeatCount: Int = 0, atMillis: Long = 0): Boolean {
        // Creates the ViewModel, if not yet, and lets it read the settings.
        val viewModel = viewModel
        advanceUntilIdle()
        val consumed = viewModel.onKeyPress(KeyPress(RemoteKey(keyCode), KeyPressType.DOWN, repeatCount, atMillis))
        advanceUntilIdle()
        return consumed
    }

    /** A [KeyPressType.UP] of [keyCode] at [atMillis]. */
    private fun TestScope.releaseKey(keyCode: Int, atMillis: Long = 100): Boolean {
        val consumed = viewModel.onKeyPress(KeyPress(RemoteKey(keyCode), KeyPressType.UP, 0, atMillis))
        advanceUntilIdle()
        return consumed
    }

    @Test
    fun onKeyPress_nextWaypointKey_movesToTheNextWaypoint() = runTest {
        assertTrue(pressKey(KeyEvent.KEYCODE_DPAD_UP))

        assertEquals(listOf("moveToNextWaypoint"), roadbook.calls)
    }

    @Test
    fun onKeyPress_previousWaypointKey_movesToThePreviousWaypoint() = runTest {
        assertTrue(pressKey(KeyEvent.KEYCODE_DPAD_DOWN))

        assertEquals(listOf("moveToPreviousWaypoint"), roadbook.calls)
    }

    @Test
    fun onKeyPress_increasePartialKey_increasesThePartialByTheStep() = runTest {
        assertTrue(pressKey(KeyEvent.KEYCODE_DPAD_RIGHT))

        assertEquals(listOf("increaseTrip 10.0 m"), odometer.calls)
    }

    @Test
    fun onKeyPress_decreasePartialKey_decreasesThePartialByTheStep() = runTest {
        assertTrue(pressKey(KeyEvent.KEYCODE_DPAD_LEFT))

        assertEquals(listOf("decreaseTrip 10.0 m"), odometer.calls)
    }

    @Test
    fun onKeyPress_partialKeyRepeated_changesThePartialOnEveryRepeat() = runTest {
        pressKey(KeyEvent.KEYCODE_DPAD_RIGHT)
        pressKey(KeyEvent.KEYCODE_DPAD_RIGHT, repeatCount = 1, atMillis = 500)

        assertEquals(listOf("increaseTrip 10.0 m", "increaseTrip 10.0 m"), odometer.calls)
    }

    @Test
    fun onKeyPress_resetKeyHeld_resetsThePartial() = runTest {
        pressKey(KeyEvent.KEYCODE_F6)

        assertTrue(pressKey(KeyEvent.KEYCODE_F6, repeatCount = 10, atMillis = RESET_HOLD_MILLIS))

        assertEquals(listOf("resetTrip"), odometer.calls)
    }

    @Test
    fun onKeyPress_resetKeyTapped_isConsumedAndDoesNothing() = runTest {
        assertTrue(pressKey(KeyEvent.KEYCODE_F6))
        assertTrue(releaseKey(KeyEvent.KEYCODE_F6))

        assertEquals(emptyList<String>(), odometer.calls)
    }

    @Test
    fun onKeyPress_boundKeyReleased_isConsumedAndDoesNothing() = runTest {
        pressKey(KeyEvent.KEYCODE_DPAD_UP)

        assertTrue(releaseKey(KeyEvent.KEYCODE_DPAD_UP))

        assertEquals(listOf("moveToNextWaypoint"), roadbook.calls)
    }

    @Test
    fun onKeyPress_unboundKey_isNotConsumedAndDoesNothing() = runTest {
        assertFalse(pressKey(KeyEvent.KEYCODE_VOLUME_UP))

        assertEquals(emptyList<String>(), odometer.calls)
        assertEquals(emptyList<String>(), roadbook.calls)
    }

    @Test
    fun onKeyPress_otherModelChosen_followsItsKeys() = runTest {
        remoteSettings.settings.value = RemoteSettings(model = RemoteModel.TERRA_PIRATA)

        assertTrue(pressKey(KeyEvent.KEYCODE_VOLUME_UP))
        assertFalse(pressKey(KeyEvent.KEYCODE_DPAD_RIGHT))

        assertEquals(listOf("increaseTrip 10.0 m"), odometer.calls)
    }

    @Test
    fun onKeyPress_settingsNotReadYet_isNotConsumed() = runTest {
        val unread = object : RemoteSettingsSource {
            override val settings: Flow<RemoteSettings> = emptyFlow()
        }
        val viewModel = DashboardViewModel(odometer, roadbook, locationSource, unread)
        advanceUntilIdle()

        val consumed = viewModel.onKeyPress(KeyPress(RemoteKey(KeyEvent.KEYCODE_DPAD_RIGHT), KeyPressType.DOWN, 0, 0))
        advanceUntilIdle()

        assertFalse(consumed)
        assertEquals(emptyList<String>(), odometer.calls)
    }
}
