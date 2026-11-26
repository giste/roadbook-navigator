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
import dev.giste.map.MapSettings
import dev.giste.map.MapTilt
import dev.giste.map.MapZoomLevel
import dev.giste.roadbook.RoadbookSettings
import dev.giste.roadbook.RoadbookStyle
import dev.giste.roadbook.ShortDistanceThreshold
import dev.giste.roadbooknavigator.settings.AppSettings
import dev.giste.roadbooknavigator.settings.RemoteAction
import dev.giste.roadbooknavigator.settings.RemoteKey
import dev.giste.roadbooknavigator.settings.RemoteModel
import dev.giste.roadbooknavigator.settings.RemoteSettings
import dev.giste.roadbooknavigator.settings.ScreenOrientation
import dev.giste.roadbooknavigator.settings.ThemeMode
import dev.giste.roadbooknavigator.settings.domain.MAP_TILT_RANGE
import dev.giste.roadbooknavigator.settings.domain.MAP_ZOOM_RANGE
import dev.giste.roadbooknavigator.settings.domain.SHORT_DISTANCE_MAX
import dev.giste.roadbooknavigator.settings.domain.SHORT_DISTANCE_MIN
import dev.giste.roadbooknavigator.settings.domain.SettingsRepository
import dev.giste.roadbooknavigator.settings.domain.SettingsUpdateResult
import dev.giste.roadbooknavigator.settings.domain.stepped
import dev.giste.roadbooknavigator.settings.domain.withOnlyKey
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
 * [SettingsUiState.Loaded.saveFailed] until [dismissSaveError]. A custom key taken from another
 * action sets [SettingsUiState.Loaded.keyMoved] until [dismissKeyMoved].
 *
 * @param appSettings the app's own settings.
 * @param remoteSettings the remote control settings.
 * @param roadbookSettings the `:roadbook` settings.
 * @param mapSettings the `:map` settings.
 */
@HiltViewModel
internal class SettingsViewModel @Inject constructor(
    private val appSettings: SettingsRepository<AppSettings>,
    private val remoteSettings: SettingsRepository<RemoteSettings>,
    private val roadbookSettings: SettingsRepository<RoadbookSettings>,
    private val mapSettings: SettingsRepository<MapSettings>,
) : ViewModel() {

    /** Whether the last change could not be stored. */
    private val saveFailed = MutableStateFlow(false)

    /** The key the last custom assignment took from another action, until it is dismissed. */
    private val keyMoved = MutableStateFlow<KeyMove?>(null)

    /** The screen state. Starts at [SettingsUiState.Loading] and never fails. */
    val uiState: StateFlow<SettingsUiState> = combine(
        combine(appSettings.settings, remoteSettings.settings, roadbookSettings.settings, mapSettings.settings) {
                app, remote, roadbook, map ->
            StoredSettings(app, remote, roadbook, map)
        },
        saveFailed,
        keyMoved,
    ) { (app, remote, roadbook, map), failed, moved ->
        SettingsUiState.Loaded(
            themeMode = app.themeMode,
            orientation = app.orientation,
            remoteModel = remote.model,
            remoteKeys = remote.keys,
            roadbookStyle = roadbook.style,
            shortDistanceThreshold = roadbook.shortDistanceThreshold,
            canDecreaseShortDistanceThreshold =
                roadbook.shortDistanceThreshold > SHORT_DISTANCE_MIN,
            canIncreaseShortDistanceThreshold =
                roadbook.shortDistanceThreshold < SHORT_DISTANCE_MAX,
            canRestoreShortDistanceThreshold =
                roadbook.shortDistanceThreshold != ShortDistanceThreshold.DEFAULT,
            mapZoomLevel = map.zoomLevel,
            canDecreaseMapZoom = map.zoomLevel.value > MAP_ZOOM_RANGE.first,
            canIncreaseMapZoom = map.zoomLevel.value < MAP_ZOOM_RANGE.last,
            canRestoreMapZoom = map.zoomLevel != MapZoomLevel.DEFAULT,
            mapTilt = map.tilt,
            canDecreaseMapTilt = map.tilt.inDegrees > MAP_TILT_RANGE.start,
            canIncreaseMapTilt = map.tilt.inDegrees < MAP_TILT_RANGE.endInclusive,
            canRestoreMapTilt = map.tilt != MapTilt.DEFAULT,
            saveFailed = failed,
            keyMoved = moved,
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
     * Stores the app's screen [orientation].
     *
     * @param orientation the new screen orientation.
     */
    fun setOrientation(orientation: ScreenOrientation) {
        save { appSettings.update { it.copy(orientation = orientation) } }
    }

    /**
     * Stores the remote control [model] in use. The custom keys are kept, so they come back when
     * the rider selects [RemoteModel.CUSTOM] again.
     *
     * @param model the new remote model.
     */
    fun setRemoteModel(model: RemoteModel) {
        save { remoteSettings.update { it.copy(model = model) } }
    }

    /**
     * Makes [key] the only custom key of [action], and stores it. If another custom action had
     * [key], it loses it, and [SettingsUiState.Loaded.keyMoved] tells which. It does nothing unless
     * [RemoteModel.CUSTOM] is in use, since only the custom keys can change.
     *
     * @param action the action to assign [key] to.
     * @param key the key the rider pressed.
     */
    fun assignCustomKey(action: RemoteAction, key: RemoteKey) {
        viewModelScope.launch {
            // Decided on the stored settings, inside the update, so fast assignments never mix.
            var moved: KeyMove? = null
            val result = remoteSettings.update { settings ->
                if (settings.model != RemoteModel.CUSTOM) return@update settings
                val from = settings.customKeys.actionFor(key)
                moved = if (from != null && from != action) KeyMove(key, from, action) else null
                settings.copy(customKeys = settings.customKeys.withOnlyKey(action, key))
            }
            when (result) {
                SettingsUpdateResult.NOT_SAVED -> saveFailed.value = true
                SettingsUpdateResult.SAVED -> moved?.let { keyMoved.value = it }
            }
        }
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
        changeShortDistanceThreshold(steps = -1)
    }

    /** Makes the short-distance threshold one step longer, up to its maximum. */
    fun increaseShortDistanceThreshold() {
        changeShortDistanceThreshold(steps = 1)
    }

    /** Sets the short-distance threshold back to its default. */
    fun restoreShortDistanceThreshold() {
        save {
            roadbookSettings.update {
                it.copy(shortDistanceThreshold = ShortDistanceThreshold.DEFAULT)
            }
        }
    }

    /** Shows the map one zoom level farther, down to the farthest the screen offers. */
    fun decreaseMapZoom() {
        save { mapSettings.update { it.copy(zoomLevel = it.zoomLevel.stepped(-1)) } }
    }

    /** Shows the map one zoom level closer, up to the closest the screen offers. */
    fun increaseMapZoom() {
        save { mapSettings.update { it.copy(zoomLevel = it.zoomLevel.stepped(1)) } }
    }

    /** Makes the map look one step more straight down, down to flat. */
    fun decreaseMapTilt() {
        save { mapSettings.update { it.copy(tilt = it.tilt.stepped(-1)) } }
    }

    /** Makes the map lean one step more towards the horizon, up to the most the screen offers. */
    fun increaseMapTilt() {
        save { mapSettings.update { it.copy(tilt = it.tilt.stepped(1)) } }
    }

    /** Sets the map zoom back to its default. */
    fun restoreMapZoom() {
        save { mapSettings.update { it.copy(zoomLevel = MapZoomLevel.DEFAULT) } }
    }

    /** Sets the map tilt back to its default. */
    fun restoreMapTilt() {
        save { mapSettings.update { it.copy(tilt = MapTilt.DEFAULT) } }
    }

    /** Hides the save error, once the screen has shown it. */
    fun dismissSaveError() {
        saveFailed.value = false
    }

    /** Hides the notice of a moved key, once the screen has shown it. */
    fun dismissKeyMoved() {
        keyMoved.value = null
    }

    /** Moves the threshold by [steps] from the stored one, so fast presses are never lost. */
    private fun changeShortDistanceThreshold(steps: Int) {
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

/** The stored settings the screen shows, read together. */
private data class StoredSettings(
    val app: AppSettings,
    val remote: RemoteSettings,
    val roadbook: RoadbookSettings,
    val map: MapSettings,
)
