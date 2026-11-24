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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * [MapFileRepository] for tests: each collection emits [result] and completes, like the real one,
 * and is counted. Deletions answer [deleteResult] and are recorded; they do not change [result].
 *
 * @param result the installed map files to report.
 */
internal class FakeMapFileRepository(var result: InstalledMapFiles) : MapFileRepository {

    /** How many times [installedFiles] was collected. */
    var collections: Int = 0
        private set

    /** What [delete] answers. */
    var deleteResult: DeleteMapResult = DeleteMapResult.Deleted

    /** The files passed to [delete], in call order. */
    val deleted: MutableList<InstalledMapFile> = mutableListOf()

    override val installedFiles: Flow<InstalledMapFiles> = flow {
        collections++
        emit(result)
    }

    override suspend fun delete(file: InstalledMapFile): DeleteMapResult {
        deleted += file
        return deleteResult
    }
}

/** A readable map file for tests, at `/maps/<name>`. */
internal fun testMapFile(name: String = "spain.map"): MapFile = MapFile(name = name, path = "/maps/$name")

/**
 * An installed map file for tests, at `/maps/<name>` like [testMapFile], in the map folder itself.
 *
 * @param name the file name.
 * @param readable whether the renderer can open it.
 * @param size its size in bytes.
 */
internal fun testInstalledFile(
    name: String = "spain.map",
    readable: Boolean = true,
    size: Long = 1_000,
): InstalledMapFile = InstalledMapFile(
    name = name,
    path = "/maps/$name",
    folder = "",
    size = FileSize(size),
    readable = readable,
)
