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
 * [RoadbookParser] for tests: returns [result] for any content, and records what it was given.
 *
 * @param result what [parse] returns.
 */
internal class FakeRoadbookParser(
    var result: ParseRoadbookResult,
) : RoadbookParser {

    /** The contents passed to [parse], in call order. */
    val parsedContents: MutableList<String> = mutableListOf()

    override suspend fun parse(content: String): ParseRoadbookResult {
        parsedContents += content
        return result
    }
}
