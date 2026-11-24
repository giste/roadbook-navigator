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

/**
 * An offline map file the map can draw: a mapsforge `.map` file whose header was read
 * successfully.
 *
 * @property name the file name, e.g. `spain.map`, for the logs.
 * @property path the absolute path of the file, which the renderer opens.
 */
internal data class MapFile(val name: String, val path: String)

/** The offline maps the viewer can draw, from [InstalledMapFiles.toInstalledMaps]. */
internal sealed interface InstalledMaps {

    /**
     * Maps that can be drawn.
     *
     * Unreadable files next to them are left out (and logged), so one damaged file does not stop
     * the others from being drawn.
     *
     * @property files the readable map files, never empty, ordered by path so the result does
     * not depend on the file system.
     */
    data class Available(val files: List<MapFile>) : InstalledMaps {
        init {
            require(files.isNotEmpty()) { "Available maps need at least one file" }
        }
    }

    /** No map file is installed. */
    data object None : InstalledMaps

    /**
     * Map files are installed, or may be, but none can be read: every file is damaged or of an
     * unsupported version, or the folder itself cannot be read.
     */
    data object Unreadable : InstalledMaps
}
