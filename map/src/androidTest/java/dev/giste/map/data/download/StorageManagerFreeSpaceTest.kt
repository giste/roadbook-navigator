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

package dev.giste.map.data.download

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

/**
 * [StorageManagerFreeSpace] on a device: Robolectric's storage manager has no storage volumes, so
 * it cannot find the storage of a folder.
 */
@RunWith(AndroidJUnit4::class)
class StorageManagerFreeSpaceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    // In the app's own storage, as the map folder is.
    @get:Rule
    val temporaryFolder = TemporaryFolder(context.noBackupFilesDir)

    @Test
    fun bytesFreeIn_existingFolder_isSomeSpace() {
        val free = StorageManagerFreeSpace(context).bytesFreeIn(temporaryFolder.root)

        assertTrue("$free bytes", free > 0)
    }

    @Test
    fun bytesFreeIn_folderNotCreatedYet_isTheSpaceOfItsStorage() {
        // The storage manager finds the storage from the path, so it needs no folder there yet.
        val free = StorageManagerFreeSpace(context).bytesFreeIn(File(temporaryFolder.root, "missing"))

        assertTrue("$free bytes", free > 0)
    }
}
