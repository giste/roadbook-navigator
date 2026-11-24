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

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.geo.Distance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        showStoppedActions = false,
    )

    private val partialsSet = mutableListOf<Distance>()
    private var resets = 0
    private var settingsOpened = 0

    /** Shows the dashboard for [state] in a [width] × [height] box, with tagged placeholders. */
    private fun show(state: DashboardUiState, width: Dp = 500.dp, height: Dp = 1_000.dp) {
        composeRule.setContent { TestDashboard(state, width, height) }
    }

    /** The dashboard for [state] in a [width] × [height] box, recording the odometer commands. */
    @Composable
    private fun TestDashboard(state: DashboardUiState, width: Dp = 500.dp, height: Dp = 1_000.dp) {
        GisteTheme {
            Box(modifier = Modifier.size(width, height)) {
                DashboardContent(
                    state = state,
                    onSetPartial = { partialsSet += it },
                    onResetAll = { resets++ },
                    onOpenSettings = { settingsOpened++ },
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

    private fun SemanticsNodeInteraction.longPress() {
        performTouchInput { longClick() }
    }

    /** Matches the text [value] inside the reading tagged [tag]. */
    private fun shownIn(tag: String, value: String): SemanticsMatcher =
        hasText(value) and hasAnyAncestor(hasTestTag(tag))

    private fun total(): SemanticsNodeInteraction = composeRule.onNodeWithTag(DashboardTestTags.TOTAL)

    private fun partial(): SemanticsNodeInteraction =
        composeRule.onNodeWithTag(DashboardTestTags.PARTIAL)

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
                showStoppedActions = false,
            ),
        )

        composeRule.onNodeWithText("999.99").assertExists()
        composeRule.onNodeWithText("9,999.9").assertExists()
    }

    @Test
    fun dashboardContent_available_showsEachDistanceInItsReading() {
        show(moving)

        composeRule.onNode(shownIn(DashboardTestTags.PARTIAL, "12.35")).assertExists()
        composeRule.onNode(shownIn(DashboardTestTags.TOTAL, "234.6")).assertExists()
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
    fun dashboardContent_spanish_usesDecimalComma() {
        show(moving)

        composeRule.onNode(shownIn(DashboardTestTags.PARTIAL, "12,35")).assertExists()
    }

    @Test
    fun dashboardContent_widerThanTall_showsSideColumnAndRoadbookOnTheRight() {
        show(moving, width = 1_000.dp, height = 500.dp)

        val side = 1_000.dp * LANDSCAPE_SIDE_COLUMN_WEIGHT
        val totalHeight = 500.dp * LANDSCAPE_TOTAL_WEIGHT
        val partialHeight = 500.dp * LANDSCAPE_PARTIAL_WEIGHT
        // The settings slot, on the total's left, is as wide as the total is tall.
        total().assertBounds(
            left = totalHeight,
            top = 0.dp,
            width = side - totalHeight,
            height = totalHeight,
        )
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
        // The settings slot, on the total's left, is as wide as the total is tall.
        total().assertBounds(
            left = distancesHeight,
            top = 0.dp,
            width = totalWidth - distancesHeight,
            height = distancesHeight,
        )
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

    @Test
    fun dashboardContent_partialLongPressed_opensSetPartialDialog() {
        show(moving)

        partial().longPress()

        composeRule.onNodeWithText("Set partial").assertIsDisplayed()
    }

    @Test
    fun dashboardContent_partialTapped_opensNothing() {
        show(moving)

        partial().performClick()

        composeRule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun dashboardContent_partialSet_setsItAndClosesTheDialog() {
        show(moving)
        partial().longPress()
        composeRule.pressNumpadKeys("3.5")

        composeRule.onNodeWithText("Set").performClick()

        assertEquals(listOf(Distance.meters(3_500.0)), partialsSet)
        composeRule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun dashboardContent_setPartialCancelled_closesWithoutSetting() {
        show(moving)
        partial().longPress()
        composeRule.pressNumpadKeys("3.5")

        composeRule.onNodeWithText("Cancel").performClick()

        assertEquals(emptyList<Distance>(), partialsSet)
        composeRule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun dashboardContent_setPartialReopened_startsEmpty() {
        show(moving)
        partial().longPress()
        composeRule.pressNumpadKeys("3.5")
        composeRule.onNodeWithText("Cancel").performClick()

        partial().longPress()

        composeRule.onNode(shownInPartialDisplay("0 km")).assertIsDisplayed()
    }

    @Test
    fun dashboardContent_totalLongPressed_opensResetAllDialog() {
        show(moving)

        total().longPress()

        composeRule.onNodeWithText("Reset distances?").assertIsDisplayed()
    }

    @Test
    fun dashboardContent_resetAllConfirmed_resetsAndClosesTheDialog() {
        show(moving)
        total().longPress()

        composeRule.onNodeWithText("Reset").performClick()

        assertEquals(1, resets)
        composeRule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun dashboardContent_resetAllCancelled_closesWithoutResetting() {
        show(moving)
        total().longPress()

        composeRule.onNodeWithText("Cancel").performClick()

        assertEquals(0, resets)
        composeRule.onNode(isDialog()).assertDoesNotExist()
    }

    @Test
    fun dashboardContent_loading_distancesCanStillBeLongPressed() {
        show(DashboardUiState.INITIAL)

        partial().longPress()

        composeRule.onNodeWithText("Set partial").assertIsDisplayed()
    }

    @Test
    fun dashboardContent_recreated_keepsTheOpenDialogAndTheTypedText() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent { TestDashboard(moving) }
        partial().longPress()
        composeRule.pressNumpadKeys("3.5")

        restoration.emulateSavedInstanceStateRestore()

        composeRule.onNodeWithText("Set partial").assertIsDisplayed()
        composeRule.onNode(shownInPartialDisplay("3.5 km")).assertIsDisplayed()
    }

    @Test
    fun dashboardContent_stopped_showsTheSettingsButton() {
        show(DashboardUiState.INITIAL)

        composeRule.onNodeWithTag(DashboardTestTags.SETTINGS).assertIsDisplayed()
    }

    @Test
    fun dashboardContent_moving_hidesTheSettingsButton() {
        show(moving)

        composeRule.onNodeWithTag(DashboardTestTags.SETTINGS).assertDoesNotExist()
    }

    @Test
    fun dashboardContent_settingsButtonPressed_opensTheSettings() {
        show(DashboardUiState.INITIAL)

        composeRule.onNodeWithTag(DashboardTestTags.SETTINGS).performClick()

        assertEquals(1, settingsOpened)
    }

    @Test
    fun dashboardContent_keyModeFocusRequestedOnTheSettingsButton_buttonDoesNotTakeIt() {
        lateinit var inputModeManager: InputModeManager
        composeRule.setContent {
            inputModeManager = LocalInputModeManager.current
            TestDashboard(DashboardUiState.INITIAL)
        }
        // As after a key of the remote: in touch mode no button takes the focus anyway.
        composeRule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        val button = composeRule.onNodeWithTag(DashboardTestTags.SETTINGS)

        // Where an unbound arrow or Tab key would move the focus.
        button.requestFocus()

        button.assert(isFocused().not())
    }

    @Test
    fun dashboardContent_portrait_settingsButtonSitsOnTheStartSideOfTheTotal() {
        show(DashboardUiState.INITIAL, width = 500.dp, height = 1_000.dp)

        assertSettingsButtonOnTheStartSideOfTheTotal()
    }

    @Test
    fun dashboardContent_landscape_settingsButtonSitsOnTheStartSideOfTheTotal() {
        show(DashboardUiState.INITIAL, width = 1_000.dp, height = 500.dp)

        assertSettingsButtonOnTheStartSideOfTheTotal()
    }

    @Test
    fun dashboardContent_totalShorterThanATouchTarget_settingsButtonKeepsTheTouchTargetWidth() {
        // Landscape, with the total 400 × 0.12 = 48 dp tall: less than the 56 dp target.
        show(DashboardUiState.INITIAL, width = 1_000.dp, height = 400.dp)

        val total = total().getUnclippedBoundsInRoot()
        val button = composeRule.onNodeWithTag(DashboardTestTags.SETTINGS).getUnclippedBoundsInRoot()

        assertEquals(MIN_TOUCH_TARGET, button.right - button.left)
        assertEquals(total.bottom - total.top, button.bottom - button.top)
        assertEquals(MIN_TOUCH_TARGET, total.left)
    }

    @Test
    fun dashboardContent_moving_keepsTheTotalWhereItIsWhenStopped() {
        var state by mutableStateOf(DashboardUiState.INITIAL)
        composeRule.setContent { TestDashboard(state) }
        val stopped = total().getUnclippedBoundsInRoot()

        state = moving

        assertEquals(stopped, total().getUnclippedBoundsInRoot())
    }

    /** Asserts the settings button is a square as tall as the total, right before it. */
    private fun assertSettingsButtonOnTheStartSideOfTheTotal() {
        val total = total().getUnclippedBoundsInRoot()
        val button = composeRule.onNodeWithTag(DashboardTestTags.SETTINGS).getUnclippedBoundsInRoot()
        val height = total.bottom - total.top

        assertEquals(total.left - height, button.left)
        assertEquals(total.left, button.right)
        assertEquals(total.top, button.top)
        assertEquals(total.bottom, button.bottom)
    }

    @Test
    fun dashboardContent_shown_keepsTheScreenOn() {
        lateinit var view: View
        composeRule.setContent {
            view = LocalView.current
            TestDashboard(moving)
        }

        composeRule.runOnIdle { assertTrue(view.keepScreenOn) }
    }

    @Test
    fun dashboardContent_leavesTheComposition_letsTheScreenGoOff() {
        lateinit var view: View
        var shown by mutableStateOf(true)
        composeRule.setContent {
            view = LocalView.current
            if (shown) TestDashboard(moving)
        }
        composeRule.runOnIdle { assertTrue(view.keepScreenOn) }

        shown = false

        composeRule.runOnIdle { assertFalse(view.keepScreenOn) }
    }

    private companion object {
        const val ROADBOOK_TAG = "roadbook"
        const val MAP_TAG = "map"

        /** The default theme's minimum touch target. */
        val MIN_TOUCH_TARGET = 56.dp
    }
}
