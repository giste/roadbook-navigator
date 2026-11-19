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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.geo.Distance

// Preview-only sample data and sizes: a phone in portrait.
private const val WIDTH = 400
private const val HEIGHT = 700

/** The dashboard in [state], with a placeholder instead of the roadbook. */
@Composable
private fun DashboardPreview(state: DashboardUiState) {
    GisteTheme {
        DashboardContent(state = state) { showImportButton, modifier ->
            Box(
                modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Text("Roadbook (import button shown: $showImportButton)")
            }
        }
    }
}

@Preview(name = "Loading, light", uiMode = Configuration.UI_MODE_NIGHT_NO, widthDp = WIDTH, heightDp = HEIGHT)
@Preview(name = "Loading, dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun DashboardLoadingPreview() {
    DashboardPreview(DashboardUiState.INITIAL)
}

@Preview(name = "Moving, light", uiMode = Configuration.UI_MODE_NIGHT_NO, widthDp = WIDTH, heightDp = HEIGHT)
@Preview(name = "Moving, dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun DashboardMovingPreview() {
    DashboardPreview(
        DashboardUiState(
            odometer = OdometerUiState.Available(
                partial = Distance.meters(12_345.0),
                total = Distance.meters(234_567.0),
            ),
            showImportButton = false,
        ),
    )
}

@Preview(name = "Long distances", uiMode = Configuration.UI_MODE_NIGHT_NO, widthDp = WIDTH, heightDp = HEIGHT)
@Composable
private fun DashboardLongDistancesPreview() {
    DashboardPreview(
        DashboardUiState(
            odometer = OdometerUiState.Available(
                partial = Distance.meters(1_234_567.0),
                total = Distance.meters(12_345_678.0),
            ),
            showImportButton = true,
        ),
    )
}
