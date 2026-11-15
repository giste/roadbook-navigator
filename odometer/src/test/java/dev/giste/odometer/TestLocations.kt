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

/** Meters along a meridian per degree of latitude, on the sphere the odometer uses. */
internal const val METERS_PER_DEGREE_OF_LATITUDE: Double = 6_371_000.0 * Math.PI / 180.0

/** Where every test route starts. */
internal val START: GeoPoint = GeoPoint(latitude = 40.0, longitude = -3.0)

/**
 * A fix [metersNorth] of [START], accurate and moving unless the test says otherwise.
 *
 * Moving due north along a meridian makes the expected distance exact.
 */
internal fun testFix(
    metersNorth: Double = 0.0,
    horizontalAccuracy: Distance? = Distance.meters(5.0),
    speed: Speed? = Speed.metersPerSecond(10.0),
    altitudeInMeters: Double? = null,
    verticalAccuracy: Distance? = null,
): OdometerLocation = OdometerLocation(
    position = GeoPoint(
        latitude = START.latitude + metersNorth / METERS_PER_DEGREE_OF_LATITUDE,
        longitude = START.longitude,
    ),
    horizontalAccuracy = horizontalAccuracy,
    altitudeInMeters = altitudeInMeters,
    verticalAccuracy = verticalAccuracy,
    speed = speed,
)
