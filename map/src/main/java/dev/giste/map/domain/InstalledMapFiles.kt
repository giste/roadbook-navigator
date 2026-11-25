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

import java.time.Instant

/**
 * The size of a file.
 *
 * @property bytes the size in bytes, never negative.
 */
@JvmInline
internal value class FileSize(val bytes: Long) {
    init {
        require(bytes >= 0) { "A file size cannot be negative: $bytes" }
    }
}

/**
 * A map file in the map folder, whether the renderer can read it or not, as the map manager lists
 * it.
 *
 * @property name the file name, e.g. `spain.map`.
 * @property path the absolute path of the file.
 * @property folder the sub-folder that holds it, relative to the map folder with `/` between
 * folders (e.g. `europe/france`), or an empty string when it is in the map folder itself.
 * @property size the size of the file.
 * @property readable whether the renderer can open it. Unreadable files are not drawn, but they
 * are listed, so they can be deleted.
 * @property modified when the file last changed. For a downloaded map, it is when the map last
 * changed on the download site (its `Last-Modified`), which is how updates are found.
 */
internal data class InstalledMapFile(
    val name: String,
    val path: String,
    val folder: String,
    val size: FileSize,
    val readable: Boolean,
    val modified: Instant,
)

/** Every map file installed on the device, as found by [MapFileRepository]. */
internal sealed interface InstalledMapFiles {

    /**
     * The map folder was read.
     *
     * @property files the map files in it and in its sub-folders, readable or not, ordered by
     * path so the result does not depend on the file system. Empty when no map is installed.
     */
    data class Listed(val files: List<InstalledMapFile>) : InstalledMapFiles

    /** The map folder cannot be read, or created, so it is unknown which maps are installed. */
    data object FolderUnreadable : InstalledMapFiles
}

/**
 * Sums up these files as the maps the viewer can draw.
 *
 * @return [InstalledMaps.None] when no file is listed, [InstalledMaps.Unreadable] when none of them
 * can be read or the folder cannot be read, and otherwise [InstalledMaps.Available] with the
 * readable files, in the same order.
 */
internal fun InstalledMapFiles.toInstalledMaps(): InstalledMaps = when (this) {
    InstalledMapFiles.FolderUnreadable -> InstalledMaps.Unreadable
    is InstalledMapFiles.Listed -> {
        val readable = files.filter { it.readable }
        when {
            files.isEmpty() -> InstalledMaps.None
            readable.isEmpty() -> InstalledMaps.Unreadable
            else -> InstalledMaps.Available(readable.map { MapFile(name = it.name, path = it.path) })
        }
    }
}

/** What happened when a map file was to be deleted. */
internal sealed interface DeleteMapResult {

    /** The file is gone: it was deleted, or it no longer existed. */
    data object Deleted : DeleteMapResult

    /** The file could not be deleted, e.g. the file system refused it. It is still installed. */
    data object Failed : DeleteMapResult
}
