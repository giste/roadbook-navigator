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

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.geo.Bearing
import dev.giste.geo.GeoPoint
import dev.giste.map.MapSettings
import dev.giste.map.R
import dev.giste.map.domain.MapCenter
import dev.giste.map.domain.MapFile

/** Test tags of the map viewer, for the elements whose identity is not in their text. */
internal object MapViewerTestTags {

    /** The empty area shown while the maps are looked for. */
    const val LOADING: String = "map_viewer_loading"
}

/** Transparency of the attribution's background: the map shows through, the text stays legible. */
private const val ATTRIBUTION_BACKGROUND_ALPHA: Float = 0.7f

/**
 * Shows the map viewer in [state]: the map, or why there is none.
 *
 * - [MapUiState.Loading]: an empty area, as looking for the maps takes a moment.
 * - [MapUiState.NoMaps] and [MapUiState.UnreadableMaps]: a message on that area.
 * - [MapUiState.Ready]: the map, with the OpenStreetMap attribution that the map data's license
 *   requires in the bottom end corner.
 *
 * @param state what to show.
 * @param modifier the modifier applied to the viewer.
 * @param interactive whether the user may move, zoom, rotate and tilt the map with gestures.
 * Read-only by default.
 * @param map draws the map of a ready state, interactive or not, in the modifier it is given. A
 * slot, so tests and previews can use a placeholder instead of the OpenGL view.
 */
@Composable
internal fun MapViewerContent(
    state: MapUiState,
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    map: @Composable (state: MapUiState.Ready, interactive: Boolean, modifier: Modifier) -> Unit =
        { readyState, isInteractive, mapModifier ->
            VtmMap(state = readyState, interactive = isInteractive, modifier = mapModifier)
        },
) {
    when (state) {
        MapUiState.Loading -> Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
            // The tag goes on a child: a test tag in the caller's modifier would hide one here.
            Box(modifier = Modifier.fillMaxSize().testTag(MapViewerTestTags.LOADING))
        }
        MapUiState.NoMaps -> MapMessage(stringResource(R.string.map_no_maps), modifier)
        MapUiState.UnreadableMaps -> MapMessage(stringResource(R.string.map_unreadable_maps), modifier)
        is MapUiState.Ready -> Box(modifier = modifier) {
            map(state, interactive, Modifier.fillMaxSize())
            Text(
                text = stringResource(R.string.map_attribution),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = ATTRIBUTION_BACKGROUND_ALPHA),
                    )
                    .padding(horizontal = GisteTheme.dimensions.spacingExtraSmall),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

/**
 * A message in place of the map, centered on the map's area.
 *
 * @param text the message.
 * @param modifier the modifier applied to the area.
 */
@Composable
private fun MapMessage(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(GisteTheme.dimensions.spacingMedium),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}

/** Preview size, about the map's share of a portrait tablet dashboard. */
private const val PREVIEW_WIDTH: Int = 400
private const val PREVIEW_HEIGHT: Int = 240

/** A ready state for previews: one map, centered with a bearing. */
private val PREVIEW_READY = MapUiState.Ready(
    files = listOf(MapFile(name = "spain.map", path = "/maps/spain.map")),
    settings = MapSettings(),
    center = MapCenter(position = GeoPoint(40.4168, -3.7038), bearing = Bearing.degrees(45.0)),
)

/** The viewer in [state] in the app theme, with a plain area in place of the OpenGL map. */
@Composable
private fun MapViewerPreview(state: MapUiState) {
    GisteTheme {
        MapViewerContent(
            state = state,
            modifier = Modifier.fillMaxSize(),
            map = { _, _, mapModifier ->
                Box(mapModifier.background(MaterialTheme.colorScheme.tertiaryContainer))
            },
        )
    }
}

@Preview(
    name = "Loading, light",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    widthDp = PREVIEW_WIDTH,
    heightDp = PREVIEW_HEIGHT,
)
@Preview(
    name = "Loading, dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = PREVIEW_WIDTH,
    heightDp = PREVIEW_HEIGHT,
)
@Composable
private fun MapViewerLoadingPreview() {
    MapViewerPreview(MapUiState.Loading)
}

@Preview(
    name = "No maps, light",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    widthDp = PREVIEW_WIDTH,
    heightDp = PREVIEW_HEIGHT,
)
@Preview(
    name = "No maps, dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = PREVIEW_WIDTH,
    heightDp = PREVIEW_HEIGHT,
)
@Composable
private fun MapViewerNoMapsPreview() {
    MapViewerPreview(MapUiState.NoMaps)
}

@Preview(
    name = "Unreadable maps, light",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    widthDp = PREVIEW_WIDTH,
    heightDp = PREVIEW_HEIGHT,
)
@Preview(
    name = "Unreadable maps, dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = PREVIEW_WIDTH,
    heightDp = PREVIEW_HEIGHT,
)
@Composable
private fun MapViewerUnreadableMapsPreview() {
    MapViewerPreview(MapUiState.UnreadableMaps)
}

@Preview(
    name = "Ready, light",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    widthDp = PREVIEW_WIDTH,
    heightDp = PREVIEW_HEIGHT,
)
@Preview(
    name = "Ready, dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = PREVIEW_WIDTH,
    heightDp = PREVIEW_HEIGHT,
)
@Composable
private fun MapViewerReadyPreview() {
    MapViewerPreview(PREVIEW_READY)
}
