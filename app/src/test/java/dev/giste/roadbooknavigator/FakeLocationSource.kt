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

import dev.giste.location.LocationEvent
import dev.giste.location.LocationSource
import dev.giste.location.MotionState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * [LocationSource] whose events and motion states the test emits with [emit]. Like the real one,
 * both flows are hot, never complete, and give a new collector the latest value. Motion starts as
 * [MotionState.STOPPED]; it is not derived from the events, so tests set each one directly.
 */
internal class FakeLocationSource : LocationSource {

    private val mutableEvents = MutableSharedFlow<LocationEvent>(replay = 1)

    override val events: MutableSharedFlow<LocationEvent> = mutableEvents

    override val motion: MutableStateFlow<MotionState> = MutableStateFlow(MotionState.STOPPED)

    /** Sends [event] to the current collectors, and keeps it for the next ones. */
    suspend fun emit(event: LocationEvent) {
        mutableEvents.emit(event)
    }
}
