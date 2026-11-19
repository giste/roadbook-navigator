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
import androidx.datastore.core.DataStoreFactory
import dev.giste.roadbook.FakeLogger
import dev.giste.roadbook.domain.roadbook.SaveRoadbookResult
import dev.giste.roadbook.domain.roadbook.SavedRoadbook
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** [DataStoreRoadbookRepository] on a real DataStore file in a temporary folder. */
class DataStoreRoadbookRepositoryIntegrationTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val logger = FakeLogger()

    private val file: File by lazy { File(folder.root, RoadbookFileSerializer.FILE_NAME) }

    /**
     * Opens the DataStore file in a child scope of the test, so the test can close it again
     * before reopening the same file: DataStore allows one open instance per file.
     */
    private fun TestScope.openDataStore(job: Job): DataStore<SavedRoadbook> =
        DataStoreFactory.create(
            serializer = RoadbookFileSerializer(logger, StandardTestDispatcher(testScheduler)),
            scope = CoroutineScope(coroutineContext + job),
            produceFile = { file },
        )

    @Test
    fun roadbook_noFile_emitsNone() = runTest {
        val job = Job()
        val repository = DataStoreRoadbookRepository(openDataStore(job), logger)

        assertEquals(SavedRoadbook.None, repository.roadbook.first())
        job.cancelAndJoin()
    }

    @Test
    fun save_savedRoadbook_isReadAfterReopeningTheFile() = runTest {
        val firstJob = Job()
        val result = DataStoreRoadbookRepository(openDataStore(firstJob), logger)
            .save(FULL_ROADBOOK)
        firstJob.cancelAndJoin()

        val secondJob = Job()
        val reopened = DataStoreRoadbookRepository(openDataStore(secondJob), logger)

        assertEquals(SaveRoadbookResult.Saved, result)
        assertEquals(SavedRoadbook.Available(FULL_ROADBOOK), reopened.roadbook.first())
        secondJob.cancelAndJoin()
    }

    @Test
    fun roadbook_damagedFile_emitsUnreadableAndKeepsFile() = runTest {
        file.writeText("{damaged")
        val job = Job()
        val repository = DataStoreRoadbookRepository(openDataStore(job), logger)

        assertEquals(SavedRoadbook.Unreadable, repository.roadbook.first())
        job.cancelAndJoin()
        assertEquals("{damaged", file.readText())
    }

    @Test
    fun save_afterDamagedFile_replacesIt() = runTest {
        file.writeText("{damaged")
        val firstJob = Job()
        DataStoreRoadbookRepository(openDataStore(firstJob), logger).save(FULL_ROADBOOK)
        firstJob.cancelAndJoin()

        val secondJob = Job()
        val reopened = DataStoreRoadbookRepository(openDataStore(secondJob), logger)

        assertEquals(SavedRoadbook.Available(FULL_ROADBOOK), reopened.roadbook.first())
        secondJob.cancelAndJoin()
    }
}
