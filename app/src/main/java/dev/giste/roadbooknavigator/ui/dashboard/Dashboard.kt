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
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.giste.core.ui.component.AutoSizeText
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.geo.Distance
import dev.giste.map.MapViewer
import dev.giste.roadbook.RoadbookViewer
import dev.giste.roadbooknavigator.R
import dev.giste.roadbooknavigator.ui.InterceptKeyEvents
import dev.giste.roadbooknavigator.ui.RiderTypography

/** Landscape: share of the width taken by the side column (total, partial and map). */
internal const val LANDSCAPE_SIDE_COLUMN_WEIGHT: Float = 0.3f

/** Landscape: share of the side column's height taken by the total. */
internal const val LANDSCAPE_TOTAL_WEIGHT: Float = 0.12f

/** Landscape: share of the side column's height taken by the partial. */
internal const val LANDSCAPE_PARTIAL_WEIGHT: Float = 0.14f

/** Landscape: share of the side column's height taken by the map, below the distances. */
internal const val LANDSCAPE_MAP_WEIGHT: Float = 0.74f

/** Portrait: share of the height taken by the row of distances, at the top. */
internal const val PORTRAIT_DISTANCES_WEIGHT: Float = 0.08f

/** Portrait: share of the height taken by the map, below the distances. */
internal const val PORTRAIT_MAP_WEIGHT: Float = 0.32f

/** Portrait: share of the height taken by the roadbook, at the bottom. */
internal const val PORTRAIT_ROADBOOK_WEIGHT: Float = 0.6f

/** Portrait: share of the distances row's width taken by the total, on the left. */
internal const val PORTRAIT_TOTAL_WEIGHT: Float = 0.45f

/** Portrait: share of the distances row's width taken by the partial, on the right. */
internal const val PORTRAIT_PARTIAL_WEIGHT: Float = 0.55f

/** Share of the settings button taken by the gear, which leaves a margin around it. */
internal const val SETTINGS_ICON_FRACTION: Float = 0.7f

/** Test tags of the dashboard parts that show no text of their own to find them by. */
internal object DashboardTestTags {

    /** The partial reading. */
    const val PARTIAL: String = "dashboard_partial"

    /** The total reading. */
    const val TOTAL: String = "dashboard_total"

    /** The settings button, which shows an icon instead of text. */
    const val SETTINGS: String = "dashboard_settings"
}

/**
 * The main screen: the odometer, the map and the roadbook, with their state from Hilt.
 *
 * Showing it makes the odometer measure (see [DashboardViewModel]), so it must only be shown once
 * the location permission is granted.
 *
 * While it is shown, the keys of the remote control act on it (see [DashboardViewModel.onKeyPress]),
 * before the system gets them. Elsewhere, and while one of its dialogs is open, they do nothing
 * special. The dashboard takes the focus, so the window always has a focused view: otherwise,
 * after any touch, Android spends the next D-pad key (the DND2 remote's keys) on leaving touch
 * mode, before the activity sees it, and that press is lost.
 *
 * @param onOpenSettings called when the rider opens the settings.
 * @param onManageMaps called when the rider opens the map manager from the map.
 * @param modifier applied to the root layout.
 */
@Composable
internal fun Dashboard(onOpenSettings: () -> Unit, onManageMaps: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: DashboardViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InterceptKeyEvents { event -> event.toKeyPress()?.let(viewModel::onKeyPress) ?: false }
    val focusRequester = remember { FocusRequester() }
    // Also when it comes back from the settings.
    LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
    DashboardContent(
        state = state,
        onSetPartial = viewModel::setPartial,
        onResetAll = viewModel::resetAll,
        onOpenSettings = onOpenSettings,
        // Keeps the window focused, so no D-pad key is spent leaving touch mode.
        modifier = modifier.focusRequester(focusRequester).focusable(),
        roadbook = { showImportButton, roadbookModifier ->
            RoadbookViewer(showImportButton = showImportButton, modifier = roadbookModifier)
        },
        map = { showManageButton, mapModifier ->
            MapViewer(modifier = mapModifier, showManageButton = showManageButton, onManageMaps = onManageMaps)
        },
    )
}

/**
 * The dashboard for [state], laid out for the space it gets.
 *
 * - **Landscape** (wider than tall): a side column on the left with the total, the partial and the
 *   map, from top to bottom, and the roadbook on the right.
 * - **Portrait** (otherwise): from top to bottom, the total and the partial side by side, the map
 *   and the roadbook.
 *
 * The orientation comes from the size the dashboard is given, not from the window or the device,
 * so it also works in split screen and in previews. The shares of each part are the `LANDSCAPE_*`
 * and `PORTRAIT_*` constants.
 *
 * The distances have no visible label, so their digits can be as large as their boxes allow. Each is
 * sized for its largest value (999.99 km for the partial, 9,999.9 km for the total), so the digits
 * keep their size as the distance grows, and a longer distance is shown as that largest value.
 * Tests find them by their [DashboardTestTags].
 *
 * Long-pressing a distance opens its control: the partial opens [SetPartialDialog], and the total
 * opens [ResetAllDialog], which asks before resetting both distances. Both work while the distances
 * are still loading, because the odometer applies commands in order. Which dialog is open, and the
 * text typed in it, survive configuration changes.
 *
 * The settings button sits on the start side of the total, in a square slot of its own that the
 * total's background covers, and its gear grows with the slot. It is shown only while [DashboardUiState.showStoppedActions] is set, so a touch
 * while riding cannot leave the dashboard (which would stop the odometer). While moving, the slot
 * stays empty instead of going away, so the total keeps its size and place. The roadbook offers its
 * import button, and the map its manage button, under the same condition.
 *
 * The screen stays on while the dashboard is in the composition, and may go off again once it
 * leaves it.
 *
 * @param state what to show.
 * @param onSetPartial called with the new partial when the rider sets it.
 * @param onResetAll called when the rider confirms resetting the partial and the total.
 * @param onOpenSettings called when the rider presses the settings button.
 * @param modifier applied to the root layout.
 * @param roadbook draws the roadbook in the modifier it is given, and shows its import button only
 * when told to. A slot, so tests and previews can use a placeholder instead of the Hilt-backed
 * viewer.
 * @param map draws the map in the modifier it is given, and shows its manage button when told to
 * (it may show it anyway when it has no map to draw). A slot for the same reason.
 */
@Composable
internal fun DashboardContent(
    state: DashboardUiState,
    onSetPartial: (Distance) -> Unit,
    onResetAll: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    roadbook: @Composable (showImportButton: Boolean, modifier: Modifier) -> Unit,
    map: @Composable (showManageButton: Boolean, modifier: Modifier) -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val loading = stringResource(R.string.app_odometer_value_loading)
    val (partial, total) = when (val odometer = state.odometer) {
        OdometerUiState.Loading -> loading to loading
        is OdometerUiState.Available ->
            odometer.partial.formatKilometers(PARTIAL_DECIMALS, PARTIAL_INTEGER_DIGITS, locale) to
                odometer.total.formatKilometers(TOTAL_DECIMALS, TOTAL_INTEGER_DIGITS, locale)
    }
    // The digits are sized for the largest value, so they keep their size as the distance grows.
    val widestPartial = maxKilometersText(PARTIAL_DECIMALS, PARTIAL_INTEGER_DIGITS, locale)
    val widestTotal = maxKilometersText(TOTAL_DECIMALS, TOTAL_INTEGER_DIGITS, locale)
    var openDialog by rememberSaveable { mutableStateOf<OdometerDialog?>(null) }
    var partialText by rememberSaveable { mutableStateOf("") }

    // The total is plain; the partial, which the rider looks at most, is highlighted. The settings
    // button has a fixed place on the total's start side, in a slot kept empty while moving, so the
    // total never changes size when the button comes and goes.
    val totalReading: @Composable (Modifier) -> Unit = { readingModifier ->
        Row(modifier = readingModifier.background(MaterialTheme.colorScheme.surface)) {
            SettingsSlot(
                showButton = state.showStoppedActions,
                onOpenSettings = onOpenSettings,
                modifier = Modifier.fillMaxHeight(),
            )
            DistanceReading(
                value = total,
                widestValue = widestTotal,
                color = MaterialTheme.colorScheme.onSurface,
                highlighted = false,
                onLongPress = { openDialog = OdometerDialog.RESET_ALL },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .testTag(DashboardTestTags.TOTAL),
            )
        }
    }
    val partialReading: @Composable (Modifier) -> Unit = { readingModifier ->
        DistanceReading(
            value = partial,
            widestValue = widestPartial,
            color = MaterialTheme.colorScheme.onPrimary,
            highlighted = true,
            onLongPress = {
                // Each time the dialog opens, it starts empty.
                partialText = ""
                openDialog = OdometerDialog.SET_PARTIAL
            },
            modifier = readingModifier
                .background(MaterialTheme.colorScheme.primary)
                .testTag(DashboardTestTags.PARTIAL),
        )
    }
    val roadbookSlot: @Composable (Modifier) -> Unit = { roadbookModifier ->
        roadbook(state.showStoppedActions, roadbookModifier)
    }
    val mapSlot: @Composable (Modifier) -> Unit = { mapModifier ->
        map(state.showStoppedActions, mapModifier)
    }

    // The rider reads the dashboard in glances, without touching it, so the screen must not go off
    // while it is shown. Elsewhere (the settings, a coffee stop) the screen may sleep as usual.
    BoxWithConstraints(
        modifier = modifier
            .keepScreenOn()
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (maxWidth > maxHeight) {
            LandscapeLayout(
                total = totalReading,
                partial = partialReading,
                map = mapSlot,
                roadbook = roadbookSlot,
            )
        } else {
            PortraitLayout(
                total = totalReading,
                partial = partialReading,
                map = mapSlot,
                roadbook = roadbookSlot,
            )
        }
    }

    when (openDialog) {
        OdometerDialog.SET_PARTIAL -> SetPartialDialog(
            text = partialText,
            onTextChange = { partialText = it },
            onConfirm = { partial ->
                openDialog = null
                onSetPartial(partial)
            },
            onDismiss = { openDialog = null },
        )
        OdometerDialog.RESET_ALL -> ResetAllDialog(
            onConfirm = {
                openDialog = null
                onResetAll()
            },
            onDismiss = { openDialog = null },
        )
        null -> Unit
    }
}

/** The odometer dialogs [DashboardContent] can show, one at a time. */
private enum class OdometerDialog {

    /** [SetPartialDialog], opened by long-pressing the partial. */
    SET_PARTIAL,

    /** [ResetAllDialog], opened by long-pressing the total. */
    RESET_ALL,
}

/**
 * The landscape layout: total, partial and map stacked in a side column, roadbook on the right.
 *
 * @param total draws the total in the modifier it is given.
 * @param partial draws the partial in the modifier it is given.
 * @param map draws the map in the modifier it is given.
 * @param roadbook draws the roadbook in the modifier it is given.
 */
@Composable
private fun LandscapeLayout(
    total: @Composable (Modifier) -> Unit,
    partial: @Composable (Modifier) -> Unit,
    map: @Composable (Modifier) -> Unit,
    roadbook: @Composable (Modifier) -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.weight(LANDSCAPE_SIDE_COLUMN_WEIGHT).fillMaxHeight()) {
            total(Modifier.weight(LANDSCAPE_TOTAL_WEIGHT).fillMaxWidth())
            partial(Modifier.weight(LANDSCAPE_PARTIAL_WEIGHT).fillMaxWidth())
            map(Modifier.weight(LANDSCAPE_MAP_WEIGHT).fillMaxWidth())
        }
        roadbook(Modifier.weight(1f - LANDSCAPE_SIDE_COLUMN_WEIGHT).fillMaxHeight())
    }
}

/**
 * The portrait layout: total and partial side by side, then the map, then the roadbook.
 *
 * @param total draws the total in the modifier it is given.
 * @param partial draws the partial in the modifier it is given.
 * @param map draws the map in the modifier it is given.
 * @param roadbook draws the roadbook in the modifier it is given.
 */
@Composable
private fun PortraitLayout(
    total: @Composable (Modifier) -> Unit,
    partial: @Composable (Modifier) -> Unit,
    map: @Composable (Modifier) -> Unit,
    roadbook: @Composable (Modifier) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.weight(PORTRAIT_DISTANCES_WEIGHT).fillMaxWidth()) {
            total(Modifier.weight(PORTRAIT_TOTAL_WEIGHT).fillMaxHeight())
            partial(Modifier.weight(PORTRAIT_PARTIAL_WEIGHT).fillMaxHeight())
        }
        map(Modifier.weight(PORTRAIT_MAP_WEIGHT).fillMaxWidth())
        roadbook(Modifier.weight(PORTRAIT_ROADBOOK_WEIGHT).fillMaxWidth())
    }
}

/**
 * The settings button's place, on the start side of the total: a square as tall as the total's
 * box, so the gear grows with the space, but never narrower than the minimum touch target. It
 * keeps its size whether the button is shown or not.
 *
 * @param showButton whether to show the button; otherwise the slot stays empty.
 * @param onOpenSettings called when the button is pressed.
 * @param modifier applied to the root layout. It must give the slot a bounded height.
 */
@Composable
private fun SettingsSlot(
    showButton: Boolean,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val minWidth = GisteTheme.dimensions.minTouchTarget
    Box(
        modifier = modifier.layout { measurable, constraints ->
            // As wide as tall, or the minimum touch target on boxes shorter than that.
            val height = constraints.maxHeight
            val width = maxOf(height, minWidth.roundToPx())
            val placeable = measurable.measure(Constraints.fixed(width, height))
            layout(width, height) { placeable.place(0, 0) }
        },
        contentAlignment = Alignment.Center,
    ) {
        if (showButton) SettingsButton(onClick = onOpenSettings, modifier = Modifier.fillMaxSize())
    }
}

/**
 * The button that opens the settings: a gear in the primary color, with no container. It takes the
 * size [modifier] gives it, and the gear fills [SETTINGS_ICON_FRACTION] of it, so a larger box
 * shows a larger gear. Without a container it reads as part of the total's box rather than as a
 * separate control, as in the previous app; the primary color keeps it easy to spot.
 *
 * It never takes the focus. It is meant to be touched, and the remote's keys act on the dashboard as
 * a whole. Otherwise, in key mode (after a key press), an arrow or Tab key not bound to a remote
 * action would move the focus onto it: it would draw its focus circle, and an Enter or centre key
 * would leave the dashboard and stop the odometer.
 *
 * @param onClick called when the button is pressed.
 * @param modifier applied to the button. It sets the button's size.
 */
@Composable
private fun SettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .testTag(DashboardTestTags.SETTINGS)
            .focusProperties { canFocus = false },
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
        ),
    ) {
        Icon(
            painter = painterResource(R.drawable.app_ic_settings),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(SETTINGS_ICON_FRACTION),
        )
    }
}

/**
 * One distance, as large as its box allows, centered vertically and without a visible label.
 *
 * The long press uses a plain gesture detector instead of `combinedClickable`, so a tap does
 * nothing and shows no ripple. It vibrates, so the rider feels it was taken even with gloves.
 *
 * @param value the formatted distance.
 * @param widestValue the widest [value] can be, which sets the font size for every value.
 * @param color the text color, readable on the background set by [modifier].
 * @param highlighted whether to draw the value in bold.
 * @param onLongPress called when the reading is long-pressed.
 * @param modifier applied to the root layout. It gives the reading its box and background.
 */
@Composable
private fun DistanceReading(
    value: String,
    widestValue: String,
    color: Color,
    highlighted: Boolean,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    // The gesture detector keeps running across recompositions, so it reads the latest callback.
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentOnLongPress()
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        AutoSizeText(
            text = value,
            color = color,
            // As tall as its line and centered: the digits are usually limited by the width, so the
            // line is shorter than the box.
            modifier = Modifier
                .fillMaxSize()
                .padding(GisteTheme.dimensions.spacingExtraSmall)
                .wrapContentHeight(),
            fontWeight = if (highlighted) FontWeight.Bold else null,
            textAlign = TextAlign.End,
            widestText = widestValue,
        )
    }
}

/**
 * Stands in for the map in previews, where `:map`'s OpenGL map cannot be drawn: an empty area in
 * the surface variant color.
 *
 * @param modifier applied to the root layout.
 */
@Composable
private fun MapPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant))
}

// --- Previews ---

/** Samsung Galaxy Tab Active3 in landscape: 1920 × 1200 px at 240 dpi. */
private const val TAB_ACTIVE3_LANDSCAPE = "spec:width=1280dp,height=800dp,dpi=240"

/** Samsung Galaxy Tab Active3 in portrait. */
private const val TAB_ACTIVE3_PORTRAIT = "spec:width=800dp,height=1280dp,dpi=240"

/** A typical phone in portrait. */
private const val PHONE_PORTRAIT = "spec:width=411dp,height=891dp,dpi=420"

/** A typical phone in landscape. */
private const val PHONE_LANDSCAPE = "spec:width=891dp,height=411dp,dpi=420"

/**
 * The largest distances shown (999.99 and 9,999.9), so the previews show the digits at the size
 * they keep however long the distances get.
 */
private val LARGEST_DISTANCES = OdometerUiState.Available(
    partial = Distance.meters(999_990.0),
    total = Distance.meters(9_999_900.0),
)

/** Preview sample: stopped, with the settings and import buttons. */
private val STOPPED = DashboardUiState(odometer = LARGEST_DISTANCES, showStoppedActions = true)

/** Preview sample: moving, without the settings and import buttons. */
private val MOVING = DashboardUiState(odometer = LARGEST_DISTANCES, showStoppedActions = false)

/** The dashboard in [state], with placeholders instead of the roadbook and the map. */
@Composable
private fun DashboardPreview(state: DashboardUiState) {
    // The app's text sizes, as on the device.
    GisteTheme(typography = RiderTypography) {
        DashboardContent(
            state = state,
            onSetPartial = {},
            onResetAll = {},
            onOpenSettings = {},
            roadbook = { showImportButton, modifier ->
                Box(modifier = modifier, contentAlignment = Alignment.Center) {
                    Text(
                        text = "Roadbook (import button shown: $showImportButton)",
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            },
            map = { _, modifier -> MapPlaceholder(modifier) },
        )
    }
}

@Preview(name = "Tab Active3 landscape, stopped, light", device = TAB_ACTIVE3_LANDSCAPE, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Tab Active3 landscape, stopped, dark", device = TAB_ACTIVE3_LANDSCAPE, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tab Active3 portrait, stopped, light", device = TAB_ACTIVE3_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Tab Active3 portrait, stopped, dark", device = TAB_ACTIVE3_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DashboardTabletStoppedPreview() {
    DashboardPreview(STOPPED)
}

@Preview(name = "Phone portrait, stopped, light", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Phone portrait, stopped, dark", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Phone landscape, stopped, light", device = PHONE_LANDSCAPE, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Phone landscape, stopped, dark", device = PHONE_LANDSCAPE, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DashboardPhoneStoppedPreview() {
    DashboardPreview(STOPPED)
}

@Preview(name = "Tab Active3 landscape, moving, light", device = TAB_ACTIVE3_LANDSCAPE, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Tab Active3 landscape, moving, dark", device = TAB_ACTIVE3_LANDSCAPE, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Phone portrait, moving, light", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Phone portrait, moving, dark", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DashboardMovingPreview() {
    DashboardPreview(MOVING)
}
