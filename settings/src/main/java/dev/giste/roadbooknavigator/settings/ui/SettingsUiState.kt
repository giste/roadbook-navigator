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

import dev.giste.roadbook.RoadbookStyle
import dev.giste.roadbook.ShortDistanceThreshold
import dev.giste.roadbooknavigator.settings.RemoteAction
import dev.giste.roadbooknavigator.settings.RemoteKey
import dev.giste.roadbooknavigator.settings.RemoteKeys
import dev.giste.roadbooknavigator.settings.RemoteModel
import dev.giste.roadbooknavigator.settings.ScreenOrientation
import dev.giste.roadbooknavigator.settings.ThemeMode

/** What the settings screen shows. */
internal sealed interface SettingsUiState {

    /** The settings have not been read yet. */
    data object Loading : SettingsUiState

    /**
     * The current settings.
     *
     * @property themeMode the app's light or dark colors.
     * @property orientation which way the app is shown on the screen.
     * @property remoteModel the remote control in use.
     * @property remoteKeys the keys in use, those of [remoteModel].
     * @property roadbookStyle the colors the roadbook is drawn with.
     * @property shortDistanceThreshold below which distance a waypoint is highlighted.
     * @property canDecreaseThreshold whether the threshold can get shorter.
     * @property canIncreaseThreshold whether the threshold can get longer.
     * @property saveFailed whether the last change could not be stored, so the screen tells the
     * user until they dismiss it.
     * @property keyMoved the key the last custom assignment took from another action, so the
     * screen tells the user until they dismiss it; `null` if none.
     */
    data class Loaded(
        val themeMode: ThemeMode,
        val orientation: ScreenOrientation,
        val remoteModel: RemoteModel,
        val remoteKeys: RemoteKeys,
        val roadbookStyle: RoadbookStyle,
        val shortDistanceThreshold: ShortDistanceThreshold,
        val canDecreaseThreshold: Boolean,
        val canIncreaseThreshold: Boolean,
        val saveFailed: Boolean,
        val keyMoved: KeyMove? = null,
    ) : SettingsUiState
}

/**
 * A custom key that moved from one action to another, because a key triggers only one action.
 *
 * @property key the key.
 * @property from the action that had it, and now has no key.
 * @property to the action that has it now.
 */
internal data class KeyMove(val key: RemoteKey, val from: RemoteAction, val to: RemoteAction)
