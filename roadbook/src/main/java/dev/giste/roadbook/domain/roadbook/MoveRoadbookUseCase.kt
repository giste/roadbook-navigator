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

import dev.giste.core.logging.Logger
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Moves the roadbook one waypoint forward or back, and saves the new position.
 *
 * The saved position may not belong to the current roadbook (e.g. it is past its last waypoint),
 * so it is first kept within the roadbook. Nothing changes at either end, or when there is no
 * roadbook to move. It never throws.
 *
 * @param roadbookRepository the current roadbook.
 * @param positionRepository the position to move.
 * @param logger logs moves that cannot happen.
 */
internal class MoveRoadbookUseCase @Inject constructor(
    private val roadbookRepository: RoadbookRepository,
    private val positionRepository: RoadbookPositionRepository,
    private val logger: Logger,
) {

    /**
     * Moves the roadbook one waypoint in [direction].
     *
     * @param direction where to move.
     */
    suspend operator fun invoke(direction: MoveDirection) {
        val roadbook = when (val saved = roadbookRepository.roadbook.first()) {
            is SavedRoadbook.Available -> saved.roadbook
            SavedRoadbook.None, SavedRoadbook.Unreadable -> {
                logger.d("No roadbook to move %s", direction)
                return
            }
        }
        val current = roadbook.coerceWaypointNumber(positionRepository.position.first())
        val target = when (direction) {
            MoveDirection.NEXT -> roadbook.waypointNumberAfter(current)
            MoveDirection.PREVIOUS -> roadbook.waypointNumberBefore(current)
        }
        if (target == current) {
            logger.d("The roadbook cannot move %s from waypoint %s", direction, current)
            return
        }
        positionRepository.save(target)
    }
}

/** Where [MoveRoadbookUseCase] moves the roadbook. */
internal enum class MoveDirection {

    /** Forward: the next waypoint is shown at the top. */
    NEXT,

    /** Back: the previous waypoint is shown at the top. */
    PREVIOUS,
}
