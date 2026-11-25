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

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Runs downloads after asking, if needed, for the permission to post the download notification.
 *
 * The returned function takes a key that says what to download (e.g. a map's name). If the app may
 * post notifications, [download] is called with it at once. Otherwise the permission is asked for,
 * and [download] is called with the key once the user answers, whatever the answer: the
 * notification is optional, so a refusal does not stop the download. The key waits in saved state,
 * so the download still happens if the activity is recreated while the request is shown. Android
 * stops showing the request after the user refuses it twice; [download] is then called at once.
 *
 * @param download starts the download that the key stands for.
 * @return the function that starts a download, asking first if needed.
 */
@Composable
internal fun rememberDownloadWithNotificationPermission(download: (key: String) -> Unit): (key: String) -> Unit {
    val context = LocalContext.current
    val currentDownload by rememberUpdatedState(download)
    // The key waiting for the permission answer, so it survives recreation.
    var awaiting by rememberSaveable { mutableStateOf<String?>(null) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        val key = awaiting
        awaiting = null
        if (key != null) currentDownload(key)
    }
    return { key ->
        val canNotify = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (canNotify) {
            currentDownload(key)
        } else {
            awaiting = key
            ask.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
