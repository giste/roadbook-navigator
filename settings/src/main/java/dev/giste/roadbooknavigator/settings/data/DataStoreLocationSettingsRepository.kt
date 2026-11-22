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
import androidx.datastore.preferences.core.longPreferencesKey
import dev.giste.core.logging.Logger
import dev.giste.location.LocationSettings
import dev.giste.location.LocationSettingsSource
import javax.inject.Inject

/**
 * Stores the `:location` settings in the `settings_location` DataStore, and provides them to
 * `:location` as its [LocationSettingsSource].
 *
 * @param dataStore the `:location` settings DataStore.
 * @param logger logs storage failures and invalid stored values.
 */
internal class DataStoreLocationSettingsRepository @Inject constructor(
    @LocationSettingsDataStore dataStore: DataStore<Preferences>,
    logger: Logger,
) : DataStoreSettingsRepository<LocationSettings>(dataStore, LocationSettingsCodec, logger, "location"),
    LocationSettingsSource {

    internal companion object {
        /** Name of the DataStore file. Prefixed with the module name (AGENTS.md §7). */
        const val FILE_NAME: String = "settings_location"
    }
}

/**
 * [SettingsCodec] of [LocationSettings].
 *
 * The motion speeds are read as a pair: if the stored moving speed is below the stored stopped
 * speed (which [LocationSettings] rejects), both fall back to their defaults.
 */
internal object LocationSettingsCodec : SettingsCodec<LocationSettings> {

    /** Desired time between fixes, in milliseconds. */
    val UPDATE_INTERVAL_KEY: Preferences.Key<Long> = longPreferencesKey("update_interval_millis")

    /** Minimum movement between fixes, in meters. */
    val MIN_UPDATE_DISTANCE_KEY: Preferences.Key<Double> =
        doublePreferencesKey("min_update_distance_meters")

    /** Speed above which a stopped device starts moving, in meters per second. */
    val MOVING_SPEED_KEY: Preferences.Key<Double> = doublePreferencesKey("moving_speed_mps")

    /** Speed below which a moving device is stopped again, in meters per second. */
    val STOPPED_SPEED_KEY: Preferences.Key<Double> = doublePreferencesKey("stopped_speed_mps")

    override val default: LocationSettings = LocationSettings()

    override fun read(preferences: Preferences, logger: Logger): LocationSettings {
        var movingSpeed = preferences.readSpeed(MOVING_SPEED_KEY, default.movingSpeed, logger)
        var stoppedSpeed = preferences.readSpeed(STOPPED_SPEED_KEY, default.stoppedSpeed, logger)
        if (movingSpeed < stoppedSpeed) {
            logger.w(
                "Stored moving speed %s is below stopped speed %s, using the defaults",
                movingSpeed,
                stoppedSpeed,
            )
            movingSpeed = default.movingSpeed
            stoppedSpeed = default.stoppedSpeed
        }
        return LocationSettings(
            updateInterval = preferences.readPositiveDuration(
                UPDATE_INTERVAL_KEY,
                default.updateInterval,
                logger,
            ),
            minUpdateDistance = preferences.readDistance(
                MIN_UPDATE_DISTANCE_KEY,
                default.minUpdateDistance,
                logger,
            ),
            movingSpeed = movingSpeed,
            stoppedSpeed = stoppedSpeed,
        )
    }

    override fun write(preferences: MutablePreferences, settings: LocationSettings) {
        preferences.storeUnlessDefault(
            UPDATE_INTERVAL_KEY,
            settings.updateInterval.inWholeMilliseconds,
            default.updateInterval.inWholeMilliseconds,
        )
        preferences.storeUnlessDefault(
            MIN_UPDATE_DISTANCE_KEY,
            settings.minUpdateDistance.inMeters,
            default.minUpdateDistance.inMeters,
        )
        preferences.storeUnlessDefault(
            MOVING_SPEED_KEY,
            settings.movingSpeed.inMetersPerSecond,
            default.movingSpeed.inMetersPerSecond,
        )
        preferences.storeUnlessDefault(
            STOPPED_SPEED_KEY,
            settings.stoppedSpeed.inMetersPerSecond,
            default.stoppedSpeed.inMetersPerSecond,
        )
    }
}
