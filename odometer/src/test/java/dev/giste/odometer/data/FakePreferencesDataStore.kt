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

package dev.giste.odometer.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * In-memory `DataStore<Preferences>` for tests, which can be told to fail.
 *
 * A real DataStore cannot be made to fail on demand, so the error paths are tested with this
 * fake. [DataStoreOdometerStateRepositoryIntegrationTest] covers the real one.
 *
 * @param initial the preferences it starts with.
 */
internal class FakePreferencesDataStore(
    initial: Preferences = emptyPreferences(),
) : DataStore<Preferences> {

    /** What is "on disk". Tests read it to check what was saved. */
    val stored: MutableStateFlow<Preferences> = MutableStateFlow(initial)

    /** When `true`, reading [data] throws an [IOException]. */
    var failReads: Boolean = false

    /** When `true`, [updateData] throws an [IOException] and stores nothing. */
    var failWrites: Boolean = false

    /** How many writes succeeded. */
    var writeCount: Int = 0
        private set

    override val data: Flow<Preferences> = flow {
        if (failReads) throw IOException("Simulated read failure")
        emitAll(stored)
    }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        if (failWrites) throw IOException("Simulated write failure")
        val updated = transform(stored.value)
        stored.value = updated
        writeCount++
        return updated
    }
}
