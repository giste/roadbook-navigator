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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GeoPointTest {

    @Test
    fun constructor_validCoordinates_keepsCoordinates() {
        val point = GeoPoint(latitude = 40.4168, longitude = -3.7038)

        assertEquals(40.4168, point.latitude, 0.0)
        assertEquals(-3.7038, point.longitude, 0.0)
    }

    @Test
    fun constructor_latitudeAtPoles_isAccepted() {
        val north = GeoPoint(latitude = 90.0, longitude = 0.0)
        val south = GeoPoint(latitude = -90.0, longitude = 0.0)

        assertEquals(90.0, north.latitude, 0.0)
        assertEquals(-90.0, south.latitude, 0.0)
    }

    @Test
    fun constructor_longitudeAtAntimeridian_isKeptAsGiven() {
        val east = GeoPoint(latitude = 0.0, longitude = 180.0)
        val west = GeoPoint(latitude = 0.0, longitude = -180.0)

        assertEquals(180.0, east.longitude, 0.0)
        assertEquals(-180.0, west.longitude, 0.0)
    }

    @Test
    fun constructor_latitudeAboveRange_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            GeoPoint(latitude = 90.000001, longitude = 0.0)
        }
    }

    @Test
    fun constructor_latitudeBelowRange_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            GeoPoint(latitude = -90.000001, longitude = 0.0)
        }
    }

    @Test
    fun constructor_latitudeNan_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            GeoPoint(latitude = Double.NaN, longitude = 0.0)
        }
    }

    @Test
    fun constructor_longitudeAboveRange_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            GeoPoint(latitude = 0.0, longitude = 180.000001)
        }
    }

    @Test
    fun constructor_longitudeBelowRange_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            GeoPoint(latitude = 0.0, longitude = -180.000001)
        }
    }

    @Test
    fun constructor_longitudeNan_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            GeoPoint(latitude = 0.0, longitude = Double.NaN)
        }
    }

    @Test
    fun copy_invalidCoordinate_throws() {
        val point = GeoPoint(latitude = 40.0, longitude = -3.0)

        assertThrows(IllegalArgumentException::class.java) { point.copy(latitude = 91.0) }
    }
}
