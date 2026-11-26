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

package dev.giste.roadbooknavigator.domain

import kotlinx.coroutines.CompletableDeferred

/**
 * [OpenSourceLibraryRepository] for tests: [read] waits until the test calls [complete], so tests can see
 * the loading state first, and counts its calls.
 */
internal class FakeOpenSourceLibraryRepository : OpenSourceLibraryRepository {

    private val result = CompletableDeferred<OpenSourceLibrariesResult>()

    /** How many times [read] was called. */
    var reads: Int = 0
        private set

    /** Makes [read] return [value], now or as soon as it is called. */
    fun complete(value: OpenSourceLibrariesResult) {
        result.complete(value)
    }

    override suspend fun read(): OpenSourceLibrariesResult {
        reads++
        return result.await()
    }
}
