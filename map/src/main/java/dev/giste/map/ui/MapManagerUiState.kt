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
import dev.giste.map.domain.MapDownload
import dev.giste.map.domain.isActive
import dev.giste.map.domain.RemoteMapPath

/** Everything the map manager shows. It holds typed data only: the screen turns it into text. */
internal sealed interface MapManagerUiState {

    /** The installed maps are still being looked for. */
    data object Loading : MapManagerUiState

    /** The map folder cannot be read, so the installed maps are unknown. */
    data object FolderUnreadable : MapManagerUiState

    /**
     * The installed maps.
     *
     * @property files the installed map files, readable or not, in path order, with their updates.
     * Empty when no map is installed.
     * @property deleteFailed the file that could not be deleted, until the screen has said so, or
     * `null`.
     * @property mobileDataQuestion the updates to download whose rider must first say whether to
     * use mobile data, as the device is not on Wi-Fi; empty when there is no such question.
     */
    data class Listed(
        val files: List<ManagedMapFile>,
        val deleteFailed: InstalledMapFile? = null,
        val mobileDataQuestion: List<RemoteMapPath> = emptyList(),
    ) : MapManagerUiState {

        /** The files whose update can be started now: outdated, and not downloading already. */
        val updatable: List<ManagedMapFile>
            get() = files.filter { it.updatable }
    }
}

/**
 * An installed map file, with what the map manager knows about its update.
 *
 * @property file the file.
 * @property outdated whether the download site has a newer version of it, as the last check found.
 * @property download the download of its update, if it is waiting, running or failed; `null`
 * otherwise.
 */
internal data class ManagedMapFile(
    val file: InstalledMapFile,
    val outdated: Boolean = false,
    val download: MapDownload? = null,
) {

    /** Whether its update can be started now: it is outdated, and not downloading already. */
    val updatable: Boolean
        get() = outdated && !download.isActive
}
