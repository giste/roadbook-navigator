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

package dev.giste.roadbook.domain.roadbook

import dev.giste.roadbook.RoadbookEvent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * The one channel every [RoadbookEvent] goes through, whoever emits it: the controller (a roadbook
 * was loaded) or the screen (the user asked to set the partial). `RoadbookController.events`
 * exposes it to the app.
 *
 * It has no replay and no buffer: an event only reaches the collectors present when it is
 * emitted, and [emit] waits until they have all taken it. Without collectors, events are dropped.
 *
 * It is a singleton, so every emitter and collector share it.
 */
@Singleton
internal class RoadbookEvents @Inject constructor() {

    private val mutableEvents = MutableSharedFlow<RoadbookEvent>()

    /** Every event, as it is emitted. Hot, never completes. */
    val events: SharedFlow<RoadbookEvent> = mutableEvents.asSharedFlow()

    /**
     * Sends [event] to the collectors of [events].
     *
     * @param event what happened.
     */
    suspend fun emit(event: RoadbookEvent) {
        mutableEvents.emit(event)
    }
}
