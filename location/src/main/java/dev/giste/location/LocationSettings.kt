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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * How often [LocationSource] asks the platform for location fixes.
 *
 * Both values are hints: the platform may deliver fixes less often, e.g. without a GPS signal.
 *
 * @property updateInterval desired time between fixes. Defaults to [DEFAULT_UPDATE_INTERVAL].
 * Shorter intervals track the route more closely but use more battery.
 * @property minUpdateDistance minimum movement between fixes: a fix closer than this to the
 * previous one is not delivered. Defaults to [DEFAULT_MIN_UPDATE_DISTANCE]. [Distance.ZERO]
 * delivers every fix.
 * @throws IllegalArgumentException if [updateInterval] is not positive or is infinite.
 */
public data class LocationSettings(
    public val updateInterval: Duration = DEFAULT_UPDATE_INTERVAL,
    public val minUpdateDistance: Distance = DEFAULT_MIN_UPDATE_DISTANCE,
) {

    init {
        require(updateInterval.isPositive() && updateInterval.isFinite()) {
            "The update interval must be positive and finite, but was $updateInterval"
        }
    }

    /** Default values of [LocationSettings]. */
    public companion object {

        /** Default [updateInterval]: half a second. */
        public val DEFAULT_UPDATE_INTERVAL: Duration = 500.milliseconds

        /** Default [minUpdateDistance]: 2 meters, about the noise of a good GPS fix. */
        public val DEFAULT_MIN_UPDATE_DISTANCE: Distance = Distance.meters(2.0)
    }
}
