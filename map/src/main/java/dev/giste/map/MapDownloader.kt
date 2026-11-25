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
import dev.giste.map.ui.MapDownloaderContent
import dev.giste.map.ui.MapDownloaderViewModel

/**
 * The map downloader screen, with its state from Hilt: the maps on the download site
 * ([MapDownloadServer]), one folder at a time, with the maps already installed marked.
 *
 * Pressing a folder opens it. In a sub-folder, the screen's back button and the system back
 * gesture go up one folder. If a folder cannot be read (e.g. there is no connection), the screen
 * says why and offers to try again. The folder being shown survives configuration changes and the
 * process being stopped.
 *
 * Open it from [MapManager]'s add button, e.g. as a navigation destination of its own. It
 * needs the `INTERNET` permission, which `:map` declares.
 *
 * @param onBack called when the user leaves the screen with its back button in the download site's
 * maps folder. The system back gesture there is the caller's, e.g. its navigation.
 * @param modifier applied to the root layout.
 */
@Composable
public fun MapDownloader(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: MapDownloaderViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    MapDownloaderContent(
        state = state,
        onBack = onBack,
        onOpen = viewModel::open,
        onUp = viewModel::goUp,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}
