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

package dev.giste.map.ui.manager

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.map.R
import dev.giste.map.domain.FileSize
import dev.giste.map.domain.InstalledMapFile
import dev.giste.map.domain.MapDownload
import dev.giste.map.domain.MapDownloadFailure
import dev.giste.map.domain.RemoteFolderPath
import dev.giste.map.domain.RemoteMapPath
import dev.giste.map.domain.isActive
import dev.giste.map.ui.ActiveDownloadButton
import dev.giste.map.ui.CancelDownloadDialog
import dev.giste.map.ui.MobileDataDialog
import dev.giste.map.ui.RetryDownloadButton
import dev.giste.map.ui.UpdateMapButton
import dev.giste.map.ui.format
import dev.giste.map.ui.toMessage
import java.time.Instant

/** Test tags of the map manager, for the elements whose identity is not in their text. */
internal object MapManagerTestTags {

    /** The back button, which shows an icon instead of text. */
    const val BACK: String = "map_manager_back"

    /** The floating add button, which opens the map downloader and shows an icon instead of text. */
    const val ADD: String = "map_manager_add"

    /** The list of installed map files, a container. */
    const val LIST: String = "map_manager_list"

    /** The empty area shown while the maps are looked for. */
    const val LOADING: String = "map_manager_loading"

    /** The delete button of a file's row, which shows an icon instead of text. */
    const val DELETE: String = "map_manager_delete"

    /** The mark of a file that cannot be read, an icon. */
    const val UNREADABLE: String = "map_manager_unreadable"

    /** The top bar's button that updates every outdated map, which shows an icon instead of text. */
    const val UPDATE_ALL: String = "map_manager_update_all"

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
 * The map manager for [state]: a top bar with a back button, then the installed map files. If
 * [onDownloadMaps] is given, a floating add button at the bottom end opens the map downloader, in
 * every state, and the list leaves room under its last row so the button never hides it.
 *
 * - [MapManagerUiState.Loading]: only the top bar, as looking for the maps takes a moment.
 * - [MapManagerUiState.FolderUnreadable]: a message.
 * - [MapManagerUiState.Listed] without files: the same message as the map viewer without maps.
 * - [MapManagerUiState.Listed] with files: one row per file, with its name, the sub-folder that
 *   holds it (when it is not the map folder itself), its size and a delete button. A file that
 *   cannot be read has a warning mark before its name. Before the delete button, an outdated file
 *   has an update button; while its update waits or runs, the waiting mark or the progress, which
 *   asks whether to stop it; if its update failed, a button to try again, with the reason under
 *   its name. That slot is only there when some file needs it, so rows line up either way.
 *
 * When two or more files can be updated, the top bar has a button that updates all of them. When
 * [MapManagerUiState.Listed.mobileDataQuestion] is set, a dialog asks whether to download those
 * updates now with mobile data or to wait for Wi-Fi, or to download nothing.
 *
 * Actions are icons, so they need no translation and take the same space in every language.
 * Deleting cannot be undone, so the delete button opens a dialog that asks first; which file it
 * asks about survives configuration changes. When [MapManagerUiState.Listed.deleteFailed] is set, a
 * snackbar says the file could not be deleted, and [onDeleteErrorShown] is called once it has been
 * shown.
 *
 * @param state what to show.
 * @param onBack called when the back button is pressed.
 * @param onDownloadMaps called when the add button is pressed, or `null` for no add button.
 * @param onDelete called with a file once the rider has confirmed deleting it.
 * @param onDeleteErrorShown called once the deletion failure has been shown.
 * @param onUpdate called with a file when its update button, or its button to try again, is
 * pressed.
 * @param onUpdateAll called when the button that updates every outdated map is pressed.
 * @param onCancelUpdate called with a file once the rider has confirmed stopping its update.
 * @param onMobileData called with the answer to the mobile data question: `true` to download now
 * with mobile data, `false` to wait for Wi-Fi.
 * @param onDismissMobileData called when the mobile data question is dismissed without an answer.
 * @param modifier applied to the root layout.
 */
@Composable
internal fun MapManagerContent(
    state: MapManagerUiState,
    onBack: () -> Unit,
    onDownloadMaps: (() -> Unit)?,
    onDelete: (InstalledMapFile) -> Unit,
    onDeleteErrorShown: () -> Unit,
    onUpdate: (InstalledMapFile) -> Unit,
    onUpdateAll: () -> Unit,
    onCancelUpdate: (InstalledMapFile) -> Unit,
    onMobileData: (useMobileData: Boolean) -> Unit,
    onDismissMobileData: () -> Unit,
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
    // The path of the file whose update stop is being confirmed, if any.
    var confirmingCancel by rememberSaveable { mutableStateOf<String?>(null) }
    val listed = state as? MapManagerUiState.Listed

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
                actions = {
                    if ((listed?.updatable?.size ?: 0) >= MIN_FILES_TO_UPDATE_ALL) {
                        IconButton(
                            onClick = onUpdateAll,
                            modifier = Modifier
                                .size(GisteTheme.dimensions.minTouchTarget)
                                .testTag(MapManagerTestTags.UPDATE_ALL),
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Icon(painter = painterResource(R.drawable.map_ic_update), contentDescription = null)
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (onDownloadMaps != null) {
                val minTouchTarget = GisteTheme.dimensions.minTouchTarget
                FloatingActionButton(
                    onClick = onDownloadMaps,
                    modifier = Modifier
                        .defaultMinSize(minWidth = minTouchTarget, minHeight = minTouchTarget)
                        .testTag(MapManagerTestTags.ADD),
                ) {
                    Icon(painter = painterResource(R.drawable.map_ic_add), contentDescription = null)
                }
            }
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
                        onUpdate = onUpdate,
                        onCancelRequest = { confirmingCancel = it.path },
                        modifier = bodyModifier,
                        clearOfAddButton = onDownloadMaps != null,
                    )
                }
        }
    }

    // Only while the file is still listed: once it is gone, there is nothing to confirm.
    val toDelete = listed?.files?.find { it.file.path == confirming }?.file
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
    // Only while the update still downloads: once it has finished or stopped, there is nothing to stop.
    val toCancel = listed?.files?.find { it.file.path == confirmingCancel && it.download.isActive }?.file
    if (toCancel != null) {
        CancelDownloadDialog(
            name = toCancel.name,
            onConfirm = {
                confirmingCancel = null
                onCancelUpdate(toCancel)
            },
            onDismiss = { confirmingCancel = null },
        )
    }
    listed?.mobileDataQuestion?.takeIf { it.isNotEmpty() }?.let { maps ->
        MobileDataDialog(names = maps.map { it.name }, onAnswer = onMobileData, onDismiss = onDismissMobileData)
    }
}

/** How many files must be updatable for the top bar to offer updating them all. */
private const val MIN_FILES_TO_UPDATE_ALL = 2

/**
 * The installed map files, one row each, with a divider between rows.
 *
 * @param files the files, never empty.
 * @param onDelete called with a file when its delete button is pressed.
 * @param onUpdate called with a file when its update button, or its button to try again, is
 * pressed.
 * @param onCancelRequest called with a file when its update's waiting mark or progress is pressed.
 * @param modifier applied to the list.
 * @param clearOfAddButton whether the add button floats over the list's bottom end. If so, the
 * list ends with room for it, so the last row, and its delete button, can scroll out from under it.
 */
@Composable
private fun MapFileList(
    files: List<ManagedMapFile>,
    onDelete: (InstalledMapFile) -> Unit,
    onUpdate: (InstalledMapFile) -> Unit,
    onCancelRequest: (InstalledMapFile) -> Unit,
    modifier: Modifier = Modifier,
    clearOfAddButton: Boolean = false,
) {
    val dimensions = GisteTheme.dimensions
    // The button's height, the scaffold's margin under it and the same again above it.
    val bottomPadding = if (clearOfAddButton) dimensions.minTouchTarget + dimensions.spacingMedium * 2 else 0.dp
    LazyColumn(
        modifier = modifier.testTag(MapManagerTestTags.LIST),
        contentPadding = PaddingValues(bottom = bottomPadding),
    ) {
        // Only while some file has something to show there, so rows keep their room for the name.
        val withUpdateSlot = files.any { it.outdated || it.download.isActive }
        items(files, key = { it.file.path }) { managed ->
            MapFileRow(
                managed = managed,
                withUpdateSlot = withUpdateSlot,
                onDelete = { onDelete(managed.file) },
                onUpdate = { onUpdate(managed.file) },
                onCancelRequest = { onCancelRequest(managed.file) },
            )
            HorizontalDivider()
        }
    }
}

/**
 * One map file: a warning mark if it cannot be read, its name and sub-folder (and why its update
 * failed, if it did), its size, its update slot (see [UpdateSlot]) and its delete button.
 *
 * @param managed the file and its update.
 * @param withUpdateSlot whether the row has an update slot, empty if the file needs none.
 * @param onDelete called when its delete button is pressed.
 * @param onUpdate called when its update button, or the button to try again, is pressed.
 * @param onCancelRequest called when its update's waiting mark or progress is pressed.
 * @param modifier applied to the row.
 */
@Composable
private fun MapFileRow(
    managed: ManagedMapFile,
    withUpdateSlot: Boolean,
    onDelete: () -> Unit,
    onUpdate: () -> Unit,
    onCancelRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val file = managed.file
    // A failed download only matters while the map is still outdated: it is its update's.
    val failure = (managed.download as? MapDownload.Failed)?.reason?.takeIf { managed.outdated }
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
            failure?.let { reason ->
                Text(
                    text = reason.toMessage(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Text(text = file.size.format(), style = MaterialTheme.typography.bodyMedium)
        if (withUpdateSlot) {
            UpdateSlot(
                managed = managed,
                failed = failure != null,
                onUpdate = onUpdate,
                onCancelRequest = onCancelRequest,
            )
        }
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
 * The update of a file, in the space of a button, so rows line up whatever it shows:
 *
 * - the waiting mark or the progress while its update is queued or running, in a button that asks
 *   to stop it,
 * - else a button to try again, in the error color, if its update [failed],
 * - else a button to update it, if it is outdated,
 * - else nothing.
 *
 * @param managed the file and its update.
 * @param failed whether its update failed, and the failure is to be shown.
 * @param onUpdate called when the update button, or the button to try again, is pressed.
 * @param onCancelRequest called when the waiting mark or the progress is pressed.
 */
@Composable
private fun UpdateSlot(
    managed: ManagedMapFile,
    failed: Boolean,
    onUpdate: () -> Unit,
    onCancelRequest: () -> Unit,
) {
    Box(modifier = Modifier.size(GisteTheme.dimensions.minTouchTarget), contentAlignment = Alignment.Center) {
        val download = managed.download
        when {
            download != null && download.isActive -> ActiveDownloadButton(download, onCancelRequest)
            failed -> RetryDownloadButton(onClick = onUpdate)
            managed.outdated -> UpdateMapButton(onClick = onUpdate)
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

// --- Previews ---

/** When the preview files last changed. */
private val PREVIEW_MODIFIED: Instant = Instant.parse("2026-09-01T00:00:00Z")

/** Preview sample files: two readable maps, one in a sub-folder, and a damaged one. */
private val PREVIEW_FILES = listOf(
    InstalledMapFile("andorra.map", "/maps/andorra.map", "", FileSize(6_291_456), true, PREVIEW_MODIFIED),
    InstalledMapFile("damaged.map", "/maps/damaged.map", "", FileSize(1_024), false, PREVIEW_MODIFIED),
    InstalledMapFile("spain.map", "/maps/europe/spain.map", "europe", FileSize(851_443_712), true, PREVIEW_MODIFIED),
)

/** Preview sample updates: one outdated map, one updating and one whose update failed. */
private val PREVIEW_UPDATES = listOf(
    InstalledMapFile("andorra.map", "/maps/andorra.map", "", FileSize(6_291_456), true, PREVIEW_MODIFIED),
    InstalledMapFile("france.map", "/maps/europe/france.map", "europe", FileSize(4_000_000_000), true, PREVIEW_MODIFIED),
    InstalledMapFile("italy.map", "/maps/europe/italy.map", "europe", FileSize(2_000_000_000), true, PREVIEW_MODIFIED),
    InstalledMapFile("spain.map", "/maps/europe/spain.map", "europe", FileSize(851_443_712), true, PREVIEW_MODIFIED),
).let { (andorra, france, italy, spain) ->
    val europe = RemoteFolderPath.ROOT.child("europe")
    listOf(
        ManagedMapFile(andorra),
        ManagedMapFile(
            france,
            outdated = true,
            download = MapDownload.Running(RemoteMapPath(europe, "france.map"), FileSize(1_500_000_000), FileSize(4_000_000_000)),
        ),
        ManagedMapFile(
            italy,
            outdated = true,
            download = MapDownload.Failed(RemoteMapPath(europe, "italy.map"), MapDownloadFailure.NOT_ENOUGH_SPACE),
        ),
        ManagedMapFile(spain, outdated = true),
    )
}

/** The manager in [state] in the app theme. */
@Composable
private fun MapManagerPreview(state: MapManagerUiState) {
    GisteTheme {
        MapManagerContent(
            state = state,
            onBack = {},
            onDownloadMaps = {},
            onDelete = {},
            onDeleteErrorShown = {},
            onUpdate = {},
            onUpdateAll = {},
            onCancelUpdate = {},
            onMobileData = {},
            onDismissMobileData = {},
        )
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
    MapManagerPreview(MapManagerUiState.Listed(PREVIEW_FILES.map(::ManagedMapFile)))
}

@Preview(name = "Updates, light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Updates, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MapManagerUpdatesPreview() {
    MapManagerPreview(MapManagerUiState.Listed(PREVIEW_UPDATES))
}

@Preview(name = "Delete dialog, light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Delete dialog, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DeleteMapDialogPreview() {
    GisteTheme {
        DeleteMapDialog(file = PREVIEW_FILES.last(), onConfirm = {}, onDismiss = {})
    }
}
