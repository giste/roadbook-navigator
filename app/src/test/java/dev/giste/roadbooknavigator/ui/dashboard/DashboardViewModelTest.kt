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

import app.cash.turbine.test
import dev.giste.geo.Distance
import dev.giste.location.MotionState
import dev.giste.odometer.OdometerState
import dev.giste.roadbooknavigator.FakeLocationSource
import dev.giste.roadbooknavigator.FakeOdometerController
import dev.giste.roadbooknavigator.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val odometer = FakeOdometerController()
    private val locationSource = FakeLocationSource()
    // Created in each test, after the rule has replaced Main: viewModelScope takes Main's
    // immediate dispatcher when the ViewModel is created.
    private val viewModel by lazy { DashboardViewModel(odometer, locationSource) }

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
}
