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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.giste.map.domain.DeleteMapResult
import dev.giste.map.domain.InstalledMapFile
import dev.giste.map.domain.InstalledMapFiles
import dev.giste.map.domain.MapFileRepository
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The state of the map manager: the installed map files, and deleting them.
 *
 * It looks for the files once per subscription, and again after each deletion, so the list always
 * shows what is on the device.
 *
 * @param repository the installed map files.
 */
@HiltViewModel
internal class MapManagerViewModel @Inject constructor(
    private val repository: MapFileRepository,
) : ViewModel() {

    /** Grows by one each time the files must be looked for again. */
    private val reloads = MutableStateFlow(0)

    /** The file that could not be deleted, until the screen has said so. */
    private val deleteFailed = MutableStateFlow<InstalledMapFile?>(null)

    /**
     * The state of the manager. It starts as [MapManagerUiState.Loading], and is shared while the
     * screen is shown and for [STOP_TIMEOUT_MILLIS] after, so a configuration change does not look
     * for the files again. After a deletion, the previous list stays until the new one is found.
     */
    // flatMapLatest is still experimental in kotlinx.coroutines.
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MapManagerUiState> = reloads
        .flatMapLatest { repository.installedFiles }
        .combine(deleteFailed) { files, failed ->
            when (files) {
                InstalledMapFiles.FolderUnreadable -> MapManagerUiState.FolderUnreadable
                is InstalledMapFiles.Listed -> MapManagerUiState.Listed(files.files, failed)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MapManagerUiState.Loading,
        )

    /**
     * Deletes [file], then looks for the files again. If it cannot be deleted, the state says so
     * until [dismissDeleteError].
     *
     * @param file the file to delete, from the listed state.
     */
    fun delete(file: InstalledMapFile) {
        viewModelScope.launch {
            when (repository.delete(file)) {
                DeleteMapResult.Deleted -> Unit
                DeleteMapResult.Failed -> deleteFailed.value = file
            }
            // Also after a failure: the folder may have changed in other ways.
            reloads.update { it + 1 }
        }
    }

    /** Clears the deletion failure, once the screen has said so. */
    fun dismissDeleteError() {
        deleteFailed.value = null
    }

    private companion object {
        /** Keeps the state across short gaps, such as a configuration change. */
        const val STOP_TIMEOUT_MILLIS: Long = 5_000
    }
}
