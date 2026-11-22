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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.giste.core.ui.component.AutoSizeText
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.geo.Distance
import dev.giste.roadbooknavigator.R
import dev.giste.roadbooknavigator.ui.RiderTypography
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Landscape: share of the width taken by the title, the typed partial and the buttons. */
internal const val SET_PARTIAL_LANDSCAPE_INFO_WEIGHT: Float = 0.45f

/** Portrait: share of the height left for the typed partial, above the numpad. */
internal const val SET_PARTIAL_PORTRAIT_DISPLAY_WEIGHT: Float = 0.25f

/** Portrait: share of the height left for the numpad, below the typed partial. */
internal const val SET_PARTIAL_PORTRAIT_NUMPAD_WEIGHT: Float = 0.75f

/** The digit keys of the numpad by row, as on a phone keypad; the last row holds the zero. */
private val DIGIT_ROWS: List<List<Int>> = listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9))

/** Test tags of the set-partial dialog parts that tests cannot find by their own text. */
internal object SetPartialTestTags {

    /** The typed partial: a number, which a key with the same digit would also show. */
    const val DISPLAY: String = "set_partial_display"

    /** The delete key, which shows an icon instead of text. */
    const val DELETE: String = "set_partial_delete"
}

/**
 * Asks for a new partial, in kilometers, with a big-key numpad that riders can use with gloves.
 *
 * The keys follow [pressNumpadKey], so the typed text is always a partial the dashboard can show,
 * or empty. The set button is enabled only for a non-empty partial. Every key vibrates: a short
 * tick when it changes the partial, and a rejection when it is ignored (e.g. a third decimal).
 * The separator key shows the decimal separator of the locale.
 *
 * The dialog takes almost the whole screen, so the keys are as large as possible. Tapping outside
 * it does nothing, so a bump does not lose the typed partial; back and cancel close it.
 *
 * Stateless: the caller keeps [text], so it survives configuration changes.
 *
 * @param text what the rider has typed so far, with [PARTIAL_SEPARATOR] as the separator.
 * @param onTextChange called with the new text when a key changes it.
 * @param onConfirm called with the typed partial when the rider sets it. The caller closes the
 * dialog.
 * @param onDismiss called when the rider cancels or presses back.
 * @param modifier applied to the dialog's panel.
 */
@Composable
internal fun SetPartialDialog(
    text: String,
    onTextChange: (String) -> Unit,
    onConfirm: (Distance) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
        ),
    ) {
        SetPartialPanel(
            text = text,
            onTextChange = onTextChange,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            modifier = modifier
                .fillMaxSize()
                .padding(GisteTheme.dimensions.spacingLarge),
        )
    }
}

/**
 * The content of [SetPartialDialog], without the dialog window, so previews can show it.
 *
 * - **Landscape** (wider than tall): the title, the typed partial and the buttons on the left,
 *   and the numpad on the right.
 * - **Portrait** (otherwise): from top to bottom, the title, the typed partial, the numpad and
 *   the buttons.
 *
 * @param text what the rider has typed so far.
 * @param onTextChange called with the new text when a key changes it.
 * @param onConfirm called with the typed partial when the rider sets it.
 * @param onDismiss called when the rider cancels.
 * @param modifier applied to the panel.
 */
@Composable
private fun SetPartialPanel(
    text: String,
    onTextChange: (String) -> Unit,
    onConfirm: (Distance) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val input = parsePartialInput(text)
    val haptics = LocalHapticFeedback.current
    val onKey: (NumpadKey) -> Unit = { key ->
        val newText = pressNumpadKey(text, key)
        if (newText == text) {
            haptics.performHapticFeedback(HapticFeedbackType.Reject)
        } else {
            haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
            onTextChange(newText)
        }
    }
    val spacing = GisteTheme.dimensions.spacingMedium

    val title: @Composable () -> Unit = {
        Text(
            text = stringResource(R.string.app_odometer_set_partial_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
    val display: @Composable (Modifier) -> Unit = { displayModifier ->
        PartialDisplay(text = text, modifier = displayModifier)
    }
    val numpad: @Composable (Modifier) -> Unit = { numpadModifier ->
        Numpad(onKey = onKey, modifier = numpadModifier)
    }
    val actions: @Composable (Modifier) -> Unit = { actionsModifier ->
        Actions(
            canSet = input is PartialInput.Valid,
            onSet = { if (input is PartialInput.Valid) onConfirm(input.partial) },
            onCancel = onDismiss,
            modifier = actionsModifier,
        )
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
    ) {
        BoxWithConstraints(modifier = Modifier.padding(GisteTheme.dimensions.spacingLarge)) {
            if (maxWidth > maxHeight) {
                Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    Column(
                        modifier = Modifier
                            .weight(SET_PARTIAL_LANDSCAPE_INFO_WEIGHT)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        title()
                        display(Modifier.weight(1f).fillMaxWidth())
                        actions(Modifier.fillMaxWidth())
                    }
                    numpad(
                        Modifier
                            .weight(1f - SET_PARTIAL_LANDSCAPE_INFO_WEIGHT)
                            .fillMaxHeight(),
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
                    title()
                    display(Modifier.weight(SET_PARTIAL_PORTRAIT_DISPLAY_WEIGHT).fillMaxWidth())
                    numpad(Modifier.weight(SET_PARTIAL_PORTRAIT_NUMPAD_WEIGHT).fillMaxWidth())
                    actions(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/**
 * The typed partial with its unit, as large as its box allows. It is sized for the largest
 * partial, so the digits keep their size while typing. Empty text shows a muted zero.
 *
 * @param text what the rider has typed so far, with [PARTIAL_SEPARATOR] as the separator.
 * @param modifier applied to the display's box.
 */
@Composable
private fun PartialDisplay(text: String, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val value = text.ifEmpty { "0" }.replace(PARTIAL_SEPARATOR, locale.decimalSeparator())
    val widest = maxKilometersText(PARTIAL_DECIMALS, PARTIAL_INTEGER_DIGITS, locale)
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
            .testTag(SetPartialTestTags.DISPLAY),
        contentAlignment = Alignment.Center,
    ) {
        AutoSizeText(
            text = stringResource(R.string.app_odometer_kilometers, value),
            color = if (text.isEmpty()) {
                MaterialTheme.colorScheme.outline
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(GisteTheme.dimensions.spacingMedium),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            widestText = stringResource(R.string.app_odometer_kilometers, widest),
        )
    }
}

/**
 * The numpad: digits 1 to 9 in three rows, then the separator, 0 and delete. The keys share the
 * space they get, and are never smaller than the minimum touch target.
 *
 * @param onKey called with each pressed key.
 * @param modifier applied to the numpad's column.
 */
@Composable
private fun Numpad(onKey: (NumpadKey) -> Unit, modifier: Modifier = Modifier) {
    val spacing = GisteTheme.dimensions.spacingMedium
    val separator = LocalConfiguration.current.locales[0].decimalSeparator().toString()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        val rowModifier = Modifier.weight(1f).fillMaxWidth()
        DIGIT_ROWS.forEach { digits ->
            Row(modifier = rowModifier, horizontalArrangement = Arrangement.spacedBy(spacing)) {
                digits.forEach { digit ->
                    TextKey(label = "$digit", onClick = { onKey(NumpadKey.Digit(digit)) })
                }
            }
        }
        Row(modifier = rowModifier, horizontalArrangement = Arrangement.spacedBy(spacing)) {
            TextKey(label = separator, onClick = { onKey(NumpadKey.Separator) })
            TextKey(label = "0", onClick = { onKey(NumpadKey.Digit(0)) })
            Key(
                onClick = { onKey(NumpadKey.Delete) },
                modifier = Modifier.testTag(SetPartialTestTags.DELETE),
            ) {
                Icon(
                    painter = painterResource(R.drawable.app_ic_backspace),
                    contentDescription = null,
                    modifier = Modifier.size(GisteTheme.dimensions.minTouchTarget),
                )
            }
        }
    }
}

/**
 * A numpad key that shows [label].
 *
 * @param label the digit or separator on the key.
 * @param onClick called when the key is pressed.
 */
@Composable
private fun RowScope.TextKey(label: String, onClick: () -> Unit) {
    Key(onClick = onClick) {
        Text(
            text = label,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

/**
 * A numpad key: a third of its row, filling the row's height, and at least the minimum touch
 * target high.
 *
 * @param onClick called when the key is pressed.
 * @param modifier applied to the key.
 * @param content what the key shows.
 */
@Composable
private fun RowScope.Key(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .weight(1f)
            .fillMaxHeight()
            .heightIn(min = GisteTheme.dimensions.minTouchTarget),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

/**
 * The cancel and set buttons, side by side and at least the minimum touch target high.
 *
 * @param canSet whether the typed partial can be set.
 * @param onSet called when the rider sets the partial.
 * @param onCancel called when the rider cancels.
 * @param modifier applied to the buttons' row.
 */
@Composable
private fun Actions(
    canSet: Boolean,
    onSet: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonModifier = Modifier.heightIn(min = GisteTheme.dimensions.minTouchTarget)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(GisteTheme.dimensions.spacingMedium),
    ) {
        OutlinedButton(onClick = onCancel, modifier = buttonModifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_action_cancel),
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Button(onClick = onSet, enabled = canSet, modifier = buttonModifier.weight(1f)) {
            Text(
                text = stringResource(R.string.app_action_set),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

/** The decimal separator of this locale, e.g. `.` in English and `,` in Spanish. */
private fun Locale.decimalSeparator(): Char = DecimalFormatSymbols.getInstance(this).decimalSeparator

// --- Previews ---

/** Samsung Galaxy Tab Active3 in landscape: 1920 × 1200 px at 240 dpi. */
private const val TAB_ACTIVE3_LANDSCAPE = "spec:width=1280dp,height=800dp,dpi=240"

/** Samsung Galaxy Tab Active3 in portrait. */
private const val TAB_ACTIVE3_PORTRAIT = "spec:width=800dp,height=1280dp,dpi=240"

/** A typical phone in portrait. */
private const val PHONE_PORTRAIT = "spec:width=411dp,height=891dp,dpi=420"

/** A typical phone in landscape. */
private const val PHONE_LANDSCAPE = "spec:width=891dp,height=411dp,dpi=420"

/** The panel with [text] typed, filling the preview like the dialog fills the screen. */
@Composable
private fun SetPartialPreview(text: String) {
    // The app's text sizes, as on the device.
    GisteTheme(typography = RiderTypography) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim)) {
            SetPartialPanel(
                text = text,
                onTextChange = {},
                onConfirm = {},
                onDismiss = {},
                modifier = Modifier
                    .fillMaxSize()
                    .padding(GisteTheme.dimensions.spacingLarge),
            )
        }
    }
}

@Preview(name = "Tab Active3 landscape, light", device = TAB_ACTIVE3_LANDSCAPE, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Tab Active3 landscape, dark", device = TAB_ACTIVE3_LANDSCAPE, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tab Active3 portrait, light", device = TAB_ACTIVE3_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Tab Active3 portrait, dark", device = TAB_ACTIVE3_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SetPartialTypedPreview() {
    SetPartialPreview("12.35")
}

@Preview(name = "Phone portrait, empty, light", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Phone portrait, empty, dark", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Phone landscape, empty, light", device = PHONE_LANDSCAPE, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Phone landscape, empty, dark", device = PHONE_LANDSCAPE, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SetPartialEmptyPreview() {
    SetPartialPreview("")
}

@Preview(name = "Phone portrait, largest partial", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun SetPartialLargestPreview() {
    SetPartialPreview("999.99")
}
