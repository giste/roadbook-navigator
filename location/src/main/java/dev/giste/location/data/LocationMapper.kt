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

package dev.giste.location.data

import android.location.Location
import dev.giste.geo.Bearing
import dev.giste.geo.Distance
import dev.giste.geo.GeoPoint
import dev.giste.geo.Speed
import dev.giste.location.LocationError
import dev.giste.location.UserLocation
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant

/**
 * Maps a platform [Location] to a [UserLocation].
 *
 * Optional values are `null` when the platform says it does not have them (`hasSpeed()`, …), so
 * the `0` that [Location] returns for a missing value is never taken as a real measurement.
 *
 * @return the fix, or `null` if the platform sent a value that no valid [UserLocation] can hold
 * (e.g. a latitude out of range or a `NaN` accuracy). Such a fix is broken, so it is dropped
 * instead of crashing the location flow.
 */
internal fun Location.toUserLocation(): UserLocation? = try {
    UserLocation(
        position = GeoPoint(latitude = latitude, longitude = longitude),
        horizontalAccuracy = if (hasAccuracy()) Distance.meters(accuracy.toDouble()) else null,
        altitudeInMeters = if (hasAltitude()) altitude else null,
        verticalAccuracy = if (hasVerticalAccuracy()) {
            Distance.meters(verticalAccuracyMeters.toDouble())
        } else {
            null
        },
        speed = if (hasSpeed()) Speed.metersPerSecond(speed.toDouble()) else null,
        bearing = if (hasBearing()) Bearing.degrees(bearing.toDouble()) else null,
        time = Instant.fromEpochMilliseconds(time),
        elapsedRealtime = elapsedRealtimeNanos.nanoseconds,
    )
} catch (_: IllegalArgumentException) {
    // The :geo value objects reject invalid values with IllegalArgumentException.
    null
}

/**
 * Maps a failure to start location updates to the [LocationError] reported to collectors.
 *
 * `LocationManager.requestLocationUpdates` throws [SecurityException] without the location
 * permission and [IllegalArgumentException] when the provider does not exist on the device.
 *
 * @return the reason to report.
 * @throws CancellationException if [this] is one, because cancellation must never be swallowed.
 */
internal fun RuntimeException.toLocationError(): LocationError = when (this) {
    is CancellationException -> throw this
    is SecurityException -> LocationError.PERMISSION_DENIED
    is IllegalArgumentException -> LocationError.PROVIDER_UNAVAILABLE
    else -> LocationError.UNKNOWN
}
