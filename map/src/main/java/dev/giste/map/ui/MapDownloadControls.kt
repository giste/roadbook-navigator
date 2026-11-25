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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.map.R
import dev.giste.map.domain.FileSize
import dev.giste.map.domain.MapDownload
import dev.giste.map.domain.MapDownloadFailure

/**
 * Test tags of the download controls that the map screens share, for the elements whose identity
 * is not in their text.
 */
internal object MapDownloadTestTags {

    /** The mark of a map waiting to be downloaded, an icon. */
    const val QUEUED: String = "map_download_queued"

    /** The progress of a map being downloaded, a progress indicator. */
    const val PROGRESS: String = "map_download_progress"

    /** The stop mark inside the progress, an icon. */
    const val STOP_MARK: String = "map_download_stop_mark"

    /** The button that downloads again a map that failed, which shows an icon instead of text. */
    const val RETRY_DOWNLOAD: String = "map_download_retry_download"

    /**
     * The button around a waiting mark or a progress, which asks to stop the download. It shows
     * the mark or the progress instead of text.
     */
    const val CANCEL_DOWNLOAD: String = "map_download_cancel_download"

    /** The stop dialog's button that stops the download, which shows an icon instead of text. */
    const val CONFIRM_CANCEL: String = "map_download_confirm_cancel"

    /** The stop dialog's button that keeps downloading, which shows an icon instead of text. */
    const val KEEP_DOWNLOADING: String = "map_download_keep_downloading"

    /** The mobile data dialog's button that downloads now, which shows an icon instead of text. */
    const val USE_MOBILE_DATA: String = "map_download_use_mobile_data"

    /** The mobile data dialog's button that waits for Wi-Fi, which shows an icon instead of text. */
    const val WAIT_FOR_WIFI: String = "map_download_wait_for_wifi"

    /** The mobile data dialog's button that downloads nothing, which shows an icon instead of text. */
    const val DISMISS_MOBILE_DATA: String = "map_download_dismiss_mobile_data"
}

/** Whether this download is waiting or running, so it can still be stopped. */
internal val MapDownload?.isActive: Boolean
    get() = this is MapDownload.Queued || this is MapDownload.Running

/**
 * A waiting or running [download], in a button as large as a touch target that asks to stop it.
 * It shows a waiting mark while the download is queued, or its progress while it runs (see
 * [DownloadMark]).
 *
 * @param download a queued or running download.
 * @param onCancelRequest called when the button is pressed.
 * @param modifier applied to the button.
 */
@Composable
internal fun ActiveDownloadButton(download: MapDownload, onCancelRequest: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onCancelRequest,
        modifier = modifier
            .size(GisteTheme.dimensions.minTouchTarget)
            .testTag(MapDownloadTestTags.CANCEL_DOWNLOAD),
    ) {
        DownloadMark(download)
    }
}

/**
 * A button that downloads again a map whose download failed, in the error color, as large as a
 * touch target.
 *
 * @param onClick called when the button is pressed.
 * @param modifier applied to the button.
 */
@Composable
internal fun RetryDownloadButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(GisteTheme.dimensions.minTouchTarget)
            .testTag(MapDownloadTestTags.RETRY_DOWNLOAD),
        colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error),
    ) {
        Icon(painter = painterResource(R.drawable.map_ic_refresh), contentDescription = null)
    }
}

/**
 * How [download] goes: a waiting mark while it is queued, or its progress while it runs (a spinner
 * while its size is unknown) with a stop mark inside, which says that pressing it stops the
 * download. The waiting mark has none: why the map waits (another download, or Wi-Fi) matters
 * more there, and pressing it asks the same.
 *
 * @param download a queued or running download.
 */
@Composable
private fun DownloadMark(download: MapDownload) {
    if (download is MapDownload.Running) {
        Box(contentAlignment = Alignment.Center) {
            val progressModifier = Modifier.testTag(MapDownloadTestTags.PROGRESS)
            val fraction = download.fraction
            if (fraction != null) {
                CircularProgressIndicator(progress = { fraction }, modifier = progressModifier)
            } else {
                CircularProgressIndicator(modifier = progressModifier)
            }
            Icon(
                painter = painterResource(R.drawable.map_ic_close),
                contentDescription = null,
                // Material's default icon size, which fits inside the ring.
                modifier = Modifier.testTag(MapDownloadTestTags.STOP_MARK),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    } else {
        Icon(
            painter = painterResource(R.drawable.map_ic_queued),
            contentDescription = null,
            modifier = Modifier.testTag(MapDownloadTestTags.QUEUED),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Asks the rider to confirm stopping the download of the map [name]: the name in the title, and
 * icon buttons to stop it, which throws away what was downloaded, or to keep downloading. The stop
 * button is filled in the error color, so it stands apart from the outlined one.
 *
 * @param name the map's file name.
 * @param onConfirm called when the rider confirms.
 * @param onDismiss called when the rider keeps downloading, taps outside or presses back.
 */
@Composable
internal fun CancelDownloadDialog(name: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val minTouchTarget = GisteTheme.dimensions.minTouchTarget
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.map_download_cancel_title, name)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .heightIn(min = minTouchTarget)
                    .testTag(MapDownloadTestTags.CONFIRM_CANCEL),
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
                    .testTag(MapDownloadTestTags.KEEP_DOWNLOADING),
            ) {
                Icon(painter = painterResource(R.drawable.map_ic_close), contentDescription = null)
            }
        },
    )
}

/**
 * Asks whether to download the map [name] now with mobile data, or to wait for Wi-Fi, as the
 * device is not on Wi-Fi. The buttons are icons: mobile data (filled, as it downloads now), Wi-Fi
 * (outlined) and close, which downloads nothing. The text says what each choice does.
 *
 * @param name the map's file name.
 * @param onAnswer called with `true` to download now with mobile data, `false` to wait for Wi-Fi.
 * @param onDismiss called when the rider closes the dialog, taps outside or presses back.
 */
@Composable
internal fun MobileDataDialog(name: String, onAnswer: (useMobileData: Boolean) -> Unit, onDismiss: () -> Unit) {
    val dimensions = GisteTheme.dimensions
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.map_download_mobile_data_title)) },
        text = { Text(stringResource(R.string.map_download_mobile_data_message, name)) },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall)) {
                OutlinedButton(
                    onClick = { onAnswer(false) },
                    modifier = Modifier
                        .heightIn(min = dimensions.minTouchTarget)
                        .testTag(MapDownloadTestTags.WAIT_FOR_WIFI),
                ) {
                    Icon(painter = painterResource(R.drawable.map_ic_wifi), contentDescription = null)
                }
                Button(
                    onClick = { onAnswer(true) },
                    modifier = Modifier
                        .heightIn(min = dimensions.minTouchTarget)
                        .testTag(MapDownloadTestTags.USE_MOBILE_DATA),
                ) {
                    Icon(painter = painterResource(R.drawable.map_ic_mobile_data), contentDescription = null)
                }
            }
        },
        dismissButton = {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(dimensions.minTouchTarget)
                    .testTag(MapDownloadTestTags.DISMISS_MOBILE_DATA),
            ) {
                Icon(painter = painterResource(R.drawable.map_ic_close), contentDescription = null)
            }
        },
    )
}

/** The message that explains this download failure. */
@Composable
internal fun MapDownloadFailure.toMessage(): String = stringResource(
    when (this) {
        MapDownloadFailure.SERVER_ERROR -> R.string.map_download_failed_server
        MapDownloadFailure.NOT_ENOUGH_SPACE -> R.string.map_download_failed_space
        MapDownloadFailure.UNREADABLE_MAP -> R.string.map_download_failed_unreadable
        MapDownloadFailure.STORAGE_ERROR -> R.string.map_download_failed_storage
    },
)

/** This size as short text in the current locale, e.g. "812 MB". */
@Composable
internal fun FileSize.format(): String = Formatter.formatShortFileSize(LocalContext.current, bytes)

// --- Previews ---

@Preview(name = "Mobile data question, light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Mobile data question, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MobileDataDialogPreview() {
    GisteTheme {
        MobileDataDialog(name = "andorra.map", onAnswer = {}, onDismiss = {})
    }
}

@Preview(name = "Stop download dialog, light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Stop download dialog, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CancelDownloadDialogPreview() {
    GisteTheme {
        CancelDownloadDialog(name = "austria.map", onConfirm = {}, onDismiss = {})
    }
}
