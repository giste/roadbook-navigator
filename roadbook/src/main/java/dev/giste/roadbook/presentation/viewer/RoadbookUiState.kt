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

import dev.giste.roadbook.ImportRoadbookFailure
import dev.giste.roadbook.RoadbookSettings
import dev.giste.roadbook.RoadbookStyle
import dev.giste.roadbook.ShortDistanceThreshold
import dev.giste.roadbook.domain.roadbook.Roadbook
import dev.giste.roadbook.domain.roadbook.WaypointNumber

/**
 * Everything the roadbook viewer shows. It holds typed data only: the viewer turns it into text.
 *
 * @property style how the roadbook is colored.
 * @property roadbook the roadbook, or why there is none to show.
 * @property import the state of the last import the user started from this viewer.
 */
internal data class RoadbookUiState(
    val style: RoadbookStyle = RoadbookSettings().style,
    val roadbook: RoadbookContentState = RoadbookContentState.Loading,
    val import: ImportState = ImportState.Idle,
)

/** The roadbook part of [RoadbookUiState]. */
internal sealed interface RoadbookContentState {

    /** The saved roadbook and its position are still being read. */
    data object Loading : RoadbookContentState

    /** No roadbook has been imported yet. */
    data object Empty : RoadbookContentState

    /** A roadbook was saved, but it cannot be read back, so it must be imported again. */
    data object Unreadable : RoadbookContentState

    /**
     * A roadbook to navigate.
     *
     * @property roadbook the roadbook.
     * @property position the waypoint shown at the top, always one of the roadbook's.
     * @property shortDistanceThreshold below which distance from the previous waypoint a waypoint
     * is highlighted.
     */
    data class Content(
        val roadbook: Roadbook,
        val position: WaypointNumber,
        val shortDistanceThreshold: ShortDistanceThreshold,
    ) : RoadbookContentState
}

/** The state of an import the user started from the roadbook viewer. */
internal sealed interface ImportState {

    /** No import is running, and the last one has nothing left to report. */
    data object Idle : ImportState

    /** A file is being read, parsed and saved. */
    data object Importing : ImportState

    /**
     * The last import failed. The viewer reports it once, then tells the ViewModel so.
     *
     * @property reason why it failed.
     */
    data class Failed(val reason: ImportRoadbookFailure) : ImportState
}
