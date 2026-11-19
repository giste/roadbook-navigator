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

import kotlinx.coroutines.flow.Flow

/**
 * Keeps the current [Roadbook] and persists it, so it survives process death and restarts.
 *
 * It stores the roadbook as it was imported, not the file it came from: the roadbook a user
 * restores is always the one they were navigating, even if a later version of the module would
 * read the file differently.
 *
 * Implementations never throw: storage failures are logged and reported as values.
 */
internal interface RoadbookRepository {

    /**
     * The saved roadbook, once it has been read from storage, followed by every change.
     *
     * It never throws and never completes.
     */
    val roadbook: Flow<SavedRoadbook>

    /**
     * Saves [roadbook] in place of the current one. On success, the
     * [roadbook][RoadbookRepository.roadbook] flow emits it.
     *
     * @param roadbook the roadbook to keep.
     * @return whether it was saved. On [SaveRoadbookResult.Failed], the previous roadbook is kept.
     */
    suspend fun save(roadbook: Roadbook): SaveRoadbookResult
}

/** What [RoadbookRepository.roadbook] holds. */
internal sealed interface SavedRoadbook {

    /** No roadbook has been imported yet. */
    data object None : SavedRoadbook

    /**
     * A roadbook is saved.
     *
     * @property roadbook the saved roadbook.
     */
    data class Available(val roadbook: Roadbook) : SavedRoadbook

    /**
     * A roadbook was saved, but it cannot be read back: reading the storage failed, or it was
     * saved in a format this version cannot read. The user has to import it again. The cause is
     * logged.
     */
    data object Unreadable : SavedRoadbook
}

/** The result of [RoadbookRepository.save]. */
internal sealed interface SaveRoadbookResult {

    /** The roadbook was saved. */
    data object Saved : SaveRoadbookResult

    /** The roadbook could not be written to storage. The cause is logged. */
    data object Failed : SaveRoadbookResult
}
