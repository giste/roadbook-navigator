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
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.giste.core.logging.Logger
import dev.giste.roadbooknavigator.settings.AppSettings
import dev.giste.roadbooknavigator.settings.AppSettingsSource
import dev.giste.roadbooknavigator.settings.ScreenOrientation
import dev.giste.roadbooknavigator.settings.ThemeMode
import javax.inject.Inject

/**
 * Stores the [AppSettings] in the `settings_app` DataStore, and provides them to the app as its
 * [AppSettingsSource].
 *
 * @param dataStore the app settings DataStore.
 * @param logger logs storage failures and invalid stored values.
 */
internal class DataStoreAppSettingsRepository @Inject constructor(
    @AppSettingsDataStore dataStore: DataStore<Preferences>,
    logger: Logger,
) : DataStoreSettingsRepository<AppSettings>(dataStore, AppSettingsCodec, logger, "app"),
    AppSettingsSource {

    internal companion object {
        /** Name of the DataStore file. Prefixed with the module name (AGENTS.md §7). */
        const val FILE_NAME: String = "settings_app"
    }
}

/** [SettingsCodec] of [AppSettings]. */
internal object AppSettingsCodec : SettingsCodec<AppSettings> {

    /** Name of the [ThemeMode]. */
    val THEME_MODE_KEY: Preferences.Key<String> = stringPreferencesKey("theme_mode")

    /** Name of the [ScreenOrientation]. */
    val ORIENTATION_KEY: Preferences.Key<String> = stringPreferencesKey("orientation")

    override val default: AppSettings = AppSettings()

    override fun read(preferences: Preferences, logger: Logger): AppSettings =
        AppSettings(
            themeMode = preferences.readEnum(THEME_MODE_KEY, default.themeMode, logger),
            orientation = preferences.readEnum(ORIENTATION_KEY, default.orientation, logger),
        )

    override fun write(preferences: MutablePreferences, settings: AppSettings) {
        preferences.storeUnlessDefault(
            THEME_MODE_KEY,
            settings.themeMode.name,
            default.themeMode.name,
        )
        preferences.storeUnlessDefault(
            ORIENTATION_KEY,
            settings.orientation.name,
            default.orientation.name,
        )
    }
}
