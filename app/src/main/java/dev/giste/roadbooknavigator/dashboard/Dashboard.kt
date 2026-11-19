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

package dev.giste.roadbooknavigator.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.giste.roadbook.RoadbookViewer

/**
 * The main screen: the odometer above the roadbook, with its state from Hilt.
 *
 * Showing it makes the odometer measure (see [DashboardViewModel]), so it must only be shown once
 * the location permission is granted.
 *
 * @param modifier applied to the root layout.
 */
@Composable
internal fun Dashboard(modifier: Modifier = Modifier) {
    val viewModel: DashboardViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(
        state = state,
        modifier = modifier,
        roadbook = { showImportButton, roadbookModifier ->
            RoadbookViewer(showImportButton = showImportButton, modifier = roadbookModifier)
        },
    )
}
