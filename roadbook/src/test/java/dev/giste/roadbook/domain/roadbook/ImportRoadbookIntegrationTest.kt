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

package dev.giste.roadbook.domain.roadbook

import androidx.datastore.core.DataStoreFactory
import dev.giste.roadbook.FakeLogger
import dev.giste.roadbook.ImportRoadbookResult
import dev.giste.roadbook.data.persistence.DataStoreRoadbookRepository
import dev.giste.roadbook.data.persistence.RoadbookFileSerializer
import dev.giste.roadbook.data.rn2.Rn2ElementMapper
import dev.giste.roadbook.data.rn2.Rn2RoadbookParser
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Imports `route_example.rn2`, a real Rally Navigator export, with the real parser and a real
 * DataStore file, and reads the saved roadbook back after reopening the file.
 */
class ImportRoadbookIntegrationTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val logger = FakeLogger()

    private fun example(): String =
        checkNotNull(javaClass.classLoader?.getResource("route_example.rn2")) {
            "route_example.rn2 not found"
        }.readText()

    private fun TestScope.repository(file: File, job: Job) = DataStoreRoadbookRepository(
        dataStore = DataStoreFactory.create(
            serializer = RoadbookFileSerializer(logger, StandardTestDispatcher(testScheduler)),
            scope = CoroutineScope(coroutineContext + job),
            produceFile = { file },
        ),
        logger = logger,
    )

    @Test
    fun import_exampleFile_isRestoredUnchangedAfterReopening() = runTest {
        val file = File(folder.root, RoadbookFileSerializer.FILE_NAME)
        val parser = Rn2RoadbookParser(
            Rn2ElementMapper(logger),
            logger,
            StandardTestDispatcher(testScheduler),
        )
        val firstJob = Job()
        val firstRepository = repository(file, firstJob)

        val result = ImportRoadbookUseCase(parser, firstRepository, logger)(
            RoadbookSource { ReadRoadbookSourceResult.Success(example()) },
        )
        val imported = firstRepository.roadbook.first()
        firstJob.cancelAndJoin()
        val secondJob = Job()
        val restored = repository(file, secondJob).roadbook.first()
        secondJob.cancelAndJoin()

        assertEquals(ImportRoadbookResult.Success, result)
        assertTrue(imported is SavedRoadbook.Available)
        assertEquals(imported, restored)
    }
}
