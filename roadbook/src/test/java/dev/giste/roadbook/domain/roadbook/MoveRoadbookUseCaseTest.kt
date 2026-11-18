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

import dev.giste.roadbook.FakeLogger
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoveRoadbookUseCaseTest {

    private val threeWaypoints = Roadbook(
        waypoints = listOf(testWaypoint(1), testWaypoint(2), testWaypoint(3)),
    )

    private val positionRepository = FakeRoadbookPositionRepository()

    private fun moveRoadbookWith(saved: SavedRoadbook) =
        MoveRoadbookUseCase(FakeRoadbookRepository(saved), positionRepository, FakeLogger())

    private val moveRoadbook = moveRoadbookWith(SavedRoadbook.Available(threeWaypoints))

    @Test
    fun invoke_next_savesNextWaypoint() = runTest {
        positionRepository.position.value = WaypointNumber(2)

        moveRoadbook(MoveDirection.NEXT)

        assertEquals(listOf(WaypointNumber(3)), positionRepository.saved)
    }

    @Test
    fun invoke_previous_savesPreviousWaypoint() = runTest {
        positionRepository.position.value = WaypointNumber(2)

        moveRoadbook(MoveDirection.PREVIOUS)

        assertEquals(listOf(WaypointNumber(1)), positionRepository.saved)
    }

    @Test
    fun invoke_nextAtLastWaypoint_savesNothing() = runTest {
        positionRepository.position.value = WaypointNumber(3)

        moveRoadbook(MoveDirection.NEXT)

        assertTrue(positionRepository.saved.isEmpty())
    }

    @Test
    fun invoke_previousAtFirstWaypoint_savesNothing() = runTest {
        moveRoadbook(MoveDirection.PREVIOUS)

        assertTrue(positionRepository.saved.isEmpty())
    }

    @Test
    fun invoke_previousFromPastLastWaypoint_movesBackFromLastOne() = runTest {
        // E.g. a position saved for a longer roadbook.
        positionRepository.position.value = WaypointNumber(50)

        moveRoadbook(MoveDirection.PREVIOUS)

        assertEquals(listOf(WaypointNumber(2)), positionRepository.saved)
    }

    @Test
    fun invoke_nextFromPastLastWaypoint_savesNothing() = runTest {
        positionRepository.position.value = WaypointNumber(50)

        moveRoadbook(MoveDirection.NEXT)

        assertTrue(positionRepository.saved.isEmpty())
    }

    @Test
    fun invoke_noRoadbook_savesNothing() = runTest {
        moveRoadbookWith(SavedRoadbook.None)(MoveDirection.NEXT)

        assertTrue(positionRepository.saved.isEmpty())
    }

    @Test
    fun invoke_unreadableRoadbook_savesNothing() = runTest {
        moveRoadbookWith(SavedRoadbook.Unreadable)(MoveDirection.NEXT)

        assertTrue(positionRepository.saved.isEmpty())
    }
}
