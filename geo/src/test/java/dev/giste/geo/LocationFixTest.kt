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

package dev.giste.geo

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/** Altitude validation is tested in [AltitudeTest], because [Altitude] owns it. */
class LocationFixTest {

    /** A fix with only the values that are always known, at [elapsedRealtime]. */
    private fun fix(elapsedRealtime: Duration = 1.seconds) = LocationFix(
        position = GeoPoint(latitude = 40.4168, longitude = -3.7038),
        horizontalAccuracy = null,
        altitude = null,
        verticalAccuracy = null,
        speed = null,
        bearing = null,
        time = Instant.fromEpochMilliseconds(1_000),
        elapsedRealtime = elapsedRealtime,
    )

    @Test
    fun constructor_positiveElapsedRealtime_isKept() {
        assertEquals(90.seconds, fix(elapsedRealtime = 90.seconds).elapsedRealtime)
    }

    @Test
    fun constructor_zeroElapsedRealtime_isAccepted() {
        assertEquals(Duration.ZERO, fix(elapsedRealtime = Duration.ZERO).elapsedRealtime)
    }

    @Test
    fun constructor_negativeElapsedRealtime_throws() {
        assertThrows(IllegalArgumentException::class.java) { fix(elapsedRealtime = (-1).seconds) }
    }

    @Test
    fun constructor_infiniteElapsedRealtime_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            fix(elapsedRealtime = Duration.INFINITE)
        }
    }
}
