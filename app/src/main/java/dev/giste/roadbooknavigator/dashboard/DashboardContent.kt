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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.roadbooknavigator.R

/**
 * The dashboard for [state]: the odometer at the top, and the roadbook below, filling the rest.
 *
 * @param state what to show.
 * @param modifier applied to the root layout.
 * @param roadbook draws the roadbook in the modifier it is given, and shows its import button only
 * when told to. A slot, so tests and previews can use a placeholder instead of the Hilt-backed
 * viewer.
 */
@Composable
internal fun DashboardContent(
    state: DashboardUiState,
    modifier: Modifier = Modifier,
    roadbook: @Composable (showImportButton: Boolean, modifier: Modifier) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        OdometerDisplay(state = state.odometer, modifier = Modifier.fillMaxWidth())
        roadbook(state.showImportButton, Modifier.weight(1f).fillMaxWidth())
    }
}

/**
 * The total (smaller, on the left) and the partial (larger and highlighted, on the right, where
 * the rider looks most), in kilometers.
 *
 * @param state the distances, or a placeholder while they load.
 * @param modifier applied to the root layout.
 */
@Composable
internal fun OdometerDisplay(state: OdometerUiState, modifier: Modifier = Modifier) {
    val locale = LocalConfiguration.current.locales[0]
    val loading = stringResource(R.string.app_odometer_value_loading)
    val (partial, total) = when (state) {
        OdometerUiState.Loading -> loading to loading
        is OdometerUiState.Available ->
            state.partial.formatKilometers(PARTIAL_DECIMALS, locale) to
                state.total.formatKilometers(TOTAL_DECIMALS, locale)
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        DistanceReading(
            label = stringResource(R.string.app_odometer_total),
            value = total,
            valueStyle = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
        DistanceReading(
            label = stringResource(R.string.app_odometer_partial),
            value = partial,
            valueStyle = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .weight(2f)
                .background(MaterialTheme.colorScheme.primary),
        )
    }
}

/**
 * One labeled distance, read as a single item by screen readers (e.g. "Partial, 12.34").
 *
 * @param label what the distance is.
 * @param value the formatted distance.
 * @param valueStyle the text style of [value].
 * @param color the color of both texts, readable on the background set by [modifier].
 * @param modifier applied to the root layout.
 */
@Composable
private fun DistanceReading(
    label: String,
    value: String,
    valueStyle: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .semantics(mergeDescendants = true) {}
            .padding(
                horizontal = GisteTheme.dimensions.spacingSmall,
                vertical = GisteTheme.dimensions.spacingExtraSmall,
            ),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = color)
        Text(text = value, style = valueStyle, color = color, maxLines = 1)
    }
}

