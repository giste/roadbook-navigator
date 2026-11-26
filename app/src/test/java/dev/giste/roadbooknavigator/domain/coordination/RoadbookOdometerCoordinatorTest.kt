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

package dev.giste.roadbooknavigator.domain.coordination

import dev.giste.geo.Distance
import dev.giste.roadbook.RoadbookEvent
import dev.giste.roadbooknavigator.FakeOdometerController
import dev.giste.roadbooknavigator.FakeRoadbookController
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoadbookOdometerCoordinatorTest {

    private val roadbook = FakeRoadbookController()
    private val odometer = FakeOdometerController()

    /** Starts a coordinator in the test's background scope and waits until it is collecting. */
    private fun TestScope.startCoordinator() {
        RoadbookOdometerCoordinator(roadbook, odometer, backgroundScope).start()
        runCurrent()
    }

    @Test
    fun start_roadbookLoaded_resetsPartialAndTotal() = runTest {
        startCoordinator()

        roadbook.emit(RoadbookEvent.Loaded)
        runCurrent()

        assertEquals(listOf("resetAll"), odometer.calls)
    }

    @Test
    fun start_setPartialRequested_setsTheTripDistance() = runTest {
        startCoordinator()

        roadbook.emit(RoadbookEvent.SetPartialRequested(Distance.meters(1_200.0)))
        runCurrent()

        assertEquals(listOf("setTrip 1200.0 m"), odometer.calls)
    }

    @Test
    fun start_severalEvents_appliesThemInOrder() = runTest {
        startCoordinator()

        roadbook.emit(RoadbookEvent.SetPartialRequested(Distance.ZERO))
        roadbook.emit(RoadbookEvent.Loaded)
        roadbook.emit(RoadbookEvent.SetPartialRequested(Distance.meters(500.0)))
        runCurrent()

        assertEquals(listOf("setTrip 0.0 m", "resetAll", "setTrip 500.0 m"), odometer.calls)
    }

    @Test
    fun start_notStarted_ignoresEvents() = runTest {
        RoadbookOdometerCoordinator(roadbook, odometer, backgroundScope)

        roadbook.emit(RoadbookEvent.Loaded)
        runCurrent()

        assertEquals(emptyList<String>(), odometer.calls)
    }
}
