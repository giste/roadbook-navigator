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
import dev.giste.location.MotionState

/**
 * The motion state after [event], given that it was this one before.
 *
 * This is the rule [dev.giste.location.LocationSource.motion] documents: a fix faster than
 * [LocationSettings.movingSpeed] means moving, one slower than [LocationSettings.stoppedSpeed]
 * means stopped, and in between (both limits included) nothing changes. Without a known speed it
 * counts as stopped: a fix without a speed, a disabled provider and a location error.
 * [LocationEvent.ProviderEnabled] brings no speed yet, so nothing changes.
 *
 * @param event the new location event.
 * @param settings the speeds that decide it.
 * @return the motion state after [event].
 */
internal fun MotionState.after(event: LocationEvent, settings: LocationSettings): MotionState =
    when (event) {
        is LocationEvent.LocationUpdated -> {
            val speed = event.location.speed
            when {
                speed == null -> MotionState.STOPPED
                speed > settings.movingSpeed -> MotionState.MOVING
                speed < settings.stoppedSpeed -> MotionState.STOPPED
                else -> this
            }
        }
        LocationEvent.ProviderEnabled -> this
        LocationEvent.ProviderDisabled -> MotionState.STOPPED
        is LocationEvent.Error -> MotionState.STOPPED
    }
