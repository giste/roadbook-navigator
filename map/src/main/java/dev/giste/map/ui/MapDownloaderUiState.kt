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

import dev.giste.map.domain.RemoteFolderPath
import dev.giste.map.domain.RemoteListingFailure
import dev.giste.map.domain.RemoteMapEntry

/** Everything the map downloader shows. It holds typed data only: the screen turns it into text. */
internal sealed interface MapDownloaderUiState {

    /** The folder of the download site being shown. */
    val folder: RemoteFolderPath

    /**
     * The folder is being read.
     *
     * @property folder the folder.
     */
    data class Loading(override val folder: RemoteFolderPath) : MapDownloaderUiState

    /**
     * The folder was read.
     *
     * @property folder the folder.
     * @property entries its folders first, then its map files. Empty if it holds neither.
     * @property installed the names of the map files in [entries] that are already installed in
     * the same folder on the device.
     */
    data class Listed(
        override val folder: RemoteFolderPath,
        val entries: List<RemoteMapEntry>,
        val installed: Set<String> = emptySet(),
    ) : MapDownloaderUiState

    /**
     * The folder could not be read.
     *
     * @property folder the folder.
     * @property reason why.
     */
    data class Failed(
        override val folder: RemoteFolderPath,
        val reason: RemoteListingFailure,
    ) : MapDownloaderUiState
}
