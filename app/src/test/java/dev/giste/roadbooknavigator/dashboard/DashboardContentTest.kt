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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.geo.Distance
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

// mdpi, so 1 dp is 1 px and the weighted sizes below are whole pixels, on a screen large enough for
// every test box (the default one is smaller and would clamp them).
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-w1000dp-h1000dp-mdpi")
class DashboardContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val moving = DashboardUiState(
        odometer = OdometerUiState.Available(
            partial = Distance.meters(12_345.0),
            total = Distance.meters(234_567.0),
        ),
        showImportButton = false,
    )

    /** Shows the dashboard for [state] in a [width] × [height] box, with tagged placeholders. */
    private fun show(state: DashboardUiState, width: Dp = 500.dp, height: Dp = 1_000.dp) {
        composeRule.setContent {
            GisteTheme {
                Box(modifier = Modifier.size(width, height)) {
                    DashboardContent(
                        state = state,
                        roadbook = { showImportButton, modifier ->
                            Box(modifier = modifier.testTag(ROADBOOK_TAG)) {
                                Text("roadbook, import $showImportButton")
                            }
                        },
                        map = { modifier -> Box(modifier = modifier.testTag(MAP_TAG)) },
                    )
                }
            }
        }
    }

    private fun total(): SemanticsNodeInteraction = composeRule.onNodeWithContentDescription("Total")

    private fun partial(): SemanticsNodeInteraction =
        composeRule.onNodeWithContentDescription("Partial")

    private fun SemanticsNodeInteraction.assertBounds(left: Dp, top: Dp, width: Dp, height: Dp) {
        assertLeftPositionInRootIsEqualTo(left)
        assertTopPositionInRootIsEqualTo(top)
        assertWidthIsEqualTo(width)
        assertHeightIsEqualTo(height)
    }

    @Test
    fun dashboardContent_loading_showsPlaceholders() {
        show(DashboardUiState.INITIAL)

        composeRule.onAllNodesWithText("–").assertCountEquals(2)
    }

    @Test
    fun dashboardContent_available_showsPartialAndTotalInKilometers() {
        show(moving)

        composeRule.onNodeWithText("12.35").assertExists()
        composeRule.onNodeWithText("234.6").assertExists()
    }

    @Test
    fun dashboardContent_distancesAboveTheirLargestValues_showsThemCapped() {
        show(
            DashboardUiState(
                odometer = OdometerUiState.Available(
                    partial = Distance.meters(1_234_567.0),
                    total = Distance.meters(12_345_678.0),
                ),
                showImportButton = false,
            ),
        )

        composeRule.onNodeWithText("999.99").assertExists()
        composeRule.onNodeWithText("9,999.9").assertExists()
    }

    @Test
    fun dashboardContent_available_hidesTheLabels() {
        show(moving)

        composeRule.onNodeWithText("Partial").assertDoesNotExist()
        composeRule.onNodeWithText("Total").assertDoesNotExist()
    }

    @Test
    fun dashboardContent_available_readsEachDistanceWithItsLabel() {
        show(moving)

        // The label and the value are merged into one node for screen readers.
        composeRule.onNode(hasContentDescription("Partial") and hasText("12.35")).assertExists()
        composeRule.onNode(hasContentDescription("Total") and hasText("234.6")).assertExists()
    }

    @Test
    fun dashboardContent_stopped_tellsTheRoadbookToShowImport() {
        show(DashboardUiState.INITIAL)

        composeRule.onNodeWithText("roadbook, import true").assertExists()
    }

    @Test
    fun dashboardContent_moving_tellsTheRoadbookToHideImport() {
        show(moving)

        composeRule.onNodeWithText("roadbook, import false").assertExists()
    }

    @Test
    @Config(qualifiers = "es-w1000dp-h1000dp-mdpi")
    fun dashboardContent_spanish_usesSpanishLabelsAndDecimalComma() {
        show(moving)

        composeRule.onNode(hasContentDescription("Parcial") and hasText("12,35")).assertExists()
    }

    @Test
    fun dashboardContent_widerThanTall_showsSideColumnAndRoadbookOnTheRight() {
        show(moving, width = 1_000.dp, height = 500.dp)

        val side = 1_000.dp * LANDSCAPE_SIDE_COLUMN_WEIGHT
        val totalHeight = 500.dp * LANDSCAPE_TOTAL_WEIGHT
        val partialHeight = 500.dp * LANDSCAPE_PARTIAL_WEIGHT
        total().assertBounds(left = 0.dp, top = 0.dp, width = side, height = totalHeight)
        partial().assertBounds(left = 0.dp, top = totalHeight, width = side, height = partialHeight)
        composeRule.onNodeWithTag(MAP_TAG).assertBounds(
            left = 0.dp,
            top = totalHeight + partialHeight,
            width = side,
            height = 500.dp * LANDSCAPE_MAP_WEIGHT,
        )
        composeRule.onNodeWithTag(ROADBOOK_TAG)
            .assertBounds(left = side, top = 0.dp, width = 1_000.dp - side, height = 500.dp)
    }

    @Test
    fun dashboardContent_tallerThanWide_showsDistancesMapAndRoadbookStacked() {
        show(moving, width = 500.dp, height = 1_000.dp)

        val distancesHeight = 1_000.dp * PORTRAIT_DISTANCES_WEIGHT
        val mapHeight = 1_000.dp * PORTRAIT_MAP_WEIGHT
        val totalWidth = 500.dp * PORTRAIT_TOTAL_WEIGHT
        total().assertBounds(left = 0.dp, top = 0.dp, width = totalWidth, height = distancesHeight)
        partial().assertBounds(
            left = totalWidth,
            top = 0.dp,
            width = 500.dp * PORTRAIT_PARTIAL_WEIGHT,
            height = distancesHeight,
        )
        composeRule.onNodeWithTag(MAP_TAG)
            .assertBounds(left = 0.dp, top = distancesHeight, width = 500.dp, height = mapHeight)
        composeRule.onNodeWithTag(ROADBOOK_TAG).assertBounds(
            left = 0.dp,
            top = distancesHeight + mapHeight,
            width = 500.dp,
            height = 1_000.dp * PORTRAIT_ROADBOOK_WEIGHT,
        )
    }

    @Test
    fun dashboardContent_square_usesThePortraitLayout() {
        show(moving, width = 500.dp, height = 500.dp)

        composeRule.onNodeWithTag(ROADBOOK_TAG)
            .assertTopPositionInRootIsEqualTo(500.dp * (1f - PORTRAIT_ROADBOOK_WEIGHT))
        composeRule.onNodeWithTag(ROADBOOK_TAG).assertLeftPositionInRootIsEqualTo(0.dp)
    }

    private companion object {
        const val ROADBOOK_TAG = "roadbook"
        const val MAP_TAG = "map"
    }
}
