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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.giste.core.logging.Logger
import dev.giste.roadbook.RoadbookSettings
import dev.giste.roadbook.RoadbookSettingsSource
import dev.giste.roadbook.RoadbookStyle
import dev.giste.roadbook.ShortDistanceThreshold
import dev.giste.roadbooknavigator.settings.domain.withinOfferedRange
import javax.inject.Inject

/**
 * Stores the `:roadbook` settings in the `settings_roadbook` DataStore, and provides them to
 * `:roadbook` as its [RoadbookSettingsSource].
 *
 * @param dataStore the `:roadbook` settings DataStore.
 * @param logger logs storage failures and invalid stored values.
 */
internal class DataStoreRoadbookSettingsRepository @Inject constructor(
    @RoadbookSettingsDataStore dataStore: DataStore<Preferences>,
    logger: Logger,
) : DataStoreSettingsRepository<RoadbookSettings>(dataStore, RoadbookSettingsCodec, logger, "roadbook"),
    RoadbookSettingsSource {

    internal companion object {
        /** Name of the DataStore file. Prefixed with the module name (AGENTS.md §7). */
        const val FILE_NAME: String = "settings_roadbook"
    }
}

/**
 * [SettingsCodec] of [RoadbookSettings].
 *
 * A stored threshold is brought into [ShortDistanceThreshold.RANGE] with
 * [ShortDistanceThreshold.coerce], as that type asks for values read from storage, and then into
 * the narrower range the settings screen offers ([withinOfferedRange]), so the screen and the
 * roadbook always agree.
 */
internal object RoadbookSettingsCodec : SettingsCodec<RoadbookSettings> {

    /** Short-distance threshold, in meters. */
    val SHORT_DISTANCE_THRESHOLD_KEY: Preferences.Key<Double> =
        doublePreferencesKey("short_distance_threshold_meters")

    /** Name of the [RoadbookStyle]. */
    val STYLE_KEY: Preferences.Key<String> = stringPreferencesKey("style")

    override val default: RoadbookSettings = RoadbookSettings()

    override fun read(preferences: Preferences, logger: Logger): RoadbookSettings {
        val distance = preferences.readDistance(
            SHORT_DISTANCE_THRESHOLD_KEY,
            default.shortDistanceThreshold.distance,
            logger,
        )
        return RoadbookSettings(
            shortDistanceThreshold = ShortDistanceThreshold.coerce(distance).withinOfferedRange(),
            style = preferences.readEnum(STYLE_KEY, default.style, logger),
        )
    }

    override fun write(preferences: MutablePreferences, settings: RoadbookSettings) {
        preferences.storeUnlessDefault(
            SHORT_DISTANCE_THRESHOLD_KEY,
            settings.shortDistanceThreshold.distance.inMeters,
            default.shortDistanceThreshold.distance.inMeters,
        )
        preferences.storeUnlessDefault(STYLE_KEY, settings.style.name, default.style.name)
    }
}
