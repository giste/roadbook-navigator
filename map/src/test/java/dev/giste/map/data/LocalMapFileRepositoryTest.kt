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
import dev.giste.map.domain.InstalledMaps
import dev.giste.map.domain.MapFile
import java.io.File
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

    private fun addFile(relativePath: String): File =
        File(mapFolder, relativePath).apply {
            parentFile?.mkdirs()
            writeText("content does not matter: the checker is fake")
        }

    private fun mapFile(relativePath: String): MapFile {
        val file = File(mapFolder, relativePath)
        return MapFile(name = file.name, path = file.absolutePath)
    }

    @Test
    fun installedMaps_missingFolder_createsItAndIsNone() = runTest {
        repository().installedMaps.test {
            assertEquals(InstalledMaps.None, awaitItem())
            awaitComplete()
        }
        assertTrue(mapFolder.isDirectory)
    }

    @Test
    fun installedMaps_onlyOtherFiles_isNone() = runTest {
        addFile("readme.txt")
        addFile("spain.map.tmp")

        repository().installedMaps.test {
            assertEquals(InstalledMaps.None, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun installedMaps_mapFiles_areAvailableInPathOrder() = runTest {
        addFile("spain.map")
        addFile("andorra.map")
        addFile("notes.txt")

        repository().installedMaps.test {
            assertEquals(
                InstalledMaps.Available(listOf(mapFile("andorra.map"), mapFile("spain.map"))),
                awaitItem(),
            )
            awaitComplete()
        }
    }

    @Test
    fun installedMaps_upperCaseExtension_isIncluded() = runTest {
        addFile("PORTUGAL.MAP")

        repository().installedMaps.test {
            assertEquals(InstalledMaps.Available(listOf(mapFile("PORTUGAL.MAP"))), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun installedMaps_mapFilesInSubFolders_areIncluded() = runTest {
        addFile("europe/spain.map")
        addFile("europe/france/corsica.map")

        repository().installedMaps.test {
            assertEquals(
                InstalledMaps.Available(
                    listOf(mapFile("europe/france/corsica.map"), mapFile("europe/spain.map")),
                ),
                awaitItem(),
            )
            awaitComplete()
        }
    }

    @Test
    fun installedMaps_folderNamedLikeAMap_isNotAFile() = runTest {
        File(mapFolder, "folder.map").mkdirs()

        repository().installedMaps.test {
            assertEquals(InstalledMaps.None, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun installedMaps_someUnreadable_leavesThemOutAndLogs() = runTest {
        addFile("spain.map")
        addFile("damaged.map")
        unreadableNames += "damaged.map"

        repository().installedMaps.test {
            assertEquals(InstalledMaps.Available(listOf(mapFile("spain.map"))), awaitItem())
            awaitComplete()
        }
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun installedMaps_allUnreadable_isUnreadable() = runTest {
        addFile("damaged.map")
        unreadableNames += "damaged.map"

        repository().installedMaps.test {
            assertEquals(InstalledMaps.Unreadable, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun installedMaps_folderIsAFile_isUnreadable() = runTest {
        mapFolder.writeText("a file where the folder should be")

        repository().installedMaps.test {
            assertEquals(InstalledMaps.Unreadable, awaitItem())
            awaitComplete()
        }
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun installedMaps_folderCannotBeListed_isUnreadable() = runTest {
        addFile("spain.map")
        mapFolder.setReadable(false)
        try {
            repository().installedMaps.test {
                assertEquals(InstalledMaps.Unreadable, awaitItem())
                awaitComplete()
            }
        } finally {
            // Lets TemporaryFolder delete it.
            mapFolder.setReadable(true)
        }
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun installedMaps_fileSystemDeniesAccess_isUnreadableAndLogs() = runTest {
        addFile("spain.map")
        val failure = SecurityException("denied")

        repository(testChecker = { throw failure }).installedMaps.test {
            assertEquals(InstalledMaps.Unreadable, awaitItem())
            awaitComplete()
        }
        assertEquals(listOf<Throwable>(failure), logger.loggedErrors)
    }

    @Test
    fun installedMaps_unexpectedFailure_propagates() = runTest {
        addFile("spain.map")
        val failure = IllegalStateException("a bug")

        repository(testChecker = { throw failure }).installedMaps.test {
            // Coroutines may rebuild the exception across coroutines, so compare its contents.
            val error = awaitError()
            assertTrue(error is IllegalStateException)
            assertEquals(failure.message, error.message)
        }
    }

    @Test
    fun installedMaps_eachCollection_looksAgain() = runTest {
        val repository = repository()
        repository.installedMaps.test {
            assertEquals(InstalledMaps.None, awaitItem())
            awaitComplete()
        }
        addFile("spain.map")

        repository.installedMaps.test {
            assertEquals(InstalledMaps.Available(listOf(mapFile("spain.map"))), awaitItem())
            awaitComplete()
        }
    }
}
