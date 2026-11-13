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

/** Reads location events from the platform with a fixed set of [LocationSettings]. */
internal interface LocationRepository {

    /**
     * Returns a cold flow of location events.
     *
     * Each collection registers its own platform request with [settings], and cancelling the
     * collection removes it. The flow never throws: if the request cannot be started, it emits
     * one [LocationEvent.Error] and completes.
     *
     * @param settings the interval and minimum distance of the request.
     * @return the events of this request.
     */
    fun events(settings: LocationSettings): Flow<LocationEvent>
}
