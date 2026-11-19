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

package dev.giste.roadbook.domain.roadbook

import dev.giste.geo.Altitude
import dev.giste.geo.Distance
import dev.giste.geo.GeoPoint
import dev.giste.roadbook.domain.drawing.CanvasPoint
import dev.giste.roadbook.domain.drawing.Road
import dev.giste.roadbook.domain.drawing.Text
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Every test route runs east along the equator, one STEP between consecutive points, so the
// expected distances are whole multiples of STEP.
class RoutePointTest {

    @Test
    fun toWaypoints_emptyRoute_isEmpty() {
        assertTrue(emptyList<RoutePoint>().toWaypoints().isEmpty())
    }

    @Test
    fun toWaypoints_noPointShown_isEmpty() {
        val route = listOf(point(0, shown = false), point(1, shown = false))

        assertTrue(route.toWaypoints().isEmpty())
    }

    @Test
    fun toWaypoints_singlePoint_hasNoDistances() {
        val waypoint = listOf(point(0)).toWaypoints().single()

        assertEquals(Distance.ZERO, waypoint.partial)
        assertEquals(Distance.ZERO, waypoint.distanceFromPrevious)
    }

    @Test
    fun toWaypoints_shownPoints_areNumberedFromOne() {
        val route = listOf(point(0), point(1, shown = false), point(2), point(3))

        val numbers = route.toWaypoints().map { it.number.value }

        assertEquals(listOf(1, 2, 3), numbers)
    }

    @Test
    fun toWaypoints_consecutivePoints_addUpThePartial() {
        val waypoints = listOf(point(0), point(1), point(2)).toWaypoints()

        assertDistance(0, waypoints[0].partial)
        assertDistance(1, waypoints[1].partial)
        assertDistance(2, waypoints[2].partial)
    }

    @Test
    fun toWaypoints_consecutivePoints_measureFromThePrevious() {
        val waypoints = listOf(point(0), point(1), point(2)).toWaypoints()

        assertDistance(0, waypoints[0].distanceFromPrevious)
        assertDistance(1, waypoints[1].distanceFromPrevious)
        assertDistance(1, waypoints[2].distanceFromPrevious)
    }

    @Test
    fun toWaypoints_hiddenPointsInBetween_addToBothDistances() {
        val route = listOf(point(0), point(1, shown = false), point(2, shown = false), point(3))

        val second = route.toWaypoints()[1]

        assertDistance(3, second.partial)
        assertDistance(3, second.distanceFromPrevious)
    }

    @Test
    fun toWaypoints_hiddenPointsBeforeTheFirstShown_addToItsPartialOnly() {
        val route = listOf(point(0, shown = false), point(1, shown = false), point(2))

        val first = route.toWaypoints().single()

        assertDistance(2, first.partial)
        assertEquals(Distance.ZERO, first.distanceFromPrevious)
    }

    @Test
    fun toWaypoints_shownPointResetsPartial_countsUpToItAndRestartsAfterIt() {
        val route = listOf(point(0), point(1), point(2, resetsPartial = true), point(3), point(4))

        val waypoints = route.toWaypoints()

        assertDistance(2, waypoints[2].partial)
        assertDistance(1, waypoints[3].partial)
        assertDistance(2, waypoints[4].partial)
    }

    @Test
    fun toWaypoints_shownPointResetsPartial_doesNotChangeDistanceFromPrevious() {
        val route = listOf(point(0), point(1, resetsPartial = true), point(2))

        assertDistance(1, route.toWaypoints()[2].distanceFromPrevious)
    }

    @Test
    fun toWaypoints_hiddenPointResetsPartial_stillResetsIt() {
        val route = listOf(point(0), point(1, shown = false, resetsPartial = true), point(2))

        val second = route.toWaypoints()[1]

        assertDistance(1, second.partial)
        assertDistance(2, second.distanceFromPrevious)
    }

    @Test
    fun toWaypoints_shownPoint_keepsItsValues() {
        val road = Road(start = null, end = CanvasPoint(0.0, -50.0))
        val text = Text(
            text = "CAP 120",
            lineHeight = 1.0,
            width = 0.0,
            height = 0.0,
            maxWidth = 180.0,
            maxHeight = 100.0,
            center = CanvasPoint.CENTER,
        )
        val shown = RoutePoint(
            position = position(0),
            altitude = Altitude.meters(730.0),
            shown = true,
            resetsPartial = true,
            dangerLevel = DangerLevel.HIGH,
            tulip = listOf(road),
            notes = listOf(text, text),
        )

        val waypoint = listOf(shown).toWaypoints().single()

        assertEquals(position(0), waypoint.position)
        assertEquals(Altitude.meters(730.0), waypoint.altitude)
        assertTrue(waypoint.resetsPartial)
        assertEquals(DangerLevel.HIGH, waypoint.dangerLevel)
        assertEquals(listOf(road), waypoint.tulip)
        assertEquals(listOf(text, text), waypoint.notes)
    }

    @Test
    fun toWaypoints_anyRoute_isAValidRoadbook() {
        val route = listOf(point(0, shown = false), point(1), point(2, shown = false), point(3))

        // Roadbook checks its invariants: this throws if the waypoints break them.
        Roadbook(waypoints = route.toWaypoints())
    }

    private fun point(index: Int, shown: Boolean = true, resetsPartial: Boolean = false) =
        RoutePoint(
            position = position(index),
            altitude = null,
            shown = shown,
            resetsPartial = resetsPartial,
        )

    private fun position(index: Int) = GeoPoint(latitude = 0.0, longitude = 0.001 * index)

    private fun assertDistance(steps: Int, actual: Distance) =
        assertEquals(steps * STEP_METERS, actual.inMeters, steps * DISTANCE_TOLERANCE_METERS)

    private companion object {
        /** 0.001° along the equator on WGS84: the equatorial radius times the angle. */
        const val STEP_METERS: Double = 6_378_137.0 * 0.001 * Math.PI / 180

        /**
         * How close a measured distance must be to the expected one, per step measured: 10 µm.
         * Vincenty's iteration stops within about 6 µm of the exact geodesic (see
         * GeoPoint.distanceTo), and a distance over several steps adds up their errors.
         */
        const val DISTANCE_TOLERANCE_METERS: Double = 1e-5
    }
}
