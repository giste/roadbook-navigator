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
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import dev.giste.roadbook.FakeLogger
import dev.giste.roadbook.domain.roadbook.WaypointNumber
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** [DataStoreRoadbookPositionRepository] on a real DataStore file in a temporary folder. */
class DataStoreRoadbookPositionRepositoryIntegrationTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val logger = FakeLogger()

    private val file: File by lazy {
        File(folder.root, "${DataStoreRoadbookPositionRepository.FILE_NAME}.preferences_pb")
    }

    /**
     * Opens the DataStore file in a child scope of the test, so the test can close it again
     * before reopening the same file: DataStore allows one open instance per file.
     */
    private fun TestScope.openDataStore(job: Job): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(coroutineContext + job),
            produceFile = { file },
        )

    @Test
    fun position_newFile_emitsFirstWaypoint() = runTest {
        val job = Job()
        val repository = DataStoreRoadbookPositionRepository(openDataStore(job), logger)

        assertEquals(WaypointNumber.FIRST, repository.position.first())
        job.cancelAndJoin()
    }

    @Test
    fun save_savedPosition_isReadAfterReopeningTheFile() = runTest {
        val firstJob = Job()
        DataStoreRoadbookPositionRepository(openDataStore(firstJob), logger)
            .save(WaypointNumber(7))
        firstJob.cancelAndJoin()

        val secondJob = Job()
        val reopened = DataStoreRoadbookPositionRepository(openDataStore(secondJob), logger)

        assertEquals(WaypointNumber(7), reopened.position.first())
        secondJob.cancelAndJoin()
    }
}
