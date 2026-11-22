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

package dev.giste.roadbooknavigator.ui.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.geo.Distance
import dev.giste.roadbooknavigator.R

/**
 * Asks for a new partial, in kilometers.
 *
 * The typed [text] is validated with [parsePartialInput] on every change: the set button is only
 * enabled for a valid partial, and an invalid one shows why below the field. Blank text is not an
 * error, it just cannot be set. The field takes the focus when the dialog opens, so the keyboard
 * comes up at once, and the keyboard's done key sets a valid partial too.
 *
 * Stateless: the caller keeps [text], so it survives configuration changes.
 *
 * @param text what the rider has typed so far.
 * @param onTextChange called with the new text on every edit.
 * @param onConfirm called with the typed partial when the rider sets a valid one. The caller closes
 * the dialog.
 * @param onDismiss called when the rider cancels, taps outside or presses back.
 * @param modifier applied to the dialog.
 */
@Composable
internal fun SetPartialDialog(
    text: String,
    onTextChange: (String) -> Unit,
    onConfirm: (Distance) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val input = parsePartialInput(text)
    val confirm = { if (input is PartialInput.Valid) onConfirm(input.partial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(R.string.app_odometer_set_partial_title)) },
        text = {
            val focusRequester = remember { FocusRequester() }
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                label = { Text(stringResource(R.string.app_odometer_set_partial_field_label)) },
                suffix = { Text(stringResource(R.string.app_odometer_unit_km)) },
                supportingText = (input as? PartialInput.Invalid)?.let { invalid ->
                    { Text(invalid.reason.toMessage()) }
                },
                isError = input is PartialInput.Invalid,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { confirm() }),
                singleLine = true,
            )
            // Here, in the dialog's own composition, so the field is attached when it runs.
            LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
        },
        confirmButton = {
            TextButton(onClick = confirm, enabled = input is PartialInput.Valid) {
                Text(stringResource(R.string.app_action_set))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.app_action_cancel))
            }
        },
    )
}

/** What to tell the rider about this rejected partial. */
@Composable
private fun PartialInputError.toMessage(): String = when (this) {
    PartialInputError.NOT_A_NUMBER ->
        stringResource(R.string.app_odometer_set_partial_error_not_a_number)
    PartialInputError.TOO_LARGE -> stringResource(
        R.string.app_odometer_set_partial_error_too_large,
        maxKilometersText(
            PARTIAL_DECIMALS,
            PARTIAL_INTEGER_DIGITS,
            LocalConfiguration.current.locales[0],
        ),
    )
    PartialInputError.TOO_MANY_DECIMALS -> pluralStringResource(
        R.plurals.app_odometer_set_partial_error_too_many_decimals,
        PARTIAL_DECIMALS,
        PARTIAL_DECIMALS,
    )
}

// --- Previews ---

/** The dialog with [text] typed. */
@Composable
private fun SetPartialDialogPreview(text: String) {
    GisteTheme {
        SetPartialDialog(text = text, onTextChange = {}, onConfirm = {}, onDismiss = {})
    }
}

@Preview(name = "Empty, light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Empty, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SetPartialDialogEmptyPreview() {
    SetPartialDialogPreview("")
}

@Preview(name = "Valid, light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Valid, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SetPartialDialogValidPreview() {
    SetPartialDialogPreview("12.35")
}

@Preview(name = "Not a number, light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Not a number, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SetPartialDialogNotANumberPreview() {
    SetPartialDialogPreview("12..3")
}

@Preview(name = "Too large, light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Too large, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SetPartialDialogTooLargePreview() {
    SetPartialDialogPreview("1234")
}

@Preview(name = "Too many decimals, light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Too many decimals, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SetPartialDialogTooManyDecimalsPreview() {
    SetPartialDialogPreview("12.345")
}
