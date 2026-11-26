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
import androidx.datastore.preferences.core.intPreferencesKey
import dev.giste.core.logging.Logger
import dev.giste.roadbooknavigator.settings.DashboardSettings
import dev.giste.roadbooknavigator.settings.DashboardSettingsSource
import dev.giste.roadbooknavigator.settings.ScreenShare
import dev.giste.roadbooknavigator.settings.domain.withinOfferedRanges
import javax.inject.Inject

/**
 * Stores the [DashboardSettings] in the `settings_dashboard` DataStore, and provides them to the
 * app as its [DashboardSettingsSource].
 *
 * @param dataStore the dashboard settings DataStore.
 * @param logger logs storage failures and invalid stored values.
 */
internal class DataStoreDashboardSettingsRepository @Inject constructor(
    @DashboardSettingsDataStore dataStore: DataStore<Preferences>,
    logger: Logger,
) : DataStoreSettingsRepository<DashboardSettings>(
    dataStore,
    DashboardSettingsCodec,
    logger,
    "dashboard",
),
    DashboardSettingsSource {

    internal companion object {
        /** Name of the DataStore file. Prefixed with the module name (AGENTS.md §7). */
        const val FILE_NAME: String = "settings_dashboard"
    }
}

/**
 * [SettingsCodec] of [DashboardSettings].
 *
 * Each share is stored as a whole percentage. One that is not a valid [ScreenShare] is the
 * default, with a warning; valid ones are brought into the ranges and steps this app offers
 * (`withinOfferedRanges`), so the screen and the dashboard always agree.
 */
internal object DashboardSettingsCodec : SettingsCodec<DashboardSettings> {

    /** Landscape side column share, in percent. */
    val LANDSCAPE_SIDE_COLUMN_KEY: Preferences.Key<Int> =
        intPreferencesKey("landscape_side_column_percent")

    /** Portrait distances row share, in percent. */
    val PORTRAIT_DISTANCES_KEY: Preferences.Key<Int> =
        intPreferencesKey("portrait_distances_percent")

    override val default: DashboardSettings = DashboardSettings()

    override fun read(preferences: Preferences, logger: Logger): DashboardSettings =
        DashboardSettings(
            landscapeSideColumn = preferences.readShare(
                LANDSCAPE_SIDE_COLUMN_KEY,
                default.landscapeSideColumn,
                logger,
            ),
            portraitDistances = preferences.readShare(
                PORTRAIT_DISTANCES_KEY,
                default.portraitDistances,
                logger,
            ),
        ).withinOfferedRanges()

    override fun write(preferences: MutablePreferences, settings: DashboardSettings) {
        preferences.storeUnlessDefault(
            LANDSCAPE_SIDE_COLUMN_KEY,
            settings.landscapeSideColumn.percent,
            default.landscapeSideColumn.percent,
        )
        preferences.storeUnlessDefault(
            PORTRAIT_DISTANCES_KEY,
            settings.portraitDistances.percent,
            default.portraitDistances.percent,
        )
    }

    /** The share stored under [key], or [default] if it is missing or not a valid share. */
    private fun Preferences.readShare(
        key: Preferences.Key<Int>,
        default: ScreenShare,
        logger: Logger,
    ): ScreenShare {
        val percent = this[key] ?: return default
        if (!ScreenShare.isValid(percent)) {
            logger.w("Invalid stored %s: %s, using the default", key.name, percent)
            return default
        }
        return ScreenShare.of(percent)
    }
}
