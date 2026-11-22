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
import dev.giste.odometer.OdometerSettings
import dev.giste.roadbooknavigator.settings.FakeLogger
import dev.giste.roadbooknavigator.settings.data.OdometerSettingsCodec.MAX_HORIZONTAL_ACCURACY_KEY
import dev.giste.roadbooknavigator.settings.data.OdometerSettingsCodec.MAX_VERTICAL_ACCURACY_KEY
import dev.giste.roadbooknavigator.settings.data.OdometerSettingsCodec.MIN_SPEED_KEY
import dev.giste.roadbooknavigator.settings.data.OdometerSettingsCodec.MIN_STEP_DISTANCE_KEY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OdometerSettingsCodecTest {

    private val logger = FakeLogger()

    private val custom = OdometerSettings(
        minSpeed = Speed.metersPerSecond(1.0),
        maxHorizontalAccuracy = Distance.meters(10.0),
        minStepDistance = Distance.meters(3.0),
        useAltitude = true,
        maxVerticalAccuracy = Distance.meters(2.0),
    )

    @Test
    fun read_nothingStored_isTheDefaults() {
        assertEquals(OdometerSettings(), OdometerSettingsCodec.read(preferencesOf(), logger))
    }

    @Test
    fun read_writtenSettings_isTheSameSettings() {
        val preferences = mutablePreferencesOf()
        OdometerSettingsCodec.write(preferences, custom)

        assertEquals(custom, OdometerSettingsCodec.read(preferences, logger))
    }

    @Test
    fun write_defaults_storesNothing() {
        val preferences = mutablePreferencesOf()

        OdometerSettingsCodec.write(preferences, OdometerSettings())

        assertTrue(preferences.asMap().isEmpty())
    }

    @Test
    fun read_negativeMinSpeed_usesTheDefaultAndWarns() {
        val settings = OdometerSettingsCodec.read(preferencesOf(MIN_SPEED_KEY to -1.0), logger)

        assertEquals(OdometerSettings.DEFAULT_MIN_SPEED, settings.minSpeed)
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun read_invalidDistances_useTheDefaultsAndWarn() {
        val settings = OdometerSettingsCodec.read(
            preferencesOf(
                MAX_HORIZONTAL_ACCURACY_KEY to Double.POSITIVE_INFINITY,
                MIN_STEP_DISTANCE_KEY to -5.0,
                MAX_VERTICAL_ACCURACY_KEY to Double.NaN,
            ),
            logger,
        )

        assertEquals(OdometerSettings(), settings)
        assertEquals(3, logger.warnings.size)
    }
}
