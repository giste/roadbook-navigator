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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.giste.map.domain.InstalledMapFiles
import dev.giste.map.domain.MapFileRepository
import dev.giste.map.domain.RemoteFolderPath
import dev.giste.map.domain.RemoteListing
import dev.giste.map.domain.RemoteMapEntry
import dev.giste.map.domain.RemoteMapRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * The state of the map downloader: one folder of the download site at a time, and which of its
 * maps are already installed.
 *
 * The folder being shown is kept in [savedStateHandle], so it survives the process being stopped.
 * Each folder is read when it is shown, and again on [retry].
 *
 * @param savedStateHandle keeps the folder being shown.
 * @param remoteRepository the maps on the download site.
 * @param installedRepository the maps on the device, to mark the installed ones.
 */
@HiltViewModel
internal class MapDownloaderViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val remoteRepository: RemoteMapRepository,
    private val installedRepository: MapFileRepository,
) : ViewModel() {

    /** The folder being shown. */
    private val folder: StateFlow<String> = savedStateHandle.getStateFlow(KEY_FOLDER, "")

    /** Grows by one each time the folder must be read again. */
    private val reloads = MutableStateFlow(0)

    /**
     * The state of the downloader. It starts as [MapDownloaderUiState.Loading] for the folder
     * being shown, and is shared while the screen is shown and for [STOP_TIMEOUT_MILLIS] after, so
     * a configuration change does not read the folder again. Each time the folder changes or
     * [retry] is called, it goes back to loading until the folder is read.
     */
    // flatMapLatest is still experimental in kotlinx.coroutines.
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MapDownloaderUiState> = combine(folder, reloads) { path, _ -> path }
        .flatMapLatest { path -> read(RemoteFolderPath.parse(path)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MapDownloaderUiState.Loading(RemoteFolderPath.parse(folder.value)),
        )

    /**
     * Shows [entry], a folder inside the one being shown.
     *
     * @param entry the folder to show.
     */
    fun open(entry: RemoteMapEntry.Folder) {
        savedStateHandle[KEY_FOLDER] = RemoteFolderPath.parse(folder.value).child(entry.name).toString()
    }

    /** Shows the folder that holds the one being shown. Nothing happens in the maps folder itself. */
    fun goUp() {
        val parent = RemoteFolderPath.parse(folder.value).parent() ?: return
        savedStateHandle[KEY_FOLDER] = parent.toString()
    }

    /** Reads the folder being shown again, e.g. after it could not be read. */
    fun retry() {
        reloads.update { it + 1 }
    }

    /** Reads [path], marking its maps already installed in the same folder on the device. */
    private fun read(path: RemoteFolderPath): Flow<MapDownloaderUiState> = flow {
        emit(MapDownloaderUiState.Loading(path))
        val state = when (val listing = remoteRepository.list(path)) {
            is RemoteListing.Failed -> MapDownloaderUiState.Failed(path, listing.reason)
            is RemoteListing.Listed -> {
                val mapNames = listing.entries.filterIsInstance<RemoteMapEntry.MapFile>().map { it.name }
                MapDownloaderUiState.Listed(path, listing.entries, installedNames(path) intersect mapNames.toSet())
            }
        }
        emit(state)
    }

    /**
     * The names of the map files installed in the device folder that matches [path]. If the map
     * folder cannot be read, none are marked: the list itself is still useful.
     */
    private suspend fun installedNames(path: RemoteFolderPath): Set<String> {
        val folderName = path.toString()
        return installedRepository.installedFiles
            .map { files ->
                when (files) {
                    InstalledMapFiles.FolderUnreadable -> emptySet()
                    is InstalledMapFiles.Listed ->
                        files.files.filter { it.folder == folderName }.mapTo(mutableSetOf()) { it.name }
                }
            }
            .first()
    }

    private companion object {
        /** The key of the folder being shown in the saved state. */
        const val KEY_FOLDER: String = "folder"

        /** Keeps the state across short gaps, such as a configuration change. */
        const val STOP_TIMEOUT_MILLIS: Long = 5_000
    }
}
