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

package dev.giste.map.domain

import dev.giste.geo.Bearing
import dev.giste.geo.GeoPoint
import dev.giste.map.testFix
import org.junit.Assert.assertEquals
import org.junit.Test

class MapCenterTest {

    private val start = GeoPoint(latitude = 40.0, longitude = -3.0)
    private val next = GeoPoint(latitude = 40.1, longitude = -3.1)

    @Test
    fun follow_firstFixWithBearing_takesItsPositionAndBearing() {
        val center = null.follow(testFix(start, Bearing.degrees(45.0)))

        assertEquals(MapCenter(start, Bearing.degrees(45.0)), center)
    }

    @Test
    fun follow_firstFixWithoutBearing_hasNoBearing() {
        val center = null.follow(testFix(start, bearing = null))

        assertEquals(MapCenter(start, bearing = null), center)
    }

    @Test
    fun follow_fixWithBearing_replacesThePreviousBearing() {
        val previous = MapCenter(start, Bearing.degrees(45.0))

        val center = previous.follow(testFix(next, Bearing.degrees(180.0)))

        assertEquals(MapCenter(next, Bearing.degrees(180.0)), center)
    }

    @Test
    fun follow_fixWithoutBearing_keepsThePreviousBearing() {
        val previous = MapCenter(start, Bearing.degrees(45.0))

        val center = previous.follow(testFix(next, bearing = null))

        assertEquals(MapCenter(next, Bearing.degrees(45.0)), center)
    }
}
