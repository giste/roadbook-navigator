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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.giste.geo.Distance
import dev.giste.location.LocationSource
import dev.giste.location.MotionState
import dev.giste.odometer.OdometerController
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** How long the state stays collected after the last collector leaves, e.g. during a rotation. */
private const val STOP_TIMEOUT_MILLIS: Long = 5_000

/**
 * State of the dashboard (the odometer distances and whether importing is allowed), and the
 * odometer commands the rider gives from it.
 *
 * Collecting [uiState] collects [OdometerController.state], which is what makes the odometer
 * measure, and [LocationSource.motion]. The dashboard is only shown behind the permission gate,
 * so both run only once the location permission is granted, and only while the dashboard is
 * visible (plus [STOP_TIMEOUT_MILLIS]).
 *
 * @param odometer the distances, and the commands to change them.
 * @param locationSource whether the vehicle is stopped.
 */
@HiltViewModel
internal class DashboardViewModel @Inject constructor(
    private val odometer: OdometerController,
    locationSource: LocationSource,
) : ViewModel() {

    /** The dashboard state. Starts at [DashboardUiState.INITIAL] and never fails. */
    val uiState: StateFlow<DashboardUiState> = combine(
        odometer.state
            .map<_, OdometerUiState> { OdometerUiState.Available(partial = it.trip, total = it.total) }
            .onStart { emit(OdometerUiState.Loading) },
        locationSource.motion,
    ) { odometerState, motion ->
        // Importing is only offered while stopped, so the rider is not tempted to do it riding.
        DashboardUiState(odometer = odometerState, showImportButton = motion == MotionState.STOPPED)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DashboardUiState.INITIAL,
    )

    /**
     * Sets the partial (the odometer's trip distance) to [partial]. The new value arrives through
     * [uiState].
     *
     * @param partial the new partial.
     */
    fun setPartial(partial: Distance) {
        viewModelScope.launch { odometer.setTrip(partial) }
    }

    /** Resets the partial and the total to zero. The new values arrive through [uiState]. */
    fun resetAll() {
        viewModelScope.launch { odometer.resetAll() }
    }
}
