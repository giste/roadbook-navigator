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
import android.location.LocationManager
import dev.giste.geo.Altitude
import dev.giste.geo.Bearing
import dev.giste.geo.Distance
import dev.giste.geo.GeoPoint
import dev.giste.geo.Speed
import dev.giste.location.LocationError
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Runs on Robolectric because [Location] is an Android class. */
@RunWith(RobolectricTestRunner::class)
class LocationMapperTest {

    private fun minimalLocation(): Location = Location(LocationManager.GPS_PROVIDER).apply {
        latitude = 40.4168
        longitude = -3.7038
        time = 1_700_000_000_000
        elapsedRealtimeNanos = 123_456_789
    }

    @Test
    fun toLocationFix_allValuesPresent_mapsEveryValue() {
        val location = minimalLocation().apply {
            accuracy = 5f
            altitude = -12.5
            verticalAccuracyMeters = 3f
            speed = 10f
            bearing = 90f
        }

        val result = location.toLocationFix()!!

        assertEquals(GeoPoint(latitude = 40.4168, longitude = -3.7038), result.position)
        assertEquals(Distance.meters(5.0), result.horizontalAccuracy)
        assertEquals(Altitude.meters(-12.5), result.altitude)
        assertEquals(Distance.meters(3.0), result.verticalAccuracy)
        assertEquals(Speed.metersPerSecond(10.0), result.speed)
        assertEquals(Bearing.degrees(90.0), result.bearing)
        assertEquals(Instant.fromEpochMilliseconds(1_700_000_000_000), result.time)
        assertEquals(123_456_789.nanoseconds, result.elapsedRealtime)
    }

    @Test
    fun toLocationFix_optionalValuesMissing_mapsThemToNull() {
        val result = minimalLocation().toLocationFix()!!

        assertNull(result.horizontalAccuracy)
        assertNull(result.altitude)
        assertNull(result.verticalAccuracy)
        assertNull(result.speed)
        assertNull(result.bearing)
    }

    @Test
    fun toLocationFix_zeroSpeed_mapsToZeroNotNull() {
        val location = minimalLocation().apply { speed = 0f }

        assertEquals(Speed.ZERO, location.toLocationFix()!!.speed)
    }

    @Test
    fun toLocationFix_latitudeOutOfRange_returnsNull() {
        val location = minimalLocation().apply { latitude = 91.0 }

        assertNull(location.toLocationFix())
    }

    @Test
    fun toLocationFix_nanSpeed_returnsNull() {
        val location = minimalLocation().apply { speed = Float.NaN }

        assertNull(location.toLocationFix())
    }

    @Test
    fun toLocationFix_nanAltitude_returnsNull() {
        val location = minimalLocation().apply { altitude = Double.NaN }

        assertNull(location.toLocationFix())
    }

    @Test
    fun toLocationError_securityException_isPermissionDenied() {
        assertEquals(LocationError.PERMISSION_DENIED, SecurityException().toLocationError())
    }

    @Test
    fun toLocationError_illegalArgumentException_isProviderUnavailable() {
        assertEquals(
            LocationError.PROVIDER_UNAVAILABLE,
            IllegalArgumentException().toLocationError(),
        )
    }

    @Test
    fun toLocationError_otherRuntimeException_isUnknown() {
        assertEquals(LocationError.UNKNOWN, IllegalStateException().toLocationError())
    }

    @Test
    fun toLocationError_cancellationException_isRethrown() {
        assertThrows(CancellationException::class.java) {
            CancellationException("cancelled").toLocationError()
        }
    }
}
