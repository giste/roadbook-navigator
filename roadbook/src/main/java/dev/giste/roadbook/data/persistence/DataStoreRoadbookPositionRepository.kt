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

package dev.giste.roadbook.data.persistence

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import dev.giste.core.logging.Logger
import dev.giste.roadbook.domain.roadbook.RoadbookPositionRepository
import dev.giste.roadbook.domain.roadbook.WaypointNumber
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * [RoadbookPositionRepository] that keeps the position in memory and persists it in a
 * Preferences DataStore.
 *
 * The position is read from [dataStore] once, on first use. After that the copy in memory is the
 * source of truth, because this repository is its only writer, so a failed write does not lose
 * the position while the process lives. Every change is written at once: the position changes
 * once per waypoint, not continuously.
 *
 * Storage failures never escape. A failed read starts from [WaypointNumber.FIRST], an invalid
 * stored value (below 1) is read as [WaypointNumber.FIRST], and a failed write keeps the position
 * in memory. All are logged.
 *
 * It must be a singleton: two instances would each keep their own copy of the position.
 *
 * @param dataStore the roadbook state DataStore. Nothing else may write the position key.
 * @param logger logs storage failures.
 */
internal class DataStoreRoadbookPositionRepository @Inject constructor(
    @param:RoadbookStateDataStore private val dataStore: DataStore<Preferences>,
    private val logger: Logger,
) : RoadbookPositionRepository {

    /** Serializes loading and saving, so a load never overwrites a newer saved position. */
    private val mutex = Mutex()

    /** The current position, or `null` until it has been read from [dataStore]. */
    private val current = MutableStateFlow<WaypointNumber?>(null)

    override val position: Flow<WaypointNumber> = current
        .onStart { mutex.withLock { loadIfNeeded() } }
        .filterNotNull()

    override suspend fun save(position: WaypointNumber) {
        mutex.withLock {
            current.value = position
            try {
                dataStore.edit { preferences -> preferences[POSITION_KEY] = position.value }
            } catch (error: IOException) {
                // The position in memory is still right, and the next change writes it again.
                logger.e(error, "Could not save the roadbook position")
            }
        }
    }

    /**
     * Reads the position from [dataStore], unless it is already known. Call it with [mutex]
     * held.
     */
    private suspend fun loadIfNeeded() {
        if (current.value != null) return

        val preferences = try {
            dataStore.data.first()
        } catch (error: IOException) {
            logger.e(error, "Could not read the roadbook position, using the first waypoint")
            emptyPreferences()
        }
        current.value = preferences.position()
    }

    /** Reads the stored position, or [WaypointNumber.FIRST] if it is missing or invalid. */
    private fun Preferences.position(): WaypointNumber {
        val number = this[POSITION_KEY] ?: return WaypointNumber.FIRST
        if (number < WaypointNumber.FIRST.value) {
            logger.w("Invalid stored roadbook position: %d, using the first waypoint", number)
            return WaypointNumber.FIRST
        }
        return WaypointNumber(number)
    }

    internal companion object {

        /** Name of the DataStore file. Prefixed with the module name (AGENTS.md §7). */
        const val FILE_NAME: String = "roadbook_state"

        /** The number of the waypoint shown at the top. */
        val POSITION_KEY: Preferences.Key<Int> = intPreferencesKey("top_waypoint_number")
    }
}
