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
import dev.giste.geo.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WaypointTest {

    @Test
    fun partialToSet_waypointDoesNotResetPartial_isThePartial() {
        val waypoint = testWaypoint(3, partial = Distance.meters(1_250.0), resetsPartial = false)

        assertEquals(Distance.meters(1_250.0), waypoint.partialToSet)
    }

    @Test
    fun partialToSet_waypointResetsPartial_isZero() {
        val waypoint = testWaypoint(3, partial = Distance.meters(1_250.0), resetsPartial = true)

        assertEquals(Distance.ZERO, waypoint.partialToSet)
    }

    @Test
    fun partial_waypointResetsPartial_stillCountsUpToIt() {
        val waypoint = testWaypoint(3, partial = Distance.meters(1_250.0), resetsPartial = true)

        assertEquals(Distance.meters(1_250.0), waypoint.partial)
    }

    @Test
    fun constructor_optionalValuesOmitted_usesDefaults() {
        val waypoint = Waypoint(
            number = WaypointNumber.FIRST,
            position = GeoPoint(latitude = 40.0, longitude = -3.0),
            altitude = null,
            partial = Distance.ZERO,
            distanceFromPrevious = Distance.ZERO,
        )

        assertFalse(waypoint.resetsPartial)
        assertEquals(DangerLevel.NONE, waypoint.dangerLevel)
        assertTrue(waypoint.tulip.isEmpty())
        assertTrue(waypoint.notes.isEmpty())
    }
}
