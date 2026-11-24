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

/** The offline map files installed on the device. */
internal interface MapFileRepository {

    /**
     * Every installed map file, readable or not.
     *
     * A cold flow: each collection looks for the files once, emits the result and completes, so
     * collect it again to see changes. It never throws: a folder that cannot be read is
     * [InstalledMapFiles.FolderUnreadable].
     */
    val installedFiles: Flow<InstalledMapFiles>

    /**
     * Deletes [file] from the device. Main-safe.
     *
     * @param file a file listed by [installedFiles].
     * @return [DeleteMapResult.Deleted] once the file is gone (also when it was already gone), or
     * [DeleteMapResult.Failed] when it is still there. It never throws, except to cancel.
     */
    suspend fun delete(file: InstalledMapFile): DeleteMapResult
}
