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

package dev.giste.odometer.domain

import app.cash.turbine.test
import dev.giste.geo.Distance
import dev.giste.geo.LocationFix
import dev.giste.geo.Speed
import dev.giste.odometer.FakeLogger
import dev.giste.odometer.FakeOdometerLocationSource
import dev.giste.odometer.FakeOdometerSettingsSource
import dev.giste.odometer.OdometerSettings
import dev.giste.odometer.OdometerState
import dev.giste.odometer.testFix
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// runCurrent() and advanceTimeBy() are still experimental in kotlinx-coroutines-test.
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultOdometerControllerTest {

    private val repository = FakeOdometerStateRepository()
    private val locations = MutableSharedFlow<LocationFix>()
    private val settings = MutableStateFlow(OdometerSettings())
    private val logger = FakeLogger()

    /** Creates the controller under test, sharing its state in the test's background scope. */
    private fun TestScope.createController(
        locationFlow: Flow<LocationFix> = locations,
        settingsFlow: Flow<OdometerSettings> = settings,
    ) = DefaultOdometerController(
        repository = repository,
        locationSource = FakeOdometerLocationSource(locationFlow),
        settingsSource = FakeOdometerSettingsSource(settingsFlow),
        logger = logger,
        scope = backgroundScope,
    )

    /** Emits [fixes] one by one, and lets the controller process each of them. */
    private suspend fun TestScope.emitFixes(vararg fixes: LocationFix) {
        fixes.forEach { fix ->
            locations.emit(fix)
            runCurrent()
        }
    }

    /** Sum of the distances added to the repository, in meters. */
    private fun travelledMeters(): Double = repository.travelled.sumOf { it.inMeters }

    @Test
    fun state_collected_emitsRepositoryState() = runTest {
        val stored = OdometerState(trip = Distance.meters(10.0), total = Distance.meters(20.0))
        repository.update { stored }
        val controller = createController()

        controller.state.test {
            assertEquals(stored, awaitItem())
        }
    }

    @Test
    fun state_repositoryChanges_emitsNewState() = runTest {
        val controller = createController()

        controller.state.test {
            awaitItem()
            repository.update { it.copy(trip = Distance.meters(5.0)) }

            assertEquals(Distance.meters(5.0), awaitItem().trip)
        }
    }

    @Test
    fun state_notCollected_doesNotCollectLocations() = runTest {
        createController()

        runCurrent()

        assertEquals(0, locations.subscriptionCount.value)
    }

    @Test
    fun state_collected_collectsLocations() = runTest {
        val controller = createController()

        controller.state.test {
            runCurrent()

            assertEquals(1, locations.subscriptionCount.value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun state_lastCollectorLeaves_stopsCollectingLocationsAfterTimeout() = runTest {
        val controller = createController()
        controller.state.test {
            runCurrent()
            cancelAndIgnoreRemainingEvents()
        }

        advanceTimeBy(4_999)
        runCurrent()
        assertEquals(1, locations.subscriptionCount.value)

        advanceTimeBy(2)
        runCurrent()
        assertEquals(0, locations.subscriptionCount.value)
    }

    @Test
    fun state_firstAccurateFix_addsNothing() = runTest {
        val controller = createController()

        controller.state.test {
            runCurrent()
            emitFixes(testFix(0.0))

            assertTrue(repository.travelled.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun state_movingFixes_addsDistanceBetweenThem() = runTest {
        val controller = createController()

        controller.state.test {
            runCurrent()
            emitFixes(testFix(0.0), testFix(30.0), testFix(100.0))

            assertEquals(2, repository.travelled.size)
            assertEquals(100.0, travelledMeters(), 1e-6)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun state_movingFixes_emitsUpdatedDistances() = runTest {
        val controller = createController()

        controller.state.test {
            assertEquals(OdometerState.ZERO, awaitItem())
            emitFixes(testFix(0.0), testFix(30.0))

            val state = awaitItem()
            assertEquals(30.0, state.trip.inMeters, 1e-6)
            assertEquals(30.0, state.total.inMeters, 1e-6)
        }
    }

    @Test
    fun state_inaccurateFix_isNotCounted() = runTest {
        val controller = createController()

        controller.state.test {
            runCurrent()
            emitFixes(testFix(0.0), testFix(500.0, horizontalAccuracy = Distance.meters(50.0)))

            assertTrue(repository.travelled.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun state_stoppedFix_keepsAnchorUntilMovingAgain() = runTest {
        val controller = createController()

        controller.state.test {
            runCurrent()
            emitFixes(
                testFix(0.0),
                testFix(4.0, speed = Speed.ZERO),
                testFix(100.0),
            )

            assertEquals(1, repository.travelled.size)
            assertEquals(100.0, travelledMeters(), 1e-6)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun state_shortSteps_areCountedInOnePieceOnceFarEnough() = runTest {
        val controller = createController()

        controller.state.test {
            runCurrent()
            emitFixes(testFix(0.0), testFix(2.0), testFix(4.0), testFix(6.0))

            assertEquals(1, repository.travelled.size)
            assertEquals(6.0, travelledMeters(), 1e-6)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun state_fixAtAnchorPosition_addsNothing() = runTest {
        val controller = createController()

        controller.state.test {
            runCurrent()
            emitFixes(testFix(0.0), testFix(0.0))

            assertTrue(repository.travelled.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun state_settingsChange_appliesToNextFix() = runTest {
        val controller = createController()
        val fairFix = { meters: Double -> testFix(meters, horizontalAccuracy = Distance.meters(30.0)) }

        controller.state.test {
            runCurrent()
            emitFixes(fairFix(0.0))
            settings.value = OdometerSettings(maxHorizontalAccuracy = Distance.meters(50.0))
            runCurrent()
            emitFixes(fairFix(0.0), fairFix(40.0))

            assertEquals(40.0, travelledMeters(), 1e-6)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun state_settingsNeverEmit_doesNotMeasure() = runTest {
        val controller = createController(settingsFlow = MutableSharedFlow())

        controller.state.test {
            runCurrent()

            assertEquals(0, locations.subscriptionCount.value)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun state_settingsFail_logsAndMeasuresWithDefaults() = runTest {
        val failure = IOException("settings unavailable")
        val controller = createController(settingsFlow = flow { throw failure })

        controller.state.test {
            runCurrent()
            emitFixes(testFix(0.0), testFix(25.0))

            assertEquals(listOf<Throwable>(failure), logger.loggedErrors)
            assertEquals(25.0, travelledMeters(), 1e-6)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun state_locationsFail_logsAndKeepsEmittingState() = runTest {
        val failure = IllegalStateException("provider crashed")
        val controller = createController(locationFlow = flow { throw failure })

        controller.state.test {
            awaitItem()
            runCurrent()
            repository.update { it.copy(trip = Distance.meters(7.0)) }

            assertEquals(Distance.meters(7.0), awaitItem().trip)
            assertEquals(listOf<Throwable>(failure), logger.loggedErrors)
        }
    }

    @Test
    fun state_collectedAgainAfterStop_startsWithoutAnchor() = runTest {
        val controller = createController()
        controller.state.test {
            runCurrent()
            emitFixes(testFix(0.0))
            cancelAndIgnoreRemainingEvents()
        }
        advanceTimeBy(6_000)
        runCurrent()

        controller.state.test {
            runCurrent()
            emitFixes(testFix(1_000.0))

            assertTrue(repository.travelled.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun resetTrip_keepsTotal() = runTest {
        repository.update { OdometerState(trip = Distance.meters(10.0), total = Distance.meters(50.0)) }
        val controller = createController()

        controller.resetTrip()

        assertEquals(
            OdometerState(trip = Distance.ZERO, total = Distance.meters(50.0)),
            repository.state.value,
        )
    }

    @Test
    fun resetAll_resetsBothDistances() = runTest {
        repository.update { OdometerState(trip = Distance.meters(10.0), total = Distance.meters(50.0)) }
        val controller = createController()

        controller.resetAll()

        assertEquals(OdometerState.ZERO, repository.state.value)
    }

    @Test
    fun setTrip_setsTripAndKeepsTotal() = runTest {
        repository.update { OdometerState(trip = Distance.meters(10.0), total = Distance.meters(50.0)) }
        val controller = createController()

        controller.setTrip(Distance.meters(1_234.0))

        assertEquals(
            OdometerState(trip = Distance.meters(1_234.0), total = Distance.meters(50.0)),
            repository.state.value,
        )
    }

    @Test
    fun increaseTrip_addsToTripAndKeepsTotal() = runTest {
        repository.update { OdometerState(trip = Distance.meters(10.0), total = Distance.meters(50.0)) }
        val controller = createController()

        controller.increaseTrip(by = Distance.meters(10.0))

        assertEquals(
            OdometerState(trip = Distance.meters(20.0), total = Distance.meters(50.0)),
            repository.state.value,
        )
    }

    @Test
    fun decreaseTrip_subtractsFromTripAndKeepsTotal() = runTest {
        repository.update { OdometerState(trip = Distance.meters(30.0), total = Distance.meters(50.0)) }
        val controller = createController()

        controller.decreaseTrip(by = Distance.meters(10.0))

        assertEquals(
            OdometerState(trip = Distance.meters(20.0), total = Distance.meters(50.0)),
            repository.state.value,
        )
    }

    @Test
    fun decreaseTrip_byMoreThanTrip_stopsAtZero() = runTest {
        repository.update { OdometerState(trip = Distance.meters(5.0), total = Distance.meters(50.0)) }
        val controller = createController()

        controller.decreaseTrip(by = Distance.meters(10.0))

        assertEquals(
            OdometerState(trip = Distance.ZERO, total = Distance.meters(50.0)),
            repository.state.value,
        )
    }
}
