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

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Tells when the map files have changed in the map folder, so whoever lists them looks again.
 *
 * Everything in `:map` that adds or deletes map files calls [notifyChanged]. Files copied by other
 * means are only seen the next time the files are listed from scratch.
 */
@Singleton
internal class MapFolderChanges @Inject constructor() {

    private val count = MutableStateFlow(0L)

    /**
     * How many changes there have been since the app started. A state flow: it emits the current
     * count at once, then once after each change (changes close together may be merged).
     */
    val changes: StateFlow<Long> = count.asStateFlow()

    /** Says that map files were added or deleted. Thread-safe. */
    fun notifyChanged() {
        count.update { it + 1 }
    }
}
