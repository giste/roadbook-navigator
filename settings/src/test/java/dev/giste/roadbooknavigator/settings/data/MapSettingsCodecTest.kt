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

package dev.giste.roadbooknavigator.settings.data

import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.preferencesOf
import dev.giste.map.MapSettings
import dev.giste.map.MapTilt
import dev.giste.map.MapZoomLevel
import dev.giste.roadbooknavigator.settings.FakeLogger
import dev.giste.roadbooknavigator.settings.data.MapSettingsCodec.TILT_KEY
import dev.giste.roadbooknavigator.settings.data.MapSettingsCodec.ZOOM_LEVEL_KEY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapSettingsCodecTest {

    private val logger = FakeLogger()

    @Test
    fun read_nothingStored_isTheDefaults() {
        assertEquals(MapSettings(), MapSettingsCodec.read(preferencesOf(), logger))
    }

    @Test
    fun read_writtenSettings_isTheSameSettings() {
        val settings = MapSettings(zoomLevel = MapZoomLevel.of(16), tilt = MapTilt.of(35.0))
        val preferences = mutablePreferencesOf()
        MapSettingsCodec.write(preferences, settings)

        assertEquals(settings, MapSettingsCodec.read(preferences, logger))
    }

    @Test
    fun write_defaults_storesNothing() {
        val preferences = mutablePreferencesOf()

        MapSettingsCodec.write(preferences, MapSettings())

        assertTrue(preferences.asMap().isEmpty())
    }

    @Test
    fun write_backToTheDefaults_removesTheKeys() {
        val preferences = mutablePreferencesOf()
        MapSettingsCodec.write(preferences, MapSettings(zoomLevel = MapZoomLevel.of(15), tilt = MapTilt.of(0.0)))

        MapSettingsCodec.write(preferences, MapSettings())

        assertTrue(preferences.asMap().isEmpty())
    }

    @Test
    fun read_zoomBelowTheOfferedRange_isTheFarthestOffered() {
        val settings = MapSettingsCodec.read(preferencesOf(ZOOM_LEVEL_KEY to 12), logger)

        assertEquals(MapZoomLevel.of(15), settings.zoomLevel)
        assertEquals(MapTilt.DEFAULT, settings.tilt)
    }

    @Test
    fun read_zoomOutsideTheMapsRange_isBroughtIntoTheOfferedRange() {
        assertEquals(MapZoomLevel.of(20), MapSettingsCodec.read(preferencesOf(ZOOM_LEVEL_KEY to 99), logger).zoomLevel)
        assertEquals(MapZoomLevel.of(15), MapSettingsCodec.read(preferencesOf(ZOOM_LEVEL_KEY to -3), logger).zoomLevel)
    }

    @Test
    fun read_tiltAboveTheOfferedRange_isTheMostOffered() {
        // 65 degrees is valid for :map, but more than this app offers.
        val settings = MapSettingsCodec.read(preferencesOf(TILT_KEY to 65.0), logger)

        assertEquals(MapTilt.of(60.0), settings.tilt)
        assertEquals(MapZoomLevel.DEFAULT, settings.zoomLevel)
    }

    @Test
    fun read_tiltOutsideTheMapsRange_isBroughtIntoTheOfferedRange() {
        assertEquals(MapTilt.of(0.0), MapSettingsCodec.read(preferencesOf(TILT_KEY to -10.0), logger).tilt)
        assertEquals(
            MapTilt.of(60.0),
            MapSettingsCodec.read(preferencesOf(TILT_KEY to Double.POSITIVE_INFINITY), logger).tilt,
        )
        assertTrue(logger.warnings.isEmpty())
    }

    @Test
    fun read_tiltNotANumber_usesTheDefaultAndWarns() {
        val settings = MapSettingsCodec.read(
            preferencesOf(TILT_KEY to Double.NaN, ZOOM_LEVEL_KEY to 16),
            logger,
        )

        assertEquals(MapSettings(zoomLevel = MapZoomLevel.of(16)), settings)
        assertEquals(1, logger.warnings.size)
    }
}
