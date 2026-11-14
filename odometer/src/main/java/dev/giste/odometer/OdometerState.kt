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

package dev.giste.odometer

import dev.giste.geo.Distance

/**
 * The distances measured by the odometer.
 *
 * Both distances only grow while the device moves. The [trip] distance can also be reset, set or
 * adjusted through [OdometerController], so it measures a stretch of the route chosen by the
 * user. The [total] distance is only reset with [OdometerController.resetAll].
 *
 * There is no upper limit: the app decides how many digits it shows.
 *
 * @property trip the resettable distance, e.g. since the last reference point of the route.
 * @property total the distance travelled since the last [OdometerController.resetAll].
 */
public data class OdometerState(
    public val trip: Distance,
    public val total: Distance,
) {

    /** Well-known odometer states. */
    public companion object {

        /** Both distances at zero: the state of a new or fully reset odometer. */
        public val ZERO: OdometerState = OdometerState(trip = Distance.ZERO, total = Distance.ZERO)
    }
}
