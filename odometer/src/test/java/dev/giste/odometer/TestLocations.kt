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

import dev.giste.geo.Altitude
import dev.giste.geo.Distance
import dev.giste.geo.GeoPoint
import dev.giste.geo.LocationFix
import dev.giste.geo.Speed
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Approximate meters along a meridian per degree of latitude near [START]. It is only the first
 * guess of [pointNorthOfStart], so its precision does not matter.
 */
private const val APPROXIMATE_METERS_PER_DEGREE_OF_LATITUDE: Double = 111_000.0

/** Where every test route starts. */
internal val START: GeoPoint = GeoPoint(latitude = 40.0, longitude = -3.0)

/**
 * A fix [metersNorth] of [START], accurate and moving unless the test says otherwise.
 *
 * Every fix lies on the same meridian, where distances add up exactly, so the expected distance
 * between two fixes is the difference of their [metersNorth]. The bearing and the times are fixed,
 * because the odometer does not use them. [altitudeInMeters] is a plain number only to keep the
 * tests short: it becomes the fix's [Altitude].
 *
 * @param metersNorth how far north of [START] the fix is. Must not be negative.
 */
internal fun testFix(
    metersNorth: Double = 0.0,
    horizontalAccuracy: Distance? = Distance.meters(5.0),
    speed: Speed? = Speed.metersPerSecond(10.0),
    altitudeInMeters: Double? = null,
    verticalAccuracy: Distance? = null,
): LocationFix = LocationFix(
    position = pointNorthOfStart(metersNorth),
    horizontalAccuracy = horizontalAccuracy,
    altitude = altitudeInMeters?.let(Altitude::meters),
    verticalAccuracy = verticalAccuracy,
    speed = speed,
    bearing = null,
    time = Instant.fromEpochMilliseconds(1_000),
    elapsedRealtime = 1.seconds,
)

/**
 * The point on [START]'s meridian that is [meters] north of it, as measured by
 * [GeoPoint.distanceTo], the distance the odometer uses.
 *
 * A degree of latitude has no fixed length on the ellipsoid, so the latitude is refined a few
 * times (Newton's method with a constant slope) until the measured distance matches to well
 * under a nanometer.
 */
private fun pointNorthOfStart(meters: Double): GeoPoint {
    require(meters >= 0.0) { "Test fixes are north of START, but metersNorth was $meters" }
    var latitude = START.latitude + meters / APPROXIMATE_METERS_PER_DEGREE_OF_LATITUDE
    repeat(5) {
        val measured = START.distanceTo(GeoPoint(latitude, START.longitude)).inMeters
        latitude += (meters - measured) / APPROXIMATE_METERS_PER_DEGREE_OF_LATITUDE
    }
    return GeoPoint(latitude, START.longitude)
}
