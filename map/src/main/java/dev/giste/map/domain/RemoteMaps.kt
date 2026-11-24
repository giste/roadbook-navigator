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

import java.time.LocalDate

/**
 * A folder of the download site, relative to its maps folder.
 *
 * Its names become folder names on the device when a map is downloaded (maps keep the site's
 * folders), so each one must be a safe name (see [isSafeRemoteName]).
 *
 * @property segments the folder names from the maps folder down, e.g. `[europe, italy]`. Empty
 * for the maps folder itself.
 */
internal data class RemoteFolderPath(val segments: List<String>) {

    init {
        require(segments.all(::isSafeRemoteName)) { "Unsafe folder name in $segments" }
    }

    /** Whether this is the download site's maps folder itself. */
    val isRoot: Boolean get() = segments.isEmpty()

    /**
     * The folder [name] inside this one.
     *
     * @param name a safe folder name.
     * @return the path of that folder.
     */
    fun child(name: String): RemoteFolderPath = RemoteFolderPath(segments + name)

    /** The folder that holds this one, or `null` for the maps folder itself. */
    fun parent(): RemoteFolderPath? = if (isRoot) null else RemoteFolderPath(segments.dropLast(1))

    /**
     * The folder names with `/` between them, e.g. `europe/italy`, or an empty string for the maps
     * folder itself: the same form as [InstalledMapFile.folder], so they can be compared.
     */
    override fun toString(): String = segments.joinToString(SEPARATOR)

    /** The maps folder, and reading paths back from text. */
    companion object {

        private const val SEPARATOR = "/"

        /** The download site's maps folder itself. */
        val ROOT: RemoteFolderPath = RemoteFolderPath(emptyList())

        /**
         * Reads a path written by [toString].
         *
         * @param path folder names with `/` between them, or an empty string for [ROOT].
         * @return the path.
         * @throws IllegalArgumentException if a name in it is not safe.
         */
        fun parse(path: String): RemoteFolderPath =
            if (path.isEmpty()) ROOT else RemoteFolderPath(path.split(SEPARATOR))
    }
}

/**
 * Whether [name] can be used as a file or folder name on the device: not blank, not `.` or `..`,
 * and without path separators or control characters. Names come from the download site, so a
 * name that could reach outside the map folder is never used.
 *
 * @param name a file or folder name from the download site.
 * @return `true` if it is safe.
 */
internal fun isSafeRemoteName(name: String): Boolean =
    name.isNotBlank() &&
        name != "." &&
        name != ".." &&
        name.none { it == '/' || it == '\\' || it.isISOControl() }

/** Something listed in a folder of the download site. */
internal sealed interface RemoteMapEntry {

    /** Its name, always safe (see [isSafeRemoteName]). */
    val name: String

    /**
     * A folder, which holds more maps or folders.
     *
     * @property name the folder name, e.g. `europe`.
     */
    data class Folder(override val name: String) : RemoteMapEntry

    /**
     * A map file.
     *
     * @property name the file name, e.g. `andorra.map`.
     * @property approximateSize the size the site lists, rounded by the site (e.g. "2.4M"), or
     * `null` if it lists none.
     * @property modified the day the site lists it as changed, or `null` if it lists none. The
     * site lists it in its own time zone, so it may be a day off near midnight.
     */
    data class MapFile(
        override val name: String,
        val approximateSize: FileSize?,
        val modified: LocalDate?,
    ) : RemoteMapEntry
}

/** What a folder of the download site holds, as [RemoteMapRepository] found it. */
internal sealed interface RemoteListing {

    /**
     * The folder was read.
     *
     * @property entries its folders first, then its map files, each group in the site's order.
     * Other files are left out. Empty if it holds neither.
     */
    data class Listed(val entries: List<RemoteMapEntry>) : RemoteListing

    /**
     * The folder could not be read.
     *
     * @property reason why.
     */
    data class Failed(val reason: RemoteListingFailure) : RemoteListing
}

/** Why a folder of the download site could not be read. */
internal enum class RemoteListingFailure {

    /** The site could not be reached, e.g. there is no connection. Trying again may work. */
    NO_CONNECTION,

    /** The site answered with an error, e.g. the folder is no longer there. */
    SERVER_ERROR,

    /** The site answered, but not with a folder listing this module can read. */
    UNREADABLE_LISTING,
}

/** The maps on the download site. */
internal interface RemoteMapRepository {

    /**
     * Reads one folder of the download site. Main-safe. It never throws, except to cancel.
     *
     * @param folder the folder to read.
     * @return its folders and map files, or why it could not be read.
     */
    suspend fun list(folder: RemoteFolderPath): RemoteListing
}
