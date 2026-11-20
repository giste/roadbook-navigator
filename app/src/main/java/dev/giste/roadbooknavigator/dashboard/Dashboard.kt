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

package dev.giste.roadbooknavigator.dashboard

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.giste.core.ui.component.AutoSizeText
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.geo.Distance
import dev.giste.roadbook.RoadbookViewer
import dev.giste.roadbooknavigator.R

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

/**
 * The main screen: the odometer, the map and the roadbook, with their state from Hilt.
 *
 * Showing it makes the odometer measure (see [DashboardViewModel]), so it must only be shown once
 * the location permission is granted.
 *
 * @param modifier applied to the root layout.
 */
@Composable
internal fun Dashboard(modifier: Modifier = Modifier) {
    val viewModel: DashboardViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardContent(
        state = state,
        modifier = modifier,
        roadbook = { showImportButton, roadbookModifier ->
            RoadbookViewer(showImportButton = showImportButton, modifier = roadbookModifier)
        },
        // TODO(map): replace with the map once the :map module exists.
        map = { mapModifier -> MapPlaceholder(mapModifier) },
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
 * Screen readers still read each one with its label (e.g. "Partial, 12.35").
 *
 * @param state what to show.
 * @param modifier applied to the root layout.
 * @param roadbook draws the roadbook in the modifier it is given, and shows its import button only
 * when told to. A slot, so tests and previews can use a placeholder instead of the Hilt-backed
 * viewer.
 * @param map draws the map in the modifier it is given. A slot for the same reason.
 */
@Composable
internal fun DashboardContent(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
    roadbook: @Composable (showImportButton: Boolean, modifier: Modifier) -> Unit,
    map: @Composable (modifier: Modifier) -> Unit,
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
    val partialLabel = stringResource(R.string.app_odometer_partial)
    val totalLabel = stringResource(R.string.app_odometer_total)

    // The total is plain; the partial, which the rider looks at most, is highlighted.
    val totalReading: @Composable (Modifier) -> Unit = { readingModifier ->
        DistanceReading(
            label = totalLabel,
            value = total,
            widestValue = widestTotal,
            color = MaterialTheme.colorScheme.onSurface,
            highlighted = false,
            modifier = readingModifier.background(MaterialTheme.colorScheme.surface),
        )
    }
    val partialReading: @Composable (Modifier) -> Unit = { readingModifier ->
        DistanceReading(
            label = partialLabel,
            value = partial,
            widestValue = widestPartial,
            color = MaterialTheme.colorScheme.onPrimary,
            highlighted = true,
            modifier = readingModifier.background(MaterialTheme.colorScheme.primary),
        )
    }
    val roadbookSlot: @Composable (Modifier) -> Unit = { roadbookModifier ->
        roadbook(state.showImportButton, roadbookModifier)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (maxWidth > maxHeight) {
            LandscapeLayout(
                total = totalReading,
                partial = partialReading,
                map = map,
                roadbook = roadbookSlot,
            )
        } else {
            PortraitLayout(
                total = totalReading,
                partial = partialReading,
                map = map,
                roadbook = roadbookSlot,
            )
        }
    }
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
 * One distance, as large as its box allows and without a visible label. Screen readers read it as
 * a single item with its label (e.g. "Partial, 12.35").
 *
 * @param label what the distance is, read by screen readers only.
 * @param value the formatted distance.
 * @param widestValue the widest [value] can be, which sets the font size for every value.
 * @param color the text color, readable on the background set by [modifier].
 * @param highlighted whether to draw the value in bold.
 * @param modifier applied to the root layout. It gives the reading its box and background.
 */
@Composable
private fun DistanceReading(
    label: String,
    value: String,
    widestValue: String,
    color: Color,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        AutoSizeText(
            text = value,
            color = color,
            modifier = Modifier
                .fillMaxSize()
                .padding(GisteTheme.dimensions.spacingExtraSmall),
            fontWeight = if (highlighted) FontWeight.Bold else null,
            textAlign = TextAlign.End,
            widestText = widestValue,
        )
    }
}

/**
 * Keeps the map's place until the map module exists: an empty area in the surface variant color.
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

/** Preview sample: moving, with ordinary distances. */
private val MOVING = DashboardUiState(
    odometer = OdometerUiState.Available(
        partial = Distance.meters(12_345.0),
        total = Distance.meters(234_567.0),
    ),
    showImportButton = false,
)

/** Preview sample: stopped, with the largest distances shown (999.99 and 9,999.9). */
private val LARGEST_DISTANCES = DashboardUiState(
    odometer = OdometerUiState.Available(
        partial = Distance.meters(999_990.0),
        total = Distance.meters(9_999_900.0),
    ),
    showImportButton = true,
)

/** The dashboard in [state], with placeholders instead of the roadbook and the map. */
@Composable
private fun DashboardPreview(state: DashboardUiState) {
    GisteTheme {
        DashboardContent(
            state = state,
            roadbook = { showImportButton, modifier ->
                Box(modifier = modifier, contentAlignment = Alignment.Center) {
                    Text(
                        text = "Roadbook (import button shown: $showImportButton)",
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            },
            map = { modifier -> MapPlaceholder(modifier) },
        )
    }
}

@Preview(name = "Tab Active3 landscape, light", device = TAB_ACTIVE3_LANDSCAPE, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Tab Active3 landscape, dark", device = TAB_ACTIVE3_LANDSCAPE, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Tab Active3 portrait, light", device = TAB_ACTIVE3_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Tab Active3 portrait, dark", device = TAB_ACTIVE3_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DashboardTabletPreview() {
    DashboardPreview(MOVING)
}

@Preview(name = "Phone portrait, light", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Phone portrait, dark", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DashboardPhonePortraitPreview() {
    DashboardPreview(MOVING)
}

@Preview(name = "Phone portrait, largest distances", device = PHONE_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Tab Active3 portrait, largest distances", device = TAB_ACTIVE3_PORTRAIT, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Phone landscape, largest distances", device = PHONE_LANDSCAPE, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun DashboardLargestDistancesPreview() {
    DashboardPreview(LARGEST_DISTANCES)
}

@Preview(name = "Phone landscape, loading, light", device = PHONE_LANDSCAPE, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Phone landscape, loading, dark", device = PHONE_LANDSCAPE, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DashboardLoadingPreview() {
    DashboardPreview(DashboardUiState.INITIAL)
}
