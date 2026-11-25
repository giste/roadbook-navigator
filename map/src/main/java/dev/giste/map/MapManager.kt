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

package dev.giste.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.giste.map.ui.MapManagerContent
import dev.giste.map.ui.MapManagerViewModel

/**
 * The map manager screen, with its state from Hilt: the offline map files installed in the app's
 * `map_files` folder (in its no-backup folder), with their size, and a way to delete them.
 *
 * Files the map cannot read are listed too, with a warning mark, so they can be deleted. Deleting
 * asks first, and the screen tells the user if a file could not be deleted. The list is looked for
 * each time the screen is shown and after each deletion; a [MapViewer] shown afterwards draws what
 * is left.
 *
 * Open it from [MapViewer]'s manage button, e.g. as a navigation destination of its own.
 *
 * @param onBack called when the user leaves the screen with its back button. The system back
 * gesture is the caller's, e.g. its navigation.
 * @param modifier applied to the root layout.
 * @param onDownloadMaps called when the floating add button is pressed, e.g. to show
 * [MapDownloader]. Without it, the screen has no add button.
 */
@Composable
public fun MapManager(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onDownloadMaps: (() -> Unit)? = null,
) {
    val viewModel: MapManagerViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MapManagerContent(
        state = state,
        onBack = onBack,
        onDownloadMaps = onDownloadMaps,
        onDelete = viewModel::delete,
        onDeleteErrorShown = viewModel::dismissDeleteError,
        modifier = modifier,
    )
}
