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

package dev.giste.roadbook.data

import android.net.Uri
import app.cash.turbine.test
import dev.giste.roadbook.FakeLogger
import dev.giste.roadbook.ImportRoadbookFailure
import dev.giste.roadbook.ImportRoadbookResult
import dev.giste.roadbook.RoadbookEvent
import dev.giste.roadbook.data.source.RoadbookSourceFactory
import dev.giste.roadbook.domain.roadbook.FakeRoadbookParser
import dev.giste.roadbook.domain.roadbook.FakeRoadbookPositionRepository
import dev.giste.roadbook.domain.roadbook.FakeRoadbookRepository
import dev.giste.roadbook.domain.roadbook.ImportRoadbookUseCase
import dev.giste.roadbook.domain.roadbook.MoveRoadbookUseCase
import dev.giste.roadbook.domain.roadbook.ParseRoadbookResult
import dev.giste.roadbook.domain.roadbook.ReadRoadbookSourceResult
import dev.giste.roadbook.domain.roadbook.Roadbook
import dev.giste.roadbook.domain.roadbook.RoadbookSource
import dev.giste.roadbook.domain.roadbook.SavedRoadbook
import dev.giste.roadbook.domain.roadbook.WaypointNumber
import dev.giste.roadbook.domain.roadbook.testWaypoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests [DefaultRoadbookController] with the real use cases on fake repositories.
 *
 * It runs on Robolectric only because the public API takes an Android [Uri].
 */
@RunWith(RobolectricTestRunner::class)
class DefaultRoadbookControllerTest {

    private val threeWaypoints = Roadbook(
        waypoints = listOf(testWaypoint(1), testWaypoint(2), testWaypoint(3)),
    )
    private val uri: Uri = Uri.parse("content://dev.giste.roadbook.test/documents/1")

    private val logger = FakeLogger()
    private val parser = FakeRoadbookParser(ParseRoadbookResult.Success(threeWaypoints))
    private val roadbookRepository = FakeRoadbookRepository()
    private val positionRepository = FakeRoadbookPositionRepository(WaypointNumber(2))

    /** The URIs sources were created for, in call order. */
    private val requestedUris = mutableListOf<Uri>()
    private var readResult: ReadRoadbookSourceResult = ReadRoadbookSourceResult.Success("{}")
    private val sourceFactory = RoadbookSourceFactory { uri ->
        requestedUris += uri
        RoadbookSource { readResult }
    }

    private val controller = DefaultRoadbookController(
        importRoadbookUseCase = ImportRoadbookUseCase(
            parser,
            roadbookRepository,
            positionRepository,
            logger,
        ),
        moveRoadbook = MoveRoadbookUseCase(roadbookRepository, positionRepository, logger),
        sourceFactory = sourceFactory,
        logger = logger,
    )

    @Test
    fun importRoadbook_validFile_succeedsAndSavesRoadbookFromFirstWaypoint() = runTest {
        val result = controller.importRoadbook(uri)

        assertEquals(ImportRoadbookResult.Success, result)
        assertEquals(SavedRoadbook.Available(threeWaypoints), roadbookRepository.roadbook.value)
        assertEquals(WaypointNumber.FIRST, positionRepository.position.value)
    }

    @Test
    fun importRoadbook_readsFileAtUri() = runTest {
        controller.importRoadbook(uri)

        assertEquals(listOf(uri), requestedUris)
    }

    @Test
    fun importRoadbook_validFile_emitsLoaded() = runTest {
        controller.events.test {
            controller.importRoadbook(uri)

            assertEquals(RoadbookEvent.Loaded, awaitItem())
        }
    }

    @Test
    fun importRoadbook_failure_returnsReasonAndEmitsNothing() = runTest {
        readResult = ReadRoadbookSourceResult.Unreadable

        controller.events.test {
            val result = controller.importRoadbook(uri)

            assertEquals(ImportRoadbookResult.Failure(ImportRoadbookFailure.UNREADABLE), result)
            expectNoEvents()
        }
    }

    @Test
    fun importRoadbook_noCollector_stillCompletes() = runTest {
        // Nobody collects the events: emitting must not wait for a collector.
        assertEquals(ImportRoadbookResult.Success, controller.importRoadbook(uri))
    }

    @Test
    fun importRoadbook_collectorMovesOnLoaded_movesFromFirstWaypoint() = runTest {
        // Loaded is emitted once the position is reset, so a move in response starts from the
        // first waypoint of the new roadbook, not from the previous position (2).
        roadbookRepository.roadbook.value = SavedRoadbook.Available(threeWaypoints)
        val collector = launch {
            controller.events.collect { event ->
                if (event == RoadbookEvent.Loaded) controller.moveToNextWaypoint()
            }
        }
        testScheduler.runCurrent()

        controller.importRoadbook(uri)
        testScheduler.runCurrent()

        assertEquals(WaypointNumber(2), positionRepository.position.value)
        collector.cancel()
    }

    @Test
    fun moveToNextWaypoint_withRoadbook_movesForward() = runTest {
        roadbookRepository.roadbook.value = SavedRoadbook.Available(threeWaypoints)

        controller.moveToNextWaypoint()

        assertEquals(WaypointNumber(3), positionRepository.position.value)
    }

    @Test
    fun moveToPreviousWaypoint_withRoadbook_movesBack() = runTest {
        roadbookRepository.roadbook.value = SavedRoadbook.Available(threeWaypoints)

        controller.moveToPreviousWaypoint()

        assertEquals(WaypointNumber.FIRST, positionRepository.position.value)
    }

    @Test
    fun moveToNextWaypoint_withoutRoadbook_keepsPosition() = runTest {
        controller.moveToNextWaypoint()

        assertEquals(WaypointNumber(2), positionRepository.position.value)
    }
}
