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

package dev.giste.map.ui

import dev.giste.map.MapSettings
import dev.giste.map.domain.MapCenter
import dev.giste.map.domain.MapFile

/** Everything the map viewer shows. It holds typed data only: the viewer turns it into text. */
internal sealed interface MapUiState {

    /** The installed maps are still being looked for. */
    data object Loading : MapUiState

    /** No map is installed, so there is nothing to draw. */
    data object NoMaps : MapUiState

    /** Map files are installed, but none can be read. */
    data object UnreadableMaps : MapUiState

    /**
     * A map to draw.
     *
     * @property files the map files to draw, never empty.
     * @property settings the zoom level and tilt.
     * @property center where the map is centered and which way it is turned, or `null` before the
     * first location fix.
     */
    data class Ready(
        val files: List<MapFile>,
        val settings: MapSettings,
        val center: MapCenter?,
    ) : MapUiState
}
