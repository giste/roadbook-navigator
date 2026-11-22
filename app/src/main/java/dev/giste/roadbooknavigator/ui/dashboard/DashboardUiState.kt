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

import dev.giste.geo.Distance

/**
 * What the dashboard shows.
 *
 * @property odometer the partial and total distances.
 * @property showStoppedActions whether the actions offered only while the vehicle is stopped are
 * shown: importing a roadbook and opening the settings, so the rider is not tempted to use them
 * while riding.
 */
internal data class DashboardUiState(
    val odometer: OdometerUiState,
    val showStoppedActions: Boolean,
) {
    companion object {
        /** Before anything is known: the distances are loading and the vehicle counts as stopped. */
        val INITIAL: DashboardUiState =
            DashboardUiState(odometer = OdometerUiState.Loading, showStoppedActions = true)
    }
}

/** The odometer distances, in rally terms: the odometer's trip distance is the partial. */
internal sealed interface OdometerUiState {

    /** The distances have not been read from storage yet. */
    data object Loading : OdometerUiState

    /**
     * The current distances.
     *
     * @property partial the distance since the last reset or set of the partial.
     * @property total the distance since the last reset of both.
     */
    data class Available(val partial: Distance, val total: Distance) : OdometerUiState
}
