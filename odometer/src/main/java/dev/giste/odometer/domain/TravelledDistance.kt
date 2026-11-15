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

package dev.giste.odometer.domain

import dev.giste.geo.Altitude
import dev.giste.geo.Distance
import dev.giste.geo.GeoPoint
import dev.giste.geo.LocationFix
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/** Mean Earth radius in meters (IUGG), the usual radius for the haversine formula. */
private const val EARTH_RADIUS_METERS: Double = 6_371_000.0

/**
 * Distance between [from] and [to] along the Earth's surface, with the haversine formula.
 *
 * It treats the Earth as a sphere, which is off by at most about 0.5%. That is much less than
 * GPS noise over the few meters between two fixes.
 */
internal fun surfaceDistance(from: GeoPoint, to: GeoPoint): Distance {
    val fromLatitude = Math.toRadians(from.latitude)
    val toLatitude = Math.toRadians(to.latitude)
    val deltaLatitude = toLatitude - fromLatitude
    val deltaLongitude = Math.toRadians(to.longitude - from.longitude)

    val halfChordSquared = sin(deltaLatitude / 2) * sin(deltaLatitude / 2) +
        cos(fromLatitude) * cos(toLatitude) * sin(deltaLongitude / 2) * sin(deltaLongitude / 2)
    // Rounding can push the value slightly outside [0, 1] for antipodal points, and
    // sqrt(1 - a) would then be NaN.
    val a = halfChordSquared.coerceIn(0.0, 1.0)
    val centralAngle = 2 * atan2(sqrt(a), sqrt(1 - a))

    return Distance.meters(EARTH_RADIUS_METERS * centralAngle)
}

/**
 * Distance travelled from [from] to [to].
 *
 * It is the [surfaceDistance] between both positions, combined with the climb (up or down) when
 * both fixes know their altitude with a vertical accuracy of [maxVerticalAccuracy] or better. A
 * bad altitude adds more error than it removes, so it is ignored then.
 *
 * @param maxVerticalAccuracy the worst vertical accuracy at which the altitude is still used, or
 * `null` to never use the altitude.
 */
internal fun travelledDistance(
    from: LocationFix,
    to: LocationFix,
    maxVerticalAccuracy: Distance?,
): Distance {
    val horizontal = surfaceDistance(from.position, to.position)
    if (maxVerticalAccuracy == null) return horizontal
    val climb = climb(from, to, maxVerticalAccuracy) ?: return horizontal
    return Distance.meters(hypot(horizontal.inMeters, climb.inMeters))
}

/**
 * Vertical distance between [from] and [to], or `null` if either altitude is unknown or less
 * accurate than [maxVerticalAccuracy]. Going up and going down count the same.
 */
private fun climb(
    from: LocationFix,
    to: LocationFix,
    maxVerticalAccuracy: Distance,
): Distance? {
    val fromAltitude = from.accurateAltitude(maxVerticalAccuracy) ?: return null
    val toAltitude = to.accurateAltitude(maxVerticalAccuracy) ?: return null
    return fromAltitude.verticalDistanceTo(toAltitude)
}

/**
 * The altitude of this fix, or `null` if it is unknown or less accurate than
 * [maxVerticalAccuracy].
 */
private fun LocationFix.accurateAltitude(maxVerticalAccuracy: Distance): Altitude? {
    val accuracy = verticalAccuracy ?: return null
    return altitude?.takeIf { accuracy <= maxVerticalAccuracy }
}
