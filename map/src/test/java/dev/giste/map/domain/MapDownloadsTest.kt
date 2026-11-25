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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MapDownloadsTest {

    private val europe = RemoteFolderPath.ROOT.child("europe")
    private val italy = europe.child("italy")
    private val map = RemoteMapPath(europe, "andorra.map")

    // --- RemoteMapPath ---

    @Test
    fun remoteMapPath_inTheMapsFolder_isItsName() {
        assertEquals("world.map", RemoteMapPath(RemoteFolderPath.ROOT, "world.map").toString())
    }

    @Test
    fun remoteMapPath_inASubFolder_isItsFoldersAndName() {
        assertEquals("europe/italy/centro.map", RemoteMapPath(italy, "centro.map").toString())
    }

    @Test
    fun remoteMapPath_parse_readsWhatToStringWrites() {
        for (path in listOf(RemoteMapPath(RemoteFolderPath.ROOT, "world.map"), RemoteMapPath(italy, "centro.map"))) {
            assertEquals(path, RemoteMapPath.parse(path.toString()))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun remoteMapPath_unsafeName_isRejected() {
        RemoteMapPath(europe, "..")
    }

    @Test(expected = IllegalArgumentException::class)
    fun remoteMapPath_parseLeavingTheMapsFolder_isRejected() {
        RemoteMapPath.parse("europe/../../secret.map")
    }

    @Test(expected = IllegalArgumentException::class)
    fun remoteMapPath_parseEmpty_isRejected() {
        RemoteMapPath.parse("")
    }

    // --- MapDownload.Running.fraction ---

    private fun running(downloaded: Long, total: Long?) =
        MapDownload.Running(RemoteMapPath(europe, "andorra.map"), FileSize(downloaded), total?.let(::FileSize))

    @Test
    fun fraction_knownTotal_isTheShareDownloaded() {
        assertEquals(0.25f, running(25, 100).fraction)
    }

    @Test
    fun fraction_unknownTotal_isNull() {
        assertNull(running(25, null).fraction)
    }

    @Test
    fun fraction_emptyTotal_isNull() {
        assertNull(running(0, 0).fraction)
    }

    @Test
    fun fraction_moreThanTheTotal_isAtMostOne() {
        assertEquals(1f, running(120, 100).fraction)
    }

    // --- isActive ---

    @Test
    fun isActive_queuedOrRunning_isTrue() {
        assertTrue(MapDownload.Queued(map).isActive)
        assertTrue(MapDownload.Running(map, FileSize(1), null).isActive)
    }

    @Test
    fun isActive_failedOrNone_isFalse() {
        assertFalse(MapDownload.Failed(map, MapDownloadFailure.SERVER_ERROR).isActive)
        assertFalse((null as MapDownload?).isActive)
    }
}
