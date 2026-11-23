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

package dev.giste.roadbooknavigator.adapter

import dev.giste.geo.LocationFix
import dev.giste.location.LocationEvent
import dev.giste.location.LocationSource
import dev.giste.map.MapLocationSource
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

/**
 * Feeds `:map` with the fixes of `:location` (AGENTS.md §4.2).
 *
 * Only [LocationEvent.LocationUpdated] carries a fix. Provider changes and errors are dropped:
 * the map simply stays where it is while there are no fixes. `:location` shares one request among
 * its collectors, so the map and the odometer do not start the GPS twice. Collecting [locations]
 * collects [LocationSource.events], so it must only be collected once the location permission is
 * granted: the map collects it only while it is shown, which the dashboard does behind the
 * permission gate.
 *
 * @param locationSource the device location.
 */
internal class LocationMapLocationSource @Inject constructor(
    private val locationSource: LocationSource,
) : MapLocationSource {

    /** The fixes of [LocationSource.events], in order. It never throws, like its source. */
    override val locations: Flow<LocationFix> = locationSource.events
        .filterIsInstance<LocationEvent.LocationUpdated>()
        .map { it.location }
}
