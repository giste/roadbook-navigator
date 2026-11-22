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

package dev.giste.location.domain

import app.cash.turbine.test
import dev.giste.geo.Distance
import dev.giste.geo.Speed
import dev.giste.location.FakeLocationSettingsSource
import dev.giste.location.FakeLogger
import dev.giste.location.LocationEvent
import dev.giste.location.LocationSettings
import dev.giste.location.MotionState
import dev.giste.location.fixAt
import dev.giste.location.testLocationFix
import java.io.IOException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// runCurrent() is still experimental in kotlinx-coroutines-test.
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultLocationSourceTest {

    private val repository = FakeLocationRepository()
    private val logger = FakeLogger()
    private val settings = MutableSharedFlow<LocationSettings>(replay = 1)

    private val customSettings = LocationSettings(
        updateInterval = 2.seconds,
        minUpdateDistance = Distance.meters(10.0),
    )

    /** Creates the source under test, sharing its request in the test's background scope. */
    private fun TestScope.createSource(
        settingsFlow: Flow<LocationSettings> = settings,
    ) = DefaultLocationSource(
        repository = repository,
        settingsSource = FakeLocationSettingsSource(settingsFlow),
        logger = logger,
        scope = backgroundScope,
    )

    @Test
    fun events_notCollected_doesNotRequestUpdates() = runTest {
        settings.emit(customSettings)
        createSource()

        runCurrent()

        assertTrue(repository.requests.isEmpty())
    }

    @Test
    fun events_collected_requestsUpdatesWithCurrentSettings() = runTest {
        settings.emit(customSettings)
        val source = createSource()

        source.events.test {
            runCurrent()

            assertEquals(listOf(customSettings), repository.requests)
        }
    }

    @Test
    fun events_repositoryEmits_forwardsEvent() = runTest {
        settings.emit(LocationSettings())
        val source = createSource()
        val event = LocationEvent.LocationUpdated(testLocationFix())

        source.events.test {
            runCurrent()
            repository.emit(event)

            assertEquals(event, awaitItem())
        }
    }

    @Test
    fun events_settingsChange_restartsWithNewSettings() = runTest {
        settings.emit(LocationSettings())
        val source = createSource()

        source.events.test {
            runCurrent()
            settings.emit(customSettings)
            runCurrent()

            assertEquals(listOf(LocationSettings(), customSettings), repository.requests)
            assertEquals(1, repository.activeRequests)
        }
    }

    @Test
    fun events_equalSettingsEmittedAgain_doesNotRestart() = runTest {
        settings.emit(customSettings)
        val source = createSource()

        source.events.test {
            runCurrent()
            settings.emit(customSettings.copy())
            runCurrent()

            assertEquals(listOf(customSettings), repository.requests)
        }
    }

    @Test
    fun events_settingsSourceFails_usesDefaultSettings() = runTest {
        val failingSettings = flow<LocationSettings> { throw IOException("Settings unreadable") }
        val source = createSource(settingsFlow = failingSettings)

        source.events.test {
            runCurrent()

            assertEquals(listOf(LocationSettings()), repository.requests)
        }
    }

    @Test
    fun events_settingsSourceFails_logsError() = runTest {
        val failure = IOException("Settings unreadable")
        val source = createSource(settingsFlow = flow { throw failure })

        source.events.test {
            runCurrent()

            assertEquals(listOf<Throwable>(failure), logger.loggedErrors)
        }
    }

    @Test
    fun events_twoCollectors_shareOneRequest() = runTest {
        settings.emit(LocationSettings())
        val source = createSource()
        val event = LocationEvent.ProviderDisabled

        source.events.test {
            val first = this
            source.events.test {
                runCurrent()
                repository.emit(event)

                assertEquals(1, repository.requests.size)
                assertEquals(event, awaitItem())
                assertEquals(event, first.awaitItem())
            }
        }
    }

    @Test
    fun events_lateCollector_receivesLatestEvent() = runTest {
        settings.emit(LocationSettings())
        val source = createSource()
        val latest = LocationEvent.LocationUpdated(testLocationFix(latitude = 41.0))

        source.events.test {
            runCurrent()
            repository.emit(LocationEvent.ProviderEnabled)
            repository.emit(latest)
            // Let the shared flow store the event in its replay cache before leaving.
            runCurrent()
            cancelAndIgnoreRemainingEvents()
        }
        // Collect again within the stop timeout, so updates are still running.
        source.events.test {
            assertEquals(latest, awaitItem())
        }
    }

    @Test
    fun events_lastCollectorLeaves_keepsUpdatesForFiveSeconds() = runTest {
        settings.emit(LocationSettings())
        val source = createSource()
        val collector = backgroundScope.launch { source.events.collect {} }
        runCurrent()

        collector.cancel()
        advanceTimeBy(4_999)

        assertEquals(1, repository.activeRequests)
    }

    @Test
    fun events_lastCollectorLeaves_stopsUpdatesAfterFiveSeconds() = runTest {
        settings.emit(LocationSettings())
        val source = createSource()
        val collector = backgroundScope.launch { source.events.collect {} }
        runCurrent()

        collector.cancel()
        advanceTimeBy(5_001)

        assertEquals(0, repository.activeRequests)
    }

    @Test
    fun events_collectedAgainAfterStop_doesNotReplayStaleEvent() = runTest {
        settings.emit(LocationSettings())
        val source = createSource()
        source.events.test {
            runCurrent()
            repository.emit(LocationEvent.LocationUpdated(testLocationFix()))
            cancelAndIgnoreRemainingEvents()
        }
        advanceTimeBy(5_001)

        source.events.test {
            runCurrent()

            expectNoEvents()
            assertEquals(2, repository.requests.size)
        }
    }

    @Test
    fun events_onlyMotionSpeedsChange_doesNotRestart() = runTest {
        settings.emit(LocationSettings())
        val source = createSource()

        source.events.test {
            runCurrent()
            settings.emit(LocationSettings(stoppedSpeed = Speed.kilometersPerHour(1.0)))
            runCurrent()

            assertEquals(1, repository.requests.size)
        }
    }

    @Test
    fun motion_beforeAnyEvent_isStopped() = runTest {
        settings.emit(LocationSettings())
        val source = createSource()

        source.motion.test {
            assertEquals(MotionState.STOPPED, awaitItem())
        }
    }

    @Test
    fun motion_collected_requestsUpdates() = runTest {
        settings.emit(LocationSettings())
        val source = createSource()

        source.motion.test {
            runCurrent()

            assertEquals(1, repository.activeRequests)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun motion_events_emitsOnlyChanges() = runTest {
        settings.emit(LocationSettings())
        val source = createSource()

        source.motion.test {
            skipItems(1)
            runCurrent()
            repository.emit(fixAt(0.0)) // still stopped
            repository.emit(fixAt(20.0)) // moving
            repository.emit(fixAt(7.0)) // still moving: between the speeds
            repository.emit(fixAt(3.0)) // stopped

            assertEquals(MotionState.MOVING, awaitItem())
            assertEquals(MotionState.STOPPED, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun motion_speedsChange_judgesTheLatestEventAgain() = runTest {
        settings.emit(LocationSettings())
        val source = createSource()

        source.motion.test {
            skipItems(1)
            runCurrent()
            repository.emit(fixAt(7.0)) // between the default speeds: still stopped
            runCurrent()

            settings.emit(LocationSettings(movingSpeed = Speed.kilometersPerHour(6.0)))

            assertEquals(MotionState.MOVING, awaitItem())
        }
    }

    @Test
    fun motion_settingsSourceFails_usesDefaultSpeeds() = runTest {
        val source = createSource(settingsFlow = flow { throw IOException("Settings unreadable") })

        source.motion.test {
            skipItems(1)
            runCurrent()
            repository.emit(fixAt(10.1))

            assertEquals(MotionState.MOVING, awaitItem())
        }
    }

    @Test
    fun motion_lateCollector_receivesTheCurrentState() = runTest {
        settings.emit(LocationSettings())
        val source = createSource()
        val first = backgroundScope.launch { source.motion.collect {} }
        runCurrent()
        repository.emit(fixAt(20.0))
        runCurrent()

        source.motion.test {
            assertEquals(MotionState.MOVING, awaitItem())
        }
        first.cancel()
    }

    @Test
    fun motion_collectedAgainAfterStop_startsStopped() = runTest {
        settings.emit(LocationSettings())
        val source = createSource()
        source.motion.test {
            runCurrent()
            repository.emit(fixAt(20.0))
            cancelAndIgnoreRemainingEvents()
        }
        advanceTimeBy(5_001)

        source.motion.test {
            assertEquals(MotionState.STOPPED, awaitItem())
        }
    }
}
