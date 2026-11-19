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

package dev.giste.roadbooknavigator

import dev.giste.geo.Distance
import dev.giste.geo.GeoPoint
import dev.giste.geo.LocationFix
import dev.giste.geo.Speed
import dev.giste.location.LocationEvent
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * A fix at a fixed place and time with [speed]. Only the speed matters to the code under test:
 * the odometer adapter forwards fixes unchanged, and the dashboard only reads the speed.
 */
internal fun testFix(speed: Speed? = Speed.ZERO): LocationFix = LocationFix(
    position = GeoPoint(latitude = 40.0, longitude = -3.0),
    horizontalAccuracy = Distance.meters(5.0),
    altitude = null,
    verticalAccuracy = null,
    speed = speed,
    bearing = null,
    time = Instant.fromEpochMilliseconds(1_000),
    elapsedRealtime = 1.seconds,
)

/** A [LocationEvent.LocationUpdated] with a fix at [kilometersPerHour], or without a speed. */
internal fun fixAt(kilometersPerHour: Double?): LocationEvent.LocationUpdated =
    LocationEvent.LocationUpdated(testFix(kilometersPerHour?.let(Speed::kilometersPerHour)))
