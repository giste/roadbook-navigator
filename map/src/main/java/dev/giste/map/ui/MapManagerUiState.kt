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

package dev.giste.map.ui

import dev.giste.map.domain.InstalledMapFile

/** Everything the map manager shows. It holds typed data only: the screen turns it into text. */
internal sealed interface MapManagerUiState {

    /** The installed maps are still being looked for. */
    data object Loading : MapManagerUiState

    /** The map folder cannot be read, so the installed maps are unknown. */
    data object FolderUnreadable : MapManagerUiState

    /**
     * The installed maps.
     *
     * @property files the installed map files, readable or not, in path order. Empty when no map
     * is installed.
     * @property deleteFailed the file that could not be deleted, until the screen has said so, or
     * `null`.
     */
    data class Listed(
        val files: List<InstalledMapFile>,
        val deleteFailed: InstalledMapFile? = null,
    ) : MapManagerUiState
}
