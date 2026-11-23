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
import dev.giste.roadbook.RoadbookController
import dev.giste.roadbooknavigator.domain.KeyPress
import dev.giste.roadbooknavigator.domain.RemoteKeyInterpreter
import dev.giste.roadbooknavigator.domain.RemoteKeyOutcome
import dev.giste.roadbooknavigator.settings.RemoteAction
import dev.giste.roadbooknavigator.settings.RemoteKeys
import dev.giste.roadbooknavigator.settings.RemoteSettingsSource
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

/** How much one press of the remote's partial keys changes the partial. */
internal val REMOTE_PARTIAL_STEP: Distance = Distance.meters(10.0)

/** No key bound to any action: the remote keys until the stored ones are read. */
private val NO_REMOTE_KEYS = RemoteKeys(emptyMap())

/**
 * State of the dashboard (the odometer distances and whether importing is allowed), and the
 * commands the rider gives from it: the odometer dialogs, and the keys of a remote control.
 *
 * Collecting [uiState] collects [OdometerController.state], which is what makes the odometer
 * measure, and [LocationSource.motion]. The dashboard is only shown behind the permission gate,
 * so both run only once the location permission is granted, and only while the dashboard is
 * visible (plus [STOP_TIMEOUT_MILLIS]).
 *
 * The remote control's keys are read from the start, and follow every change of the settings.
 *
 * @param odometer the distances, and the commands to change them.
 * @param roadbook the commands to move between waypoints.
 * @param locationSource whether the vehicle is stopped.
 * @param remoteSettings the keys of the remote control.
 */
@HiltViewModel
internal class DashboardViewModel @Inject constructor(
    private val odometer: OdometerController,
    private val roadbook: RoadbookController,
    locationSource: LocationSource,
    remoteSettings: RemoteSettingsSource,
) : ViewModel() {

    /**
     * The keys bound to each remote action, always up to date, because key events must be answered
     * at once. None until the settings are read, so until then every key reaches the system.
     */
    private val remoteKeys: StateFlow<RemoteKeys> = remoteSettings.settings
        .map { it.keys }
        .stateIn(viewModelScope, SharingStarted.Eagerly, NO_REMOTE_KEYS)

    /** Decides what each remote key press does. Called only from the main thread. */
    private val remoteKeyInterpreter = RemoteKeyInterpreter()

    /** The dashboard state. Starts at [DashboardUiState.INITIAL] and never fails. */
    val uiState: StateFlow<DashboardUiState> = combine(
        odometer.state
            .map<_, OdometerUiState> { OdometerUiState.Available(partial = it.trip, total = it.total) }
            .onStart { emit(OdometerUiState.Loading) },
        locationSource.motion,
    ) { odometerState, motion ->
        // Importing and the settings are only offered while stopped, so the rider is not tempted
        // to use them while riding.
        DashboardUiState(
            odometer = odometerState,
            showStoppedActions = motion == MotionState.STOPPED,
        )
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

    /**
     * Handles a key press from a remote control (or a keyboard), following the remote settings: it
     * moves between waypoints, changes the partial by [REMOTE_PARTIAL_STEP], or resets it when the
     * reset key is held (see [RemoteKeyInterpreter]). The new partial arrives through [uiState].
     *
     * Call it from the main thread, for every key event while the dashboard is shown.
     *
     * @param press the key event.
     * @return whether the key is bound to a remote action, so the event is consumed and the system
     * must not act on it (e.g. change the volume), even if nothing happens on this event.
     */
    fun onKeyPress(press: KeyPress): Boolean =
        when (val outcome = remoteKeyInterpreter.interpret(press, remoteKeys.value)) {
            RemoteKeyOutcome.NotBound -> false
            RemoteKeyOutcome.Consumed -> true
            is RemoteKeyOutcome.Perform -> {
                perform(outcome.action)
                true
            }
        }

    /** Performs [action] on the module that owns it. */
    private fun perform(action: RemoteAction) {
        viewModelScope.launch {
            when (action) {
                RemoteAction.NEXT_WAYPOINT -> roadbook.moveToNextWaypoint()
                RemoteAction.PREVIOUS_WAYPOINT -> roadbook.moveToPreviousWaypoint()
                RemoteAction.INCREASE_PARTIAL -> odometer.increaseTrip(REMOTE_PARTIAL_STEP)
                RemoteAction.DECREASE_PARTIAL -> odometer.decreaseTrip(REMOTE_PARTIAL_STEP)
                RemoteAction.RESET_PARTIAL -> odometer.resetTrip()
            }
        }
    }
}
