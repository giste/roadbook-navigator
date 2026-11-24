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
import dev.giste.map.domain.FileSize
import dev.giste.map.domain.InstalledMapFile
import dev.giste.map.domain.InstalledMapFiles
import java.io.File
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** [LocalMapFileRepository] with the real VTM checker, on generated map files. */
class LocalMapFileRepositoryIntegrationTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val logger = FakeLogger()

    @Test
    fun installedFiles_validAndDamagedFiles_marksOnlyTheValidOnesReadable() = runTest {
        val folder = temporaryFolder.newFolder("map_files")
        val valid = writeMapFile(File(folder, "valid.map"))
        val damaged = File(folder, "damaged.map").apply { writeText("not a map") }
        val future = writeMapFile(File(folder, "future.map"), fileVersion = 99)
        val repository =
            LocalMapFileRepository(folder, VtmMapFileChecker(), StandardTestDispatcher(testScheduler), logger)

        repository.installedFiles.test {
            assertEquals(
                InstalledMapFiles.Listed(
                    listOf(installed(damaged, readable = false), installed(future, readable = false), installed(valid, readable = true)),
                ),
                awaitItem(),
            )
            awaitComplete()
        }
        assertEquals(2, logger.warnings.size)
    }

    /** [file] as listed, in the map folder itself. */
    private fun installed(file: File, readable: Boolean) = InstalledMapFile(
        name = file.name,
        path = file.absolutePath,
        folder = "",
        size = FileSize(file.length()),
        readable = readable,
    )
}
