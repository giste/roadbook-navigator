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

package dev.giste.roadbook.ui.viewer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.geo.Distance
import dev.giste.roadbook.ImportRoadbookFailure
import dev.giste.roadbook.ShortDistanceThreshold
import dev.giste.roadbook.domain.roadbook.Roadbook
import dev.giste.roadbook.domain.roadbook.WaypointNumber
import dev.giste.roadbook.domain.roadbook.testWaypoint
import dev.giste.roadbook.ui.waypoint.WaypointTestTags
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoadbookViewerContentTest {

    @get:Rule
    val rule = createComposeRule()

    /** Ten waypoints, 1 km apart, so the partial of waypoint n is (n - 1) km. */
    private val roadbook = Roadbook(
        waypoints = (1..10).map { number ->
            testWaypoint(number, partial = Distance.kilometers(number - 1.0))
        },
    )

    private fun content(position: Int = 1) = RoadbookContentState.Content(
        roadbook = roadbook,
        position = WaypointNumber(position),
        shortDistanceThreshold = ShortDistanceThreshold.DEFAULT,
    )

    private var importClicks = 0
    private var failuresShown = 0
    private val setPartials = mutableListOf<Distance>()
    private val scrollStops = mutableListOf<Int>()

    /** What the screen shows. Tests change it to update the screen. */
    private var state by mutableStateOf(RoadbookUiState())

    /** Switches between touch and key input, as a remote control's key does. */
    private lateinit var inputModeManager: InputModeManager

    private fun show(initial: RoadbookUiState, showImportButton: Boolean = true) {
        state = initial
        rule.setContent {
            inputModeManager = LocalInputModeManager.current
            GisteTheme {
                RoadbookViewerContent(
                    state = state,
                    showImportButton = showImportButton,
                    onImportClick = { importClicks++ },
                    onImportFailureShown = { failuresShown++ },
                    onSetPartial = { setPartials += it },
                    onScrollStopped = { scrollStops += it },
                )
            }
        }
    }

    private val progressIndicator =
        hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)

    @Test
    fun loading_showsProgressIndicator() {
        show(RoadbookUiState(roadbook = RoadbookContentState.Loading))

        rule.onNode(progressIndicator).assertIsDisplayed()
    }

    @Test
    fun loading_hidesImportButton() {
        show(RoadbookUiState(roadbook = RoadbookContentState.Loading))

        rule.onAllNodesWithTag(RoadbookViewerTestTags.IMPORT).assertCountEquals(0)
    }

    @Test
    fun empty_showsMessageAndImportButton_importsOnClick() {
        show(RoadbookUiState(roadbook = RoadbookContentState.Empty))

        rule.onNodeWithText("No roadbook loaded").assertIsDisplayed()
        rule.onNodeWithTag(RoadbookViewerTestTags.IMPORT).performClick()

        assertEquals(1, importClicks)
    }

    @Test
    fun empty_whileRiding_showsMessageAndHidesImportButton() {
        show(RoadbookUiState(roadbook = RoadbookContentState.Empty), showImportButton = false)

        rule.onNodeWithText("No roadbook loaded").assertIsDisplayed()
        rule.onAllNodesWithTag(RoadbookViewerTestTags.IMPORT).assertCountEquals(0)
    }

    @Test
    fun unreadable_asksToImportAgain_importsOnClick() {
        show(RoadbookUiState(roadbook = RoadbookContentState.Unreadable))

        rule.onNodeWithText("The saved roadbook can't be read. Import it again.")
            .assertIsDisplayed()
        rule.onNodeWithTag(RoadbookViewerTestTags.IMPORT).performClick()

        assertEquals(1, importClicks)
    }

    @Test
    fun unreadable_whileRiding_hidesImportButton() {
        show(RoadbookUiState(roadbook = RoadbookContentState.Unreadable), showImportButton = false)

        rule.onAllNodesWithTag(RoadbookViewerTestTags.IMPORT).assertCountEquals(0)
    }

    @Test
    fun content_showsWaypointsFromPosition() {
        show(RoadbookUiState(roadbook = content(position = 3)))

        // The partial of waypoint 3, the first one shown.
        rule.onNodeWithText("2.00").assertIsDisplayed()
        rule.onAllNodesWithText("0.00").assertCountEquals(0)
    }

    @Test
    fun content_importButtonShown_importsOnClick() {
        show(RoadbookUiState(roadbook = content()))

        rule.onNodeWithTag(RoadbookViewerTestTags.IMPORT).performClick()

        assertEquals(1, importClicks)
    }

    @Test
    fun content_whileRiding_hidesImportButton() {
        show(RoadbookUiState(roadbook = content()), showImportButton = false)

        rule.onAllNodesWithTag(RoadbookViewerTestTags.IMPORT).assertCountEquals(0)
    }

    @Test
    fun importing_showsProgressAndHidesImportButton() {
        show(RoadbookUiState(roadbook = content(), import = ImportState.Importing))

        rule.onNode(progressIndicator).assertIsDisplayed()
        rule.onAllNodesWithTag(RoadbookViewerTestTags.IMPORT).assertCountEquals(0)
    }

    @Test
    fun importing_withoutRoadbook_showsProgressAndHidesImportButton() {
        show(RoadbookUiState(roadbook = RoadbookContentState.Empty, import = ImportState.Importing))

        rule.onNode(progressIndicator).assertIsDisplayed()
        rule.onAllNodesWithTag(RoadbookViewerTestTags.IMPORT).assertCountEquals(0)
    }

    @Test
    fun importButton_keyModeFocusRequested_doesNotTakeIt() {
        show(RoadbookUiState(roadbook = content()))
        // As after a key of a remote: in touch mode no button takes the focus anyway.
        rule.runOnIdle { inputModeManager.requestInputMode(InputMode.Keyboard) }
        val button = rule.onNodeWithTag(RoadbookViewerTestTags.IMPORT)

        // Where an unhandled arrow or Tab key would move the focus.
        button.requestFocus()

        button.assert(isFocused().not())
    }

    @Test
    fun importFailed_eachReason_showsItsMessage() {
        val messages = mapOf(
            ImportRoadbookFailure.UNREADABLE to "The file can't be read.",
            ImportRoadbookFailure.MALFORMED to "The file is not a valid roadbook.",
            ImportRoadbookFailure.NO_WAYPOINTS to "The roadbook has no waypoints.",
            ImportRoadbookFailure.NOT_SAVED to "The roadbook can't be saved on this device.",
        )
        show(RoadbookUiState(roadbook = RoadbookContentState.Empty))

        // waitUntil fails the test when a message is not shown.
        messages.forEach { (reason, message) ->
            state = state.copy(import = ImportState.Failed(reason))
            rule.waitUntil { rule.onAllNodesWithText(message).fetchSemanticsNodes().isNotEmpty() }
            // Back to idle, as the ViewModel does once the failure is shown.
            state = state.copy(import = ImportState.Idle)
            rule.waitForIdle()
        }
    }

    @Test
    fun importFailed_reportsItWasShown() {
        show(
            RoadbookUiState(
                roadbook = RoadbookContentState.Empty,
                import = ImportState.Failed(ImportRoadbookFailure.MALFORMED),
            ),
        )

        rule.onNodeWithText("The file is not a valid roadbook.").assertIsDisplayed()
        rule.mainClock.advanceTimeBy(SNACKBAR_DURATION_MILLIS)
        rule.waitForIdle()

        assertEquals(1, failuresShown)
    }

    @Test
    fun longPress_waypointDistances_setsItsPartial() {
        show(RoadbookUiState(roadbook = content(position = 3)))

        rule.onAllNodesWithTag(WaypointTestTags.DISTANCES)
            .onFirst()
            .performTouchInput { longClick() }

        assertEquals(listOf(Distance.kilometers(2.0)), setPartials)
    }

    @Test
    fun positionChange_scrollsToIt() {
        show(RoadbookUiState(roadbook = content(position = 1)))

        state = state.copy(roadbook = content(position = 6))
        rule.waitForIdle()

        // Waypoint 6 is at the top, and the list reports it once it stops.
        rule.onNodeWithText("5.00").assertIsDisplayed()
        assertEquals(5, scrollStops.last())
    }

    @Test
    fun positionChange_lastWaypoint_scrollsItToTheTop() {
        show(RoadbookUiState(roadbook = content(position = 1)))

        state = state.copy(roadbook = content(position = 10))
        rule.waitForIdle()

        rule.onNodeWithText("9.00").assertIsDisplayed()
        assertEquals(9, scrollStops.last())
    }

    @Test
    fun userScroll_reportsFirstVisibleWaypointWhenItStops() {
        show(RoadbookUiState(roadbook = content(position = 1)))
        scrollStops.clear()

        rule.onNode(hasScrollAction()).performTouchInput { swipeUp() }
        rule.waitForIdle()

        assertTrue(scrollStops.last() > 0)
    }

    @Test
    fun userDrag_lessThanHalfARow_snapsBackToTheSameWaypoint() {
        show(RoadbookUiState(roadbook = content(position = 1)))
        scrollStops.clear()

        dragUpAndHold(rowFraction = 0.3f)

        assertTopRowAligned()
        assertEquals(0, scrollStops.last())
    }

    @Test
    fun userDrag_moreThanHalfARow_snapsToTheNextWaypoint() {
        show(RoadbookUiState(roadbook = content(position = 1)))
        scrollStops.clear()

        dragUpAndHold(rowFraction = 0.7f)

        assertTopRowAligned()
        assertEquals(1, scrollStops.last())
    }

    @Test
    fun userFling_leavesAWaypointAtTheTop() {
        show(RoadbookUiState(roadbook = content(position = 1)))
        scrollStops.clear()

        rule.onNode(hasScrollAction()).performTouchInput { swipeUp() }
        rule.waitForIdle()

        assertTopRowAligned()
    }

    /**
     * One node per waypoint shown: its distance section, which takes the whole height of the row. Rows
     * the list keeps composed but not placed, to reuse them, are left out: their positions are
     * stale.
     */
    private fun rows(): List<SemanticsNode> =
        rule.onAllNodesWithTag(WaypointTestTags.DISTANCES)
            .fetchSemanticsNodes()
            .filter { it.layoutInfo.isPlaced }

    /**
     * Drags the list up by [rowFraction] of a row and holds still before lifting the finger, so
     * the list is released without velocity, as after a slow drag.
     */
    private fun dragUpAndHold(rowFraction: Float) {
        val rowHeight = rows().first().size.height
        rule.onNode(hasScrollAction()).performTouchInput {
            down(center)
            // Small steps, so the whole distance scrolls the list once past the touch slop.
            moveBy(Offset(0f, -viewConfiguration.touchSlop))
            repeat(DRAG_STEPS) { moveBy(Offset(0f, -rowHeight * rowFraction / DRAG_STEPS)) }
            advanceEventTime(HOLD_MILLIS)
            up()
        }
        rule.waitForIdle()
    }

    /** Checks that a waypoint starts exactly at the top of the list, and none is cut by it. */
    private fun assertTopRowAligned() {
        val listTop = rule.onNode(hasScrollAction()).fetchSemanticsNode().positionInRoot.y
        // Unclipped positions: a row half scrolled out of the list starts above it.
        val tops = rows().map { it.positionInRoot.y to it.positionInRoot.y + it.size.height }
        assertTrue(tops.any { (top, _) -> abs(top - listTop) < TOLERANCE_PX })
        assertTrue(
            tops.none { (top, bottom) ->
                top < listTop - TOLERANCE_PX && bottom > listTop + TOLERANCE_PX
            },
        )
    }

    private companion object {
        /** Steps of a slow drag. */
        const val DRAG_STEPS = 10

        /** Longer than the velocity tracker remembers, so the finger is lifted without speed. */
        const val HOLD_MILLIS = 500L

        /** Rounding allowed when comparing positions in pixels. */
        const val TOLERANCE_PX = 1f

        /** Longer than a short snackbar is shown. */
        const val SNACKBAR_DURATION_MILLIS = 10_000L
    }
}
