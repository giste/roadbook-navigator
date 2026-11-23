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

import dev.giste.core.coroutines.IoDispatcher
import dev.giste.core.logging.Logger
import dev.giste.map.domain.InstalledMaps
import dev.giste.map.domain.MapFile
import dev.giste.map.domain.MapFileRepository
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Finds the map files in [folder] and its sub-folders: every file with the `.map` extension
 * (in any case) whose header the renderer can read.
 *
 * The folder is created if it does not exist, so map files can be copied into it. Unreadable files
 * are left out and logged with their name. If the folder cannot be read, the result is
 * [InstalledMaps.Unreadable].
 *
 * @param folder the folder that holds the map files.
 * @param checker tells which files the renderer can open.
 * @param ioDispatcher the dispatcher the file system is read on.
 * @param logger logs left-out files and failures.
 */
internal class LocalMapFileRepository @Inject constructor(
    @param:MapFolder private val folder: File,
    private val checker: MapFileChecker,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val logger: Logger,
) : MapFileRepository {

    override val installedMaps: Flow<InstalledMaps> = flow { emit(findMaps()) }
        .catch { error ->
            // Only file-system failures are expected here. Anything else is a bug and propagates.
            if (error !is IOException && error !is SecurityException) throw error
            logger.e(error, "Cannot read the map folder")
            emit(InstalledMaps.Unreadable)
        }
        .flowOn(ioDispatcher)

    /** Looks for the map files, checks them and sums up the result. */
    private fun findMaps(): InstalledMaps {
        folder.mkdirs()
        if (!folder.isDirectory) {
            logger.w("The map folder does not exist and cannot be created")
            return InstalledMaps.Unreadable
        }
        // listFiles() returns null when the folder cannot be read, so walk() would silently find
        // nothing: check it, to report an unreadable folder rather than no maps.
        if (folder.listFiles() == null) {
            logger.w("The map folder cannot be read")
            return InstalledMaps.Unreadable
        }
        val candidates = folder.walk()
            .filter { it.isFile && it.extension.equals(MAP_FILE_EXTENSION, ignoreCase = true) }
            .sortedBy { it.path }
            .toList()
        if (candidates.isEmpty()) return InstalledMaps.None

        val readable = candidates.filter { file ->
            val problem = checker.problemWith(file)
            if (problem != null) logger.w("Leaving out the map file %s: %s", file.name, problem)
            problem == null
        }
        return if (readable.isEmpty()) {
            InstalledMaps.Unreadable
        } else {
            InstalledMaps.Available(readable.map { MapFile(name = it.name, path = it.absolutePath) })
        }
    }

    /** Names of the files and folder this repository uses. */
    companion object {

        /** The extension of mapsforge map files. */
        const val MAP_FILE_EXTENSION: String = "map"

        /** The name of the folder that holds the map files, inside the app's no-backup folder. */
        const val FOLDER_NAME: String = "map_files"
    }
}
