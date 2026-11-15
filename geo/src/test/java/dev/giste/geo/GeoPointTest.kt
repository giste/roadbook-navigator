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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    @Test
    fun distanceTo_samePosition_isZero() {
        val point = GeoPoint(latitude = 40.0, longitude = -3.0)

        assertEquals(Distance.ZERO, point.distanceTo(point))
    }

    @Test
    fun distanceTo_vincentyReferenceLine_matchesPublishedDistance() {
        // Vincenty's reference line, Flinders Peak to Buninyong (Geoscience Australia).
        assertEquals(54_972.271, FLINDERS_PEAK.distanceTo(BUNINYONG).inMeters, 0.001)
    }

    @Test
    fun distanceTo_oneDegreeAlongTheEquator_isOneDegreeOfTheEquatorialRadius() {
        val distance = GeoPoint(0.0, 0.0).distanceTo(GeoPoint(0.0, 1.0))

        assertEquals(6_378_137.0 * Math.PI / 180, distance.inMeters, 1e-6)
    }

    @Test
    fun distanceTo_equatorToPole_isTheMeridianQuadrant() {
        val distance = GeoPoint(0.0, 0.0).distanceTo(GeoPoint(90.0, 0.0))

        // Length of the WGS84 meridian quadrant.
        assertEquals(10_001_965.729, distance.inMeters, 0.001)
    }

    @Test
    fun distanceTo_alongAMeridian_matchesTheMeridianArc() {
        // The WGS84 meridian arc from 40°N to 41°N, integrated numerically from the meridian
        // radius of curvature: 111 044.261 m. The haversine sphere gives 111 194.9 m.
        val distance = GeoPoint(40.0, -3.0).distanceTo(GeoPoint(41.0, -3.0))

        assertEquals(111_044.261, distance.inMeters, 0.001)
    }

    @Test
    fun distanceTo_acrossTheAntimeridian_takesTheShortWay() {
        val distance = GeoPoint(0.0, 179.5).distanceTo(GeoPoint(0.0, -179.5))

        assertEquals(6_378_137.0 * Math.PI / 180, distance.inMeters, 1e-6)
    }

    @Test
    fun distanceTo_isSymmetric() {
        assertEquals(
            FLINDERS_PEAK.distanceTo(BUNINYONG).inMeters,
            BUNINYONG.distanceTo(FLINDERS_PEAK).inMeters,
            1e-6,
        )
    }

    @Test
    fun distanceTo_antipodalPoints_fallsBackToTheSphere() {
        // Vincenty's formula does not converge here. The real geodesic (over a pole) is
        // 20 003 931.459 m, and the spherical fallback is within 0.1 % of it.
        val distance = GeoPoint(0.0, 0.0).distanceTo(GeoPoint(0.0, 180.0))

        assertEquals(6_371_008.8 * Math.PI, distance.inMeters, 1e-3)
        assertEquals(20_003_931.459, distance.inMeters, 20_003_931.459 * 0.001)
    }

    @Test
    fun distanceTo_samePositionWithOtherLongitudeSign_isZero() {
        // -180 and 180 are the same meridian.
        val distance = GeoPoint(10.0, 180.0).distanceTo(GeoPoint(10.0, -180.0))

        assertEquals(0.0, distance.inMeters, 1e-6)
    }

    @Test
    fun bearingTo_samePosition_isNull() {
        val point = GeoPoint(latitude = 40.0, longitude = -3.0)

        assertNull(point.bearingTo(point))
    }

    @Test
    fun bearingTo_vincentyReferenceLine_matchesPublishedAzimuth() {
        val bearing = FLINDERS_PEAK.bearingTo(BUNINYONG)

        assertEquals(degrees(306, 52, 5.37), bearing!!.inDegrees, 1e-5)
    }

    @Test
    fun bearingTo_vincentyReferenceLineBack_matchesPublishedReverseAzimuth() {
        // The published reverse azimuth, 127°10'25.07", is the bearing from Buninyong back to
        // Flinders Peak. It is not the forward azimuth plus 180°, because the bearing changes
        // along the geodesic.
        val bearing = BUNINYONG.bearingTo(FLINDERS_PEAK)

        assertEquals(degrees(127, 10, 25.07), bearing!!.inDegrees, 1e-5)
    }

    @Test
    fun bearingTo_dueNorth_isNorth() {
        val bearing = GeoPoint(40.0, -3.0).bearingTo(GeoPoint(41.0, -3.0))

        assertEquals(0.0, bearing!!.inDegrees, 1e-9)
    }

    @Test
    fun bearingTo_dueEastAlongTheEquator_isEast() {
        val bearing = GeoPoint(0.0, 0.0).bearingTo(GeoPoint(0.0, 1.0))

        assertEquals(90.0, bearing!!.inDegrees, 1e-9)
    }

    @Test
    fun bearingTo_dueSouth_isSouth() {
        val bearing = GeoPoint(41.0, -3.0).bearingTo(GeoPoint(40.0, -3.0))

        assertEquals(180.0, bearing!!.inDegrees, 1e-9)
    }

    @Test
    fun bearingTo_dueWestAlongTheEquator_isWest() {
        val bearing = GeoPoint(0.0, 1.0).bearingTo(GeoPoint(0.0, 0.0))

        assertEquals(270.0, bearing!!.inDegrees, 1e-9)
    }

    @Test
    fun bearingTo_acrossTheAntimeridian_takesTheShortWay() {
        val bearing = GeoPoint(0.0, 179.5).bearingTo(GeoPoint(0.0, -179.5))

        assertEquals(90.0, bearing!!.inDegrees, 1e-9)
    }

    @Test
    fun bearingTo_antipodalPoints_fallsBackToASphericalBearing() {
        // Every direction is a shortest path to the antipode. The fallback returns one of them.
        val bearing = GeoPoint(0.0, 0.0).bearingTo(GeoPoint(0.0, 180.0))

        assertNotNull(bearing)
    }

    private companion object {
        /** Flinders Peak, start of Vincenty's reference line: 37°57'03.72030"S 144°25'29.52440"E. */
        val FLINDERS_PEAK = GeoPoint(-degrees(37, 57, 3.72030), degrees(144, 25, 29.52440))

        /** Buninyong, end of Vincenty's reference line: 37°39'10.15610"S 143°55'35.38390"E. */
        val BUNINYONG = GeoPoint(-degrees(37, 39, 10.15610), degrees(143, 55, 35.38390))

        /** Converts degrees, minutes and seconds to decimal degrees. */
        fun degrees(degrees: Int, minutes: Int, seconds: Double): Double =
            degrees + minutes / 60.0 + seconds / 3600.0
    }
}
