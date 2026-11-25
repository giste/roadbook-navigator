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

package dev.giste.map.ui.viewer

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
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

    /** The button that opens the map manager, which shows an icon instead of text. */
    const val MANAGE: String = "map_viewer_manage"
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
 * With [onManageMaps], a manage button floats in the top end corner while [showManageButton] is
 * set, and always when there is no map to draw ([MapUiState.NoMaps] or [MapUiState.UnreadableMaps]),
 * since managing the maps is then the only way forward. Without [onManageMaps] there is never one.
 * The button never takes the focus: it is meant to be touched, and an app may want the keys of a
 * remote control for itself.
 *
 * @param state what to show.
 * @param modifier the modifier applied to the viewer.
 * @param interactive whether the user may move, zoom, rotate and tilt the map with gestures.
 * Read-only by default.
 * @param showManageButton whether to show the manage button while there are maps.
 * @param onManageMaps called when the manage button is pressed, or `null` for no button.
 * @param map draws the map of a ready state, interactive or not, in the modifier it is given. A
 * slot, so tests and previews can use a placeholder instead of the OpenGL view.
 */
@Composable
internal fun MapViewerContent(
    state: MapUiState,
    modifier: Modifier = Modifier,
    interactive: Boolean = false,
    showManageButton: Boolean = false,
    onManageMaps: (() -> Unit)? = null,
    map: @Composable (state: MapUiState.Ready, interactive: Boolean, modifier: Modifier) -> Unit =
        { readyState, isInteractive, mapModifier ->
            VtmMap(state = readyState, interactive = isInteractive, modifier = mapModifier)
        },
) {
    Box(modifier = modifier) {
        when (state) {
            MapUiState.Loading ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag(MapViewerTestTags.LOADING),
                )
            MapUiState.NoMaps -> MapMessage(stringResource(R.string.map_no_maps), Modifier.fillMaxSize())
            MapUiState.UnreadableMaps ->
                MapMessage(stringResource(R.string.map_unreadable_maps), Modifier.fillMaxSize())
            is MapUiState.Ready -> {
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
        val noMapToDraw = state == MapUiState.NoMaps || state == MapUiState.UnreadableMaps
        if (onManageMaps != null && (showManageButton || noMapToDraw)) {
            ManageMapsButton(
                onClick = onManageMaps,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(GisteTheme.dimensions.spacingSmall),
            )
        }
    }
}

/**
 * The button that opens the map manager: a map icon on a filled primary circle, so it stands out
 * on any map, as large as the minimum touch target. It never takes the focus.
 *
 * @param onClick called when the button is pressed.
 * @param modifier applied to the button.
 */
@Composable
private fun ManageMapsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledIconButton(
        onClick = onClick,
        modifier = modifier
            .size(GisteTheme.dimensions.minTouchTarget)
            .focusProperties { canFocus = false }
            .testTag(MapViewerTestTags.MANAGE),
    ) {
        Icon(painter = painterResource(R.drawable.map_ic_map), contentDescription = null)
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

/**
 * The viewer in [state] in the app theme, with its manage button and a plain area in place of the
 * OpenGL map.
 */
@Composable
private fun MapViewerPreview(state: MapUiState) {
    GisteTheme {
        MapViewerContent(
            state = state,
            modifier = Modifier.fillMaxSize(),
            showManageButton = true,
            onManageMaps = {},
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
