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
import dev.giste.map.domain.DeleteMapResult
import dev.giste.map.domain.FileSize
import dev.giste.map.domain.InstalledMapFile
import dev.giste.map.domain.InstalledMapFiles
import dev.giste.map.domain.MapFileRepository
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Finds the map files in [folder] and its sub-folders (every file with the `.map` extension, in
 * any case), checks which ones the renderer can read, and deletes them.
 *
 * The folder is created if it does not exist, so map files can be copied into it. Unreadable files
 * are listed as such and logged with their name. If the folder cannot be read, the result is
 * [InstalledMapFiles.FolderUnreadable].
 *
 * @param folder the folder that holds the map files.
 * @param checker tells which files the renderer can open.
 * @param ioDispatcher the dispatcher the file system is read and changed on.
 * @param logger logs unreadable files and failures.
 */
internal class LocalMapFileRepository @Inject constructor(
    @param:MapFolder private val folder: File,
    private val checker: MapFileChecker,
    private val folderChanges: MapFolderChanges,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val logger: Logger,
) : MapFileRepository {

    override val installedFiles: Flow<InstalledMapFiles> = folderChanges.changes
        .map { findFilesSafely() }
        .flowOn(ioDispatcher)

    override suspend fun delete(file: InstalledMapFile): DeleteMapResult = withContext(ioDispatcher) {
        try {
            deleteFile(File(file.path))
        } catch (error: Exception) {
            // Only file-system failures are expected here (cancellation is not one, so it is
            // rethrown too). Anything else is a bug and propagates.
            if (!error.isFileSystemFailure()) throw error
            logger.e(error, "Cannot delete the map file %s", file.name)
            DeleteMapResult.Failed
        } finally {
            // Also after a failure: the folder may have changed in other ways.
            folderChanges.notifyChanged()
        }
    }

    /**
     * Looks for the map files, reporting a failure of the file system as an unreadable folder.
     * Caught here rather than with `catch` on the flow, so the flow goes on after a failure.
     */
    private fun findFilesSafely(): InstalledMapFiles = try {
        findFiles()
    } catch (error: Exception) {
        // Only file-system failures are expected here. Anything else is a bug and propagates.
        if (!error.isFileSystemFailure()) throw error
        logger.e(error, "Cannot read the map folder")
        InstalledMapFiles.FolderUnreadable
    }

    /** Looks for the map files and checks them. */
    private fun findFiles(): InstalledMapFiles {
        folder.mkdirs()
        if (!folder.isDirectory) {
            logger.w("The map folder does not exist and cannot be created")
            return InstalledMapFiles.FolderUnreadable
        }
        // listFiles() returns null when the folder cannot be read, so walk() would silently find
        // nothing: check it, to report an unreadable folder rather than no maps.
        if (folder.listFiles() == null) {
            logger.w("The map folder cannot be read")
            return InstalledMapFiles.FolderUnreadable
        }
        val files = folder.walk()
            .filter { it.isFile && it.extension.equals(MAP_FILE_EXTENSION, ignoreCase = true) }
            .sortedBy { it.path }
            .map { file ->
                val problem = checker.problemWith(file)
                if (problem != null) logger.w("Unreadable map file %s: %s", file.name, problem)
                InstalledMapFile(
                    name = file.name,
                    path = file.absolutePath,
                    folder = file.parentFile?.relativeTo(folder)?.invariantSeparatorsPath.orEmpty(),
                    size = FileSize(file.length()),
                    readable = problem == null,
                )
            }
            .toList()
        return InstalledMapFiles.Listed(files)
    }

    /**
     * Deletes [file], but only if it is a map file inside [folder]: the path comes from outside
     * the repository, and nothing else in the app's storage may be deleted through it.
     */
    private fun deleteFile(file: File): DeleteMapResult {
        val canonical = file.canonicalFile
        val inFolder = canonical.toPath().startsWith(folder.canonicalFile.toPath())
        if (!inFolder || !canonical.extension.equals(MAP_FILE_EXTENSION, ignoreCase = true)) {
            logger.w("Not deleting %s: it is not a map file in the map folder", file.name)
            return DeleteMapResult.Failed
        }
        // A file that is already gone counts as deleted: the rider wanted it gone.
        if (!canonical.exists()) return DeleteMapResult.Deleted
        if (canonical.isFile && canonical.delete()) {
            logger.i("Deleted the map file %s", file.name)
            return DeleteMapResult.Deleted
        }
        logger.w("Cannot delete the map file %s", file.name)
        return DeleteMapResult.Failed
    }

    /** Whether this is a failure of the file system, which is expected and reported. */
    private fun Throwable.isFileSystemFailure(): Boolean = this is IOException || this is SecurityException

    /** Names of the files and folder this repository uses. */
    companion object {

        /** The extension of mapsforge map files. */
        const val MAP_FILE_EXTENSION: String = "map"

        /** The name of the folder that holds the map files, inside the app's no-backup folder. */
        const val FOLDER_NAME: String = "map_files"
    }
}
