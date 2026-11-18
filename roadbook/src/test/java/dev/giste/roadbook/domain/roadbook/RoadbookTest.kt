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

import dev.giste.geo.Distance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RoadbookTest {

    @Test
    fun constructor_singleWaypoint_isAccepted() {
        val roadbook = Roadbook(waypoints = listOf(testWaypoint(1)))

        assertEquals(1, roadbook.waypoints.size)
    }

    @Test
    fun constructor_consecutiveWaypoints_keepsThemInOrder() {
        val waypoints = listOf(testWaypoint(1), testWaypoint(2), testWaypoint(3))

        val roadbook = Roadbook(waypoints = waypoints)

        assertEquals(waypoints, roadbook.waypoints)
    }

    @Test
    fun constructor_metadataOmitted_isEmpty() {
        val roadbook = Roadbook(waypoints = listOf(testWaypoint(1)))

        assertEquals("", roadbook.name)
        assertEquals("", roadbook.description)
        assertEquals("", roadbook.startLocation)
        assertEquals("", roadbook.endLocation)
    }

    @Test
    fun constructor_metadata_isKept() {
        val roadbook = Roadbook(
            name = "Stage 1",
            description = "Desert loop",
            startLocation = "Erfoud",
            endLocation = "Merzouga",
            waypoints = listOf(testWaypoint(1)),
        )

        assertEquals("Stage 1", roadbook.name)
        assertEquals("Desert loop", roadbook.description)
        assertEquals("Erfoud", roadbook.startLocation)
        assertEquals("Merzouga", roadbook.endLocation)
    }

    @Test
    fun constructor_noWaypoints_throws() {
        assertThrows(IllegalArgumentException::class.java) { Roadbook(waypoints = emptyList()) }
    }

    @Test
    fun constructor_firstWaypointNotNumberedOne_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            Roadbook(waypoints = listOf(testWaypoint(2, distanceFromPrevious = Distance.ZERO)))
        }
    }

    @Test
    fun constructor_gapInNumbers_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            Roadbook(waypoints = listOf(testWaypoint(1), testWaypoint(2), testWaypoint(4)))
        }
    }

    @Test
    fun constructor_numbersOutOfOrder_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            Roadbook(
                waypoints = listOf(
                    testWaypoint(1),
                    testWaypoint(3),
                    testWaypoint(2),
                ),
            )
        }
    }

    @Test
    fun constructor_repeatedNumber_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            Roadbook(waypoints = listOf(testWaypoint(1), testWaypoint(2), testWaypoint(2)))
        }
    }

    @Test
    fun constructor_firstWaypointWithDistanceFromPrevious_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            Roadbook(waypoints = listOf(testWaypoint(1, distanceFromPrevious = Distance.meters(5.0))))
        }
    }

    @Test
    fun constructor_firstWaypointWithPartial_isAccepted() {
        // Waypoints that are not shown before the first one still add distance to its partial.
        val first = testWaypoint(1, partial = Distance.meters(300.0))

        val roadbook = Roadbook(waypoints = listOf(first))

        assertEquals(Distance.meters(300.0), roadbook.waypoints.first().partial)
    }

    @Test
    fun constructor_laterWaypointAtZeroDistance_isAccepted() {
        // Two waypoints can be at the same place, e.g. a junction and a sign right after it.
        val waypoints = listOf(testWaypoint(1), testWaypoint(2, distanceFromPrevious = Distance.ZERO))

        val roadbook = Roadbook(waypoints = waypoints)

        assertEquals(Distance.ZERO, roadbook.waypoints[1].distanceFromPrevious)
    }

    private val threeWaypoints = Roadbook(
        waypoints = listOf(testWaypoint(1), testWaypoint(2), testWaypoint(3)),
    )

    @Test
    fun lastWaypointNumber_isNumberOfLastWaypoint() {
        assertEquals(WaypointNumber(3), threeWaypoints.lastWaypointNumber)
    }

    @Test
    fun coerceWaypointNumber_withinRoadbook_isUnchanged() {
        assertEquals(WaypointNumber(2), threeWaypoints.coerceWaypointNumber(WaypointNumber(2)))
    }

    @Test
    fun coerceWaypointNumber_lastWaypoint_isUnchanged() {
        assertEquals(WaypointNumber(3), threeWaypoints.coerceWaypointNumber(WaypointNumber(3)))
    }

    @Test
    fun coerceWaypointNumber_pastLastWaypoint_isLastWaypoint() {
        assertEquals(WaypointNumber(3), threeWaypoints.coerceWaypointNumber(WaypointNumber(50)))
    }

    @Test
    fun waypointNumberAfter_middleWaypoint_isNextOne() {
        assertEquals(WaypointNumber(3), threeWaypoints.waypointNumberAfter(WaypointNumber(2)))
    }

    @Test
    fun waypointNumberAfter_lastWaypoint_staysAtLastOne() {
        assertEquals(WaypointNumber(3), threeWaypoints.waypointNumberAfter(WaypointNumber(3)))
    }

    @Test
    fun waypointNumberAfter_pastLastWaypoint_isLastOne() {
        assertEquals(WaypointNumber(3), threeWaypoints.waypointNumberAfter(WaypointNumber(50)))
    }

    @Test
    fun waypointNumberBefore_middleWaypoint_isPreviousOne() {
        assertEquals(WaypointNumber(1), threeWaypoints.waypointNumberBefore(WaypointNumber(2)))
    }

    @Test
    fun waypointNumberBefore_firstWaypoint_staysAtFirstOne() {
        val before = threeWaypoints.waypointNumberBefore(WaypointNumber.FIRST)

        assertEquals(WaypointNumber.FIRST, before)
    }

    @Test
    fun waypointNumberBefore_pastLastWaypoint_isOneBeforeLast() {
        assertEquals(WaypointNumber(2), threeWaypoints.waypointNumberBefore(WaypointNumber(50)))
    }

    @Test
    fun waypointNumberAfter_singleWaypoint_staysAtIt() {
        val roadbook = Roadbook(waypoints = listOf(testWaypoint(1)))

        assertEquals(WaypointNumber.FIRST, roadbook.waypointNumberAfter(WaypointNumber.FIRST))
    }
}
