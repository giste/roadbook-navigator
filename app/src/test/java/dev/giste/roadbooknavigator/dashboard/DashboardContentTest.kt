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

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.geo.Distance
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en")
class DashboardContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun show(state: DashboardUiState) {
        composeRule.setContent {
            GisteTheme {
                DashboardContent(state = state) { showImportButton, modifier ->
                    Text("roadbook, import $showImportButton", modifier)
                }
            }
        }
    }

    @Test
    fun dashboardContent_loading_showsLabelsWithPlaceholders() {
        show(DashboardUiState.INITIAL)

        composeRule.onNodeWithText("Partial").assertExists()
        composeRule.onNodeWithText("Total").assertExists()
        composeRule.onAllNodesWithText("–").assertCountEquals(2)
    }

    @Test
    fun dashboardContent_available_showsPartialAndTotalInKilometers() {
        show(
            DashboardUiState(
                odometer = OdometerUiState.Available(
                    partial = Distance.meters(12_345.0),
                    total = Distance.meters(234_567.0),
                ),
                showImportButton = false,
            ),
        )

        composeRule.onNodeWithText("12.35").assertExists()
        composeRule.onNodeWithText("234.6").assertExists()
    }

    @Test
    fun dashboardContent_available_readsEachDistanceWithItsLabel() {
        show(
            DashboardUiState(
                odometer = OdometerUiState.Available(Distance.meters(1_000.0), Distance.ZERO),
                showImportButton = false,
            ),
        )

        // The label and the value are merged into one node for screen readers.
        composeRule.onNode(hasText("Partial") and hasText("1.00")).assertExists()
    }

    @Test
    fun dashboardContent_stopped_tellsTheRoadbookToShowImport() {
        show(DashboardUiState.INITIAL)

        composeRule.onNodeWithText("roadbook, import true").assertExists()
    }

    @Test
    fun dashboardContent_moving_tellsTheRoadbookToHideImport() {
        show(DashboardUiState.INITIAL.copy(showImportButton = false))

        composeRule.onNodeWithText("roadbook, import false").assertExists()
    }

    @Test
    @Config(qualifiers = "es")
    fun dashboardContent_spanish_usesSpanishLabelsAndDecimalComma() {
        show(
            DashboardUiState(
                odometer = OdometerUiState.Available(Distance.meters(12_345.0), Distance.ZERO),
                showImportButton = true,
            ),
        )

        composeRule.onNodeWithText("Parcial").assertExists()
        composeRule.onNodeWithText("12,35").assertExists()
    }
}
