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

package dev.giste.roadbooknavigator.settings.domain

import dev.giste.map.MapSettings
import dev.giste.map.MapTilt
import dev.giste.map.MapZoomLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class MapViewStepTest {

    @Test
    fun zoomStepped_upOrDown_movesOneLevelPerStep() {
        assertEquals(MapZoomLevel.of(19), MapZoomLevel.of(18).stepped(1))
        assertEquals(MapZoomLevel.of(16), MapZoomLevel.of(18).stepped(-2))
    }

    @Test
    fun zoomStepped_pastTheOfferedRange_stopsAtItsEnds() {
        assertEquals(MapZoomLevel.of(20), MapZoomLevel.of(20).stepped(1))
        assertEquals(MapZoomLevel.of(15), MapZoomLevel.of(15).stepped(-1))
    }

    @Test
    fun zoomStepped_fromOutsideTheOfferedRange_landsInIt() {
        // A zoom level :map accepts but this app does not offer.
        assertEquals(MapZoomLevel.of(15), MapZoomLevel.of(10).stepped(1))
    }

    @Test
    fun tiltStepped_upOrDown_movesFiveDegreesPerStep() {
        assertEquals(MapTilt.of(55.0), MapTilt.of(60.0).stepped(-1))
        assertEquals(MapTilt.of(10.0), MapTilt.of(0.0).stepped(2))
    }

    @Test
    fun tiltStepped_pastTheOfferedRange_stopsAtItsEnds() {
        assertEquals(MapTilt.of(60.0), MapTilt.of(60.0).stepped(1))
        assertEquals(MapTilt.of(0.0), MapTilt.of(0.0).stepped(-1))
        assertEquals(MapTilt.of(60.0), MapTilt.of(65.0).stepped(1))
    }

    @Test
    fun withinOfferedRanges_valuesInside_areKept() {
        val settings = MapSettings(zoomLevel = MapZoomLevel.of(17), tilt = MapTilt.of(30.0))

        assertEquals(settings, settings.withinOfferedRanges())
    }

    @Test
    fun withinOfferedRanges_valuesOutside_areBroughtToTheNearestEnd() {
        val settings = MapSettings(zoomLevel = MapZoomLevel.of(2), tilt = MapTilt.of(65.0))

        assertEquals(
            MapSettings(zoomLevel = MapZoomLevel.of(15), tilt = MapTilt.of(60.0)),
            settings.withinOfferedRanges(),
        )
    }
}
