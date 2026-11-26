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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.geo.Distance
import dev.giste.map.MapTilt
import dev.giste.map.MapZoomLevel
import dev.giste.roadbook.RoadbookStyle
import dev.giste.roadbook.ShortDistanceThreshold
import dev.giste.roadbooknavigator.settings.R
import dev.giste.roadbooknavigator.settings.RemoteAction
import dev.giste.roadbooknavigator.settings.RemoteKey
import dev.giste.roadbooknavigator.settings.RemoteKeys
import dev.giste.roadbooknavigator.settings.RemoteModel
import dev.giste.roadbooknavigator.settings.ScreenOrientation
import dev.giste.roadbooknavigator.settings.ThemeMode
import kotlin.math.roundToInt

/** Test tags of the settings screen parts that tests cannot find by their own text. */
internal object SettingsTestTags {

    /** The back button, which shows an icon instead of text. */
    const val BACK: String = "settings_back"

    /** The short-distance threshold's stepper, whose − and + repeat in the other steppers. */
    const val SHORT_DISTANCE_THRESHOLD_STEPPER: String = "settings_short_distance_threshold_stepper"

    /** The map zoom's stepper. */
    const val MAP_ZOOM_STEPPER: String = "settings_map_zoom_stepper"

    /** The map tilt's stepper. */
    const val MAP_TILT_STEPPER: String = "settings_map_tilt_stepper"

    /** The button that restores the short-distance threshold's default, an icon without text. */
    const val SHORT_DISTANCE_THRESHOLD_RESTORE: String = "settings_short_distance_threshold_restore"

    /** The button that restores the map zoom's default. */
    const val MAP_ZOOM_RESTORE: String = "settings_map_zoom_restore"

    /** The button that restores the map tilt's default. */
    const val MAP_TILT_RESTORE: String = "settings_map_tilt_restore"

    /**
     * The row of one remote action and its keys, whose texts may repeat in other rows (e.g. "No
     * key").
     *
     * @param action the action of the row.
     * @return the tag of its row, e.g. `settings_key_binding_next_waypoint`.
     */
    fun keyBinding(action: RemoteAction): String =
        "settings_key_binding_${action.name.lowercase()}"
}

/**
 * The settings screen for [state]: a top bar with a back button, then the appearance, remote
 * control, roadbook and map sections, and an About section that opens the app's About screen.
 * Every control is at least the minimum touch target high, so it works with gloves.
 *
 * - **Theme:** system, light or dark.
 * - **Screen orientation:** system, vertical or horizontal.
 * - **Remote:** DND2, Terra Pirata or custom, and the keys of each action, on a button. For DND2
 *   and Terra Pirata the buttons are disabled (grayed), since their keys cannot change. With
 *   custom, each button opens [KeyCaptureDialog], which assigns the key the rider presses; which
 *   action's dialog is open survives configuration changes.
 * - **Roadbook colors:** the app theme or the FIA palette.
 * - **Highlight close waypoints:** the short-distance threshold, changed in steps with − and +.
 * - **Map zoom** and **map tilt:** how close the map is shown and how much it leans, changed in
 *   steps with − and + within the ranges the app offers.
 * - Each of these three has a restore icon at the end of its title, which sets it back to its
 *   default at once. It is disabled (grayed) while the value already is the default.
 * - **About and licences:** a button that opens the About screen.
 *
 * When [SettingsUiState.Loaded.saveFailed] is set, a snackbar says the change was not stored, and
 * [onSaveErrorShown] is called once it has been shown. Likewise, when
 * [SettingsUiState.Loaded.keyMoved] is set, a snackbar says which action lost its key, and
 * [onKeyMoveShown] is called once it has been shown. While loading, only the top bar is shown.
 *
 * @param state what to show.
 * @param onBack called when the back button is pressed.
 * @param onThemeModeChange called with the theme mode the user picks.
 * @param onOrientationChange called with the screen orientation the user picks.
 * @param onRemoteModelChange called with the remote model the user picks.
 * @param onAssignCustomKey called with a custom action and the key the user pressed for it.
 * @param onRoadbookStyleChange called with the roadbook style the user picks.
 * @param onDecreaseShortDistanceThreshold called when the threshold's − is pressed.
 * @param onIncreaseShortDistanceThreshold called when the threshold's + is pressed.
 * @param onRestoreShortDistanceThreshold called when the threshold's restore button is pressed.
 * @param onDecreaseMapZoom called when the map zoom's − is pressed.
 * @param onIncreaseMapZoom called when the map zoom's + is pressed.
 * @param onRestoreMapZoom called when the map zoom's restore button is pressed.
 * @param onDecreaseMapTilt called when the map tilt's − is pressed.
 * @param onIncreaseMapTilt called when the map tilt's + is pressed.
 * @param onRestoreMapTilt called when the map tilt's restore button is pressed.
 * @param onSaveErrorShown called once the save error has been shown.
 * @param onKeyMoveShown called once the notice of a moved key has been shown.
 * @param onOpenAbout called when the About button is pressed.
 * @param modifier applied to the root layout.
 */
@Composable
internal fun SettingsContent(
    state: SettingsUiState,
    onBack: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOrientationChange: (ScreenOrientation) -> Unit,
    onRemoteModelChange: (RemoteModel) -> Unit,
    onAssignCustomKey: (RemoteAction, RemoteKey) -> Unit,
    onRoadbookStyleChange: (RoadbookStyle) -> Unit,
    onDecreaseShortDistanceThreshold: () -> Unit,
    onIncreaseShortDistanceThreshold: () -> Unit,
    onRestoreShortDistanceThreshold: () -> Unit,
    onDecreaseMapZoom: () -> Unit,
    onIncreaseMapZoom: () -> Unit,
    onRestoreMapZoom: () -> Unit,
    onDecreaseMapTilt: () -> Unit,
    onIncreaseMapTilt: () -> Unit,
    onRestoreMapTilt: () -> Unit,
    onSaveErrorShown: () -> Unit,
    onKeyMoveShown: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val saveFailed = (state as? SettingsUiState.Loaded)?.saveFailed == true
    val saveErrorMessage = stringResource(R.string.settings_error_not_saved)
    LaunchedEffect(saveFailed) {
        if (saveFailed) {
            snackbarHostState.showSnackbar(saveErrorMessage)
            onSaveErrorShown()
        }
    }
    val keyMoved = (state as? SettingsUiState.Loaded)?.keyMoved
    val keyMovedMessage = keyMoved?.let {
        stringResource(R.string.settings_key_moved, it.key.label(), it.from.label(), it.to.label())
    }
    LaunchedEffect(keyMovedMessage) {
        if (keyMovedMessage != null) {
            snackbarHostState.showSnackbar(keyMovedMessage)
            onKeyMoveShown()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(GisteTheme.dimensions.minTouchTarget)
                            .testTag(SettingsTestTags.BACK),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.settings_ic_arrow_back),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (state is SettingsUiState.Loaded) {
            SettingsSections(
                state = state,
                onThemeModeChange = onThemeModeChange,
                onOrientationChange = onOrientationChange,
                onRemoteModelChange = onRemoteModelChange,
                onAssignCustomKey = onAssignCustomKey,
                onRoadbookStyleChange = onRoadbookStyleChange,
                onDecreaseShortDistanceThreshold = onDecreaseShortDistanceThreshold,
                onIncreaseShortDistanceThreshold = onIncreaseShortDistanceThreshold,
                onRestoreShortDistanceThreshold = onRestoreShortDistanceThreshold,
                onDecreaseMapZoom = onDecreaseMapZoom,
                onIncreaseMapZoom = onIncreaseMapZoom,
                onRestoreMapZoom = onRestoreMapZoom,
                onDecreaseMapTilt = onDecreaseMapTilt,
                onIncreaseMapTilt = onIncreaseMapTilt,
                onRestoreMapTilt = onRestoreMapTilt,
                onOpenAbout = onOpenAbout,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }
}

/**
 * The sections of the loaded settings, in a scrolling column.
 *
 * @param state the loaded settings.
 * @param onThemeModeChange called with the theme mode the user picks.
 * @param onOrientationChange called with the screen orientation the user picks.
 * @param onRemoteModelChange called with the remote model the user picks.
 * @param onAssignCustomKey called with a custom action and the key the user pressed for it.
 * @param onRoadbookStyleChange called with the roadbook style the user picks.
 * @param onDecreaseShortDistanceThreshold called when the threshold's − is pressed.
 * @param onIncreaseShortDistanceThreshold called when the threshold's + is pressed.
 * @param onRestoreShortDistanceThreshold called when the threshold's restore button is pressed.
 * @param onDecreaseMapZoom called when the map zoom's − is pressed.
 * @param onIncreaseMapZoom called when the map zoom's + is pressed.
 * @param onRestoreMapZoom called when the map zoom's restore button is pressed.
 * @param onDecreaseMapTilt called when the map tilt's − is pressed.
 * @param onIncreaseMapTilt called when the map tilt's + is pressed.
 * @param onRestoreMapTilt called when the map tilt's restore button is pressed.
 * @param onOpenAbout called when the About button is pressed.
 * @param modifier applied to the column.
 */
@Composable
private fun SettingsSections(
    state: SettingsUiState.Loaded,
    onThemeModeChange: (ThemeMode) -> Unit,
    onOrientationChange: (ScreenOrientation) -> Unit,
    onRemoteModelChange: (RemoteModel) -> Unit,
    onAssignCustomKey: (RemoteAction, RemoteKey) -> Unit,
    onRoadbookStyleChange: (RoadbookStyle) -> Unit,
    onDecreaseShortDistanceThreshold: () -> Unit,
    onIncreaseShortDistanceThreshold: () -> Unit,
    onRestoreShortDistanceThreshold: () -> Unit,
    onDecreaseMapZoom: () -> Unit,
    onIncreaseMapZoom: () -> Unit,
    onRestoreMapZoom: () -> Unit,
    onDecreaseMapTilt: () -> Unit,
    onIncreaseMapTilt: () -> Unit,
    onRestoreMapTilt: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimensions = GisteTheme.dimensions
    // The action whose key is being captured, if any.
    var capturing by rememberSaveable { mutableStateOf<RemoteAction?>(null) }
    val editable = state.remoteModel == RemoteModel.CUSTOM
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(dimensions.spacingMedium),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingMedium),
    ) {
        SectionTitle(stringResource(R.string.settings_section_appearance))
        SettingTitle(stringResource(R.string.settings_theme_title))
        Choices(
            options = ThemeMode.entries,
            selected = state.themeMode,
            label = { it.label() },
            onSelect = onThemeModeChange,
        )
        SettingTitle(stringResource(R.string.settings_orientation_title))
        Choices(
            options = ScreenOrientation.entries,
            selected = state.orientation,
            label = { it.label() },
            onSelect = onOrientationChange,
        )

        SectionTitle(stringResource(R.string.settings_section_remote))
        SettingTitle(stringResource(R.string.settings_remote_model_title))
        Choices(
            options = RemoteModel.entries,
            selected = state.remoteModel,
            label = { it.label() },
            onSelect = onRemoteModelChange,
        )
        SettingTitle(stringResource(R.string.settings_remote_keys_title))
        if (editable) {
            Text(
                text = stringResource(R.string.settings_remote_keys_custom_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RemoteAction.entries.forEach { action ->
            KeyBinding(
                action = action,
                keys = state.remoteKeys.keysFor(action),
                editable = editable,
                onClick = { capturing = action },
            )
        }

        SectionTitle(stringResource(R.string.settings_section_roadbook))
        SettingTitle(stringResource(R.string.settings_roadbook_style_title))
        Choices(
            options = RoadbookStyle.entries,
            selected = state.roadbookStyle,
            label = { it.label() },
            onSelect = onRoadbookStyleChange,
        )
        RestorableSettingTitle(
            text = stringResource(R.string.settings_short_distance_title),
            canRestore = state.canRestoreShortDistanceThreshold,
            onRestore = onRestoreShortDistanceThreshold,
            restoreTag = SettingsTestTags.SHORT_DISTANCE_THRESHOLD_RESTORE,
        )
        Text(
            text = stringResource(R.string.settings_short_distance_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Stepper(
            value = stringResource(
                R.string.settings_distance_meters,
                state.shortDistanceThreshold.distance.inMeters.roundToInt(),
            ),
            canDecrease = state.canDecreaseShortDistanceThreshold,
            canIncrease = state.canIncreaseShortDistanceThreshold,
            onDecrease = onDecreaseShortDistanceThreshold,
            onIncrease = onIncreaseShortDistanceThreshold,
            modifier = Modifier.testTag(SettingsTestTags.SHORT_DISTANCE_THRESHOLD_STEPPER),
        )

        SectionTitle(stringResource(R.string.settings_section_map))
        RestorableSettingTitle(
            text = stringResource(R.string.settings_map_zoom_title),
            canRestore = state.canRestoreMapZoom,
            onRestore = onRestoreMapZoom,
            restoreTag = SettingsTestTags.MAP_ZOOM_RESTORE,
        )
        Text(
            text = stringResource(R.string.settings_map_zoom_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Stepper(
            value = stringResource(R.string.settings_map_zoom_level, state.mapZoomLevel.value),
            canDecrease = state.canDecreaseMapZoom,
            canIncrease = state.canIncreaseMapZoom,
            onDecrease = onDecreaseMapZoom,
            onIncrease = onIncreaseMapZoom,
            modifier = Modifier.testTag(SettingsTestTags.MAP_ZOOM_STEPPER),
        )
        RestorableSettingTitle(
            text = stringResource(R.string.settings_map_tilt_title),
            canRestore = state.canRestoreMapTilt,
            onRestore = onRestoreMapTilt,
            restoreTag = SettingsTestTags.MAP_TILT_RESTORE,
        )
        Text(
            text = stringResource(R.string.settings_map_tilt_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Stepper(
            value = stringResource(R.string.settings_degrees, state.mapTilt.inDegrees.roundToInt()),
            canDecrease = state.canDecreaseMapTilt,
            canIncrease = state.canIncreaseMapTilt,
            onDecrease = onDecreaseMapTilt,
            onIncrease = onIncreaseMapTilt,
            modifier = Modifier.testTag(SettingsTestTags.MAP_TILT_STEPPER),
        )

        SectionTitle(stringResource(R.string.settings_section_about))
        AboutButton(onClick = onOpenAbout)
    }

    capturing?.let { action ->
        KeyCaptureDialog(
            action = action,
            currentKeys = state.remoteKeys.keysFor(action),
            onKeyCaptured = { key ->
                capturing = null
                onAssignCustomKey(action, key)
            },
            onDismiss = { capturing = null },
        )
    }
}

/** The title of a group of settings. */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

/** The name of one setting. */
@Composable
private fun SettingTitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleMedium)
}

/**
 * The name of one setting with a restore icon at the end of the row, which sets the setting back
 * to its default. The icon sits on the title row rather than next to the stepper, because a phone
 * is too narrow for − value + and a fourth button at the touch target size.
 *
 * @param text the setting's name.
 * @param canRestore whether the value differs from the default. If not, the button is disabled
 * (grayed), so the row looks the same whatever the value.
 * @param onRestore called when the button is pressed, only while [canRestore].
 * @param restoreTag the button's test tag, as it shows an icon instead of text.
 */
@Composable
private fun RestorableSettingTitle(
    text: String,
    canRestore: Boolean,
    onRestore: () -> Unit,
    restoreTag: String,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = onRestore,
            enabled = canRestore,
            modifier = Modifier
                .size(GisteTheme.dimensions.minTouchTarget)
                .testTag(restoreTag),
        ) {
            Icon(
                painter = painterResource(R.drawable.settings_ic_restore),
                contentDescription = null,
            )
        }
    }
}

/**
 * One choice among [options], as a row of segmented buttons as wide as the screen.
 *
 * The selected button shows no check mark: its color already marks it, and the label gets the
 * whole button, so longer names (e.g. "Terra Pirata") fit on narrow screens with large text.
 *
 * @param T the type of the options.
 * @param options the options, in the order they are shown.
 * @param selected the current option.
 * @param label the text of each option.
 * @param onSelect called with the option the user picks.
 */
@Composable
private fun <T> Choices(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                modifier = Modifier.heightIn(min = GisteTheme.dimensions.minTouchTarget),
                icon = {},
            ) {
                Text(label(option))
            }
        }
    }
}

/**
 * The button that opens the About screen: as wide as the screen and at least the minimum touch
 * target high, with its label on the start side and a chevron on the end side, as it opens
 * another screen.
 *
 * @param onClick called when the button is pressed.
 */
@Composable
private fun AboutButton(onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = GisteTheme.dimensions.minTouchTarget),
    ) {
        Text(text = stringResource(R.string.settings_about_open), modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(R.drawable.settings_ic_chevron_right),
            contentDescription = null,
        )
    }
}

/**
 * One action and the keys that trigger it, on one row: the action on the start side, and a button
 * on the end side with its keys one below the other, or a note that it has none. The button is at
 * least the minimum touch target in size.
 *
 * @param action the action.
 * @param keys its keys.
 * @param editable whether the keys can change. If not, the button is disabled, so it looks grayed
 * and says that they cannot change now, while the layout stays the same for every remote.
 * @param onClick called when the button is pressed, only while [editable].
 */
@Composable
private fun KeyBinding(
    action: RemoteAction,
    keys: Set<RemoteKey>,
    editable: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(SettingsTestTags.keyBinding(action)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = action.label(), style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.weight(1f))
        FilledTonalButton(
            onClick = onClick,
            enabled = editable,
            modifier = Modifier
                .heightIn(min = GisteTheme.dimensions.minTouchTarget)
                .widthIn(min = GisteTheme.dimensions.minTouchTarget * 2),
        ) {
            KeyNames(keys)
        }
    }
}

/**
 * The names of [keys], one below the other and aligned to the end, or a note that there are none.
 * Their color is the button's content color, enabled or disabled.
 */
@Composable
private fun KeyNames(keys: Set<RemoteKey>) {
    Column(horizontalAlignment = Alignment.End) {
        if (keys.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_remote_no_key),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        // Sorted by code, so the order never changes between recompositions or runs.
        keys.sortedBy { it.code }.forEach { key ->
            Text(text = key.label(), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

/**
 * A value with − and + buttons around it. Each button is disabled at its end of the range.
 *
 * @param value the formatted value.
 * @param canDecrease whether − is enabled.
 * @param canIncrease whether + is enabled.
 * @param onDecrease called when − is pressed.
 * @param onIncrease called when + is pressed.
 * @param modifier applied to the row.
 */
@Composable
private fun Stepper(
    value: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimensions = GisteTheme.dimensions
    val buttonModifier = Modifier
        .heightIn(min = dimensions.minTouchTarget)
        .widthIn(min = dimensions.minTouchTarget * 2)
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingMedium),
    ) {
        FilledTonalButton(onClick = onDecrease, enabled = canDecrease, modifier = buttonModifier) {
            Text(
                text = stringResource(R.string.settings_action_decrease),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = dimensions.minTouchTarget * 2),
        )
        FilledTonalButton(onClick = onIncrease, enabled = canIncrease, modifier = buttonModifier) {
            Text(
                text = stringResource(R.string.settings_action_increase),
                style = MaterialTheme.typography.headlineSmall,
            )
        }
    }
}

/** The name of this theme mode, as the user sees it. */
@Composable
private fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    },
)

/** The name of this screen orientation, as the user sees it. */
@Composable
private fun ScreenOrientation.label(): String = stringResource(
    when (this) {
        ScreenOrientation.FOLLOW_SYSTEM -> R.string.settings_orientation_system
        ScreenOrientation.VERTICAL -> R.string.settings_orientation_vertical
        ScreenOrientation.HORIZONTAL -> R.string.settings_orientation_horizontal
    },
)

/** The name of this remote model, as the user sees it. */
@Composable
private fun RemoteModel.label(): String = stringResource(
    when (this) {
        RemoteModel.DND2 -> R.string.settings_remote_model_dnd2
        RemoteModel.TERRA_PIRATA -> R.string.settings_remote_model_terra_pirata
        RemoteModel.CUSTOM -> R.string.settings_remote_model_custom
    },
)

/** The name of this remote action, as the user sees it. */
@Composable
internal fun RemoteAction.label(): String = stringResource(
    when (this) {
        RemoteAction.NEXT_WAYPOINT -> R.string.settings_remote_action_next_waypoint
        RemoteAction.PREVIOUS_WAYPOINT -> R.string.settings_remote_action_previous_waypoint
        RemoteAction.INCREASE_PARTIAL -> R.string.settings_remote_action_increase_partial
        RemoteAction.DECREASE_PARTIAL -> R.string.settings_remote_action_decrease_partial
        RemoteAction.RESET_PARTIAL -> R.string.settings_remote_action_reset_partial
    },
)

/**
 * The name of this key, as the user sees it: a translated name for the keys remotes usually send,
 * `F1` to `F12` for the function keys, and otherwise Android's name for the key code, which is
 * still enough to tell keys apart.
 */
@Composable
internal fun RemoteKey.label(): String {
    val named = KEY_NAMES[code]
    return when {
        named != null -> stringResource(named)
        code in KeyEvent.KEYCODE_F1..KeyEvent.KEYCODE_F12 ->
            stringResource(R.string.settings_key_function, code - KeyEvent.KEYCODE_F1 + 1)
        else -> stringResource(
            R.string.settings_key_other,
            KeyEvent.keyCodeToString(code).removePrefix("KEYCODE_"),
        )
    }
}

/** The translated names of the keys remotes usually send, by key code. */
private val KEY_NAMES: Map<Int, Int> = mapOf(
    KeyEvent.KEYCODE_DPAD_UP to R.string.settings_key_dpad_up,
    KeyEvent.KEYCODE_DPAD_DOWN to R.string.settings_key_dpad_down,
    KeyEvent.KEYCODE_DPAD_LEFT to R.string.settings_key_dpad_left,
    KeyEvent.KEYCODE_DPAD_RIGHT to R.string.settings_key_dpad_right,
    KeyEvent.KEYCODE_DPAD_CENTER to R.string.settings_key_dpad_center,
    KeyEvent.KEYCODE_ENTER to R.string.settings_key_enter,
    KeyEvent.KEYCODE_VOLUME_UP to R.string.settings_key_volume_up,
    KeyEvent.KEYCODE_VOLUME_DOWN to R.string.settings_key_volume_down,
    KeyEvent.KEYCODE_MEDIA_NEXT to R.string.settings_key_media_next,
    KeyEvent.KEYCODE_MEDIA_PREVIOUS to R.string.settings_key_media_previous,
    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE to R.string.settings_key_media_play_pause,
    KeyEvent.KEYCODE_MEDIA_PLAY to R.string.settings_key_media_play,
    KeyEvent.KEYCODE_MEDIA_PAUSE to R.string.settings_key_media_pause,
)

/** The name of this roadbook style, as the user sees it. */
@Composable
private fun RoadbookStyle.label(): String = stringResource(
    when (this) {
        RoadbookStyle.APP_THEME -> R.string.settings_roadbook_style_app_theme
        RoadbookStyle.FIA -> R.string.settings_roadbook_style_fia
    },
)

// --- Previews ---

/** Samsung Galaxy Tab Active3 in landscape: 1920 × 1200 px at 240 dpi. */
private const val TAB_ACTIVE3_LANDSCAPE = "spec:width=1280dp,height=800dp,dpi=240"

/**
 * The app makes every text 1.25 times the Material size (its `RiderTypography`), which this
 * module cannot see, so the previews scale the text by the same factor to look as on the device.
 */
private const val APP_FONT_SCALE = 1.25f

/** A typical phone in portrait. */
private const val PHONE_PORTRAIT = "spec:width=411dp,height=891dp,dpi=420"

/** Preview sample: the defaults, with Terra Pirata (an action with two keys). */
private val DEFAULTS = SettingsUiState.Loaded(
    themeMode = ThemeMode.SYSTEM,
    orientation = ScreenOrientation.FOLLOW_SYSTEM,
    remoteModel = RemoteModel.TERRA_PIRATA,
    remoteKeys = RemoteKeys.TERRA_PIRATA,
    roadbookStyle = RoadbookStyle.APP_THEME,
    shortDistanceThreshold = ShortDistanceThreshold.DEFAULT,
    canDecreaseShortDistanceThreshold = true,
    canIncreaseShortDistanceThreshold = true,
    canRestoreShortDistanceThreshold = false,
    mapZoomLevel = MapZoomLevel.DEFAULT,
    canDecreaseMapZoom = true,
    canIncreaseMapZoom = true,
    canRestoreMapZoom = false,
    mapTilt = MapTilt.DEFAULT,
    canDecreaseMapTilt = true,
    canIncreaseMapTilt = false,
    canRestoreMapTilt = false,
    saveFailed = false,
)

/**
 * Preview sample: dark theme, horizontal, DND2, FIA colors, the shortest threshold, the closest
 * map zoom and a flat map.
 */
private val CHANGED = SettingsUiState.Loaded(
    themeMode = ThemeMode.DARK,
    orientation = ScreenOrientation.HORIZONTAL,
    remoteModel = RemoteModel.DND2,
    remoteKeys = RemoteKeys.DND2,
    roadbookStyle = RoadbookStyle.FIA,
    shortDistanceThreshold = ShortDistanceThreshold.of(Distance.meters(100.0)),
    canDecreaseShortDistanceThreshold = false,
    canIncreaseShortDistanceThreshold = true,
    canRestoreShortDistanceThreshold = true,
    mapZoomLevel = MapZoomLevel.of(20),
    canDecreaseMapZoom = true,
    canIncreaseMapZoom = false,
    canRestoreMapZoom = true,
    mapTilt = MapTilt.of(0.0),
    canDecreaseMapTilt = false,
    canIncreaseMapTilt = true,
    canRestoreMapTilt = true,
    saveFailed = false,
)

/**
 * Preview sample: custom keys, editable, after D-pad up moved to reset the partial, so the next
 * waypoint has no key.
 */
private val CUSTOM_KEYS = DEFAULTS.copy(
    remoteModel = RemoteModel.CUSTOM,
    remoteKeys = RemoteKeys(
        RemoteKeys.DND2.bindings - RemoteAction.NEXT_WAYPOINT +
            (RemoteAction.RESET_PARTIAL to setOf(RemoteKey(KeyEvent.KEYCODE_DPAD_UP))),
    ),
)

/** The settings screen in [state]. */
@Composable
private fun SettingsPreview(state: SettingsUiState) {
    GisteTheme {
        SettingsContent(
            state = state,
            onBack = {},
            onThemeModeChange = {},
            onOrientationChange = {},
            onRemoteModelChange = {},
            onAssignCustomKey = { _, _ -> },
            onRoadbookStyleChange = {},
            onDecreaseShortDistanceThreshold = {},
            onIncreaseShortDistanceThreshold = {},
            onRestoreShortDistanceThreshold = {},
            onDecreaseMapZoom = {},
            onIncreaseMapZoom = {},
            onRestoreMapZoom = {},
            onDecreaseMapTilt = {},
            onIncreaseMapTilt = {},
            onRestoreMapTilt = {},
            onSaveErrorShown = {},
            onKeyMoveShown = {},
            onOpenAbout = {},
        )
    }
}

@Preview(name = "Phone, defaults, light", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_NO, fontScale = APP_FONT_SCALE)
@Preview(name = "Phone, defaults, dark", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = APP_FONT_SCALE)
@Preview(name = "Tab Active3, defaults, light", device = TAB_ACTIVE3_LANDSCAPE, uiMode = Configuration.UI_MODE_NIGHT_NO, fontScale = APP_FONT_SCALE)
@Composable
private fun SettingsDefaultsPreview() {
    SettingsPreview(DEFAULTS)
}

@Preview(name = "Phone, changed, light", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_NO, fontScale = APP_FONT_SCALE)
@Preview(name = "Phone, changed, dark", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = APP_FONT_SCALE)
@Composable
private fun SettingsChangedPreview() {
    SettingsPreview(CHANGED)
}

@Preview(name = "Phone, custom keys, light", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_NO, fontScale = APP_FONT_SCALE)
@Preview(name = "Phone, custom keys, dark", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = APP_FONT_SCALE)
@Composable
private fun SettingsCustomKeysPreview() {
    SettingsPreview(CUSTOM_KEYS)
}

@Preview(name = "Phone, loading, light", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_NO, fontScale = APP_FONT_SCALE)
@Preview(name = "Phone, loading, dark", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_YES, fontScale = APP_FONT_SCALE)
@Composable
private fun SettingsLoadingPreview() {
    SettingsPreview(SettingsUiState.Loading)
}
