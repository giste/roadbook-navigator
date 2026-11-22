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
import dev.giste.geo.Distance
import dev.giste.geo.Speed
import dev.giste.location.LocationSettings
import dev.giste.roadbooknavigator.settings.FakeLogger
import dev.giste.roadbooknavigator.settings.data.LocationSettingsCodec.MIN_UPDATE_DISTANCE_KEY
import dev.giste.roadbooknavigator.settings.data.LocationSettingsCodec.MOVING_SPEED_KEY
import dev.giste.roadbooknavigator.settings.data.LocationSettingsCodec.STOPPED_SPEED_KEY
import dev.giste.roadbooknavigator.settings.data.LocationSettingsCodec.UPDATE_INTERVAL_KEY
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationSettingsCodecTest {

    private val logger = FakeLogger()

    private val custom = LocationSettings(
        updateInterval = 2.seconds,
        minUpdateDistance = Distance.meters(10.0),
        movingSpeed = Speed.kilometersPerHour(4.0),
        stoppedSpeed = Speed.kilometersPerHour(2.0),
    )

    @Test
    fun read_nothingStored_isTheDefaults() {
        assertEquals(LocationSettings(), LocationSettingsCodec.read(preferencesOf(), logger))
    }

    @Test
    fun read_writtenSettings_isTheSameSettings() {
        val preferences = mutablePreferencesOf()
        LocationSettingsCodec.write(preferences, custom)

        assertEquals(custom, LocationSettingsCodec.read(preferences, logger))
    }

    @Test
    fun write_defaults_storesNothing() {
        val preferences = mutablePreferencesOf()

        LocationSettingsCodec.write(preferences, LocationSettings())

        assertTrue(preferences.asMap().isEmpty())
    }

    @Test
    fun write_backToDefault_removesTheStoredValue() {
        val preferences = mutablePreferencesOf()
        LocationSettingsCodec.write(preferences, custom)

        LocationSettingsCodec.write(preferences, LocationSettings())

        assertTrue(preferences.asMap().isEmpty())
    }

    @Test
    fun read_nonPositiveInterval_usesTheDefaultAndWarns() {
        val settings = LocationSettingsCodec.read(preferencesOf(UPDATE_INTERVAL_KEY to 0L), logger)

        assertEquals(LocationSettings.DEFAULT_UPDATE_INTERVAL, settings.updateInterval)
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun read_negativeDistance_usesTheDefaultAndWarns() {
        val settings =
            LocationSettingsCodec.read(preferencesOf(MIN_UPDATE_DISTANCE_KEY to -1.0), logger)

        assertEquals(LocationSettings.DEFAULT_MIN_UPDATE_DISTANCE, settings.minUpdateDistance)
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun read_notFiniteSpeed_usesTheDefaultAndWarns() {
        val settings =
            LocationSettingsCodec.read(preferencesOf(STOPPED_SPEED_KEY to Double.NaN), logger)

        assertEquals(LocationSettings.DEFAULT_STOPPED_SPEED, settings.stoppedSpeed)
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun read_movingSpeedBelowStoppedSpeed_usesBothDefaultsAndWarns() {
        val settings = LocationSettingsCodec.read(
            preferencesOf(MOVING_SPEED_KEY to 1.0, STOPPED_SPEED_KEY to 2.0),
            logger,
        )

        assertEquals(LocationSettings.DEFAULT_MOVING_SPEED, settings.movingSpeed)
        assertEquals(LocationSettings.DEFAULT_STOPPED_SPEED, settings.stoppedSpeed)
        assertEquals(1, logger.warnings.size)
    }
}
