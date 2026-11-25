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
import android.os.storage.StorageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject

/** Tells how much space is free for new files. */
internal fun interface FreeSpace {

    /**
     * The space free for this app in the file system that holds [folder].
     *
     * @param folder an existing folder.
     * @return the free space in bytes, or 0 if it cannot be known.
     */
    fun bytesFreeIn(folder: File): Long
}

/**
 * [FreeSpace] from Android's storage manager, which also counts the cached data of every app that
 * it would clear to make room, so a big map is not refused while the system could fit it.
 *
 * @param context the application context, to get the storage manager.
 */
internal class StorageManagerFreeSpace @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : FreeSpace {

    override fun bytesFreeIn(folder: File): Long {
        val storage = context.getSystemService(StorageManager::class.java) ?: return folder.usableSpace
        return try {
            storage.getAllocatableBytes(storage.getUuidForPath(folder))
        } catch (_: IOException) {
            // The folder's storage cannot be found: fall back to what the file system says. It
            // leaves out the space reserved for the system, unlike File.freeSpace.
            folder.usableSpace
        }
    }
}
