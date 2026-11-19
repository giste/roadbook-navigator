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
 * Where the content of a roadbook file to import comes from, e.g. a document the user picked.
 *
 * The domain only needs the text of the file. Where it is and how it is opened (a content URI, a
 * file, a test string) is up to each implementation, so the domain has no Android types.
 */
internal fun interface RoadbookSource {

    /**
     * Reads the whole content of the file.
     *
     * It never throws for an I/O problem: implementations catch them, log them and return
     * [ReadRoadbookSourceResult.Unreadable]. It is main-safe.
     *
     * @return the content of the file, or [ReadRoadbookSourceResult.Unreadable].
     */
    suspend fun read(): ReadRoadbookSourceResult
}

/** The result of [RoadbookSource.read]. */
internal sealed interface ReadRoadbookSourceResult {

    /**
     * The file was read.
     *
     * @property content the whole text of the file.
     */
    data class Success(val content: String) : ReadRoadbookSourceResult

    /**
     * The file could not be read: it does not exist, the app may not read it, or reading failed.
     * The cause is logged by the source.
     */
    data object Unreadable : ReadRoadbookSourceResult
}
