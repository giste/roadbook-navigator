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

package dev.giste.roadbooknavigator.core.permission

import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import dev.giste.roadbooknavigator.core.permission.data.ActivityPermissionChecker
import dev.giste.roadbooknavigator.core.permission.data.appPermissionsOf
import dev.giste.roadbooknavigator.core.permission.data.openAppSettings
import dev.giste.roadbooknavigator.core.permission.data.requestedPermissions
import dev.giste.roadbooknavigator.core.permission.presentation.PermissionGateContent
import dev.giste.roadbooknavigator.core.permission.presentation.rememberPermissionGateState

/**
 * Shows [content] only while every permission in [permissions] is granted.
 *
 * Until then it shows a full-screen explanation of the first missing permission, with a button
 * that opens the system dialog or, when the permission is permanently denied, the app settings.
 * It re-checks every time the screen resumes, so a permission granted in the settings, or
 * revoked while the app was in the background, is picked up when the user comes back.
 *
 * It must be hosted in an `Activity` (it reads `LocalActivity`), which is always the case in
 * the app.
 *
 * @param permissions the permissions [content] needs. Asked for one at a time, in
 * [AppPermission] order.
 * @param modifier applied to the root layout.
 * @param content shown once every permission is granted.
 */
@Composable
public fun PermissionGate(
    permissions: Set<AppPermission>,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val activity = checkNotNull(LocalActivity.current) {
        "PermissionGate must be hosted in an Activity"
    }
    val checker = remember(activity) { ActivityPermissionChecker(activity) }
    val state = rememberPermissionGateState(permissions, checker)
    val launcher = rememberLauncherForActivityResult(RequestMultiplePermissions()) { results ->
        state.onRequestResult(appPermissionsOf(results.keys))
    }

    LifecycleResumeEffect(state) {
        state.refresh()
        onPauseOrDispose { }
    }

    PermissionGateContent(
        pending = state.pending,
        onRequest = { permission ->
            launcher.launch(permission.requestedPermissions.toTypedArray())
        },
        onOpenSettings = activity::openAppSettings,
        modifier = modifier,
        content = content,
    )
}
