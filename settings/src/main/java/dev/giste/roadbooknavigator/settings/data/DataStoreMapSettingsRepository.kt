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
import androidx.datastore.preferences.core.intPreferencesKey
import dev.giste.core.logging.Logger
import dev.giste.map.MapSettings
import dev.giste.map.MapSettingsSource
import dev.giste.map.MapTilt
import dev.giste.map.MapZoomLevel
import dev.giste.roadbooknavigator.settings.domain.withinOfferedRanges
import javax.inject.Inject

/**
 * Stores the `:map` settings in the `settings_map` DataStore, and provides them to `:map` as its
 * [MapSettingsSource].
 *
 * @param dataStore the `:map` settings DataStore.
 * @param logger logs storage failures and invalid stored values.
 */
internal class DataStoreMapSettingsRepository @Inject constructor(
    @MapSettingsDataStore dataStore: DataStore<Preferences>,
    logger: Logger,
) : DataStoreSettingsRepository<MapSettings>(dataStore, MapSettingsCodec, logger, "map"),
    MapSettingsSource {

    internal companion object {
        /** Name of the DataStore file. Prefixed with the module name (AGENTS.md §7). */
        const val FILE_NAME: String = "settings_map"
    }
}

/**
 * [SettingsCodec] of [MapSettings].
 *
 * Stored values are brought into the ranges this app offers (`withinOfferedRanges`), which lie
 * within `:map`'s own, so a value stored outside them never reaches the map. A tilt that is not a
 * number is the default, with a warning.
 */
internal object MapSettingsCodec : SettingsCodec<MapSettings> {

    /** Zoom level. */
    val ZOOM_LEVEL_KEY: Preferences.Key<Int> = intPreferencesKey("zoom_level")

    /** Tilt, in degrees. */
    val TILT_KEY: Preferences.Key<Double> = doublePreferencesKey("tilt_degrees")

    override val default: MapSettings = MapSettings()

    override fun read(preferences: Preferences, logger: Logger): MapSettings {
        val zoomLevel = preferences[ZOOM_LEVEL_KEY]?.let(MapZoomLevel::coerce) ?: default.zoomLevel
        val tilt = when (val degrees = preferences[TILT_KEY]) {
            null -> default.tilt
            else -> if (degrees.isNaN()) {
                logger.w("Invalid stored %s: %s, using the default", TILT_KEY.name, degrees)
                default.tilt
            } else {
                MapTilt.coerce(degrees)
            }
        }
        return MapSettings(zoomLevel = zoomLevel, tilt = tilt).withinOfferedRanges()
    }

    override fun write(preferences: MutablePreferences, settings: MapSettings) {
        preferences.storeUnlessDefault(ZOOM_LEVEL_KEY, settings.zoomLevel.value, default.zoomLevel.value)
        preferences.storeUnlessDefault(TILT_KEY, settings.tilt.inDegrees, default.tilt.inDegrees)
    }
}
