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

package dev.giste.roadbook.presentation.viewer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.geo.Distance
import dev.giste.roadbook.ImportRoadbookFailure
import dev.giste.roadbook.ShortDistanceThreshold
import dev.giste.roadbook.domain.roadbook.Roadbook
import dev.giste.roadbook.domain.roadbook.WaypointNumber
import dev.giste.roadbook.domain.roadbook.testWaypoint
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

    private fun show(initial: RoadbookUiState, showImportButton: Boolean = true) {
        state = initial
        rule.setContent {
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
    fun empty_showsMessageAndImportButton() {
        show(RoadbookUiState(roadbook = RoadbookContentState.Empty))

        rule.onNodeWithText("No roadbook loaded").assertIsDisplayed()
        rule.onNodeWithText("Import roadbook").performClick()

        assertEquals(1, importClicks)
    }

    @Test
    fun unreadable_asksToImportAgain() {
        show(RoadbookUiState(roadbook = RoadbookContentState.Unreadable))

        rule.onNodeWithText("The saved roadbook can't be read. Import it again.")
            .assertIsDisplayed()
        rule.onNodeWithText("Import roadbook").performClick()

        assertEquals(1, importClicks)
    }

    @Test
    fun empty_importButtonShownEvenWhileRiding() {
        show(RoadbookUiState(roadbook = RoadbookContentState.Empty), showImportButton = false)

        rule.onNodeWithText("Import roadbook").assertIsDisplayed()
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

        rule.onNodeWithText("Import roadbook").performClick()

        assertEquals(1, importClicks)
    }

    @Test
    fun content_whileRiding_hidesImportButton() {
        show(RoadbookUiState(roadbook = content()), showImportButton = false)

        rule.onAllNodesWithText("Import roadbook").assertCountEquals(0)
    }

    @Test
    fun importing_showsProgressAndHidesImportButton() {
        show(RoadbookUiState(roadbook = content(), import = ImportState.Importing))

        rule.onNode(progressIndicator).assertIsDisplayed()
        rule.onAllNodesWithText("Import roadbook").assertCountEquals(0)
    }

    @Test
    fun importing_withoutRoadbook_disablesImportButton() {
        show(RoadbookUiState(roadbook = RoadbookContentState.Empty, import = ImportState.Importing))

        rule.onNodeWithText("Import roadbook").assertIsNotEnabled()
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

        rule.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))
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

    private companion object {
        /** Longer than a short snackbar is shown. */
        const val SNACKBAR_DURATION_MILLIS = 10_000L
    }
}
