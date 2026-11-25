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

package dev.giste.map.domain

import kotlinx.coroutines.CompletableDeferred

/**
 * [RemoteMapRepository] for tests: each folder answers what [results] holds for it, or an empty
 * listing. Calls are recorded. While [gate] is set, calls wait for it to complete first.
 */
internal class FakeRemoteMapRepository : RemoteMapRepository {

    /** What each folder answers. */
    val results: MutableMap<RemoteFolderPath, RemoteListing> = mutableMapOf()

    /** The folders passed to [list], in call order. */
    val listed: MutableList<RemoteFolderPath> = mutableListOf()

    /** If set, calls wait until it completes, e.g. to check the loading state. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun list(folder: RemoteFolderPath): RemoteListing {
        listed += folder
        gate?.await()
        return results[folder] ?: RemoteListing.Listed(emptyList())
    }
}
