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
import dev.giste.core.logging.Logger
import dev.giste.roadbook.domain.roadbook.Roadbook
import dev.giste.roadbook.domain.roadbook.RoadbookRepository
import dev.giste.roadbook.domain.roadbook.SaveRoadbookResult
import dev.giste.roadbook.domain.roadbook.SavedRoadbook
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * [RoadbookRepository] that keeps the roadbook in a DataStore file, through
 * [RoadbookFileSerializer].
 *
 * DataStore keeps the last value in memory and is the only reader and writer of the file, so
 * there is no copy here. Storage failures never escape: a failed read is
 * [SavedRoadbook.Unreadable], and a failed write is [SaveRoadbookResult.Failed]. Both are logged.
 *
 * @param dataStore the roadbook DataStore. Nothing else may write to it.
 * @param logger logs storage failures.
 */
internal class DataStoreRoadbookRepository @Inject constructor(
    private val dataStore: DataStore<SavedRoadbook>,
    private val logger: Logger,
) : RoadbookRepository {

    override val roadbook: Flow<SavedRoadbook> = dataStore.data.catch { error ->
        // The serializer turns bad content into Unreadable, so only I/O errors get here. Others
        // are bugs, and are not hidden.
        if (error !is IOException) throw error
        logger.e(error, "Could not read the saved roadbook")
        emit(SavedRoadbook.Unreadable)
    }

    override suspend fun save(roadbook: Roadbook): SaveRoadbookResult = try {
        dataStore.updateData { SavedRoadbook.Available(roadbook) }
        SaveRoadbookResult.Saved
    } catch (error: IOException) {
        logger.e(error, "Could not save the roadbook")
        SaveRoadbookResult.Failed
    }
}
