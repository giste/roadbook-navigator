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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapUpdatesTest {

    private val europe = RemoteFolderPath.ROOT.child("europe")
    private val andorraPath = RemoteMapPath(europe, "andorra.map")
    private val andorra = testInstalledFile("andorra.map", folder = "europe")

    @Test
    fun remotePath_fileInTheMapFolder_isInTheSitesMapsFolder() {
        assertEquals(RemoteMapPath(RemoteFolderPath.ROOT, "spain.map"), testInstalledFile("spain.map").remotePath())
    }

    @Test
    fun remotePath_fileInSubFolders_keepsThem() {
        val file = testInstalledFile("centro.map", folder = "europe/italy")

        assertEquals(RemoteMapPath(europe.child("italy"), "centro.map"), file.remotePath())
    }

    @Test
    fun remotePath_unsafeName_isNull() {
        assertNull(testInstalledFile("bad\\name.map").remotePath())
    }

    @Test
    fun remotePath_unsafeFolder_isNull() {
        assertNull(testInstalledFile("spain.map", folder = "europe/..").remotePath())
    }

    @Test
    fun isOutdated_siteNewer_isTrue() {
        val versions = mapOf(andorraPath to TEST_MODIFIED.plusSeconds(1))

        assertTrue(andorra.isOutdated(versions))
    }

    @Test
    fun isOutdated_siteSameDate_isFalse() {
        assertFalse(andorra.isOutdated(mapOf(andorraPath to TEST_MODIFIED)))
    }

    @Test
    fun isOutdated_fileHasFractionsOfTheSitesSecond_isFalse() {
        // E.g. a file system that keeps milliseconds the HTTP date does not have.
        val file = testInstalledFile("andorra.map", folder = "europe", modified = TEST_MODIFIED.plusMillis(999))

        assertFalse(file.isOutdated(mapOf(andorraPath to TEST_MODIFIED)))
    }

    @Test
    fun isOutdated_siteOlder_isFalse() {
        assertFalse(andorra.isOutdated(mapOf(andorraPath to TEST_MODIFIED.minusSeconds(60))))
    }

    @Test
    fun isOutdated_siteDateUnknown_isFalse() {
        assertFalse(andorra.isOutdated(emptyMap()))
    }

    @Test
    fun isOutdated_sameNameInAnotherFolder_isFalse() {
        val versions = mapOf(RemoteMapPath(RemoteFolderPath.ROOT, "andorra.map") to TEST_MODIFIED.plusSeconds(1))

        assertFalse(andorra.isOutdated(versions))
    }

    @Test
    fun isOutdated_unsafePath_isFalse() {
        val file = testInstalledFile("bad\\name.map")

        assertFalse(file.isOutdated(mapOf(andorraPath to Instant.MAX)))
    }

    @Test
    fun checkMapUpdates_installedFiles_asksAboutEachSafeOne() = runTest {
        val files = FakeMapFileRepository(
            InstalledMapFiles.Listed(listOf(andorra, testInstalledFile("bad\\name.map"), testInstalledFile("spain.map"))),
        )
        val updates = FakeMapUpdateRepository()

        CheckMapUpdatesUseCase(files, updates)()

        assertEquals(listOf(listOf(andorraPath, RemoteMapPath(RemoteFolderPath.ROOT, "spain.map"))), updates.checked)
    }

    @Test
    fun checkMapUpdates_noFiles_asksNothing() = runTest {
        val updates = FakeMapUpdateRepository()

        CheckMapUpdatesUseCase(FakeMapFileRepository(InstalledMapFiles.Listed(emptyList())), updates)()

        assertEquals(emptyList<List<RemoteMapPath>>(), updates.checked)
    }

    @Test
    fun checkMapUpdates_folderUnreadable_asksNothing() = runTest {
        val updates = FakeMapUpdateRepository()

        CheckMapUpdatesUseCase(FakeMapFileRepository(InstalledMapFiles.FolderUnreadable), updates)()

        assertEquals(emptyList<List<RemoteMapPath>>(), updates.checked)
    }
}
