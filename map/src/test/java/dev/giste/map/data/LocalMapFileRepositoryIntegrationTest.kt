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
    fun installedMaps_validAndDamagedFiles_keepsOnlyTheValidOnes() = runTest {
        val folder = temporaryFolder.newFolder("map_files")
        val valid = writeMapFile(File(folder, "valid.map"))
        File(folder, "damaged.map").writeText("not a map")
        writeMapFile(File(folder, "future.map"), fileVersion = 99)
        val repository =
            LocalMapFileRepository(folder, VtmMapFileChecker(), StandardTestDispatcher(testScheduler), logger)

        repository.installedMaps.test {
            assertEquals(
                InstalledMaps.Available(listOf(MapFile(name = "valid.map", path = valid.absolutePath))),
                awaitItem(),
            )
            awaitComplete()
        }
        assertEquals(2, logger.warnings.size)
    }
}
