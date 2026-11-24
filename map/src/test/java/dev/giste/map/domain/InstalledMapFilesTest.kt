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
import org.junit.Test

class InstalledMapFilesTest {

    @Test
    fun fileSize_zero_isAccepted() {
        assertEquals(0L, FileSize(0).bytes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun fileSize_negative_isRejected() {
        FileSize(-1)
    }

    @Test
    fun toInstalledMaps_folderUnreadable_isUnreadable() {
        assertEquals(InstalledMaps.Unreadable, InstalledMapFiles.FolderUnreadable.toInstalledMaps())
    }

    @Test
    fun toInstalledMaps_noFiles_isNone() {
        assertEquals(InstalledMaps.None, InstalledMapFiles.Listed(emptyList()).toInstalledMaps())
    }

    @Test
    fun toInstalledMaps_onlyUnreadableFiles_isUnreadable() {
        val files = InstalledMapFiles.Listed(
            listOf(testInstalledFile("a.map", readable = false), testInstalledFile("b.map", readable = false)),
        )

        assertEquals(InstalledMaps.Unreadable, files.toInstalledMaps())
    }

    @Test
    fun toInstalledMaps_someReadableFiles_areAvailableInOrderWithoutTheUnreadable() {
        val files = InstalledMapFiles.Listed(
            listOf(
                testInstalledFile("a.map"),
                testInstalledFile("b.map", readable = false),
                testInstalledFile("c.map"),
            ),
        )

        assertEquals(
            InstalledMaps.Available(listOf(testMapFile("a.map"), testMapFile("c.map"))),
            files.toInstalledMaps(),
        )
    }
}
