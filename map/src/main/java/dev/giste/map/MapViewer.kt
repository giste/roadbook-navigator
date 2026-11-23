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

package dev.giste.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.giste.map.ui.MapViewModel
import dev.giste.map.ui.MapViewerContent

/**
 * The map viewer: an offline map that follows the device location, turned so the direction of
 * travel points up.
 *
 * It gets its state from Hilt, so the app only places it: as a whole screen, or as part of one
 * (e.g. next to an odometer). It draws the mapsforge `.map` files installed in the app's
 * `map_files` folder (in its no-backup folder), with the zoom level and tilt of
 * [MapSettingsSource]. Without maps, or when none can be read, it shows a message instead. The
 * installed maps are looked for each time it is shown.
 *
 * By default it is read-only: touches pass through it to what is under it, so a glove or a bump
 * cannot move or zoom the map while riding, and it always shows the settings' zoom level and
 * tilt. An interactive map lets the user pan, zoom, rotate and tilt it with gestures until the
 * next location fix centers it again; the zoom level and tilt are kept until the settings change.
 *
 * While it is shown and has maps, it collects [MapLocationSource.locations]. The app must bind a
 * [MapLocationSource] and a [MapSettingsSource]. If the location needs a permission, show the
 * viewer only once it is granted.
 *
 * @param modifier the modifier applied to the viewer.
 * @param interactive whether the user may move, zoom, rotate and tilt the map with gestures.
 * Defaults to `false`: a read-only map.
 */
@Composable
public fun MapViewer(modifier: Modifier = Modifier, interactive: Boolean = false) {
    val viewModel: MapViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MapViewerContent(state = state, modifier = modifier, interactive = interactive)
}
