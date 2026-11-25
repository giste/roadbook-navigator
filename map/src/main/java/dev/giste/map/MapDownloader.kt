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
import dev.giste.map.domain.RemoteMapEntry
import dev.giste.map.ui.downloader.MapDownloaderContent
import dev.giste.map.ui.downloader.MapDownloaderUiState
import dev.giste.map.ui.downloader.MapDownloaderViewModel
import dev.giste.map.ui.rememberDownloadWithNotificationPermission

/**
 * The map downloader screen, with its state from Hilt: the maps on the download site
 * ([MapDownloadServer]), one folder at a time, with the maps already installed marked.
 *
 * Each map that is not installed has a download button, and each installed map that has an update
 * on the site has an update button. Updates are the ones the last [MapManager] check found (it
 * checks each time it is shown); the installed map stays in use until its update is complete. Downloads run in the background, one at
 * a time, with a notification, and go on when the screen is left or the app is closed; a download
 * that stops resumes where it stopped. They wait for Wi-Fi: off Wi-Fi, a dialog asks whether to
 * download now with mobile data (which then applies to every map waiting) or to wait. A map goes
 * to the same folder on the device as on the site. Each row shows whether its map is waiting, how
 * its download goes, or why it failed, with a button to try again. Pressing a waiting or running
 * download asks whether to stop it; its notification can stop it too. Downloaded maps appear in
 * [MapManager] and [MapViewer] at once.
 *
 * While the app may not show notifications, pressing a download button first asks for that
 * permission, then downloads whatever the answer: the notification is optional. Android stops
 * showing the request after the user refuses it twice.
 *
 * Pressing a folder opens it. In a sub-folder, the screen's back button and the system back
 * gesture go up one folder. If a folder cannot be read (e.g. there is no connection), the screen
 * says why and offers to try again. The folder being shown survives configuration changes and the
 * process being stopped.
 *
 * Open it from [MapManager]'s add button, e.g. as a navigation destination of its own. `:map`
 * declares the permissions it needs. The app must set up WorkManager with Hilt's worker factory
 * (see the `:map` README).
 *
 * @param onBack called when the user leaves the screen with its back button in the download site's
 * maps folder. The system back gesture there is the caller's, e.g. its navigation.
 * @param modifier applied to the root layout.
 */
@Composable
public fun MapDownloader(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: MapDownloaderViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val download = rememberDownloadWithNotificationPermission { name ->
        val file = (viewModel.uiState.value as? MapDownloaderUiState.Listed)?.entries
            ?.filterIsInstance<RemoteMapEntry.MapFile>()?.find { it.name == name }
        if (file != null) viewModel.download(file)
    }
    MapDownloaderContent(
        state = state,
        onBack = onBack,
        onOpen = viewModel::open,
        onUp = viewModel::goUp,
        onRetry = viewModel::retry,
        onDownload = { file -> download(file.name) },
        onCancel = viewModel::cancel,
        onMobileData = viewModel::answerMobileData,
        onDismissMobileData = viewModel::dismissMobileData,
        modifier = modifier,
    )
}
