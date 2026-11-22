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

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.giste.core.logging.Logger
import dev.giste.geo.Distance
import dev.giste.roadbook.ImportRoadbookResult
import dev.giste.roadbook.RoadbookController
import dev.giste.roadbook.RoadbookEvent
import dev.giste.roadbook.RoadbookSettings
import dev.giste.roadbook.RoadbookSettingsSource
import dev.giste.roadbook.domain.roadbook.RoadbookEvents
import dev.giste.roadbook.domain.roadbook.RoadbookPositionRepository
import dev.giste.roadbook.domain.roadbook.RoadbookRepository
import dev.giste.roadbook.domain.roadbook.SavedRoadbook
import dev.giste.roadbook.domain.roadbook.WaypointNumber
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The state and actions of the roadbook viewer.
 *
 * It combines the saved roadbook, its position and the settings into one [RoadbookUiState]. The
 * position shown is always one of the roadbook's waypoints, even if the saved one is not (e.g.
 * after importing a shorter roadbook). Imports go through the [RoadbookController], so they emit
 * their events and run one at a time with the app's commands.
 *
 * @param roadbookRepository the saved roadbook.
 * @param positionRepository the saved position, which scrolling changes.
 * @param settingsSource the roadbook settings.
 * @param controller imports roadbooks.
 * @param roadbookEvents where the request to set the partial is sent.
 * @param logger logs failures of the settings.
 */
@HiltViewModel
internal class RoadbookViewModel @Inject constructor(
    roadbookRepository: RoadbookRepository,
    private val positionRepository: RoadbookPositionRepository,
    settingsSource: RoadbookSettingsSource,
    private val controller: RoadbookController,
    private val roadbookEvents: RoadbookEvents,
    private val logger: Logger,
) : ViewModel() {

    /** The state of the import started from this viewer, if any. */
    private val importState = MutableStateFlow<ImportState>(ImportState.Idle)

    /** The settings, falling back to the defaults if the source fails. */
    private val settings = settingsSource.settings.catch { error ->
        logger.e(error, "Roadbook settings failed, using the defaults")
        emit(RoadbookSettings())
    }

    /**
     * The state of the viewer. It starts as [RoadbookContentState.Loading], and is shared while
     * the viewer is shown and for [STOP_TIMEOUT_MILLIS] after, so a configuration change does not
     * read everything again.
     */
    val uiState: StateFlow<RoadbookUiState> = combine(
        roadbookRepository.roadbook,
        positionRepository.position,
        settings,
        importState,
    ) { saved, position, settings, import ->
        RoadbookUiState(
            style = settings.style,
            roadbook = when (saved) {
                SavedRoadbook.None -> RoadbookContentState.Empty
                SavedRoadbook.Unreadable -> RoadbookContentState.Unreadable
                is SavedRoadbook.Available -> RoadbookContentState.Content(
                    roadbook = saved.roadbook,
                    position = saved.roadbook.coerceWaypointNumber(position),
                    shortDistanceThreshold = settings.shortDistanceThreshold,
                )
            },
            import = import,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = RoadbookUiState(),
    )

    /**
     * Imports the roadbook file at [uri], which the user picked.
     *
     * The state is [ImportState.Importing] meanwhile, then [ImportState.Idle] on success (the new
     * roadbook comes through the repository) or [ImportState.Failed].
     *
     * @param uri the file to import.
     */
    fun importRoadbook(uri: Uri) {
        viewModelScope.launch {
            importState.value = ImportState.Importing
            importState.value = when (val result = controller.importRoadbook(uri)) {
                ImportRoadbookResult.Success -> ImportState.Idle
                is ImportRoadbookResult.Failure -> ImportState.Failed(result.reason)
            }
        }
    }

    /** Tells that the failure of the last import was reported to the user, so it is not again. */
    fun onImportFailureShown() {
        if (importState.value is ImportState.Failed) {
            importState.value = ImportState.Idle
        }
    }

    /**
     * Asks the app to set the partial, because the user long-pressed a waypoint's distances.
     *
     * @param partial the partial the roadbook gives at that waypoint.
     */
    fun onSetPartial(partial: Distance) {
        viewModelScope.launch {
            roadbookEvents.emit(RoadbookEvent.SetPartialRequested(partial))
        }
    }

    /**
     * Saves the position after the user scrolled, so the roadbook is restored there.
     *
     * Nothing is saved without a roadbook, or when the waypoint at the top did not change.
     *
     * @param firstVisibleIndex the index of the first visible waypoint in the list, from 0.
     */
    fun onScrollStopped(firstVisibleIndex: Int) {
        val content = uiState.value.roadbook as? RoadbookContentState.Content ?: return
        val position = content.roadbook.coerceWaypointNumber(WaypointNumber(firstVisibleIndex + 1))
        if (position == content.position) return
        viewModelScope.launch { positionRepository.save(position) }
    }

    private companion object {
        /** Keeps the state across short gaps, such as a configuration change. */
        const val STOP_TIMEOUT_MILLIS: Long = 5_000
    }
}
