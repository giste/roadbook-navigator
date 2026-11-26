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
import dev.giste.roadbook.RoadbookSettings
import dev.giste.roadbook.RoadbookStyle
import dev.giste.roadbook.ShortDistanceThreshold
import dev.giste.roadbooknavigator.settings.FakeLogger
import dev.giste.roadbooknavigator.settings.data.RoadbookSettingsCodec.SHORT_DISTANCE_THRESHOLD_KEY
import dev.giste.roadbooknavigator.settings.data.RoadbookSettingsCodec.STYLE_KEY
import dev.giste.roadbooknavigator.settings.domain.SHORT_DISTANCE_MAX
import dev.giste.roadbooknavigator.settings.domain.SHORT_DISTANCE_MIN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoadbookSettingsCodecTest {

    private val logger = FakeLogger()

    private val custom = RoadbookSettings(
        shortDistanceThreshold = ShortDistanceThreshold.of(Distance.meters(400.0)),
        style = RoadbookStyle.FIA,
    )

    @Test
    fun read_nothingStored_isTheDefaults() {
        assertEquals(RoadbookSettings(), RoadbookSettingsCodec.read(preferencesOf(), logger))
    }

    @Test
    fun read_writtenSettings_isTheSameSettings() {
        val preferences = mutablePreferencesOf()
        RoadbookSettingsCodec.write(preferences, custom)

        assertEquals(custom, RoadbookSettingsCodec.read(preferences, logger))
    }

    @Test
    fun write_defaults_storesNothing() {
        val preferences = mutablePreferencesOf()

        RoadbookSettingsCodec.write(preferences, RoadbookSettings())

        assertTrue(preferences.asMap().isEmpty())
    }

    @Test
    fun read_thresholdAboveTheRange_isTheLongestOffered() {
        val settings = RoadbookSettingsCodec.read(
            preferencesOf(SHORT_DISTANCE_THRESHOLD_KEY to 5_000.0),
            logger,
        )

        assertEquals(SHORT_DISTANCE_MAX, settings.shortDistanceThreshold)
    }

    @Test
    fun read_thresholdAboveTheOfferedRange_isTheLongestOffered() {
        // 1 km: valid for :roadbook, and offered by earlier versions.
        val settings = RoadbookSettingsCodec.read(
            preferencesOf(SHORT_DISTANCE_THRESHOLD_KEY to 1_000.0),
            logger,
        )

        assertEquals(SHORT_DISTANCE_MAX, settings.shortDistanceThreshold)
    }

    @Test
    fun read_thresholdBelowTheOfferedRange_isTheShortestOffered() {
        val settings =
            RoadbookSettingsCodec.read(preferencesOf(SHORT_DISTANCE_THRESHOLD_KEY to 50.0), logger)

        assertEquals(SHORT_DISTANCE_MIN, settings.shortDistanceThreshold)
    }

    @Test
    fun read_thresholdInTheOfferedRange_isKept() {
        val settings =
            RoadbookSettingsCodec.read(preferencesOf(SHORT_DISTANCE_THRESHOLD_KEY to 400.0), logger)

        assertEquals(400.0, settings.shortDistanceThreshold.distance.inMeters, 0.0)
    }

    @Test
    fun read_negativeThreshold_usesTheDefaultAndWarns() {
        val settings =
            RoadbookSettingsCodec.read(preferencesOf(SHORT_DISTANCE_THRESHOLD_KEY to -1.0), logger)

        assertEquals(ShortDistanceThreshold.DEFAULT, settings.shortDistanceThreshold)
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun read_unknownStyle_usesTheDefaultAndWarns() {
        val settings = RoadbookSettingsCodec.read(preferencesOf(STYLE_KEY to "NEON"), logger)

        assertEquals(RoadbookStyle.APP_THEME, settings.style)
        assertEquals(1, logger.warnings.size)
    }
}
