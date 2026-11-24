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
import android.text.format.Formatter
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.map.R
import dev.giste.map.domain.FileSize
import dev.giste.map.domain.RemoteFolderPath
import dev.giste.map.domain.RemoteListingFailure
import dev.giste.map.domain.RemoteMapEntry
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
 *   icon and its name, and opens it when pressed. A map row shows its name, the day it changed,
 *   its approximate size, and a mark if it is already installed.
 *
 * In a sub-folder, the back button and the system back gesture go up one folder. In the maps
 * folder itself, the back button calls [onBack], and the system back gesture is the caller's.
 *
 * @param state what to show.
 * @param onBack called when the back button is pressed in the maps folder itself.
 * @param onOpen called with a folder when its row is pressed.
 * @param onUp called when the back button or the system back gesture is used in a sub-folder.
 * @param onRetry called when the button to try again is pressed.
 * @param modifier applied to the root layout.
 */
@Composable
internal fun MapDownloaderContent(
    state: MapDownloaderUiState,
    onBack: () -> Unit,
    onOpen: (RemoteMapEntry.Folder) -> Unit,
    onUp: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inSubFolder = !state.folder.isRoot
    BackHandler(enabled = inSubFolder, onBack = onUp)

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
                    EntryList(state.entries, state.installed, onOpen, bodyModifier)
                }
        }
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
 * @param entries the entries, never empty.
 * @param installed the names of the map files that are already installed.
 * @param onOpen called with a folder when its row is pressed.
 * @param modifier applied to the list.
 */
@Composable
private fun EntryList(
    entries: List<RemoteMapEntry>,
    installed: Set<String>,
    onOpen: (RemoteMapEntry.Folder) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        // A folder and a map file never share a name (a map file ends in ".map"), so names are
        // unique keys; the type keeps them apart anyway.
        items(entries, key = { "${it::class.simpleName}/${it.name}" }) { entry ->
            when (entry) {
                is RemoteMapEntry.Folder -> FolderRow(entry, onOpen = { onOpen(entry) })
                is RemoteMapEntry.MapFile -> MapFileRow(entry, installed = entry.name in installed)
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
 * A map file: its name and the day it changed, its approximate size, and a mark if it is already
 * installed.
 *
 * @param file the map file.
 * @param installed whether it is already installed.
 * @param modifier applied to the row.
 */
@Composable
private fun MapFileRow(file: RemoteMapEntry.MapFile, installed: Boolean, modifier: Modifier = Modifier) {
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
        }
        file.approximateSize?.let { Text(text = it.format(), style = MaterialTheme.typography.bodyMedium) }
        // The mark takes the space of a button, so rows line up whether a map is installed or not.
        Box(modifier = Modifier.size(GisteTheme.dimensions.minTouchTarget), contentAlignment = Alignment.Center) {
            if (installed) {
                Icon(
                    painter = painterResource(R.drawable.map_ic_installed),
                    contentDescription = null,
                    modifier = Modifier.testTag(MapDownloaderTestTags.INSTALLED),
                    tint = MaterialTheme.colorScheme.primary,
                )
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

/** This size as short text in the current locale, e.g. "2.5 MB". */
@Composable
private fun FileSize.format(): String = Formatter.formatShortFileSize(LocalContext.current, bytes)

/** This day as text in the current locale, e.g. "Sep 7, 2026". */
@Composable
private fun LocalDate.format(): String {
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    return formatter.format(this)
}

// --- Previews ---

/** A preview folder: a sub-folder, then two maps, one of them installed. */
private val PREVIEW_ENTRIES = listOf(
    RemoteMapEntry.Folder("italy"),
    RemoteMapEntry.MapFile("albania.map", FileSize(45_088_768), LocalDate.of(2026, 8, 21)),
    RemoteMapEntry.MapFile("andorra.map", FileSize(2_516_582), LocalDate.of(2026, 9, 7)),
)

/** The folder the previews show. */
private val PREVIEW_FOLDER = RemoteFolderPath.ROOT.child("europe")

/** The downloader in [state] in the app theme. */
@Composable
private fun MapDownloaderPreview(state: MapDownloaderUiState) {
    GisteTheme {
        MapDownloaderContent(state = state, onBack = {}, onOpen = {}, onUp = {}, onRetry = {})
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
    MapDownloaderPreview(MapDownloaderUiState.Listed(PREVIEW_FOLDER, PREVIEW_ENTRIES, setOf("andorra.map")))
}
