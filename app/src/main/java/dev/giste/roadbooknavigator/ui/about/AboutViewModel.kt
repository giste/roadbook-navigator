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

package dev.giste.roadbooknavigator.ui.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.giste.roadbooknavigator.domain.OpenSourceLibrariesResult
import dev.giste.roadbooknavigator.domain.OpenSourceLibrary
import dev.giste.roadbooknavigator.domain.OpenSourceLibraryRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/** What the About screen shows of the open-source libraries. */
internal sealed interface AboutUiState {

    /** The library list is being read. */
    data object Loading : AboutUiState

    /**
     * The library list was read.
     *
     * @property libraries every library, sorted by name.
     */
    data class Loaded(val libraries: List<OpenSourceLibrary>) : AboutUiState

    /** The library list could not be read. The rest of the screen is still shown. */
    data object LibrariesUnavailable : AboutUiState
}

/**
 * Holds the About screen's state: the open-source libraries the app ships. The rest of the screen
 * (the app, its version and the map credits) is fixed, so it is not part of the state.
 *
 * @param libraries reads the library list once the screen is shown.
 */
@HiltViewModel
internal class AboutViewModel @Inject constructor(
    libraries: OpenSourceLibraryRepository,
) : ViewModel() {

    /**
     * The screen's state: [AboutUiState.Loading] until the list is read, then the list or
     * [AboutUiState.LibrariesUnavailable]. The list never changes while the app runs, so it is
     * read once per subscription.
     */
    val uiState: StateFlow<AboutUiState> = flow {
        emit(
            when (val result = libraries.read()) {
                is OpenSourceLibrariesResult.Read -> AboutUiState.Loaded(result.libraries)
                OpenSourceLibrariesResult.Unavailable -> AboutUiState.LibrariesUnavailable
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AboutUiState.Loading)
}
