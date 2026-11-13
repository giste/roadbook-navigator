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

package dev.giste.core.permission.presentation

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
import dev.giste.core.R
import dev.giste.core.permission.RequiredPermission
import dev.giste.core.permission.domain.PermissionStatus
import dev.giste.core.ui.theme.GisteTheme

/**
 * Shows [content] when [pending] is `null`, and the [PermissionScreen] otherwise, with the texts
 * of the pending permission resolved from its string resources.
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
    onRequest: (RequiredPermission) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        if (pending == null) {
            content()
        } else {
            PermissionScreen(
                status = pending.status,
                title = stringResource(pending.permission.title),
                message = stringResource(pending.message),
                allowAction = stringResource(pending.permission.allowAction),
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
 * Texts come in already resolved, so the screen knows no permission in particular.
 *
 * @param status why the permission is not granted. Never [PermissionStatus.Granted].
 * @param title the screen title.
 * @param message the explanation that fits [status].
 * @param allowAction the label of the button that shows the system dialog.
 * @param onRequest shows the system dialog.
 * @param onOpenSettings opens the app settings.
 * @param modifier applied to the root layout.
 */
@Composable
internal fun PermissionScreen(
    status: PermissionStatus,
    title: String,
    message: String,
    allowAction: String,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimensions = GisteTheme.dimensions
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
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            val buttonModifier = Modifier
                .fillMaxWidth()
                .heightIn(min = dimensions.minTouchTarget)
            if (status == PermissionStatus.PermanentlyDenied) {
                Button(onClick = onOpenSettings, modifier = buttonModifier) {
                    Text(stringResource(R.string.core_permission_action_open_settings))
                }
                TextButton(onClick = onRequest, modifier = buttonModifier) {
                    Text(stringResource(R.string.core_permission_action_try_again))
                }
            } else {
                Button(onClick = onRequest, modifier = buttonModifier) {
                    Text(allowAction)
                }
            }
        }
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

/** Shared body of the previews: a sample location permission in [status]. */
@Composable
private fun PermissionScreenPreview(status: PermissionStatus) {
    GisteTheme {
        PermissionScreen(
            status = status,
            title = "Precise location needed",
            message = when (status) {
                PermissionStatus.ShowRationale -> "Without precise location, distances are wrong."
                PermissionStatus.PermanentlyDenied -> "Location is off. Turn it on in the settings."
                PermissionStatus.Denied, PermissionStatus.Granted ->
                    "The app uses your precise location to measure distances."
            },
            allowAction = "Allow location",
            onRequest = {},
            onOpenSettings = {},
        )
    }
}
