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

package dev.giste.roadbooknavigator.settings.ui

import android.content.res.Configuration
import android.view.KeyEvent
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEvent as ComposeKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.roadbooknavigator.settings.R
import dev.giste.roadbooknavigator.settings.RemoteAction
import dev.giste.roadbooknavigator.settings.RemoteKey

/**
 * Whether a key with [keyCode] can be assigned to a remote action.
 *
 * Back and Escape cancel the capture, modifier keys (Shift, Alt, …) are only ever pressed with
 * other keys, and an unknown key code is no key at all. Home, the recent apps key and the power
 * key never reach an app.
 */
internal fun isCapturableKey(keyCode: Int): Boolean =
    keyCode > KeyEvent.KEYCODE_UNKNOWN &&
        keyCode != KeyEvent.KEYCODE_BACK &&
        keyCode != KeyEvent.KEYCODE_ESCAPE &&
        !KeyEvent.isModifierKey(keyCode)

/**
 * Asks the rider to press the remote key for [action], and passes it to [onKeyCaptured].
 *
 * The dialog takes the focus, so it gets every key, including the D-pad, volume and media keys,
 * which it consumes so that they do nothing else (no volume change, no music skipping). The
 * first press of a capturable key (see [isCapturableKey]) is the captured key; the caller closes
 * the dialog. Back, Escape, the cancel button and a tap outside call [onDismiss].
 *
 * @param action the action being assigned.
 * @param currentKeys the keys [action] has now, shown so the rider knows what changes.
 * @param onKeyCaptured called with the key the rider pressed.
 * @param onDismiss called when the rider cancels.
 * @param modifier applied to the dialog.
 */
@Composable
internal fun KeyCaptureDialog(
    action: RemoteAction,
    currentKeys: Set<RemoteKey>,
    onKeyCaptured: (RemoteKey) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicAlertDialog(onDismissRequest = onDismiss, modifier = modifier) {
        val dialogView = LocalView.current
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(dialogView, focusRequester) {
            // The dialog is usually opened with a tap, which leaves the app in touch mode, where
            // only a view focusable in touch mode can take the focus. Without the focus the keys
            // would not come here.
            dialogView.isFocusableInTouchMode = true
            focusRequester.requestFocus()
        }
        Surface(
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            modifier = Modifier
                .onPreviewKeyEvent { event -> handleKey(event, onKeyCaptured) }
                // Focused, so the keys come here; and the window has a focused view, so Android
                // does not spend the first D-pad press on leaving touch mode.
                .focusRequester(focusRequester)
                .focusable(),
        ) {
            KeyCaptureDialogBody(action = action, currentKeys = currentKeys, onDismiss = onDismiss)
        }
    }
}

/**
 * Handles a key [event] of the dialog: captures the first press of a capturable key, and consumes
 * the rest so that they do nothing else.
 *
 * @return whether [event] was consumed. Back and Escape are not, so the dialog cancels.
 */
private fun handleKey(event: ComposeKeyEvent, onKeyCaptured: (RemoteKey) -> Unit): Boolean {
    val keyCode = event.key.nativeKeyCode
    return when {
        keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE -> false
        isCapturableKey(keyCode) &&
            event.type == KeyEventType.KeyDown &&
            event.nativeKeyEvent.repeatCount == 0 -> {
            onKeyCaptured(RemoteKey(keyCode))
            true
        }
        // Modifier keys, and the release and repeats of the captured key.
        else -> true
    }
}

/**
 * What the dialog shows, laid out as an alert dialog: the action as the title, the request to press
 * a key, the keys the action has now, and the cancel button.
 */
@Composable
private fun KeyCaptureDialogBody(
    action: RemoteAction,
    currentKeys: Set<RemoteKey>,
    onDismiss: () -> Unit,
) {
    val dimensions = GisteTheme.dimensions
    Column(
        modifier = Modifier.padding(dimensions.spacingLarge),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium),
    ) {
        Text(
            text = action.label(),
            style = MaterialTheme.typography.headlineSmall,
            color = AlertDialogDefaults.titleContentColor,
        )
        Text(
            text = stringResource(R.string.settings_key_capture_message),
            style = MaterialTheme.typography.bodyLarge,
            color = AlertDialogDefaults.textContentColor,
        )
        val current = if (currentKeys.isEmpty()) {
            stringResource(R.string.settings_remote_no_key)
        } else {
            // Sorted by code, as in the settings screen.
            currentKeys.sortedBy { it.code }.map { it.label() }.joinToString()
        }
        Text(
            text = stringResource(R.string.settings_key_capture_current, current),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.End)
                .heightIn(min = dimensions.minTouchTarget),
        ) {
            Text(stringResource(R.string.settings_action_cancel))
        }
    }
}

// --- Previews ---

/** The app's text scale (see the settings screen previews). */
private const val APP_FONT_SCALE = 1.25f

@Preview(name = "One key, light", uiMode = Configuration.UI_MODE_NIGHT_NO, fontScale = APP_FONT_SCALE)
@Preview(name = "One key, dark", uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = APP_FONT_SCALE)
@Composable
private fun KeyCaptureDialogPreview() {
    GisteTheme {
        KeyCaptureDialog(
            action = RemoteAction.NEXT_WAYPOINT,
            currentKeys = setOf(RemoteKey(KeyEvent.KEYCODE_DPAD_UP)),
            onKeyCaptured = {},
            onDismiss = {},
        )
    }
}

@Preview(name = "No key, light", uiMode = Configuration.UI_MODE_NIGHT_NO, fontScale = APP_FONT_SCALE)
@Composable
private fun KeyCaptureDialogNoKeyPreview() {
    GisteTheme {
        KeyCaptureDialog(
            action = RemoteAction.RESET_PARTIAL,
            currentKeys = emptySet(),
            onKeyCaptured = {},
            onDismiss = {},
        )
    }
}
