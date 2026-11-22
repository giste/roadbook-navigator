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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.geo.Distance
import dev.giste.roadbook.RoadbookStyle
import dev.giste.roadbook.ShortDistanceThreshold
import dev.giste.roadbooknavigator.settings.R
import dev.giste.roadbooknavigator.settings.ThemeMode
import kotlin.math.roundToInt

/** Test tags of the settings screen parts that tests cannot find by their own text. */
internal object SettingsTestTags {

    /** The back button, which shows an icon instead of text. */
    const val BACK: String = "settings_back"
}

/**
 * The settings screen for [state]: a top bar with a back button, then the appearance and roadbook
 * sections. Every control is at least the minimum touch target high, so it works with gloves.
 *
 * - **Theme:** system, light or dark.
 * - **Roadbook colors:** the app theme or the FIA palette.
 * - **Highlight close waypoints:** the short-distance threshold, changed in steps with − and +.
 *
 * When [SettingsUiState.Loaded.saveFailed] is set, a snackbar says the change was not stored, and
 * [onSaveErrorShown] is called once it has been shown. While loading, only the top bar is shown.
 *
 * @param state what to show.
 * @param onBack called when the back button is pressed.
 * @param onThemeModeChange called with the theme mode the user picks.
 * @param onRoadbookStyleChange called with the roadbook style the user picks.
 * @param onDecreaseThreshold called when the threshold's − is pressed.
 * @param onIncreaseThreshold called when the threshold's + is pressed.
 * @param onSaveErrorShown called once the save error has been shown.
 * @param modifier applied to the root layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsContent(
    state: SettingsUiState,
    onBack: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onRoadbookStyleChange: (RoadbookStyle) -> Unit,
    onDecreaseThreshold: () -> Unit,
    onIncreaseThreshold: () -> Unit,
    onSaveErrorShown: () -> Unit,
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
                onRoadbookStyleChange = onRoadbookStyleChange,
                onDecreaseThreshold = onDecreaseThreshold,
                onIncreaseThreshold = onIncreaseThreshold,
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
 * @param onRoadbookStyleChange called with the roadbook style the user picks.
 * @param onDecreaseThreshold called when the threshold's − is pressed.
 * @param onIncreaseThreshold called when the threshold's + is pressed.
 * @param modifier applied to the column.
 */
@Composable
private fun SettingsSections(
    state: SettingsUiState.Loaded,
    onThemeModeChange: (ThemeMode) -> Unit,
    onRoadbookStyleChange: (RoadbookStyle) -> Unit,
    onDecreaseThreshold: () -> Unit,
    onIncreaseThreshold: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dimensions = GisteTheme.dimensions
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

        SectionTitle(stringResource(R.string.settings_section_roadbook))
        SettingTitle(stringResource(R.string.settings_roadbook_style_title))
        Choices(
            options = RoadbookStyle.entries,
            selected = state.roadbookStyle,
            label = { it.label() },
            onSelect = onRoadbookStyleChange,
        )
        SettingTitle(stringResource(R.string.settings_short_distance_title))
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
            canDecrease = state.canDecreaseThreshold,
            canIncrease = state.canIncreaseThreshold,
            onDecrease = onDecreaseThreshold,
            onIncrease = onIncreaseThreshold,
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
 * One choice among [options], as a row of segmented buttons as wide as the screen.
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
            ) {
                Text(label(option))
            }
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
 */
@Composable
private fun Stepper(
    value: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    val dimensions = GisteTheme.dimensions
    val buttonModifier = Modifier
        .heightIn(min = dimensions.minTouchTarget)
        .widthIn(min = dimensions.minTouchTarget * 2)
    Row(
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

/** A typical phone in portrait. */
private const val PHONE_PORTRAIT = "spec:width=411dp,height=891dp,dpi=420"

/** Preview sample: the defaults. */
private val DEFAULTS = SettingsUiState.Loaded(
    themeMode = ThemeMode.SYSTEM,
    roadbookStyle = RoadbookStyle.APP_THEME,
    shortDistanceThreshold = ShortDistanceThreshold.DEFAULT,
    canDecreaseThreshold = true,
    canIncreaseThreshold = true,
    saveFailed = false,
)

/** Preview sample: dark theme, FIA colors and the shortest threshold. */
private val CHANGED = SettingsUiState.Loaded(
    themeMode = ThemeMode.DARK,
    roadbookStyle = RoadbookStyle.FIA,
    shortDistanceThreshold = ShortDistanceThreshold.of(Distance.meters(50.0)),
    canDecreaseThreshold = false,
    canIncreaseThreshold = true,
    saveFailed = false,
)

/** The settings screen in [state]. */
@Composable
private fun SettingsPreview(state: SettingsUiState) {
    GisteTheme {
        SettingsContent(
            state = state,
            onBack = {},
            onThemeModeChange = {},
            onRoadbookStyleChange = {},
            onDecreaseThreshold = {},
            onIncreaseThreshold = {},
            onSaveErrorShown = {},
        )
    }
}

@Preview(name = "Phone, defaults, light", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Phone, defaults, dark", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tab Active3, defaults, light", device = TAB_ACTIVE3_LANDSCAPE, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun SettingsDefaultsPreview() {
    SettingsPreview(DEFAULTS)
}

@Preview(name = "Phone, changed, light", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Phone, changed, dark", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsChangedPreview() {
    SettingsPreview(CHANGED)
}

@Preview(name = "Phone, loading, light", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Phone, loading, dark", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SettingsLoadingPreview() {
    SettingsPreview(SettingsUiState.Loading)
}
