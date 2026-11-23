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

package dev.giste.map.data

import java.io.File
import javax.inject.Inject
import org.oscim.tiling.source.mapfile.MapFileTileSource

/** Tells whether a file is a map file the renderer can open. */
internal fun interface MapFileChecker {

    /**
     * Checks [file]. It blocks while reading, so call it on an I/O dispatcher.
     *
     * @param file the file to check.
     * @return `null` when the renderer can open it, or why it cannot (for the logs).
     */
    fun problemWith(file: File): String?
}

/**
 * Checks map files with VTM itself: it opens each file as the renderer will, which reads and
 * validates the mapsforge header (magic bytes, version, size, bounding box, zoom levels), and
 * closes it again.
 *
 * VTM draws several files as one source that fails as a whole when any of its files fails to
 * open, so the map would stay blank. Checking each file first lets the good ones be drawn.
 */
internal class VtmMapFileChecker @Inject constructor() : MapFileChecker {

    override fun problemWith(file: File): String? {
        val source = MapFileTileSource()
        // false when the file is missing, is not a file, or cannot be read.
        if (!source.setMapFile(file.absolutePath)) return "not a readable file"
        return try {
            val result = source.open()
            if (result.isSuccess) null else result.errorMessage
        } catch (error: RuntimeException) {
            // open() reports I/O errors and invalid headers as results, but a damaged file may
            // still trip VTM's parser. A bad file must not crash the app: report it instead.
            "unexpected ${error::class.simpleName}: ${error.message}"
        } finally {
            source.close()
        }
    }
}
