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

import app.cash.turbine.test
import dev.giste.roadbook.FakeLogger
import dev.giste.roadbook.domain.roadbook.Roadbook
import dev.giste.roadbook.domain.roadbook.SaveRoadbookResult
import dev.giste.roadbook.domain.roadbook.SavedRoadbook
import dev.giste.roadbook.domain.roadbook.testWaypoint
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataStoreRoadbookRepositoryTest {

    private val logger = FakeLogger()
    private val previous = Roadbook(name = "Stage 0", waypoints = listOf(testWaypoint(1)))
    private val dataStore = FakeDataStore<SavedRoadbook>(SavedRoadbook.Available(previous))
    private val repository = DataStoreRoadbookRepository(dataStore, logger)

    @Test
    fun roadbook_emitsStoredValue() = runTest {
        assertEquals(SavedRoadbook.Available(previous), repository.roadbook.first())
    }

    @Test
    fun roadbook_readFails_emitsUnreadableAndLogsError() = runTest {
        dataStore.readFailure = IOException("Simulated read failure")

        assertEquals(SavedRoadbook.Unreadable, repository.roadbook.first())
        assertTrue(logger.loggedErrors.single() is IOException)
    }

    @Test
    fun roadbook_unexpectedError_isNotHidden() = runTest {
        dataStore.readFailure = IllegalStateException("Simulated bug")

        val error = runCatching { repository.roadbook.first() }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
    }

    @Test
    fun save_storesRoadbookAndSucceeds() = runTest {
        val roadbook = FULL_ROADBOOK

        val result = repository.save(roadbook)

        assertEquals(SaveRoadbookResult.Saved, result)
        assertEquals(SavedRoadbook.Available(roadbook), dataStore.stored.value)
    }

    @Test
    fun save_roadbookFlowEmitsNewRoadbook() = runTest {
        repository.roadbook.test {
            assertEquals(SavedRoadbook.Available(previous), awaitItem())

            repository.save(FULL_ROADBOOK)

            assertEquals(SavedRoadbook.Available(FULL_ROADBOOK), awaitItem())
        }
    }

    @Test
    fun save_writeFails_failsKeepsPreviousAndLogsError() = runTest {
        dataStore.failWrites = true

        val result = repository.save(FULL_ROADBOOK)

        assertEquals(SaveRoadbookResult.Failed, result)
        assertEquals(SavedRoadbook.Available(previous), dataStore.stored.value)
        assertTrue(logger.loggedErrors.single() is IOException)
    }
}
