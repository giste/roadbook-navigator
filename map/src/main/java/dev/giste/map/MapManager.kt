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
import dev.giste.map.ui.MapManagerUiState
import dev.giste.map.ui.MapManagerViewModel
import dev.giste.map.ui.rememberDownloadWithNotificationPermission

/**
 * The map manager screen, with its state from Hilt: the offline map files installed in the app's
 * `map_files` folder (in its no-backup folder), with their size, and a way to delete them.
 *
 * Files the map cannot read are listed too, with a warning mark, so they can be deleted. Deleting
 * asks first, and the screen tells the user if a file could not be deleted.
 *
 * Each time the screen is opened, it asks the download site ([MapDownloadServer]) whether the
 * installed maps have updates: a map that changed on the site after the file's own date gets an
 * update button, and when two or more do, the top bar has a button that updates them all. Updates
 * are downloads like those of [MapDownloader], with the same queue, notification, Wi-Fi rule
 * (off Wi-Fi it first asks whether to use mobile data), notification permission request and stop
 * button; the installed map stays in use until its update is complete and readable, then replaces
 * it. If the site cannot be reached, no update is shown. [MapDownloader] marks the same updates. The list is looked for
 * each time the screen is shown, and again after each deletion and each finished download from
 * [MapDownloader]; a [MapViewer] follows the same changes.
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
    val update = rememberDownloadWithNotificationPermission { key ->
        if (key == UPDATE_ALL_KEY) {
            viewModel.updateAll()
        } else {
            val file = (viewModel.uiState.value as? MapManagerUiState.Listed)?.files
                ?.find { it.file.path == key }?.file
            if (file != null) viewModel.update(file)
        }
    }
    MapManagerContent(
        state = state,
        onBack = onBack,
        onDownloadMaps = onDownloadMaps,
        onDelete = viewModel::delete,
        onDeleteErrorShown = viewModel::dismissDeleteError,
        onUpdate = { file -> update(file.path) },
        onUpdateAll = { update(UPDATE_ALL_KEY) },
        onCancelUpdate = viewModel::cancel,
        onMobileData = viewModel::answerMobileData,
        onDismissMobileData = viewModel::dismissMobileData,
        modifier = modifier,
    )
}

/** The download key that stands for updating every outdated map; file keys are absolute paths. */
private const val UPDATE_ALL_KEY = ""
