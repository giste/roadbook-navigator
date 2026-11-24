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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.map.R
import dev.giste.map.domain.FileSize
import dev.giste.map.domain.InstalledMapFile

/** Test tags of the map manager, for the elements whose identity is not in their text. */
internal object MapManagerTestTags {

    /** The back button, which shows an icon instead of text. */
    const val BACK: String = "map_manager_back"

    /** The empty area shown while the maps are looked for. */
    const val LOADING: String = "map_manager_loading"

    /** The delete button of a file's row, which shows an icon instead of text. */
    const val DELETE: String = "map_manager_delete"

    /** The mark of a file that cannot be read, an icon. */
    const val UNREADABLE: String = "map_manager_unreadable"

    /** The confirmation dialog's delete button, which shows an icon instead of text. */
    const val CONFIRM_DELETE: String = "map_manager_confirm_delete"

    /** The confirmation dialog's cancel button, which shows an icon instead of text. */
    const val CANCEL_DELETE: String = "map_manager_cancel_delete"

    /**
     * The row of one map file, which holds its delete button and its marks.
     *
     * @param file the file of the row.
     * @return the tag of its row, e.g. `map_manager_file_/maps/spain.map`.
     */
    fun file(file: InstalledMapFile): String = "map_manager_file_${file.path}"
}

/**
 * The map manager for [state]: a top bar with a back button, then the installed map files.
 *
 * - [MapManagerUiState.Loading]: only the top bar, as looking for the maps takes a moment.
 * - [MapManagerUiState.FolderUnreadable]: a message.
 * - [MapManagerUiState.Listed] without files: the same message as the map viewer without maps.
 * - [MapManagerUiState.Listed] with files: one row per file, with its name, the sub-folder that
 *   holds it (when it is not the map folder itself), its size and a delete button. A file that
 *   cannot be read has a warning mark before its name.
 *
 * Actions are icons, so they need no translation and take the same space in every language.
 * Deleting cannot be undone, so the delete button opens a dialog that asks first; which file it
 * asks about survives configuration changes. When [MapManagerUiState.Listed.deleteFailed] is set, a
 * snackbar says the file could not be deleted, and [onDeleteErrorShown] is called once it has been
 * shown.
 *
 * @param state what to show.
 * @param onBack called when the back button is pressed.
 * @param onDelete called with a file once the rider has confirmed deleting it.
 * @param onDeleteErrorShown called once the deletion failure has been shown.
 * @param modifier applied to the root layout.
 */
@Composable
internal fun MapManagerContent(
    state: MapManagerUiState,
    onBack: () -> Unit,
    onDelete: (InstalledMapFile) -> Unit,
    onDeleteErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val deleteFailedMessage = (state as? MapManagerUiState.Listed)?.deleteFailed?.let {
        stringResource(R.string.map_manager_delete_failed, it.name)
    }
    LaunchedEffect(deleteFailedMessage) {
        if (deleteFailedMessage != null) {
            snackbarHostState.showSnackbar(deleteFailedMessage)
            onDeleteErrorShown()
        }
    }
    // The path of the file whose deletion is being confirmed, if any. A path, so it can be saved.
    var confirming by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.map_manager_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(GisteTheme.dimensions.minTouchTarget)
                            .testTag(MapManagerTestTags.BACK),
                    ) {
                        Icon(painter = painterResource(R.drawable.map_ic_arrow_back), contentDescription = null)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val bodyModifier = Modifier.fillMaxSize().padding(padding)
        when (state) {
            MapManagerUiState.Loading -> Box(bodyModifier.testTag(MapManagerTestTags.LOADING))
            MapManagerUiState.FolderUnreadable ->
                ManagerMessage(stringResource(R.string.map_manager_folder_unreadable), bodyModifier)
            is MapManagerUiState.Listed ->
                if (state.files.isEmpty()) {
                    ManagerMessage(stringResource(R.string.map_no_maps), bodyModifier)
                } else {
                    MapFileList(
                        files = state.files,
                        onDelete = { confirming = it.path },
                        modifier = bodyModifier,
                    )
                }
        }
    }

    // Only while the file is still listed: once it is gone, there is nothing to confirm.
    val toDelete = (state as? MapManagerUiState.Listed)?.files?.find { it.path == confirming }
    if (toDelete != null) {
        DeleteMapDialog(
            file = toDelete,
            onConfirm = {
                confirming = null
                onDelete(toDelete)
            },
            onDismiss = { confirming = null },
        )
    }
}

/**
 * The installed map files, one row each, with a divider between rows.
 *
 * @param files the files, never empty.
 * @param onDelete called with a file when its delete button is pressed.
 * @param modifier applied to the list.
 */
@Composable
private fun MapFileList(
    files: List<InstalledMapFile>,
    onDelete: (InstalledMapFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        items(files, key = { it.path }) { file ->
            MapFileRow(file = file, onDelete = { onDelete(file) })
            HorizontalDivider()
        }
    }
}

/**
 * One map file: a warning mark if it cannot be read, its name and sub-folder, its size and its
 * delete button.
 *
 * @param file the file.
 * @param onDelete called when its delete button is pressed.
 * @param modifier applied to the row.
 */
@Composable
private fun MapFileRow(file: InstalledMapFile, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val dimensions = GisteTheme.dimensions
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dimensions.minTouchTarget)
            .padding(start = dimensions.spacingMedium)
            .testTag(MapManagerTestTags.file(file)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingMedium),
    ) {
        if (!file.readable) {
            Icon(
                painter = painterResource(R.drawable.map_ic_warning),
                contentDescription = null,
                modifier = Modifier.testTag(MapManagerTestTags.UNREADABLE),
                tint = MaterialTheme.colorScheme.error,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = file.name, style = MaterialTheme.typography.bodyLarge)
            if (file.folder.isNotEmpty()) {
                Text(
                    text = file.folder,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(text = file.size.format(), style = MaterialTheme.typography.bodyMedium)
        IconButton(
            onClick = onDelete,
            modifier = Modifier
                .size(dimensions.minTouchTarget)
                .testTag(MapManagerTestTags.DELETE),
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(painter = painterResource(R.drawable.map_ic_delete), contentDescription = null)
        }
    }
}

/**
 * Asks the rider to confirm deleting [file]: the file name in the title, and icon buttons to
 * delete or to cancel. The delete button is filled in the error color, so it stands apart from
 * the outlined cancel button.
 *
 * @param file the file to delete.
 * @param onConfirm called when the rider confirms.
 * @param onDismiss called when the rider cancels, taps outside or presses back.
 */
@Composable
private fun DeleteMapDialog(file: InstalledMapFile, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val minTouchTarget = GisteTheme.dimensions.minTouchTarget
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.map_manager_delete_title, file.name)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .heightIn(min = minTouchTarget)
                    .testTag(MapManagerTestTags.CONFIRM_DELETE),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Icon(painter = painterResource(R.drawable.map_ic_delete), contentDescription = null)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = minTouchTarget)
                    .testTag(MapManagerTestTags.CANCEL_DELETE),
            ) {
                Icon(painter = painterResource(R.drawable.map_ic_close), contentDescription = null)
            }
        },
    )
}

/**
 * A message in place of the list, centered.
 *
 * @param text the message.
 * @param modifier applied to the area.
 */
@Composable
private fun ManagerMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(GisteTheme.dimensions.spacingMedium), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}

/** This size as short text in the current locale, e.g. "812 MB". */
@Composable
private fun FileSize.format(): String = Formatter.formatShortFileSize(LocalContext.current, bytes)

// --- Previews ---

/** Preview sample files: two readable maps, one in a sub-folder, and a damaged one. */
private val PREVIEW_FILES = listOf(
    InstalledMapFile("andorra.map", "/maps/andorra.map", "", FileSize(6_291_456), readable = true),
    InstalledMapFile("damaged.map", "/maps/damaged.map", "", FileSize(1_024), readable = false),
    InstalledMapFile("spain.map", "/maps/europe/spain.map", "europe", FileSize(851_443_712), readable = true),
)

/** The manager in [state] in the app theme. */
@Composable
private fun MapManagerPreview(state: MapManagerUiState) {
    GisteTheme {
        MapManagerContent(state = state, onBack = {}, onDelete = {}, onDeleteErrorShown = {})
    }
}

@Preview(name = "Loading, light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Loading, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MapManagerLoadingPreview() {
    MapManagerPreview(MapManagerUiState.Loading)
}

@Preview(name = "Folder unreadable, light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Folder unreadable, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MapManagerFolderUnreadablePreview() {
    MapManagerPreview(MapManagerUiState.FolderUnreadable)
}

@Preview(name = "No maps, light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "No maps, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MapManagerNoMapsPreview() {
    MapManagerPreview(MapManagerUiState.Listed(emptyList()))
}

@Preview(name = "Listed, light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Listed, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MapManagerListedPreview() {
    MapManagerPreview(MapManagerUiState.Listed(PREVIEW_FILES))
}

@Preview(name = "Delete dialog, light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Delete dialog, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DeleteMapDialogPreview() {
    GisteTheme {
        DeleteMapDialog(file = PREVIEW_FILES.last(), onConfirm = {}, onDismiss = {})
    }
}
