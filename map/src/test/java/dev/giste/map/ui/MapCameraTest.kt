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

package dev.giste.map.ui

import dev.giste.geo.Bearing
import dev.giste.geo.GeoPoint
import dev.giste.map.MapSettings
import dev.giste.map.MapTilt
import dev.giste.map.MapZoomLevel
import dev.giste.map.domain.MapCenter
import org.junit.Assert.assertEquals
import org.junit.Test
import org.oscim.core.MapPosition
import org.oscim.map.Viewport

class MapCameraTest {

    private val madrid = GeoPoint(latitude = 40.4168, longitude = -3.7038)

    @Test
    fun applySettings_setsTheZoomLevelAsAPowerOfTwoScaleAndTheTilt() {
        val settings = MapSettings(zoomLevel = MapZoomLevel.of(16), tilt = MapTilt.of(45.0))

        val position = MapPosition().applySettings(settings)

        assertEquals(16, position.zoomLevel)
        assertEquals(65_536.0, position.scale, 0.0)
        assertEquals(45f, position.tilt, 0f)
    }

    @Test
    fun applySettings_keepsTheCenterAndTheRotation() {
        val position = MapPosition().apply {
            setPosition(madrid.latitude, madrid.longitude)
            setBearing(30f)
        }

        position.applySettings(MapSettings())

        assertEquals(madrid.latitude, position.latitude, 1e-6)
        assertEquals(madrid.longitude, position.longitude, 1e-6)
        assertEquals(30f, position.bearing, 0f)
    }

    @Test
    fun follow_centersOnThePosition() {
        val position = MapPosition().follow(MapCenter(madrid, bearing = null))

        assertEquals(madrid.latitude, position.latitude, 1e-6)
        assertEquals(madrid.longitude, position.longitude, 1e-6)
    }

    @Test
    fun follow_headingEast_turnsTheMapBackByAQuarter() {
        val position = MapPosition().follow(MapCenter(madrid, Bearing.degrees(90.0)))

        assertEquals(-90f, position.bearing, 1e-4f)
    }

    @Test
    fun follow_headingWest_turnsTheMapForwardByAQuarter() {
        // -270° and 90° are the same rotation: VTM keeps it within [-180, 180].
        val position = MapPosition().follow(MapCenter(madrid, Bearing.degrees(270.0)))

        assertEquals(90f, position.bearing, 1e-4f)
    }

    @Test
    fun follow_withoutBearing_keepsTheRotation() {
        val position = MapPosition().apply { setBearing(30f) }

        position.follow(MapCenter(madrid, bearing = null))

        assertEquals(30f, position.bearing, 0f)
    }

    @Test
    fun follow_keepsTheZoomAndTheTilt() {
        val settings = MapSettings(zoomLevel = MapZoomLevel.of(12), tilt = MapTilt.of(20.0))
        val position = MapPosition().applySettings(settings)

        position.follow(MapCenter(madrid, Bearing.degrees(10.0)))

        assertEquals(12, position.zoomLevel)
        assertEquals(20f, position.tilt, 0f)
    }

    @Test
    fun settingRanges_matchWhatVtmCanDraw() {
        assertEquals(Viewport.MIN_ZOOM_LEVEL..Viewport.MAX_ZOOM_LEVEL, MapZoomLevel.RANGE)
        assertEquals(Viewport.MIN_TILT.toDouble(), MapTilt.MIN.inDegrees, 0.0)
        assertEquals(Viewport.MAX_TILT.toDouble(), MapTilt.MAX.inDegrees, 0.0)
    }
}
