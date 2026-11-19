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

import android.net.Uri
import app.cash.turbine.test
import dev.giste.geo.Distance
import dev.giste.roadbook.FakeLogger
import dev.giste.roadbook.FakeRoadbookController
import dev.giste.roadbook.FakeRoadbookSettingsSource
import dev.giste.roadbook.ImportRoadbookFailure
import dev.giste.roadbook.ImportRoadbookResult
import dev.giste.roadbook.MainDispatcherRule
import dev.giste.roadbook.RoadbookEvent
import dev.giste.roadbook.RoadbookSettings
import dev.giste.roadbook.RoadbookStyle
import dev.giste.roadbook.ShortDistanceThreshold
import dev.giste.roadbook.domain.roadbook.FakeRoadbookPositionRepository
import dev.giste.roadbook.domain.roadbook.FakeRoadbookRepository
import dev.giste.roadbook.domain.roadbook.Roadbook
import dev.giste.roadbook.domain.roadbook.RoadbookEvents
import dev.giste.roadbook.domain.roadbook.SavedRoadbook
import dev.giste.roadbook.domain.roadbook.WaypointNumber
import dev.giste.roadbook.domain.roadbook.testWaypoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Runs on Robolectric only because importing takes an Android `Uri`. */
// advanceUntilIdle() and UnconfinedTestDispatcher are experimental in kotlinx-coroutines-test.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RoadbookViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val threeWaypoints = Roadbook(
        waypoints = listOf(testWaypoint(1), testWaypoint(2), testWaypoint(3)),
    )
    private val uri: Uri = Uri.parse("content://dev.giste.roadbook.test/documents/1")

    private val roadbookRepository =
        FakeRoadbookRepository(SavedRoadbook.Available(threeWaypoints))
    private val positionRepository = FakeRoadbookPositionRepository(WaypointNumber(2))
    private val settings = MutableStateFlow(RoadbookSettings())
    private val controller = FakeRoadbookController()
    private val roadbookEvents = RoadbookEvents()
    private val logger = FakeLogger()

    private fun createViewModel(settingsFlow: Flow<RoadbookSettings> = settings) =
        RoadbookViewModel(
            roadbookRepository = roadbookRepository,
            positionRepository = positionRepository,
            settingsSource = FakeRoadbookSettingsSource(settingsFlow),
            controller = controller,
            roadbookEvents = roadbookEvents,
            logger = logger,
        )

    /** Creates the ViewModel and collects its state, as the screen does, until the test ends. */
    private fun TestScope.collectedViewModel(
        settingsFlow: Flow<RoadbookSettings> = settings,
    ): RoadbookViewModel = createViewModel(settingsFlow).also { viewModel ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()
    }

    @Test
    fun uiState_beforeAnythingIsRead_isLoading() {
        val viewModel = createViewModel()

        assertEquals(RoadbookContentState.Loading, viewModel.uiState.value.roadbook)
    }

    @Test
    fun uiState_noRoadbook_isEmpty() = runTest {
        roadbookRepository.roadbook.value = SavedRoadbook.None

        val viewModel = collectedViewModel()

        assertEquals(RoadbookContentState.Empty, viewModel.uiState.value.roadbook)
    }

    @Test
    fun uiState_unreadableRoadbook_isUnreadable() = runTest {
        roadbookRepository.roadbook.value = SavedRoadbook.Unreadable

        val viewModel = collectedViewModel()

        assertEquals(RoadbookContentState.Unreadable, viewModel.uiState.value.roadbook)
    }

    @Test
    fun uiState_roadbook_isContentAtSavedPosition() = runTest {
        val viewModel = collectedViewModel()

        assertEquals(
            RoadbookContentState.Content(
                roadbook = threeWaypoints,
                position = WaypointNumber(2),
                shortDistanceThreshold = ShortDistanceThreshold.DEFAULT,
            ),
            viewModel.uiState.value.roadbook,
        )
    }

    @Test
    fun uiState_positionPastLastWaypoint_showsLastWaypoint() = runTest {
        positionRepository.position.value = WaypointNumber(50)

        val viewModel = collectedViewModel()

        val content = viewModel.uiState.value.roadbook as RoadbookContentState.Content
        assertEquals(WaypointNumber(3), content.position)
    }

    @Test
    fun uiState_settings_setStyleAndThreshold() = runTest {
        val threshold = ShortDistanceThreshold.of(Distance.meters(500.0))
        settings.value =
            RoadbookSettings(shortDistanceThreshold = threshold, style = RoadbookStyle.FIA)

        val viewModel = collectedViewModel()

        val state = viewModel.uiState.value
        assertEquals(RoadbookStyle.FIA, state.style)
        val content = state.roadbook as RoadbookContentState.Content
        assertEquals(threshold, content.shortDistanceThreshold)
    }

    @Test
    fun uiState_settingsChange_isApplied() = runTest {
        val viewModel = collectedViewModel()

        settings.value = RoadbookSettings(style = RoadbookStyle.FIA)
        advanceUntilIdle()

        assertEquals(RoadbookStyle.FIA, viewModel.uiState.value.style)
    }

    @Test
    fun uiState_settingsFail_usesDefaultsAndLogsError() = runTest {
        val failing = flow<RoadbookSettings> { throw IllegalStateException("Simulated failure") }

        val viewModel = collectedViewModel(settingsFlow = failing)

        assertEquals(RoadbookSettings().style, viewModel.uiState.value.style)
        assertTrue(viewModel.uiState.value.roadbook is RoadbookContentState.Content)
        assertTrue(logger.loggedErrors.single() is IllegalStateException)
    }

    @Test
    fun importRoadbook_running_isImporting() = runTest {
        controller.gate = CompletableDeferred()
        val viewModel = collectedViewModel()

        viewModel.importRoadbook(uri)
        advanceUntilIdle()

        assertEquals(ImportState.Importing, viewModel.uiState.value.import)
        assertEquals(listOf(uri), controller.importedUris)
    }

    @Test
    fun importRoadbook_success_isIdleAgain() = runTest {
        val viewModel = collectedViewModel()

        viewModel.importRoadbook(uri)
        advanceUntilIdle()

        assertEquals(ImportState.Idle, viewModel.uiState.value.import)
    }

    @Test
    fun importRoadbook_failure_reportsReason() = runTest {
        controller.result = ImportRoadbookResult.Failure(ImportRoadbookFailure.MALFORMED)
        val viewModel = collectedViewModel()

        viewModel.importRoadbook(uri)
        advanceUntilIdle()

        assertEquals(
            ImportState.Failed(ImportRoadbookFailure.MALFORMED),
            viewModel.uiState.value.import,
        )
    }

    @Test
    fun onImportFailureShown_afterFailure_isIdle() = runTest {
        controller.result = ImportRoadbookResult.Failure(ImportRoadbookFailure.UNREADABLE)
        val viewModel = collectedViewModel()
        viewModel.importRoadbook(uri)
        advanceUntilIdle()

        viewModel.onImportFailureShown()
        advanceUntilIdle()

        assertEquals(ImportState.Idle, viewModel.uiState.value.import)
    }

    @Test
    fun onImportFailureShown_whileImporting_keepsImporting() = runTest {
        controller.gate = CompletableDeferred()
        val viewModel = collectedViewModel()
        viewModel.importRoadbook(uri)
        advanceUntilIdle()

        viewModel.onImportFailureShown()
        advanceUntilIdle()

        assertEquals(ImportState.Importing, viewModel.uiState.value.import)
    }

    @Test
    fun onSetPartial_emitsSetPartialRequested() = runTest {
        val viewModel = collectedViewModel()

        roadbookEvents.events.test {
            viewModel.onSetPartial(Distance.meters(1_250.0))

            val expected = RoadbookEvent.SetPartialRequested(Distance.meters(1_250.0))
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun onScrollStopped_newWaypointAtTop_savesIt() = runTest {
        val viewModel = collectedViewModel()

        viewModel.onScrollStopped(firstVisibleIndex = 2)
        advanceUntilIdle()

        assertEquals(listOf(WaypointNumber(3)), positionRepository.saved)
    }

    @Test
    fun onScrollStopped_sameWaypointAtTop_savesNothing() = runTest {
        val viewModel = collectedViewModel()

        viewModel.onScrollStopped(firstVisibleIndex = 1)
        advanceUntilIdle()

        assertTrue(positionRepository.saved.isEmpty())
    }

    @Test
    fun onScrollStopped_pastLastWaypoint_savesLastOne() = runTest {
        val viewModel = collectedViewModel()

        viewModel.onScrollStopped(firstVisibleIndex = 10)
        advanceUntilIdle()

        assertEquals(listOf(WaypointNumber(3)), positionRepository.saved)
    }

    @Test
    fun onScrollStopped_noRoadbook_savesNothing() = runTest {
        roadbookRepository.roadbook.value = SavedRoadbook.None
        val viewModel = collectedViewModel()

        viewModel.onScrollStopped(firstVisibleIndex = 2)
        advanceUntilIdle()

        assertTrue(positionRepository.saved.isEmpty())
    }
}
