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
import dev.giste.core.logging.Logger
import dev.giste.roadbook.ImportRoadbookResult
import dev.giste.roadbook.RoadbookController
import dev.giste.roadbook.RoadbookEvent
import dev.giste.roadbook.data.source.RoadbookSourceFactory
import dev.giste.roadbook.domain.roadbook.ImportRoadbookUseCase
import dev.giste.roadbook.domain.roadbook.MoveDirection
import dev.giste.roadbook.domain.roadbook.MoveRoadbookUseCase
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * [RoadbookController] that runs the commands with the domain use cases, and emits the events.
 *
 * It lives in `data`, not in `domain`, because it adapts the public API, which takes an Android
 * `Uri`, to the domain, which reads a `RoadbookSource`. It holds no logic of its own beyond that
 * and the events.
 *
 * Commands run one at a time. An import and a move both read and write the roadbook and its
 * position, so running them together could move the new roadbook from the position of the old
 * one. Events are emitted after the command has finished, so collectors see its result (e.g. the
 * position of a loaded roadbook is already the first waypoint). They are emitted after releasing
 * the lock too: emitting waits for every collector to take the event, and a collector still busy
 * with an earlier event may be waiting to give a command.
 *
 * It must be a singleton, so every collector gets the events of every command.
 *
 * @param importRoadbookUseCase reads, parses and saves a roadbook.
 * @param moveRoadbook moves the position.
 * @param sourceFactory turns the URI of a file into a source to import.
 * @param logger logs the commands.
 */
internal class DefaultRoadbookController @Inject constructor(
    private val importRoadbookUseCase: ImportRoadbookUseCase,
    private val moveRoadbook: MoveRoadbookUseCase,
    private val sourceFactory: RoadbookSourceFactory,
    private val logger: Logger,
) : RoadbookController {

    /** Runs one command at a time. */
    private val commands = Mutex()

    /**
     * No replay and no buffer, as [RoadbookController.events] promises: an event only reaches
     * the collectors present when it is emitted, and emitting waits until they have all taken it.
     */
    private val mutableEvents = MutableSharedFlow<RoadbookEvent>()

    override val events: SharedFlow<RoadbookEvent> = mutableEvents.asSharedFlow()

    override suspend fun importRoadbook(uri: Uri): ImportRoadbookResult {
        logger.i("Importing a roadbook")
        val result = commands.withLock { importRoadbookUseCase(sourceFactory.create(uri)) }
        if (result is ImportRoadbookResult.Success) {
            mutableEvents.emit(RoadbookEvent.Loaded)
        }
        return result
    }

    override suspend fun moveToNextWaypoint() {
        logger.d("Moving to the next waypoint")
        commands.withLock { moveRoadbook(MoveDirection.NEXT) }
    }

    override suspend fun moveToPreviousWaypoint() {
        logger.d("Moving to the previous waypoint")
        commands.withLock { moveRoadbook(MoveDirection.PREVIOUS) }
    }
}
