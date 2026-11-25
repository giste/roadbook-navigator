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
import dev.giste.map.domain.MapDownload
import dev.giste.map.domain.MapDownloadRepository
import dev.giste.map.domain.MapFileRepository
import dev.giste.map.domain.NetworkConnection
import dev.giste.map.domain.RemoteFolderPath
import dev.giste.map.domain.RemoteListing
import dev.giste.map.domain.RemoteMapEntry
import dev.giste.map.domain.RemoteMapPath
import dev.giste.map.domain.RemoteMapRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The state of the map downloader: one folder of the download site at a time, which of its maps
 * are already installed, and their downloads.
 *
 * The folder being shown is kept in [savedStateHandle], so it survives the process being stopped.
 * Each folder is read when it is shown, and again on [retry]. While it is shown, the installed
 * maps and the downloads are followed, so a map's row changes as it downloads and installs.
 *
 * Downloads wait for Wi-Fi. When a map is downloaded off Wi-Fi, the state first asks whether to
 * use mobile data ([MapDownloaderUiState.Listed.mobileDataQuestion]); the question is kept in
 * [savedStateHandle] too.
 *
 * @param savedStateHandle keeps the folder being shown.
 * @param remoteRepository the maps on the download site.
 * @param installedRepository the maps on the device, to mark the installed ones.
 * @param downloadRepository downloads the maps.
 * @param network tells whether the device is on Wi-Fi.
 */
@HiltViewModel
internal class MapDownloaderViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val remoteRepository: RemoteMapRepository,
    private val installedRepository: MapFileRepository,
    private val downloadRepository: MapDownloadRepository,
    private val network: NetworkConnection,
) : ViewModel() {

    /** The folder being shown. */
    private val folder: StateFlow<String> = savedStateHandle.getStateFlow(KEY_FOLDER, "")

    /** The map waiting for the mobile data answer, as [RemoteMapPath.toString] writes it, or "". */
    private val mobileDataQuestion: StateFlow<String> = savedStateHandle.getStateFlow(KEY_MOBILE_DATA_QUESTION, "")

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

    /**
     * Downloads [entry], a map in the folder being shown, after the maps already queued. Also
     * starts again a download that failed. On Wi-Fi it is queued at once; otherwise the state asks
     * first whether to use mobile data.
     *
     * @param entry the map to download.
     */
    fun download(entry: RemoteMapEntry.MapFile) {
        val map = pathOf(entry)
        if (network.isUnmetered()) {
            viewModelScope.launch { downloadRepository.start(map, allowMobileData = false) }
        } else {
            savedStateHandle[KEY_MOBILE_DATA_QUESTION] = map.toString()
        }
    }

    /**
     * Answers the mobile data question: queues its map, with mobile data for the whole queue if
     * [useMobileData], or waiting for Wi-Fi otherwise. Nothing happens without a question.
     *
     * @param useMobileData whether to download now with mobile data.
     */
    fun answerMobileData(useMobileData: Boolean) {
        val map = mobileDataQuestion.value.takeIf { it.isNotEmpty() }?.let(RemoteMapPath::parse) ?: return
        savedStateHandle[KEY_MOBILE_DATA_QUESTION] = ""
        viewModelScope.launch { downloadRepository.start(map, allowMobileData = useMobileData) }
    }

    /** Drops the mobile data question without downloading. */
    fun dismissMobileData() {
        savedStateHandle[KEY_MOBILE_DATA_QUESTION] = ""
    }

    /**
     * Stops the download of [entry], a map in the folder being shown, and throws away what was
     * downloaded of it.
     *
     * @param entry the map whose download to stop.
     */
    fun cancel(entry: RemoteMapEntry.MapFile) {
        val map = pathOf(entry)
        viewModelScope.launch { downloadRepository.cancel(map) }
    }

    /** The path of [entry], a map in the folder being shown. */
    private fun pathOf(entry: RemoteMapEntry.MapFile): RemoteMapPath =
        RemoteMapPath(RemoteFolderPath.parse(folder.value), entry.name)

    /**
     * Reads [path], then follows which of its maps are installed in the same folder on the
     * device, and their downloads.
     */
    private fun read(path: RemoteFolderPath): Flow<MapDownloaderUiState> = flow {
        emit(MapDownloaderUiState.Loading(path))
        when (val listing = remoteRepository.list(path)) {
            is RemoteListing.Failed -> emit(MapDownloaderUiState.Failed(path, listing.reason))
            is RemoteListing.Listed -> {
                val mapNames = listing.entries
                    .filterIsInstance<RemoteMapEntry.MapFile>()
                    .mapTo(mutableSetOf()) { it.name }
                val installedFiles = installedRepository.installedFiles
                val listed = combine(
                    installedFiles,
                    downloadRepository.downloads,
                    mobileDataQuestion,
                ) { files, downloads, question ->
                    MapDownloaderUiState.Listed(
                        folder = path,
                        entries = listing.entries,
                        installed = files.namesIn(path) intersect mapNames,
                        downloads = downloads.inFolder(path).filterKeys { it in mapNames },
                        mobileDataQuestion = question.takeIf { it.isNotEmpty() }?.let(RemoteMapPath::parse),
                    )
                }
                emitAll(listed)
            }
        }
    }

    /**
     * The names of the map files installed in the device folder that matches [path]. If the map
     * folder cannot be read, none are marked: the list itself is still useful.
     */
    private fun InstalledMapFiles.namesIn(path: RemoteFolderPath): Set<String> {
        val folderName = path.toString()
        return when (this) {
            InstalledMapFiles.FolderUnreadable -> emptySet()
            is InstalledMapFiles.Listed -> files.filter { it.folder == folderName }.mapTo(mutableSetOf()) { it.name }
        }
    }

    /** The downloads of the maps in [path], by file name. */
    private fun List<MapDownload>.inFolder(path: RemoteFolderPath): Map<String, MapDownload> =
        filter { it.map.folder == path }.associateBy { it.map.name }

    private companion object {
        /** The key of the folder being shown in the saved state. */
        const val KEY_FOLDER: String = "folder"

        /** The key of the map waiting for the mobile data answer in the saved state. */
        const val KEY_MOBILE_DATA_QUESTION: String = "mobile_data_question"

        /** Keeps the state across short gaps, such as a configuration change. */
        const val STOP_TIMEOUT_MILLIS: Long = 5_000
    }
}
