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

package dev.giste.location.domain

import dev.giste.location.LocationEvent
import dev.giste.location.LocationSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * [LocationRepository] for tests. It records every request and lets the test emit events to
 * every active request.
 */
internal class FakeLocationRepository : LocationRepository {

    /** The settings of every request, in the order they started. */
    val requests: MutableList<LocationSettings> = mutableListOf()

    /** How many requests are being collected right now. */
    var activeRequests: Int = 0
        private set

    private val events = MutableSharedFlow<LocationEvent>()

    /** Sends [event] to every active request. Suspends until all of them received it. */
    suspend fun emit(event: LocationEvent) {
        events.emit(event)
    }

    override fun events(settings: LocationSettings): Flow<LocationEvent> = flow {
        requests += settings
        activeRequests++
        try {
            emitAll(events)
        } finally {
            activeRequests--
        }
    }
}
