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
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * In-memory [DataStore] for tests, which can be told to fail.
 *
 * A real DataStore cannot be made to fail on demand, so the error paths are tested with this
 * fake. [DataStoreRoadbookRepositoryIntegrationTest] and
 * [DataStoreRoadbookPositionRepositoryIntegrationTest] cover the real one.
 *
 * @param initial the value it starts with.
 */
internal class FakeDataStore<T>(initial: T) : DataStore<T> {

    /** What is "on disk". Tests read it to check what was saved. */
    val stored: MutableStateFlow<T> = MutableStateFlow(initial)

    /** When not `null`, reading [data] throws it, e.g. an [IOException] for a broken disk. */
    var readFailure: Throwable? = null

    /** When `true`, [updateData] throws an [IOException] and stores nothing. */
    var failWrites: Boolean = false

    override val data: Flow<T> = flow {
        readFailure?.let { throw it }
        emitAll(stored)
    }

    override suspend fun updateData(transform: suspend (t: T) -> T): T {
        if (failWrites) throw IOException("Simulated write failure")
        return transform(stored.value).also { stored.value = it }
    }
}
