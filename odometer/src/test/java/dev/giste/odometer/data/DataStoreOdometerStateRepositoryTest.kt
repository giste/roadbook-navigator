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

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import app.cash.turbine.test
import dev.giste.geo.Distance
import dev.giste.odometer.FakeLogger
import dev.giste.odometer.OdometerState
import dev.giste.odometer.data.DataStoreOdometerStateRepository.Companion.SAVE_DISTANCE
import dev.giste.odometer.data.DataStoreOdometerStateRepository.Companion.SAVE_INTERVAL
import dev.giste.odometer.data.DataStoreOdometerStateRepository.Companion.TOTAL_KEY
import dev.giste.odometer.data.DataStoreOdometerStateRepository.Companion.TRIP_KEY
import java.io.IOException
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataStoreOdometerStateRepositoryTest {

    private val logger = FakeLogger()

    /** Creates the repository under test, measuring time with the virtual time of the test. */
    private fun TestScope.createRepository(dataStore: FakePreferencesDataStore) =
        DataStoreOdometerStateRepository(
            dataStore = dataStore,
            timeSource = testScheduler.timeSource,
            logger = logger,
        )

    private fun stored(trip: Double, total: Double): Preferences =
        preferencesOf(TRIP_KEY to trip, TOTAL_KEY to total)

    private fun state(trip: Double, total: Double) =
        OdometerState(trip = Distance.meters(trip), total = Distance.meters(total))

    @Test
    fun state_nothingStored_emitsZero() = runTest {
        val repository = createRepository(FakePreferencesDataStore())

        assertEquals(OdometerState.ZERO, repository.state.first())
    }

    @Test
    fun state_valuesStored_emitsThem() = runTest {
        val repository = createRepository(FakePreferencesDataStore(stored(12.5, 3_000.0)))

        assertEquals(state(12.5, 3_000.0), repository.state.first())
    }

    @Test
    fun state_negativeValueStored_readsZeroAndWarns() = runTest {
        val repository = createRepository(FakePreferencesDataStore(stored(-1.0, 3_000.0)))

        assertEquals(state(0.0, 3_000.0), repository.state.first())
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun state_nanValueStored_readsZeroAndWarns() = runTest {
        val repository = createRepository(FakePreferencesDataStore(stored(12.5, Double.NaN)))

        assertEquals(state(12.5, 0.0), repository.state.first())
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun state_readFails_emitsZeroAndLogs() = runTest {
        val dataStore = FakePreferencesDataStore(stored(12.5, 3_000.0)).apply { failReads = true }
        val repository = createRepository(dataStore)

        assertEquals(OdometerState.ZERO, repository.state.first())
        assertTrue(logger.loggedErrors.single() is IOException)
    }

    @Test
    fun state_readsStorageOnlyOnce() = runTest {
        val dataStore = FakePreferencesDataStore(stored(12.5, 3_000.0))
        val repository = createRepository(dataStore)
        repository.state.first()

        // Later reads come from memory, so a broken disk no longer matters.
        dataStore.failReads = true

        assertEquals(state(12.5, 3_000.0), repository.state.first())
        assertTrue(logger.loggedErrors.isEmpty())
    }

    @Test
    fun state_afterChange_emitsNewState() = runTest {
        val repository = createRepository(FakePreferencesDataStore())

        repository.state.test {
            assertEquals(OdometerState.ZERO, awaitItem())
            repository.addTravelled(Distance.meters(10.0))

            assertEquals(state(10.0, 10.0), awaitItem())
        }
    }

    @Test
    fun addTravelled_addsToTripAndTotal() = runTest {
        val repository = createRepository(FakePreferencesDataStore(stored(5.0, 100.0)))

        repository.addTravelled(Distance.meters(10.0))

        assertEquals(state(15.0, 110.0), repository.state.first())
    }

    @Test
    fun addTravelled_belowSaveThresholds_doesNotSave() = runTest {
        val dataStore = FakePreferencesDataStore()
        val repository = createRepository(dataStore)

        repository.addTravelled(Distance.meters(499.0))

        assertEquals(0, dataStore.writeCount)
        assertNull(dataStore.stored.value[TRIP_KEY])
    }

    @Test
    fun addTravelled_reachesSaveDistance_saves() = runTest {
        val dataStore = FakePreferencesDataStore()
        val repository = createRepository(dataStore)

        repository.addTravelled(Distance.meters(300.0))
        repository.addTravelled(SAVE_DISTANCE - Distance.meters(300.0))

        assertEquals(stored(500.0, 500.0), dataStore.stored.value)
    }

    @Test
    fun addTravelled_afterSave_countsDistanceFromZeroAgain() = runTest {
        val dataStore = FakePreferencesDataStore()
        val repository = createRepository(dataStore)
        repository.addTravelled(SAVE_DISTANCE)

        repository.addTravelled(Distance.meters(499.0))

        assertEquals(1, dataStore.writeCount)
    }

    @Test
    fun addTravelled_saveIntervalPassed_saves() = runTest {
        val dataStore = FakePreferencesDataStore()
        val repository = createRepository(dataStore)
        repository.state.first()

        testScheduler.advanceTimeBy(SAVE_INTERVAL)
        repository.addTravelled(Distance.meters(1.0))

        assertEquals(stored(1.0, 1.0), dataStore.stored.value)
    }

    @Test
    fun addTravelled_justBeforeSaveInterval_doesNotSave() = runTest {
        val dataStore = FakePreferencesDataStore()
        val repository = createRepository(dataStore)
        repository.state.first()

        testScheduler.advanceTimeBy(SAVE_INTERVAL - 1.milliseconds)
        repository.addTravelled(Distance.meters(1.0))

        assertEquals(0, dataStore.writeCount)
    }

    @Test
    fun update_savesAtOnce() = runTest {
        val dataStore = FakePreferencesDataStore(stored(5.0, 100.0))
        val repository = createRepository(dataStore)

        repository.update { it.copy(trip = Distance.meters(42.0)) }

        assertEquals(stored(42.0, 100.0), dataStore.stored.value)
        assertEquals(state(42.0, 100.0), repository.state.first())
    }

    @Test
    fun update_afterUnsavedTravel_receivesAndSavesLatestState() = runTest {
        val dataStore = FakePreferencesDataStore()
        val repository = createRepository(dataStore)
        repository.addTravelled(Distance.meters(10.0))

        repository.update { it.copy(trip = Distance.ZERO) }

        assertEquals(stored(0.0, 10.0), dataStore.stored.value)
    }

    @Test
    fun update_resetsSaveDistance() = runTest {
        val dataStore = FakePreferencesDataStore()
        val repository = createRepository(dataStore)
        repository.addTravelled(Distance.meters(400.0))
        repository.update { it }

        repository.addTravelled(Distance.meters(400.0))

        assertEquals(1, dataStore.writeCount)
    }

    @Test
    fun update_writeFails_keepsStateInMemoryAndLogs() = runTest {
        val dataStore = FakePreferencesDataStore().apply { failWrites = true }
        val repository = createRepository(dataStore)

        repository.update { it.copy(trip = Distance.meters(42.0)) }

        assertEquals(state(42.0, 0.0), repository.state.first())
        assertTrue(logger.loggedErrors.single() is IOException)
    }

    @Test
    fun addTravelled_previousWriteFailed_retriesOnNextChange() = runTest {
        val dataStore = FakePreferencesDataStore().apply { failWrites = true }
        val repository = createRepository(dataStore)
        repository.addTravelled(SAVE_DISTANCE)

        dataStore.failWrites = false
        repository.addTravelled(Distance.meters(1.0))

        assertEquals(stored(501.0, 501.0), dataStore.stored.value)
    }
}
