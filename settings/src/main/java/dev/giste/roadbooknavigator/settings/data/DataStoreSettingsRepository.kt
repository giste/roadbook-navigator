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
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import dev.giste.core.logging.Logger
import dev.giste.roadbooknavigator.settings.domain.SettingsRepository
import dev.giste.roadbooknavigator.settings.domain.SettingsUpdateResult
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * [SettingsRepository] that stores one module's settings in its own DataStore file, through a
 * [SettingsCodec].
 *
 * Storage failures never escape. A read failure emits the defaults, and a write failure leaves the
 * settings as they were and returns [SettingsUpdateResult.NOT_SAVED]. Both are logged. Only
 * [IOException]s are caught, so cancellation and programming errors propagate.
 *
 * @param T the module's settings model.
 * @param dataStore the module's settings DataStore. Nothing else may write to it.
 * @param codec turns the settings into preferences and back.
 * @param logger logs storage failures and invalid stored values.
 * @param name names the module in log messages, e.g. `location`.
 */
internal abstract class DataStoreSettingsRepository<T>(
    private val dataStore: DataStore<Preferences>,
    private val codec: SettingsCodec<T>,
    private val logger: Logger,
    private val name: String,
) : SettingsRepository<T> {

    /**
     * Cold, like the DataStore it reads: each collector reads the file through DataStore, which
     * shares one copy in memory.
     */
    override val settings: Flow<T> = dataStore.data
        .catch { error ->
            if (error !is IOException) throw error
            logger.e(error, "Could not read the %s settings, using the defaults", name)
            emit(emptyPreferences())
        }
        .map { codec.read(it, logger) }
        .distinctUntilChanged()

    override suspend fun update(transform: (T) -> T): SettingsUpdateResult = try {
        dataStore.edit { preferences ->
            codec.write(preferences, transform(codec.read(preferences, logger)))
        }
        SettingsUpdateResult.SAVED
    } catch (error: IOException) {
        logger.e(error, "Could not save the %s settings", name)
        SettingsUpdateResult.NOT_SAVED
    }
}
