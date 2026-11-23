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
import dev.giste.roadbooknavigator.settings.RemoteAction
import dev.giste.roadbooknavigator.settings.RemoteKey
import dev.giste.roadbooknavigator.settings.RemoteKeys
import dev.giste.roadbooknavigator.settings.RemoteModel
import dev.giste.roadbooknavigator.settings.RemoteSettings
import dev.giste.roadbooknavigator.settings.RemoteSettingsSource
import javax.inject.Inject

/**
 * Stores the [RemoteSettings] in the `settings_remote` DataStore, and provides them to the app as
 * its [RemoteSettingsSource].
 *
 * @param dataStore the remote settings DataStore.
 * @param logger logs storage failures and invalid stored values.
 */
internal class DataStoreRemoteSettingsRepository @Inject constructor(
    @RemoteSettingsDataStore dataStore: DataStore<Preferences>,
    logger: Logger,
) : DataStoreSettingsRepository<RemoteSettings>(dataStore, RemoteSettingsCodec, logger, "remote"),
    RemoteSettingsSource {

    internal companion object {
        /** Name of the DataStore file. Prefixed with the module name (AGENTS.md §7). */
        const val FILE_NAME: String = "settings_remote"
    }
}

/**
 * [SettingsCodec] of [RemoteSettings].
 *
 * Each custom action stores its key codes as a comma-separated list, e.g. `85,126`. An empty
 * list means the action has no key, which differs from a missing value (the default keys).
 */
internal object RemoteSettingsCodec : SettingsCodec<RemoteSettings> {

    /** Separates the key codes of one action. */
    private const val SEPARATOR = ","

    /** Name of the [RemoteModel]. */
    val MODEL_KEY: Preferences.Key<String> = stringPreferencesKey("model")

    /** The custom key codes of each action, e.g. `custom_next_waypoint`. */
    val CUSTOM_KEYS: Map<RemoteAction, Preferences.Key<String>> = RemoteAction.entries.associateWith {
        stringPreferencesKey("custom_${it.name.lowercase()}")
    }

    override val default: RemoteSettings = RemoteSettings()

    override fun read(preferences: Preferences, logger: Logger): RemoteSettings = RemoteSettings(
        model = preferences.readEnum(MODEL_KEY, default.model, logger),
        customKeys = readCustomKeys(preferences, logger),
    )

    override fun write(preferences: MutablePreferences, settings: RemoteSettings) {
        preferences.storeUnlessDefault(MODEL_KEY, settings.model.name, default.model.name)
        CUSTOM_KEYS.forEach { (action, key) ->
            preferences.storeUnlessDefault(
                key,
                settings.customKeys.keysFor(action).toStored(),
                default.customKeys.keysFor(action).toStored(),
            )
        }
    }

    /**
     * The custom keys in [preferences]. An action whose value cannot be read gets its default
     * keys. If the result binds a key to two actions, which only a damaged file can do, every
     * action gets its default keys, because there is no telling which binding the rider meant.
     */
    private fun readCustomKeys(preferences: Preferences, logger: Logger): RemoteKeys {
        val bindings = CUSTOM_KEYS.mapValues { (action, key) ->
            preferences.readKeys(key, default.customKeys.keysFor(action), logger)
        }
        val all = bindings.values.flatten()
        if (all.size != all.toSet().size) {
            logger.w("Stored custom remote keys bind a key twice, using the defaults")
            return default.customKeys
        }
        return RemoteKeys(bindings)
    }

    /**
     * The keys stored under [key], or [default] if they are missing, or if any code is not a
     * positive number.
     */
    private fun Preferences.readKeys(
        key: Preferences.Key<String>,
        default: Set<RemoteKey>,
        logger: Logger,
    ): Set<RemoteKey> {
        val stored = this[key] ?: return default
        if (stored.isEmpty()) return emptySet()
        val codes = stored.split(SEPARATOR).map { it.toIntOrNull() }
        if (codes.any { it == null || it <= 0 }) {
            logger.w("Invalid stored %s: %s, using the default", key.name, stored)
            return default
        }
        return codes.map { RemoteKey(checkNotNull(it)) }.toSet()
    }

    /** These keys as stored: their codes, sorted so equal sets store equal text. */
    private fun Set<RemoteKey>.toStored(): String =
        map { it.code }.sorted().joinToString(SEPARATOR)
}
