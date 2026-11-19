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

import dev.giste.geo.Speed
import dev.giste.location.LocationEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.scan

/** Above this speed the vehicle is moving. */
internal val MOVING_SPEED: Speed = Speed.kilometersPerHour(10.0)

/** Below this speed a moving vehicle is stopped again. */
internal val STOPPED_SPEED: Speed = Speed.kilometersPerHour(5.0)

/**
 * Whether the vehicle is stopped, after every location event: `true` first, then a value after
 * each change.
 *
 * Two thresholds with a gap between them (hysteresis) keep GPS jitter around a single threshold
 * from flipping the answer: the vehicle only starts moving above [MOVING_SPEED], and only stops
 * again below [STOPPED_SPEED]. Between them it keeps its previous answer. See [isStoppedAfter]
 * for each event.
 *
 * It is cold and does not throw unless the upstream does.
 *
 * @return `true` while stopped, `false` while moving, without repeated values.
 */
internal fun Flow<LocationEvent>.stoppedStates(): Flow<Boolean> =
    scan(true) { stopped, event -> isStoppedAfter(stopped, event) }
        .distinctUntilChanged()

/**
 * Whether the vehicle is stopped after [event], given whether it was [stopped] before.
 *
 * Without a known speed it counts as stopped, so the user is never locked out of a stopped-only
 * action by a missing value:
 *
 * - A fix without a speed, a disabled provider and a location error mean stopped.
 * - A fix above [MOVING_SPEED] means moving, below [STOPPED_SPEED] stopped, and in between (both
 *   limits included) no change.
 * - [LocationEvent.ProviderEnabled] brings no speed yet, so nothing changes.
 *
 * @param stopped whether the vehicle was stopped before [event].
 * @param event the new location event.
 * @return whether the vehicle is stopped after [event].
 */
internal fun isStoppedAfter(stopped: Boolean, event: LocationEvent): Boolean = when (event) {
    is LocationEvent.LocationUpdated -> {
        val speed = event.location.speed
        when {
            speed == null -> true
            speed > MOVING_SPEED -> false
            speed < STOPPED_SPEED -> true
            else -> stopped
        }
    }
    LocationEvent.ProviderEnabled -> stopped
    LocationEvent.ProviderDisabled -> true
    is LocationEvent.Error -> true
}
