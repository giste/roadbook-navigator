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

package dev.giste.roadbooknavigator.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.giste.roadbooknavigator.settings.ui.SettingsContent
import dev.giste.roadbooknavigator.settings.ui.SettingsViewModel

/**
 * The settings screen, with its state from Hilt: the app theme, the screen orientation, the
 * remote control, the roadbook settings and the map's zoom and tilt, then a link to the app's
 * About screen.
 *
 * Every change is stored at once and reaches the modules through their settings sources, so the
 * app reacts while the screen is still open. The screen tells the user if a change could not be
 * stored.
 *
 * @param onBack called when the user leaves the screen with its back button. The system back
 * gesture is the caller's, e.g. its navigation.
 * @param onOpenAbout called when the user asks for the About screen (the app, its version and
 * its licences), which the caller shows.
 * @param modifier applied to the root layout.
 */
@Composable
public fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(
        state = state,
        onBack = onBack,
        onThemeModeChange = viewModel::setThemeMode,
        onOrientationChange = viewModel::setOrientation,
        onRemoteModelChange = viewModel::setRemoteModel,
        onAssignCustomKey = viewModel::assignCustomKey,
        onRoadbookStyleChange = viewModel::setRoadbookStyle,
        onDecreaseThreshold = viewModel::decreaseShortDistanceThreshold,
        onIncreaseThreshold = viewModel::increaseShortDistanceThreshold,
        onDecreaseMapZoom = viewModel::decreaseMapZoom,
        onIncreaseMapZoom = viewModel::increaseMapZoom,
        onDecreaseMapTilt = viewModel::decreaseMapTilt,
        onIncreaseMapTilt = viewModel::increaseMapTilt,
        onSaveErrorShown = viewModel::dismissSaveError,
        onKeyMoveShown = viewModel::dismissKeyMoved,
        onOpenAbout = onOpenAbout,
        modifier = modifier,
    )
}
