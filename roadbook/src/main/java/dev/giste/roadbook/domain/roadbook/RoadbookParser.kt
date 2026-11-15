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

/**
 * Turns the content of a roadbook file into a [Roadbook].
 *
 * It never throws for a bad file: every problem with the content is reported as a
 * [ParseRoadbookResult.Failure].
 */
internal interface RoadbookParser {

    /**
     * Parses [content], the whole text of a roadbook file.
     *
     * It is main-safe: the work runs on a background dispatcher.
     *
     * @param content the text of the file.
     * @return the roadbook, or why it could not be parsed.
     */
    suspend fun parse(content: String): ParseRoadbookResult
}

/** The result of [RoadbookParser.parse]. */
internal sealed interface ParseRoadbookResult {

    /**
     * The file is a valid roadbook.
     *
     * @property roadbook the roadbook it describes.
     */
    data class Success(val roadbook: Roadbook) : ParseRoadbookResult

    /**
     * The file is not a roadbook that can be navigated.
     *
     * @property reason why it was rejected.
     */
    data class Failure(val reason: ParseRoadbookFailure) : ParseRoadbookResult
}

/** Why a roadbook file was rejected. */
internal enum class ParseRoadbookFailure {

    /**
     * The file is not a roadbook of the expected format: it is not valid JSON, lacks required
     * fields, or has values that are not possible (e.g. a latitude of 120°).
     */
    MALFORMED,

    /** The file is well formed, but none of its points is shown as a waypoint. */
    NO_WAYPOINTS,
}
