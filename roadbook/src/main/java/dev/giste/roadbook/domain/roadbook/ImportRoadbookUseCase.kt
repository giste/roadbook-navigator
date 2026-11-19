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
import dev.giste.roadbook.ImportRoadbookFailure
import dev.giste.roadbook.ImportRoadbookResult
import javax.inject.Inject

/**
 * Imports a roadbook: reads its file from a [RoadbookSource], parses it, saves it as the current
 * roadbook, and moves the position to its first waypoint.
 *
 * Each step stops the import when it fails, so a file that cannot be read or parsed never
 * replaces the current roadbook. It never throws.
 *
 * @param parser turns the content of the file into a [Roadbook].
 * @param repository keeps the imported roadbook.
 * @param positionRepository keeps the position, which starts again at the first waypoint.
 * @param logger logs the outcome.
 */
internal class ImportRoadbookUseCase @Inject constructor(
    private val parser: RoadbookParser,
    private val repository: RoadbookRepository,
    private val positionRepository: RoadbookPositionRepository,
    private val logger: Logger,
) {

    /**
     * Imports the roadbook that [source] holds.
     *
     * @param source where the file to import is read from.
     * @return [ImportRoadbookResult.Success] when the roadbook was saved, or the step that failed.
     * The position only changes on success.
     */
    suspend operator fun invoke(source: RoadbookSource): ImportRoadbookResult {
        val content = when (val read = source.read()) {
            is ReadRoadbookSourceResult.Success -> read.content
            ReadRoadbookSourceResult.Unreadable -> return failure(ImportRoadbookFailure.UNREADABLE)
        }
        val roadbook = when (val parsed = parser.parse(content)) {
            is ParseRoadbookResult.Success -> parsed.roadbook
            is ParseRoadbookResult.Failure -> return failure(parsed.reason.toImportFailure())
        }
        return when (repository.save(roadbook)) {
            SaveRoadbookResult.Saved -> {
                // Saving the position cannot fail: the repository keeps it in memory if it
                // cannot write it.
                positionRepository.save(WaypointNumber.FIRST)
                logger.i("Imported a roadbook with %d waypoints", roadbook.waypoints.size)
                ImportRoadbookResult.Success
            }
            SaveRoadbookResult.Failed -> failure(ImportRoadbookFailure.NOT_SAVED)
        }
    }

    /** Logs that the import failed because of [reason], and returns it as a result. */
    private fun failure(reason: ImportRoadbookFailure): ImportRoadbookResult {
        logger.w("Roadbook import failed: %s", reason)
        return ImportRoadbookResult.Failure(reason)
    }

    /** The import failure that a parse failure causes. */
    private fun ParseRoadbookFailure.toImportFailure(): ImportRoadbookFailure = when (this) {
        ParseRoadbookFailure.MALFORMED -> ImportRoadbookFailure.MALFORMED
        ParseRoadbookFailure.NO_WAYPOINTS -> ImportRoadbookFailure.NO_WAYPOINTS
    }
}
