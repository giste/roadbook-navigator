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

package dev.giste.roadbooknavigator.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.giste.location.LocationSource
import dev.giste.odometer.OdometerController
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

/** How long the state stays collected after the last collector leaves, e.g. during a rotation. */
private const val STOP_TIMEOUT_MILLIS: Long = 5_000

/**
 * State of the dashboard: the odometer distances, and whether importing is allowed.
 *
 * Collecting [uiState] collects [OdometerController.state], which is what makes the odometer
 * measure, and [LocationSource.events]. The dashboard is only shown behind the permission gate,
 * so both run only once the location permission is granted, and only while the dashboard is
 * visible (plus [STOP_TIMEOUT_MILLIS]).
 *
 * @param odometer the distances.
 * @param locationSource the speed, to know whether the vehicle is stopped.
 */
@HiltViewModel
internal class DashboardViewModel @Inject constructor(
    odometer: OdometerController,
    locationSource: LocationSource,
) : ViewModel() {

    /** The dashboard state. Starts at [DashboardUiState.INITIAL] and never fails. */
    val uiState: StateFlow<DashboardUiState> = combine(
        odometer.state
            .map<_, OdometerUiState> { OdometerUiState.Available(partial = it.trip, total = it.total) }
            .onStart { emit(OdometerUiState.Loading) },
        locationSource.events.stoppedStates(),
    ) { odometerState, stopped ->
        DashboardUiState(odometer = odometerState, showImportButton = stopped)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = DashboardUiState.INITIAL,
    )
}
