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

package dev.giste.map.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.map.R
import dev.giste.map.domain.FileSize
import dev.giste.map.domain.MapDownload
import dev.giste.map.domain.MapDownloadFailure
import dev.giste.map.domain.RemoteFolderPath
import dev.giste.map.domain.RemoteListingFailure
import dev.giste.map.domain.RemoteMapEntry
import dev.giste.map.domain.RemoteMapPath
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Test tags of the map downloader, for the elements whose identity is not in their text. */
internal object MapDownloaderTestTags {

    /** The back button, which shows an icon instead of text. */
    const val BACK: String = "map_downloader_back"

    /** The empty area shown while a folder is read. */
    const val LOADING: String = "map_downloader_loading"

    /** The button that reads the folder again after a failure, which shows an icon instead of text. */
    const val RETRY: String = "map_downloader_retry"

    /** The mark of a map that is already installed, an icon. */
    const val INSTALLED: String = "map_downloader_installed"

    /** The button that downloads a map, which shows an icon instead of text. */
    const val DOWNLOAD: String = "map_downloader_download"

    /**
     * The row of one entry, which holds its marks.
     *
     * @param entry the folder or map file of the row.
     * @return the tag of its row, e.g. `map_downloader_entry_andorra.map`.
     */
    fun entry(entry: RemoteMapEntry): String = "map_downloader_entry_${entry.name}"
}

/**
 * The map downloader for [state]: a top bar with a back button, the title and the folder being
 * shown, then what that folder of the download site holds.
 *
 * - [MapDownloaderUiState.Loading]: only the top bar, as reading the folder takes a moment.
 * - [MapDownloaderUiState.Failed]: why the folder could not be read, and a button to try again.
 * - [MapDownloaderUiState.Listed] without entries: a message.
 * - [MapDownloaderUiState.Listed] with entries: one row per entry. A folder row shows a folder
 *   icon and its name, and opens it when pressed. A map row shows its name, the day it changed and
 *   its approximate size, and at its end, in this order of precedence: a waiting mark or the
 *   progress while it is queued or downloading, a mark if it is installed, a button to try again
 *   (with the reason under its name) if its download failed, or else a button to download it.
 *   Pressing the waiting mark or the progress asks, in a dialog, whether to stop the download.
 *
 * When [MapDownloaderUiState.Listed.mobileDataQuestion] is set, a dialog asks whether to download
 * that map now with mobile data or to wait for Wi-Fi, or to download nothing.
 *
 * In a sub-folder, the back button and the system back gesture go up one folder. In the maps
 * folder itself, the back button calls [onBack], and the system back gesture is the caller's.
 *
 * @param state what to show.
 * @param onBack called when the back button is pressed in the maps folder itself.
 * @param onOpen called with a folder when its row is pressed.
 * @param onUp called when the back button or the system back gesture is used in a sub-folder.
 * @param onRetry called when the button to try again is pressed.
 * @param onDownload called with a map when its download button, or its button to try again after
 * a failed download, is pressed.
 * @param onCancel called with a map once the rider has confirmed stopping its download.
 * @param onMobileData called with the answer to the mobile data question: `true` to download now
 * with mobile data, `false` to wait for Wi-Fi.
 * @param onDismissMobileData called when the mobile data question is dismissed without an answer.
 * @param modifier applied to the root layout.
 */
@Composable
internal fun MapDownloaderContent(
    state: MapDownloaderUiState,
    onBack: () -> Unit,
    onOpen: (RemoteMapEntry.Folder) -> Unit,
    onUp: () -> Unit,
    onRetry: () -> Unit,
    onDownload: (RemoteMapEntry.MapFile) -> Unit,
    onCancel: (RemoteMapEntry.MapFile) -> Unit,
    onMobileData: (useMobileData: Boolean) -> Unit,
    onDismissMobileData: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inSubFolder = !state.folder.isRoot
    BackHandler(enabled = inSubFolder, onBack = onUp)
    // The name of the map whose download stop is being confirmed, if any. A name, so it can be saved.
    var confirmingCancel by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.map_downloader_title))
                        if (inSubFolder) {
                            Text(
                                text = state.folder.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = if (inSubFolder) onUp else onBack,
                        modifier = Modifier
                            .size(GisteTheme.dimensions.minTouchTarget)
                            .testTag(MapDownloaderTestTags.BACK),
                    ) {
                        Icon(painter = painterResource(R.drawable.map_ic_arrow_back), contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        val bodyModifier = Modifier.fillMaxSize().padding(padding)
        when (state) {
            is MapDownloaderUiState.Loading -> Box(bodyModifier.testTag(MapDownloaderTestTags.LOADING))
            is MapDownloaderUiState.Failed -> FailedFolder(state.reason, onRetry, bodyModifier)
            is MapDownloaderUiState.Listed ->
                if (state.entries.isEmpty()) {
                    DownloaderMessage(stringResource(R.string.map_downloader_empty), bodyModifier)
                } else {
                    EntryList(
                        state = state,
                        onOpen = onOpen,
                        onDownload = onDownload,
                        onCancelRequest = { confirmingCancel = it.name },
                        modifier = bodyModifier,
                    )
                }
        }
    }

    val listed = state as? MapDownloaderUiState.Listed
    // Only while the map still downloads: once it has finished or stopped, there is nothing to stop.
    val toCancel = listed?.entries?.filterIsInstance<RemoteMapEntry.MapFile>()?.find { file ->
        val download = listed.downloads[file.name]
        file.name == confirmingCancel && download.isActive
    }
    if (toCancel != null) {
        CancelDownloadDialog(
            name = toCancel.name,
            onConfirm = {
                confirmingCancel = null
                onCancel(toCancel)
            },
            onDismiss = { confirmingCancel = null },
        )
    }
    listed?.mobileDataQuestion?.let { map ->
        MobileDataDialog(name = map.name, onAnswer = onMobileData, onDismiss = onDismissMobileData)
    }
}

/**
 * Why a folder could not be read, and a button to try again.
 *
 * @param reason why.
 * @param onRetry called when the button is pressed.
 * @param modifier applied to the area.
 */
@Composable
private fun FailedFolder(reason: RemoteListingFailure, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val dimensions = GisteTheme.dimensions
    Column(
        modifier = modifier.padding(dimensions.spacingMedium),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        MessageText(reason.toMessage())
        FilledIconButton(
            onClick = onRetry,
            modifier = Modifier
                .size(dimensions.minTouchTarget)
                .testTag(MapDownloaderTestTags.RETRY),
        ) {
            Icon(painter = painterResource(R.drawable.map_ic_refresh), contentDescription = null)
        }
    }
}

/**
 * The entries of a folder, one row each, with a divider between rows.
 *
 * @param state the folder, with entries.
 * @param onOpen called with a folder when its row is pressed.
 * @param onDownload called with a map when its download button is pressed.
 * @param onCancelRequest called with a map when its waiting mark or progress is pressed.
 * @param modifier applied to the list.
 */
@Composable
private fun EntryList(
    state: MapDownloaderUiState.Listed,
    onOpen: (RemoteMapEntry.Folder) -> Unit,
    onDownload: (RemoteMapEntry.MapFile) -> Unit,
    onCancelRequest: (RemoteMapEntry.MapFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        // A folder and a map file never share a name (a map file ends in ".map"), so names are
        // unique keys; the type keeps them apart anyway.
        items(state.entries, key = { "${it::class.simpleName}/${it.name}" }) { entry ->
            when (entry) {
                is RemoteMapEntry.Folder -> FolderRow(entry, onOpen = { onOpen(entry) })
                is RemoteMapEntry.MapFile -> MapFileRow(
                    file = entry,
                    installed = entry.name in state.installed,
                    download = state.downloads[entry.name],
                    onDownload = { onDownload(entry) },
                    onCancelRequest = { onCancelRequest(entry) },
                )
            }
            HorizontalDivider()
        }
    }
}

/**
 * A folder: a folder icon and its name. The whole row opens it.
 *
 * @param folder the folder.
 * @param onOpen called when the row is pressed.
 * @param modifier applied to the row.
 */
@Composable
private fun FolderRow(folder: RemoteMapEntry.Folder, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    EntryRow(entry = folder, modifier = modifier.clickable(onClick = onOpen)) {
        Icon(
            painter = painterResource(R.drawable.map_ic_folder),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(text = folder.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}

/**
 * A map file: its name and the day it changed, its approximate size, and at its end what can be
 * done with it or how its download goes (see [MapFileAction]). If its download failed, the reason
 * is shown under its name.
 *
 * @param file the map file.
 * @param installed whether it is already installed.
 * @param download its download, if it is waiting, running or failed.
 * @param onDownload called when its download button, or the button to try again, is pressed.
 * @param onCancelRequest called when its waiting mark or progress is pressed.
 * @param modifier applied to the row.
 */
@Composable
private fun MapFileRow(
    file: RemoteMapEntry.MapFile,
    installed: Boolean,
    download: MapDownload?,
    onDownload: () -> Unit,
    onCancelRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // A failure that an installed map overcame, e.g. by a later download, is no longer news.
    val failure = (download as? MapDownload.Failed)?.reason?.takeUnless { installed }
    EntryRow(entry = file, modifier = modifier) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = file.name, style = MaterialTheme.typography.bodyLarge)
            file.modified?.let { day ->
                Text(
                    text = day.format(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            failure?.let { reason ->
                Text(
                    text = reason.toMessage(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        file.approximateSize?.let { Text(text = it.format(), style = MaterialTheme.typography.bodyMedium) }
        MapFileAction(
            installed = installed,
            download = download,
            failed = failure != null,
            onDownload = onDownload,
            onCancelRequest = onCancelRequest,
        )
    }
}

/**
 * The end of a map row, in the space of a button, so rows line up whatever it shows:
 *
 * - a waiting mark while [download] is queued, or the progress while it runs (a spinner while its
 *   size is unknown), both in a button that asks to stop the download,
 * - else a mark if the map is [installed],
 * - else a button to try again, in the error color, if [failed],
 * - else a button to download it.
 *
 * @param installed whether the map is installed.
 * @param download its download, if it is waiting, running or failed.
 * @param failed whether its download failed, and the failure is to be shown.
 * @param onDownload called when the download button, or the button to try again, is pressed.
 * @param onCancelRequest called when the waiting mark or the progress is pressed.
 */
@Composable
private fun MapFileAction(
    installed: Boolean,
    download: MapDownload?,
    failed: Boolean,
    onDownload: () -> Unit,
    onCancelRequest: () -> Unit,
) {
    val size = GisteTheme.dimensions.minTouchTarget
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        when {
            download != null && download.isActive -> ActiveDownloadButton(download, onCancelRequest)
            installed -> Icon(
                painter = painterResource(R.drawable.map_ic_installed),
                contentDescription = null,
                modifier = Modifier.testTag(MapDownloaderTestTags.INSTALLED),
                tint = MaterialTheme.colorScheme.primary,
            )
            failed -> RetryDownloadButton(onClick = onDownload)
            else -> IconButton(
                onClick = onDownload,
                modifier = Modifier.size(size).testTag(MapDownloaderTestTags.DOWNLOAD),
                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(painter = painterResource(R.drawable.map_ic_download), contentDescription = null)
            }
        }
    }
}

/**
 * The layout every entry row shares: at least a touch target tall, with the entry's tag.
 *
 * @param entry the entry of the row.
 * @param modifier applied to the row.
 * @param content the row's content.
 */
@Composable
private fun EntryRow(
    entry: RemoteMapEntry,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val dimensions = GisteTheme.dimensions
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dimensions.minTouchTarget)
            .padding(horizontal = dimensions.spacingMedium)
            .testTag(MapDownloaderTestTags.entry(entry)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingMedium),
        content = content,
    )
}

/**
 * A message in place of the list, centered.
 *
 * @param text the message.
 * @param modifier applied to the area.
 */
@Composable
private fun DownloaderMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(GisteTheme.dimensions.spacingMedium), contentAlignment = Alignment.Center) {
        MessageText(text)
    }
}

/** A message's text, in the style of the other map messages. */
@Composable
private fun MessageText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
    )
}

/** The message that explains this failure. */
@Composable
private fun RemoteListingFailure.toMessage(): String = stringResource(
    when (this) {
        RemoteListingFailure.NO_CONNECTION -> R.string.map_downloader_no_connection
        RemoteListingFailure.SERVER_ERROR -> R.string.map_downloader_server_error
        RemoteListingFailure.UNREADABLE_LISTING -> R.string.map_downloader_unreadable_listing
    },
)

/** This day as text in the current locale, e.g. "Sep 7, 2026". */
@Composable
private fun LocalDate.format(): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    return formatter.format(this)
}

// --- Previews ---

/** A preview folder: a sub-folder, then maps for every download state, one of them installed. */
private val PREVIEW_ENTRIES = listOf(
    RemoteMapEntry.Folder("italy"),
    RemoteMapEntry.MapFile("albania.map", FileSize(45_088_768), LocalDate.of(2026, 8, 21)),
    RemoteMapEntry.MapFile("andorra.map", FileSize(2_516_582), LocalDate.of(2026, 9, 7)),
    RemoteMapEntry.MapFile("austria.map", FileSize(690_000_000), LocalDate.of(2026, 9, 7)),
    RemoteMapEntry.MapFile("belgium.map", FileSize(260_000_000), LocalDate.of(2026, 9, 7)),
    RemoteMapEntry.MapFile("bulgaria.map", FileSize(130_000_000), LocalDate.of(2026, 9, 7)),
)

/** The folder the previews show. */
private val PREVIEW_FOLDER = RemoteFolderPath.ROOT.child("europe")

/** Downloads of the preview maps: one running, one waiting and one failed. */
private val PREVIEW_DOWNLOADS: Map<String, MapDownload> = listOf(
    MapDownload.Running(RemoteMapPath(PREVIEW_FOLDER, "austria.map"), FileSize(420_000_000), FileSize(690_000_000)),
    MapDownload.Queued(RemoteMapPath(PREVIEW_FOLDER, "belgium.map")),
    MapDownload.Failed(RemoteMapPath(PREVIEW_FOLDER, "bulgaria.map"), MapDownloadFailure.NOT_ENOUGH_SPACE),
).associateBy { it.map.name }

/** The downloader in [state] in the app theme. */
@Composable
private fun MapDownloaderPreview(state: MapDownloaderUiState) {
    GisteTheme {
        MapDownloaderContent(
            state = state,
            onBack = {},
            onOpen = {},
            onUp = {},
            onRetry = {},
            onDownload = {},
            onCancel = {},
            onMobileData = {},
            onDismissMobileData = {},
        )
    }
}

@Preview(name = "Loading, light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Loading, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MapDownloaderLoadingPreview() {
    MapDownloaderPreview(MapDownloaderUiState.Loading(RemoteFolderPath.ROOT))
}

@Preview(name = "Failed, light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Failed, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MapDownloaderFailedPreview() {
    MapDownloaderPreview(MapDownloaderUiState.Failed(RemoteFolderPath.ROOT, RemoteListingFailure.NO_CONNECTION))
}

@Preview(name = "Empty, light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Empty, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MapDownloaderEmptyPreview() {
    MapDownloaderPreview(MapDownloaderUiState.Listed(PREVIEW_FOLDER, emptyList()))
}

@Preview(name = "Listed, light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Listed, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MapDownloaderListedPreview() {
    MapDownloaderPreview(
        MapDownloaderUiState.Listed(PREVIEW_FOLDER, PREVIEW_ENTRIES, setOf("andorra.map"), PREVIEW_DOWNLOADS),
    )
}
