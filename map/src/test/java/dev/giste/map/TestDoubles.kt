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

package dev.giste.map

import dev.giste.geo.Bearing
import dev.giste.geo.Distance
import dev.giste.geo.GeoPoint
import dev.giste.geo.LocationFix
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/**
 * [MapLocationSource] for tests: emits the fixes sent to [emit], or throws [failure] once
 * collected, and counts its collectors.
 */
internal class FakeMapLocationSource : MapLocationSource {

    private val fixes = MutableSharedFlow<LocationFix>()

    /** When set, collecting [locations] throws it, as a broken source would. */
    var failure: Throwable? = null

    /** How many collectors [locations] has now. */
    val collectors: Int get() = fixes.subscriptionCount.value

    override val locations: Flow<LocationFix> = flow {
        failure?.let { throw it }
        fixes.collect { emit(it) }
    }

    /** Sends [fix] to the current collectors. */
    suspend fun emit(fix: LocationFix) = fixes.emit(fix)
}

/** [MapSettingsSource] for tests: emits [current] and every change, or throws [failure]. */
internal class FakeMapSettingsSource(initial: MapSettings = MapSettings()) : MapSettingsSource {

    /** The settings to emit. Set it to emit new settings. */
    val current = MutableStateFlow(initial)

    /** When set, collecting [settings] throws it, as a broken source would. */
    var failure: Throwable? = null

    override val settings: Flow<MapSettings> = flow {
        failure?.let { throw it }
        current.collect { emit(it) }
    }
}

/**
 * A fix at [position] with [bearing]. The other values do not matter: the map only uses the
 * position and the bearing.
 */
internal fun testFix(
    position: GeoPoint = GeoPoint(latitude = 40.4168, longitude = -3.7038),
    bearing: Bearing? = null,
): LocationFix = LocationFix(
    position = position,
    horizontalAccuracy = Distance.meters(5.0),
    altitude = null,
    verticalAccuracy = null,
    speed = null,
    bearing = bearing,
    time = Instant.fromEpochMilliseconds(1_000),
    elapsedRealtime = 1.seconds,
)
