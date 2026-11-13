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

import dev.giste.geo.GeoPoint
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** A minimal fix with only the values that are always known. */
internal fun testUserLocation(latitude: Double = 40.4168): UserLocation = UserLocation(
    position = GeoPoint(latitude = latitude, longitude = -3.7038),
    horizontalAccuracy = null,
    altitudeInMeters = null,
    verticalAccuracy = null,
    speed = null,
    bearing = null,
    time = Instant.fromEpochMilliseconds(1_000),
    elapsedRealtime = 1.seconds,
)
