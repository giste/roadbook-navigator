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

import app.cash.turbine.test
import dev.giste.map.FakeLogger
import dev.giste.map.domain.DeleteMapResult
import dev.giste.map.domain.FileSize
import dev.giste.map.domain.InstalledMapFile
import dev.giste.map.domain.InstalledMapFiles
import java.io.File
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LocalMapFileRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val logger = FakeLogger()

    /** The files the checker rejects, by name. Every other file is readable. */
    private val unreadableNames = mutableSetOf<String>()

    /** The checker, unless a test replaces it. */
    private var checker = MapFileChecker { file ->
        if (file.name in unreadableNames) "damaged" else null
    }

    private val mapFolder: File get() = File(temporaryFolder.root, "map_files")

    private fun TestScope.repository(folder: File = mapFolder, testChecker: MapFileChecker = checker) =
        LocalMapFileRepository(folder, testChecker, StandardTestDispatcher(testScheduler), logger)

    /** Writes a file of [size] bytes at [relativePath] in the map folder. */
    private fun addFile(relativePath: String, size: Int = 10): File =
        File(mapFolder, relativePath).apply {
            parentFile?.mkdirs()
            // The content does not matter: the checker is fake.
            writeBytes(ByteArray(size))
        }

    /** The listed file expected for [relativePath], as [addFile] wrote it. */
    private fun installed(
        relativePath: String,
        folder: String = "",
        size: Long = 10,
        readable: Boolean = true,
    ): InstalledMapFile {
        val file = File(mapFolder, relativePath)
        return InstalledMapFile(
            name = file.name,
            path = file.absolutePath,
            folder = folder,
            size = FileSize(size),
            readable = readable,
        )
    }

    /** Collects the installed files once and returns them. */
    private suspend fun LocalMapFileRepository.list(): InstalledMapFiles {
        var result: InstalledMapFiles? = null
        installedFiles.test {
            result = awaitItem()
            awaitComplete()
        }
        return checkNotNull(result)
    }

    // --- installedFiles ---

    @Test
    fun installedFiles_missingFolder_createsItAndIsEmpty() = runTest {
        assertEquals(InstalledMapFiles.Listed(emptyList()), repository().list())
        assertTrue(mapFolder.isDirectory)
    }

    @Test
    fun installedFiles_onlyOtherFiles_isEmpty() = runTest {
        addFile("readme.txt")
        addFile("spain.map.tmp")

        assertEquals(InstalledMapFiles.Listed(emptyList()), repository().list())
    }

    @Test
    fun installedFiles_mapFiles_areListedInPathOrderWithTheirSizes() = runTest {
        addFile("spain.map", size = 300)
        addFile("andorra.map", size = 20)
        addFile("notes.txt")

        assertEquals(
            InstalledMapFiles.Listed(
                listOf(installed("andorra.map", size = 20), installed("spain.map", size = 300)),
            ),
            repository().list(),
        )
    }

    @Test
    fun installedFiles_upperCaseExtension_isIncluded() = runTest {
        addFile("PORTUGAL.MAP")

        assertEquals(InstalledMapFiles.Listed(listOf(installed("PORTUGAL.MAP"))), repository().list())
    }

    @Test
    fun installedFiles_mapFilesInSubFolders_areListedWithTheirFolder() = runTest {
        addFile("europe/spain.map")
        addFile("europe/france/corsica.map")

        assertEquals(
            InstalledMapFiles.Listed(
                listOf(
                    installed("europe/france/corsica.map", folder = "europe/france"),
                    installed("europe/spain.map", folder = "europe"),
                ),
            ),
            repository().list(),
        )
    }

    @Test
    fun installedFiles_folderNamedLikeAMap_isNotAFile() = runTest {
        File(mapFolder, "folder.map").mkdirs()

        assertEquals(InstalledMapFiles.Listed(emptyList()), repository().list())
    }

    @Test
    fun installedFiles_unreadableFiles_areListedAsUnreadableAndLogged() = runTest {
        addFile("spain.map")
        addFile("damaged.map")
        unreadableNames += "damaged.map"

        assertEquals(
            InstalledMapFiles.Listed(listOf(installed("damaged.map", readable = false), installed("spain.map"))),
            repository().list(),
        )
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun installedFiles_folderIsAFile_isFolderUnreadable() = runTest {
        mapFolder.writeText("a file where the folder should be")

        assertEquals(InstalledMapFiles.FolderUnreadable, repository().list())
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun installedFiles_folderCannotBeListed_isFolderUnreadable() = runTest {
        addFile("spain.map")
        mapFolder.setReadable(false)
        try {
            assertEquals(InstalledMapFiles.FolderUnreadable, repository().list())
        } finally {
            // Lets TemporaryFolder delete it.
            mapFolder.setReadable(true)
        }
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun installedFiles_fileSystemDeniesAccess_isFolderUnreadableAndLogs() = runTest {
        addFile("spain.map")
        val failure = SecurityException("denied")

        assertEquals(InstalledMapFiles.FolderUnreadable, repository(testChecker = { throw failure }).list())
        assertEquals(listOf<Throwable>(failure), logger.loggedErrors)
    }

    @Test
    fun installedFiles_unexpectedFailure_propagates() = runTest {
        addFile("spain.map")
        val failure = IllegalStateException("a bug")

        repository(testChecker = { throw failure }).installedFiles.test {
            // Coroutines may rebuild the exception across coroutines, so compare its contents.
            val error = awaitError()
            assertTrue(error is IllegalStateException)
            assertEquals(failure.message, error.message)
        }
    }

    @Test
    fun installedFiles_eachCollection_looksAgain() = runTest {
        val repository = repository()
        assertEquals(InstalledMapFiles.Listed(emptyList()), repository.list())
        addFile("spain.map")

        assertEquals(InstalledMapFiles.Listed(listOf(installed("spain.map"))), repository.list())
    }

    // --- delete ---

    @Test
    fun delete_mapFile_deletesItAndIsDeleted() = runTest {
        val file = addFile("spain.map")
        addFile("andorra.map")

        assertEquals(DeleteMapResult.Deleted, repository().delete(installed("spain.map")))
        assertFalse(file.exists())
        assertTrue(File(mapFolder, "andorra.map").exists())
    }

    @Test
    fun delete_mapFileInASubFolder_deletesIt() = runTest {
        val file = addFile("europe/spain.map")

        assertEquals(
            DeleteMapResult.Deleted,
            repository().delete(installed("europe/spain.map", folder = "europe")),
        )
        assertFalse(file.exists())
    }

    @Test
    fun delete_fileAlreadyGone_isDeleted() = runTest {
        mapFolder.mkdirs()

        assertEquals(DeleteMapResult.Deleted, repository().delete(installed("spain.map")))
    }

    @Test
    fun delete_fileOutsideTheMapFolder_isFailedAndKeepsIt() = runTest {
        mapFolder.mkdirs()
        val outside = temporaryFolder.newFile("outside.map")
        val file = installed("spain.map").copy(name = outside.name, path = outside.absolutePath)

        assertEquals(DeleteMapResult.Failed, repository().delete(file))
        assertTrue(outside.exists())
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun delete_pathLeavingTheMapFolder_isFailedAndKeepsTheFile() = runTest {
        mapFolder.mkdirs()
        val outside = temporaryFolder.newFile("outside.map")
        val file = installed("../outside.map")

        assertEquals(DeleteMapResult.Failed, repository().delete(file))
        assertTrue(outside.exists())
    }

    @Test
    fun delete_otherFileInTheMapFolder_isFailedAndKeepsIt() = runTest {
        val notes = addFile("notes.txt")

        assertEquals(DeleteMapResult.Failed, repository().delete(installed("notes.txt")))
        assertTrue(notes.exists())
    }

    @Test
    fun delete_folderNamedLikeAMap_isFailedAndKeepsIt() = runTest {
        val folder = File(mapFolder, "folder.map").apply { mkdirs() }

        assertEquals(DeleteMapResult.Failed, repository().delete(installed("folder.map")))
        assertTrue(folder.isDirectory)
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun delete_fileSystemRefuses_isFailedAndKeepsTheFile() = runTest {
        val file = addFile("spain.map")
        mapFolder.setWritable(false)
        try {
            assertEquals(DeleteMapResult.Failed, repository().delete(installed("spain.map")))
            assertTrue(file.exists())
        } finally {
            // Lets TemporaryFolder delete it.
            mapFolder.setWritable(true)
        }
    }

    @Test
    fun delete_invalidPath_isFailedAndLogs() = runTest {
        mapFolder.mkdirs()
        // The file system rejects a path with a NUL character with an IOException.
        val file = installed("spain.map").copy(path = File(mapFolder, "bad\u0000.map").path)

        assertEquals(DeleteMapResult.Failed, repository().delete(file))
        assertEquals(1, logger.loggedErrors.size)
    }
}
