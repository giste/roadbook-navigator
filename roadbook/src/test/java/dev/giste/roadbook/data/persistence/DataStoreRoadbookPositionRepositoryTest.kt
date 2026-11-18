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

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import app.cash.turbine.test
import dev.giste.roadbook.FakeLogger
import dev.giste.roadbook.data.persistence.DataStoreRoadbookPositionRepository.Companion.POSITION_KEY
import dev.giste.roadbook.domain.roadbook.WaypointNumber
import java.io.IOException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataStoreRoadbookPositionRepositoryTest {

    private val logger = FakeLogger()

    private fun repositoryOf(dataStore: FakeDataStore<Preferences>) =
        DataStoreRoadbookPositionRepository(dataStore, logger)

    private fun stored(number: Int): Preferences = preferencesOf(POSITION_KEY to number)

    @Test
    fun position_nothingStored_emitsFirstWaypoint() = runTest {
        val repository = repositoryOf(FakeDataStore(emptyPreferences()))

        assertEquals(WaypointNumber.FIRST, repository.position.first())
    }

    @Test
    fun position_valueStored_emitsIt() = runTest {
        val repository = repositoryOf(FakeDataStore(stored(12)))

        assertEquals(WaypointNumber(12), repository.position.first())
    }

    @Test
    fun position_zeroStored_emitsFirstWaypointAndWarns() = runTest {
        val repository = repositoryOf(FakeDataStore(stored(0)))

        assertEquals(WaypointNumber.FIRST, repository.position.first())
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun position_negativeStored_emitsFirstWaypointAndWarns() = runTest {
        val repository = repositoryOf(FakeDataStore(stored(-3)))

        assertEquals(WaypointNumber.FIRST, repository.position.first())
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun position_readFails_emitsFirstWaypointAndLogsError() = runTest {
        val dataStore = FakeDataStore(stored(12)).apply {
            readFailure = IOException("Simulated read failure")
        }

        assertEquals(WaypointNumber.FIRST, repositoryOf(dataStore).position.first())
        assertTrue(logger.loggedErrors.single() is IOException)
    }

    @Test
    fun position_readsStorageOnlyOnce() = runTest {
        val dataStore = FakeDataStore(stored(12))
        val repository = repositoryOf(dataStore)
        repository.position.first()

        // Later reads come from memory, so a broken disk no longer matters.
        dataStore.readFailure = IOException("Simulated read failure")

        assertEquals(WaypointNumber(12), repository.position.first())
        assertTrue(logger.loggedErrors.isEmpty())
    }

    @Test
    fun save_writesPositionAtOnce() = runTest {
        val dataStore = FakeDataStore(stored(12))

        repositoryOf(dataStore).save(WaypointNumber(13))

        assertEquals(stored(13), dataStore.stored.value)
    }

    @Test
    fun save_positionEmitsNewValue() = runTest {
        val repository = repositoryOf(FakeDataStore(stored(12)))

        repository.position.test {
            assertEquals(WaypointNumber(12), awaitItem())

            repository.save(WaypointNumber(13))

            assertEquals(WaypointNumber(13), awaitItem())
        }
    }

    @Test
    fun save_beforeFirstRead_isNotOverwrittenByStoredValue() = runTest {
        val dataStore = FakeDataStore(stored(12)).apply { failWrites = true }
        val repository = repositoryOf(dataStore)

        repository.save(WaypointNumber(5))

        // The write failed, so the disk still holds 12, but the newer position wins.
        assertEquals(WaypointNumber(5), repository.position.first())
    }

    @Test
    fun save_writeFails_keepsPositionInMemoryAndLogsError() = runTest {
        val dataStore = FakeDataStore(stored(12))
        val repository = repositoryOf(dataStore)
        repository.position.first()
        dataStore.failWrites = true

        repository.save(WaypointNumber(13))

        assertEquals(WaypointNumber(13), repository.position.first())
        assertEquals(stored(12), dataStore.stored.value)
        assertTrue(logger.loggedErrors.single() is IOException)
    }

    @Test
    fun save_afterFailedWrite_writesAgain() = runTest {
        val dataStore = FakeDataStore(stored(12)).apply { failWrites = true }
        val repository = repositoryOf(dataStore)
        repository.save(WaypointNumber(13))

        dataStore.failWrites = false
        repository.save(WaypointNumber(14))

        assertEquals(stored(14), dataStore.stored.value)
    }
}
