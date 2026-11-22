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
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import dev.giste.core.logging.Logger
import dev.giste.odometer.OdometerSettings
import dev.giste.odometer.OdometerSettingsSource
import javax.inject.Inject

/**
 * Stores the `:odometer` settings in the `settings_odometer` DataStore, and provides them to
 * `:odometer` as its [OdometerSettingsSource].
 *
 * @param dataStore the `:odometer` settings DataStore.
 * @param logger logs storage failures and invalid stored values.
 */
internal class DataStoreOdometerSettingsRepository @Inject constructor(
    @OdometerSettingsDataStore dataStore: DataStore<Preferences>,
    logger: Logger,
) : DataStoreSettingsRepository<OdometerSettings>(dataStore, OdometerSettingsCodec, logger, "odometer"),
    OdometerSettingsSource {

    internal companion object {
        /** Name of the DataStore file. Prefixed with the module name (AGENTS.md §7). */
        const val FILE_NAME: String = "settings_odometer"
    }
}

/** [SettingsCodec] of [OdometerSettings]. */
internal object OdometerSettingsCodec : SettingsCodec<OdometerSettings> {

    /** Speed below which a fix counts as stopped, in meters per second. */
    val MIN_SPEED_KEY: Preferences.Key<Double> = doublePreferencesKey("min_speed_mps")

    /** Worst horizontal accuracy a fix may have to be counted, in meters. */
    val MAX_HORIZONTAL_ACCURACY_KEY: Preferences.Key<Double> =
        doublePreferencesKey("max_horizontal_accuracy_meters")

    /** Shortest step counted at once, in meters. */
    val MIN_STEP_DISTANCE_KEY: Preferences.Key<Double> =
        doublePreferencesKey("min_step_distance_meters")

    /** Whether the altitude change is added to the distance. */
    val USE_ALTITUDE_KEY: Preferences.Key<Boolean> = booleanPreferencesKey("use_altitude")

    /** Worst vertical accuracy for the altitude change to be counted, in meters. */
    val MAX_VERTICAL_ACCURACY_KEY: Preferences.Key<Double> =
        doublePreferencesKey("max_vertical_accuracy_meters")

    override val default: OdometerSettings = OdometerSettings()

    override fun read(preferences: Preferences, logger: Logger): OdometerSettings =
        OdometerSettings(
            minSpeed = preferences.readSpeed(MIN_SPEED_KEY, default.minSpeed, logger),
            maxHorizontalAccuracy = preferences.readDistance(
                MAX_HORIZONTAL_ACCURACY_KEY,
                default.maxHorizontalAccuracy,
                logger,
            ),
            minStepDistance = preferences.readDistance(
                MIN_STEP_DISTANCE_KEY,
                default.minStepDistance,
                logger,
            ),
            useAltitude = preferences[USE_ALTITUDE_KEY] ?: default.useAltitude,
            maxVerticalAccuracy = preferences.readDistance(
                MAX_VERTICAL_ACCURACY_KEY,
                default.maxVerticalAccuracy,
                logger,
            ),
        )

    override fun write(preferences: MutablePreferences, settings: OdometerSettings) {
        preferences.storeUnlessDefault(
            MIN_SPEED_KEY,
            settings.minSpeed.inMetersPerSecond,
            default.minSpeed.inMetersPerSecond,
        )
        preferences.storeUnlessDefault(
            MAX_HORIZONTAL_ACCURACY_KEY,
            settings.maxHorizontalAccuracy.inMeters,
            default.maxHorizontalAccuracy.inMeters,
        )
        preferences.storeUnlessDefault(
            MIN_STEP_DISTANCE_KEY,
            settings.minStepDistance.inMeters,
            default.minStepDistance.inMeters,
        )
        preferences.storeUnlessDefault(USE_ALTITUDE_KEY, settings.useAltitude, default.useAltitude)
        preferences.storeUnlessDefault(
            MAX_VERTICAL_ACCURACY_KEY,
            settings.maxVerticalAccuracy.inMeters,
            default.maxVerticalAccuracy.inMeters,
        )
    }
}
