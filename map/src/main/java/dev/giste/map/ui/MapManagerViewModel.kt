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
import dev.giste.map.domain.CheckMapUpdatesUseCase
import dev.giste.map.domain.DeleteMapResult
import dev.giste.map.domain.InstalledMapFile
import dev.giste.map.domain.InstalledMapFiles
import dev.giste.map.domain.MapDownloadRepository
import dev.giste.map.domain.MapFileRepository
import dev.giste.map.domain.MapUpdateRepository
import dev.giste.map.domain.NetworkConnection
import dev.giste.map.domain.RemoteMapPath
import dev.giste.map.domain.isOutdated
import dev.giste.map.domain.remotePath
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The state of the map manager: the installed map files, their updates, and deleting them.
 *
 * It follows the installed files, which the repository looks for again after each deletion or
 * download, so the list always shows what is on the device. When it is created, that is, each time
 * the screen is opened, it asks the download site whether the installed maps have updates
 * ([checkUpdates]); a file whose site date is newer than its own is outdated, until its update is
 * installed and the file takes the site's date.
 *
 * Updates are downloads of the installed maps, queued like any other. They wait for Wi-Fi: off
 * Wi-Fi, the state first asks whether to use mobile data
 * ([MapManagerUiState.Listed.mobileDataQuestion]); the question is kept in [savedStateHandle], so
 * it survives the process being stopped.
 *
 * @param savedStateHandle keeps the mobile data question.
 * @param repository the installed map files.
 * @param downloadRepository downloads the updates.
 * @param updateRepository when the maps last changed on the site, to mark the outdated ones.
 * @param checkUpdates asks the site about the installed maps.
 * @param network tells whether the device is on Wi-Fi.
 */
@HiltViewModel
internal class MapManagerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: MapFileRepository,
    private val downloadRepository: MapDownloadRepository,
    updateRepository: MapUpdateRepository,
    checkUpdates: CheckMapUpdatesUseCase,
    private val network: NetworkConnection,
) : ViewModel() {

    /** The file that could not be deleted, until the screen has said so. */
    private val deleteFailed = MutableStateFlow<InstalledMapFile?>(null)

    /**
     * The updates waiting for the mobile data answer, as [RemoteMapPath.toString] writes them,
     * one per line, or "". Safe names hold no line breaks.
     */
    private val mobileDataQuestion: StateFlow<String> = savedStateHandle.getStateFlow(KEY_MOBILE_DATA_QUESTION, "")

    /**
     * The state of the manager. It starts as [MapManagerUiState.Loading], and is shared while the
     * screen is shown and for [STOP_TIMEOUT_MILLIS] after, so a configuration change does not look
     * for the files again. After a change, the previous list stays until the new one is found.
     */
    val uiState: StateFlow<MapManagerUiState> = combine(
        repository.installedFiles,
        downloadRepository.downloads,
        updateRepository.siteVersions,
        deleteFailed,
        mobileDataQuestion,
    ) { files, downloads, siteVersions, failed, question ->
        when (files) {
            InstalledMapFiles.FolderUnreadable -> MapManagerUiState.FolderUnreadable
            is InstalledMapFiles.Listed -> {
                val byMap = downloads.associateBy { it.map }
                MapManagerUiState.Listed(
                    files = files.files.map { file ->
                        ManagedMapFile(
                            file = file,
                            outdated = file.isOutdated(siteVersions),
                            download = file.remotePath()?.let(byMap::get),
                        )
                    },
                    deleteFailed = failed,
                    mobileDataQuestion = question.toMaps(),
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = MapManagerUiState.Loading,
    )

    init {
        viewModelScope.launch { checkUpdates() }
    }

    /**
     * Deletes [file]; the list follows once the repository has looked for the files again. If it
     * cannot be deleted, the state says so until [dismissDeleteError].
     *
     * @param file the file to delete, from the listed state.
     */
    fun delete(file: InstalledMapFile) {
        viewModelScope.launch {
            when (repository.delete(file)) {
                DeleteMapResult.Deleted -> Unit
                DeleteMapResult.Failed -> deleteFailed.value = file
            }
        }
    }

    /** Clears the deletion failure, once the screen has said so. */
    fun dismissDeleteError() {
        deleteFailed.value = null
    }

    /**
     * Downloads the update of [file], after the maps already queued; also starts again an update
     * that failed. On Wi-Fi it is queued at once; otherwise the state asks first whether to use
     * mobile data. The installed file stays in use until the update is complete and readable.
     *
     * @param file an installed file, from the listed state.
     */
    fun update(file: InstalledMapFile) {
        start(listOfNotNull(file.remotePath()))
    }

    /**
     * Downloads the update of every file that can be updated now
     * ([MapManagerUiState.Listed.updatable]), in list order, like [update]. Off Wi-Fi, it asks once
     * for all of them. Nothing happens if there is none.
     */
    fun updateAll() {
        val listed = uiState.value as? MapManagerUiState.Listed ?: return
        start(listed.updatable.mapNotNull { it.file.remotePath() })
    }

    /**
     * Answers the mobile data question: queues its updates, with mobile data for the whole queue
     * if [useMobileData], or waiting for Wi-Fi otherwise. Nothing happens without a question.
     *
     * @param useMobileData whether to download now with mobile data.
     */
    fun answerMobileData(useMobileData: Boolean) {
        val maps = mobileDataQuestion.value.toMaps()
        if (maps.isEmpty()) return
        savedStateHandle[KEY_MOBILE_DATA_QUESTION] = ""
        queue(maps, allowMobileData = useMobileData)
    }

    /** Drops the mobile data question without downloading. */
    fun dismissMobileData() {
        savedStateHandle[KEY_MOBILE_DATA_QUESTION] = ""
    }

    /**
     * Stops the update of [file], whether it is waiting or running, and throws away what was
     * downloaded of it. The installed file is kept.
     *
     * @param file an installed file, from the listed state.
     */
    fun cancel(file: InstalledMapFile) {
        val map = file.remotePath() ?: return
        viewModelScope.launch { downloadRepository.cancel(map) }
    }

    /** Queues [maps] on Wi-Fi, or asks first whether to use mobile data. */
    private fun start(maps: List<RemoteMapPath>) {
        if (maps.isEmpty()) return
        if (network.isUnmetered()) {
            queue(maps, allowMobileData = false)
        } else {
            savedStateHandle[KEY_MOBILE_DATA_QUESTION] = maps.joinToString(QUESTION_SEPARATOR)
        }
    }

    /** Queues [maps] in their order. */
    private fun queue(maps: List<RemoteMapPath>, allowMobileData: Boolean) {
        viewModelScope.launch { maps.forEach { downloadRepository.start(it, allowMobileData) } }
    }

    /** The maps a saved mobile data question holds; empty for no question. */
    private fun String.toMaps(): List<RemoteMapPath> =
        if (isEmpty()) emptyList() else split(QUESTION_SEPARATOR).map(RemoteMapPath::parse)

    private companion object {
        /** The key of the updates waiting for the mobile data answer in the saved state. */
        const val KEY_MOBILE_DATA_QUESTION: String = "mobile_data_question"

        /** Between the maps of a saved mobile data question. */
        const val QUESTION_SEPARATOR: String = "\n"

        /** Keeps the state across short gaps, such as a configuration change. */
        const val STOP_TIMEOUT_MILLIS: Long = 5_000
    }
}
