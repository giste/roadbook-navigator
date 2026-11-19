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

package dev.giste.roadbooknavigator

import dev.giste.geo.Distance
import dev.giste.odometer.OdometerController
import dev.giste.odometer.OdometerState
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * [OdometerController] that records every command in [calls], and whose state the test emits with
 * [emit]. Like the real one, a new collector first receives the current state. It does not change
 * its state on commands: tests check the calls instead.
 */
internal class FakeOdometerController : OdometerController {

    private val mutableState = MutableSharedFlow<OdometerState>(replay = 1)

    /** Every command received, in order, e.g. `"resetAll"` or `"setTrip 1200.0 m"`. */
    val calls: MutableList<String> = mutableListOf()

    override val state: MutableSharedFlow<OdometerState> = mutableState

    /** Sends [state] to the current collectors, and keeps it for the next ones. */
    suspend fun emit(state: OdometerState) {
        mutableState.emit(state)
    }

    override suspend fun resetTrip() {
        calls += "resetTrip"
    }

    override suspend fun resetAll() {
        calls += "resetAll"
    }

    override suspend fun setTrip(distance: Distance) {
        calls += "setTrip ${distance.inMeters} m"
    }

    override suspend fun increaseTrip(by: Distance) {
        calls += "increaseTrip ${by.inMeters} m"
    }

    override suspend fun decreaseTrip(by: Distance) {
        calls += "decreaseTrip ${by.inMeters} m"
    }
}
