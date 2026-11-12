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

package dev.giste.roadbooknavigator.core.permission.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import dev.giste.roadbooknavigator.core.R
import dev.giste.roadbooknavigator.core.permission.AppPermission
import dev.giste.roadbooknavigator.core.permission.domain.PermissionStatus
import dev.giste.roadbooknavigator.core.ui.theme.RoadbookNavigatorTheme

/**
 * Shows [content] when [pending] is `null`, and the [PermissionScreen] otherwise.
 *
 * Stateless, so tests can drive it without an `Activity` or real permissions.
 *
 * @param pending the permission the gate is waiting for, or `null` when all are granted.
 * @param onRequest asks Android for a permission with the system dialog.
 * @param onOpenSettings opens the app settings.
 * @param modifier applied to the root layout.
 * @param content shown once every required permission is granted.
 */
@Composable
internal fun PermissionGateContent(
    pending: PendingPermission?,
    onRequest: (AppPermission) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        if (pending == null) {
            content()
        } else {
            PermissionScreen(
                pending = pending,
                onRequest = { onRequest(pending.permission) },
                onOpenSettings = onOpenSettings,
            )
        }
    }
}

/**
 * Explains why the app needs a permission and offers the action that can grant it.
 *
 * - [PermissionStatus.Denied] and [PermissionStatus.ShowRationale]: a button that shows the
 *   system dialog.
 * - [PermissionStatus.PermanentlyDenied]: a button that opens the app settings, plus "try again"
 *   in case the dialog was only dismissed (see [PermissionStatus.resolve]).
 *
 * @param pending the permission to explain and its status.
 * @param onRequest shows the system dialog.
 * @param onOpenSettings opens the app settings.
 * @param modifier applied to the root layout.
 */
@Composable
internal fun PermissionScreen(
    pending: PendingPermission,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimensions = RoadbookNavigatorTheme.dimensions
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(dimensions.spacingLarge),
            verticalArrangement = Arrangement.spacedBy(
                space = dimensions.spacingMedium,
                alignment = Alignment.CenterVertically,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = pending.permission.title(),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = pending.message(),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            val buttonModifier = Modifier
                .fillMaxWidth()
                .heightIn(min = dimensions.minTouchTarget)
            if (pending.status == PermissionStatus.PermanentlyDenied) {
                Button(onClick = onOpenSettings, modifier = buttonModifier) {
                    Text(stringResource(R.string.core_permission_action_open_settings))
                }
                TextButton(onClick = onRequest, modifier = buttonModifier) {
                    Text(stringResource(R.string.core_permission_action_try_again))
                }
            } else {
                Button(onClick = onRequest, modifier = buttonModifier) {
                    Text(pending.permission.allowAction())
                }
            }
        }
    }
}

/** The screen title for this permission. */
@Composable
private fun AppPermission.title(): String = when (this) {
    AppPermission.PRECISE_LOCATION -> stringResource(R.string.core_permission_location_title)
}

/** The label of the button that shows the system dialog for this permission. */
@Composable
private fun AppPermission.allowAction(): String = when (this) {
    AppPermission.PRECISE_LOCATION -> stringResource(R.string.core_permission_location_action_allow)
}

/** The explanation for this permission in its current status. */
@Composable
private fun PendingPermission.message(): String = when (permission) {
    AppPermission.PRECISE_LOCATION -> when (status) {
        PermissionStatus.ShowRationale ->
            stringResource(R.string.core_permission_location_rationale)
        PermissionStatus.PermanentlyDenied ->
            stringResource(R.string.core_permission_location_permanently_denied)
        // Granted never reaches the screen; it shares the first-time explanation.
        PermissionStatus.Denied, PermissionStatus.Granted ->
            stringResource(R.string.core_permission_location_message)
    }
}

@Preview(name = "Denied - light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Denied - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PermissionScreenDeniedPreview() {
    PermissionScreenPreview(PermissionStatus.Denied)
}

@Preview(name = "Rationale - light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Rationale - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PermissionScreenRationalePreview() {
    PermissionScreenPreview(PermissionStatus.ShowRationale)
}

@Preview(name = "Permanently denied - light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Permanently denied - dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PermissionScreenPermanentlyDeniedPreview() {
    PermissionScreenPreview(PermissionStatus.PermanentlyDenied)
}

/** Shared body of the previews: the location screen in [status]. */
@Composable
private fun PermissionScreenPreview(status: PermissionStatus) {
    RoadbookNavigatorTheme {
        PermissionScreen(
            pending = PendingPermission(AppPermission.PRECISE_LOCATION, status),
            onRequest = {},
            onOpenSettings = {},
        )
    }
}
