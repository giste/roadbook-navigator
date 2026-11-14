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
import dev.giste.geo.GeoPoint
import dev.giste.geo.Speed

/**
 * One location fix, with only the values the odometer needs.
 *
 * `:odometer` does not depend on any location provider, so the app maps its own fixes to this
 * type in its [OdometerLocationSource]. Values the provider did not report are `null`, which is
 * different from a real zero (e.g. a stopped vehicle has a speed of [Speed.ZERO]):
 *
 * - A fix with an unknown [horizontalAccuracy] is never counted, because its error is unknown.
 * - A fix with an unknown [speed] is never treated as stopped.
 * - The altitude is only used when [altitudeInMeters] and [verticalAccuracy] are both known.
 *
 * @property position where the device was.
 * @property horizontalAccuracy radius of 68% confidence around [position], or `null` if unknown.
 * Smaller is better.
 * @property altitudeInMeters height above the WGS84 reference ellipsoid, in meters, or `null` if
 * unknown. It is a raw `Double` because it can be negative, and `:geo` has no altitude type yet.
 * @property verticalAccuracy 68% confidence of [altitudeInMeters], or `null` if unknown.
 * @property speed ground speed, or `null` if unknown.
 * @throws IllegalArgumentException if [altitudeInMeters] is not finite.
 */
public data class OdometerLocation(
    public val position: GeoPoint,
    public val horizontalAccuracy: Distance?,
    public val altitudeInMeters: Double?,
    public val verticalAccuracy: Distance?,
    public val speed: Speed?,
) {

    init {
        require(altitudeInMeters?.isFinite() != false) {
            "The altitude must be finite, but was $altitudeInMeters m"
        }
    }
}
