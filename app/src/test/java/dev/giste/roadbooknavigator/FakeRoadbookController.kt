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

import android.net.Uri
import dev.giste.roadbook.ImportRoadbookResult
import dev.giste.roadbook.RoadbookController
import dev.giste.roadbook.RoadbookEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * [RoadbookController] whose events the test emits with [emit], and that records the waypoint
 * moves in [calls]. Imports always succeed.
 */
internal class FakeRoadbookController : RoadbookController {

    /** The waypoint moves received, in order, e.g. `"moveToNextWaypoint"`. */
    val calls: MutableList<String> = mutableListOf()

    private val mutableEvents = MutableSharedFlow<RoadbookEvent>()

    override val events: SharedFlow<RoadbookEvent> = mutableEvents

    /** Sends [event] to the current collectors, waiting until they all received it. */
    suspend fun emit(event: RoadbookEvent) {
        mutableEvents.emit(event)
    }

    override suspend fun importRoadbook(uri: Uri): ImportRoadbookResult = ImportRoadbookResult.Success

    override suspend fun moveToNextWaypoint() {
        calls += "moveToNextWaypoint"
    }

    override suspend fun moveToPreviousWaypoint() {
        calls += "moveToPreviousWaypoint"
    }
}
