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

import dev.giste.geo.Distance
import dev.giste.geo.GeoPoint
import dev.giste.odometer.METERS_PER_DEGREE_OF_LATITUDE
import dev.giste.odometer.testFix
import kotlin.math.hypot
import org.junit.Assert.assertEquals
import org.junit.Test

class TravelledDistanceTest {

    private val maxVerticalAccuracy = Distance.meters(10.0)

    @Test
    fun surfaceDistance_samePoint_isZero() {
        val point = GeoPoint(latitude = 40.0, longitude = -3.0)

        assertEquals(Distance.ZERO, surfaceDistance(point, point))
    }

    @Test
    fun surfaceDistance_oneDegreeOfLatitude_isOneDegreeOfArc() {
        val distance = surfaceDistance(GeoPoint(0.0, 0.0), GeoPoint(1.0, 0.0))

        assertEquals(METERS_PER_DEGREE_OF_LATITUDE, distance.inMeters, 1e-6)
    }

    @Test
    fun surfaceDistance_madridToBarcelona_matchesKnownDistance() {
        val madrid = GeoPoint(latitude = 40.4168, longitude = -3.7038)
        val barcelona = GeoPoint(latitude = 41.3874, longitude = 2.1686)

        // Great-circle distance on a 6371 km sphere.
        assertEquals(505_000.0, surfaceDistance(madrid, barcelona).inMeters, 1_000.0)
    }

    @Test
    fun surfaceDistance_isSymmetric() {
        val a = GeoPoint(latitude = 40.0, longitude = -3.0)
        val b = GeoPoint(latitude = 40.01, longitude = -2.99)

        assertEquals(surfaceDistance(a, b).inMeters, surfaceDistance(b, a).inMeters, 1e-9)
    }

    @Test
    fun surfaceDistance_antipodalPoints_isHalfTheCircumference() {
        val distance = surfaceDistance(GeoPoint(0.0, 0.0), GeoPoint(0.0, 180.0))

        assertEquals(180 * METERS_PER_DEGREE_OF_LATITUDE, distance.inMeters, 1e-3)
    }

    @Test
    fun travelledDistance_noAltitude_isHorizontalDistance() {
        val distance = travelledDistance(testFix(0.0), testFix(100.0), maxVerticalAccuracy)

        assertEquals(100.0, distance.inMeters, 1e-6)
    }

    @Test
    fun travelledDistance_accurateAltitudes_includesClimb() {
        val from = testFix(0.0, altitudeInMeters = 600.0, verticalAccuracy = Distance.meters(5.0))
        val to = testFix(100.0, altitudeInMeters = 630.0, verticalAccuracy = Distance.meters(5.0))

        val distance = travelledDistance(from, to, maxVerticalAccuracy)

        assertEquals(hypot(100.0, 30.0), distance.inMeters, 1e-6)
    }

    @Test
    fun travelledDistance_noVerticalAccuracyLimit_ignoresClimb() {
        val from = testFix(0.0, altitudeInMeters = 600.0, verticalAccuracy = Distance.meters(1.0))
        val to = testFix(100.0, altitudeInMeters = 630.0, verticalAccuracy = Distance.meters(1.0))

        val distance = travelledDistance(from, to, maxVerticalAccuracy = null)

        assertEquals(100.0, distance.inMeters, 1e-6)
    }

    @Test
    fun travelledDistance_verticalAccuracyAtLimit_includesClimb() {
        val from = testFix(0.0, altitudeInMeters = 600.0, verticalAccuracy = maxVerticalAccuracy)
        val to = testFix(100.0, altitudeInMeters = 570.0, verticalAccuracy = maxVerticalAccuracy)

        val distance = travelledDistance(from, to, maxVerticalAccuracy)

        assertEquals(hypot(100.0, 30.0), distance.inMeters, 1e-6)
    }

    @Test
    fun travelledDistance_poorVerticalAccuracy_ignoresClimb() {
        val from = testFix(0.0, altitudeInMeters = 600.0, verticalAccuracy = Distance.meters(5.0))
        val to = testFix(100.0, altitudeInMeters = 630.0, verticalAccuracy = Distance.meters(15.0))

        val distance = travelledDistance(from, to, maxVerticalAccuracy)

        assertEquals(100.0, distance.inMeters, 1e-6)
    }

    @Test
    fun travelledDistance_unknownVerticalAccuracy_ignoresClimb() {
        val from = testFix(0.0, altitudeInMeters = 600.0, verticalAccuracy = null)
        val to = testFix(100.0, altitudeInMeters = 630.0, verticalAccuracy = Distance.meters(5.0))

        val distance = travelledDistance(from, to, maxVerticalAccuracy)

        assertEquals(100.0, distance.inMeters, 1e-6)
    }

    @Test
    fun travelledDistance_unknownAltitude_ignoresClimb() {
        val from = testFix(0.0, altitudeInMeters = 600.0, verticalAccuracy = Distance.meters(5.0))
        val to = testFix(100.0, altitudeInMeters = null, verticalAccuracy = Distance.meters(5.0))

        val distance = travelledDistance(from, to, maxVerticalAccuracy)

        assertEquals(100.0, distance.inMeters, 1e-6)
    }
}
