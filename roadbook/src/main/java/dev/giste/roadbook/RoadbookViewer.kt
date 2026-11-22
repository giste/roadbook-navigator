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

package dev.giste.roadbook

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.giste.roadbook.ui.viewer.RoadbookViewModel
import dev.giste.roadbook.ui.viewer.RoadbookViewerContent

/** MIME types the file picker offers: any file, as `.rn2` files have no registered type. */
private val ROADBOOK_MIME_TYPES = arrayOf("*/*")

/**
 * The roadbook viewer: the current roadbook as a list of waypoints, kept at the saved position,
 * with everything needed to import one.
 *
 * It gets its state from Hilt, so the app only places it: as a whole screen, or as part of one
 * (e.g. next to an odometer). The
 * list follows the position that `RoadbookController.moveToNextWaypoint()` and
 * `moveToPreviousWaypoint()` change, and long-pressing a waypoint's distances emits
 * [RoadbookEvent.SetPartialRequested] on `RoadbookController.events`.
 *
 * Without a roadbook, it shows a message and a button that opens the system file picker. With
 * one, a floating button does the same when [showImportButton] is `true`. A failed import is
 * reported in the viewer.
 *
 * The app must bind a [RoadbookSettingsSource], which sets the roadbook style and the short
 * distance threshold.
 *
 * @param showImportButton whether to show the floating import button over the roadbook. Hide it
 * while riding, so nothing covers the roadbook or can be pressed by accident.
 * @param modifier the modifier applied to the viewer.
 */
@Composable
public fun RoadbookViewer(showImportButton: Boolean, modifier: Modifier = Modifier) {
    val viewModel: RoadbookViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        // The user may close the picker without choosing a file.
        if (uri != null) viewModel.importRoadbook(uri)
    }

    RoadbookViewerContent(
        state = state,
        showImportButton = showImportButton,
        onImportClick = { filePicker.launch(ROADBOOK_MIME_TYPES) },
        onImportFailureShown = viewModel::onImportFailureShown,
        onSetPartial = viewModel::onSetPartial,
        onScrollStopped = viewModel::onScrollStopped,
        modifier = modifier,
    )
}
