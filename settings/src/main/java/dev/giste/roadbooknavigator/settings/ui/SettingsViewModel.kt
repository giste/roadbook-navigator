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

package dev.giste.roadbooknavigator.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.giste.roadbook.RoadbookSettings
import dev.giste.roadbook.RoadbookStyle
import dev.giste.roadbook.ShortDistanceThreshold
import dev.giste.roadbooknavigator.settings.AppSettings
import dev.giste.roadbooknavigator.settings.ThemeMode
import dev.giste.roadbooknavigator.settings.domain.SettingsRepository
import dev.giste.roadbooknavigator.settings.domain.SettingsUpdateResult
import dev.giste.roadbooknavigator.settings.domain.stepped
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** How long the state stays collected after the last collector leaves, e.g. during a rotation. */
private const val STOP_TIMEOUT_MILLIS: Long = 5_000

/**
 * State and actions of the settings screen.
 *
 * Every change is stored at once. Stored changes come back through [uiState], so the screen
 * always shows what is stored. A change that cannot be stored sets
 * [SettingsUiState.Loaded.saveFailed] until [dismissSaveError].
 *
 * @param appSettings the app's own settings.
 * @param roadbookSettings the `:roadbook` settings.
 */
@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val appSettings: SettingsRepository<AppSettings>,
    private val roadbookSettings: SettingsRepository<RoadbookSettings>,
) : ViewModel() {

    /** Whether the last change could not be stored. */
    private val saveFailed = MutableStateFlow(false)

    /** The screen state. Starts at [SettingsUiState.Loading] and never fails. */
    val uiState: StateFlow<SettingsUiState> = combine(
        appSettings.settings,
        roadbookSettings.settings,
        saveFailed,
    ) { app, roadbook, failed ->
        SettingsUiState.Loaded(
            themeMode = app.themeMode,
            roadbookStyle = roadbook.style,
            shortDistanceThreshold = roadbook.shortDistanceThreshold,
            canDecreaseThreshold = roadbook.shortDistanceThreshold > ShortDistanceThreshold.MIN,
            canIncreaseThreshold = roadbook.shortDistanceThreshold < ShortDistanceThreshold.MAX,
            saveFailed = failed,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SettingsUiState.Loading,
    )

    /**
     * Stores the app's [themeMode].
     *
     * @param themeMode the new theme mode.
     */
    fun setThemeMode(themeMode: ThemeMode) {
        save { appSettings.update { it.copy(themeMode = themeMode) } }
    }

    /**
     * Stores the roadbook's [style].
     *
     * @param style the new roadbook style.
     */
    fun setRoadbookStyle(style: RoadbookStyle) {
        save { roadbookSettings.update { it.copy(style = style) } }
    }

    /** Makes the short-distance threshold one step shorter, down to its minimum. */
    fun decreaseShortDistanceThreshold() {
        changeThreshold(steps = -1)
    }

    /** Makes the short-distance threshold one step longer, up to its maximum. */
    fun increaseShortDistanceThreshold() {
        changeThreshold(steps = 1)
    }

    /** Hides the save error, once the screen has shown it. */
    fun dismissSaveError() {
        saveFailed.value = false
    }

    /** Moves the threshold by [steps] from the stored one, so fast presses are never lost. */
    private fun changeThreshold(steps: Int) {
        save {
            roadbookSettings.update {
                it.copy(shortDistanceThreshold = it.shortDistanceThreshold.stepped(steps))
            }
        }
    }

    /** Runs [change], and shows the save error if it was not stored. */
    private fun save(change: suspend () -> SettingsUpdateResult) {
        viewModelScope.launch {
            if (change() == SettingsUpdateResult.NOT_SAVED) saveFailed.value = true
        }
    }
}
