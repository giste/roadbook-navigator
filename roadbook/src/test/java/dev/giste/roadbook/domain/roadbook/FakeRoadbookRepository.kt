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

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [RoadbookRepository] for tests, which can be told to fail saving.
 *
 * @param initial what it holds at first.
 */
internal class FakeRoadbookRepository(
    initial: SavedRoadbook = SavedRoadbook.None,
) : RoadbookRepository {

    /** What it holds. Tests read it to check what was saved. */
    override val roadbook: MutableStateFlow<SavedRoadbook> = MutableStateFlow(initial)

    /** When `true`, [save] fails and keeps the current roadbook. */
    var failSaves: Boolean = false

    /** How many times [save] was called, whether it failed or not. */
    var saveCount: Int = 0
        private set

    override suspend fun save(roadbook: Roadbook): SaveRoadbookResult {
        saveCount++
        if (failSaves) return SaveRoadbookResult.Failed
        this.roadbook.value = SavedRoadbook.Available(roadbook)
        return SaveRoadbookResult.Saved
    }
}
