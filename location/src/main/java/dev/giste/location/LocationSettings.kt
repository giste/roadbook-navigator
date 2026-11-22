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

package dev.giste.location

import dev.giste.geo.Distance
import dev.giste.geo.Speed
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * How often [LocationSource] asks the platform for location fixes, and when [LocationSource.motion]
 * counts the device as moving.
 *
 * [updateInterval] and [minUpdateDistance] are hints: the platform may deliver fixes less often,
 * e.g. without a GPS signal.
 *
 * @property updateInterval desired time between fixes. Defaults to [DEFAULT_UPDATE_INTERVAL].
 * Shorter intervals track the route more closely but use more battery.
 * @property minUpdateDistance minimum movement between fixes: a fix closer than this to the
 * previous one is not delivered. Defaults to [DEFAULT_MIN_UPDATE_DISTANCE]. [Distance.ZERO]
 * delivers every fix.
 * @property movingSpeed above this speed, a stopped device starts moving. Defaults to
 * [DEFAULT_MOVING_SPEED].
 * @property stoppedSpeed below this speed, a moving device is stopped again. Defaults to
 * [DEFAULT_STOPPED_SPEED]. The gap between both speeds keeps GPS jitter around a single speed from
 * flipping the state. An equal pair is allowed, and has no gap.
 * @throws IllegalArgumentException if [updateInterval] is not positive or is infinite, or if
 * [movingSpeed] is lower than [stoppedSpeed].
 */
public data class LocationSettings(
    public val updateInterval: Duration = DEFAULT_UPDATE_INTERVAL,
    public val minUpdateDistance: Distance = DEFAULT_MIN_UPDATE_DISTANCE,
    public val movingSpeed: Speed = DEFAULT_MOVING_SPEED,
    public val stoppedSpeed: Speed = DEFAULT_STOPPED_SPEED,
) {

    init {
        require(updateInterval.isPositive() && updateInterval.isFinite()) {
            "The update interval must be positive and finite, but was $updateInterval"
        }
        require(movingSpeed >= stoppedSpeed) {
            "The moving speed ($movingSpeed) must not be lower than the stopped speed ($stoppedSpeed)"
        }
    }

    /** Default values of [LocationSettings]. */
    public companion object {

        /** Default [updateInterval]: half a second. */
        public val DEFAULT_UPDATE_INTERVAL: Duration = 500.milliseconds

        /** Default [minUpdateDistance]: 2 meters, about the noise of a good GPS fix. */
        public val DEFAULT_MIN_UPDATE_DISTANCE: Distance = Distance.meters(2.0)

        /**
         * Default [movingSpeed]: 10 km/h, suited to motor vehicles. Apps for slower movement, such
         * as walking, should set their own.
         */
        public val DEFAULT_MOVING_SPEED: Speed = Speed.kilometersPerHour(10.0)

        /** Default [stoppedSpeed]: 5 km/h, suited to motor vehicles, like [DEFAULT_MOVING_SPEED]. */
        public val DEFAULT_STOPPED_SPEED: Speed = Speed.kilometersPerHour(5.0)
    }
}
