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

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.geo.Distance
import dev.giste.geo.GeoPoint
import dev.giste.roadbook.ImportRoadbookFailure
import dev.giste.roadbook.RoadbookStyle
import dev.giste.roadbook.ShortDistanceThreshold
import dev.giste.roadbook.domain.drawing.CanvasPoint
import dev.giste.roadbook.domain.drawing.Road
import dev.giste.roadbook.domain.drawing.RoadType
import dev.giste.roadbook.domain.drawing.Track
import dev.giste.roadbook.domain.roadbook.DangerLevel
import dev.giste.roadbook.domain.roadbook.Roadbook
import dev.giste.roadbook.domain.roadbook.Waypoint
import dev.giste.roadbook.domain.roadbook.WaypointNumber

// Preview-only sample data: a short roadbook, and the viewer in each of its states.

/** Width of the previews, a phone in portrait. */
private const val WIDTH = 420

/** Height of the previews of states without a roadbook. */
private const val SHORT = 400

/** Height of the previews with a roadbook, to show a few waypoints. */
private const val TALL = 700

/** A roadbook of six waypoints, alternating left and right turns, with some marks. */
private val SAMPLE_ROADBOOK: Roadbook = Roadbook(
    waypoints = (1..6).map { number ->
        Waypoint(
            number = WaypointNumber(number),
            position = GeoPoint(latitude = 37.1, longitude = -2.5),
            altitude = null,
            partial = Distance.meters(830.0 * (number - 1)),
            distanceFromPrevious = Distance.meters(if (number == 1) 0.0 else 830.0),
            dangerLevel = if (number == 4) DangerLevel.HIGH else DangerLevel.NONE,
            tulip = listOf(
                Track(
                    roadIn = Road(start = null, end = CanvasPoint(0.0, 45.0)),
                    roadOut = Road(
                        start = null,
                        end = CanvasPoint(if (number % 2 == 0) 75.0 else -75.0, -20.0),
                        type = RoadType.TARMAC_ROAD,
                    ),
                ),
            ),
        )
    },
)

/** The sample roadbook, from its second waypoint. */
private val SAMPLE_CONTENT = RoadbookContentState.Content(
    roadbook = SAMPLE_ROADBOOK,
    position = WaypointNumber(2),
    shortDistanceThreshold = ShortDistanceThreshold.DEFAULT,
)

/** The viewer in [state], with the import button shown, in the app theme. */
@Composable
private fun ScreenPreview(state: RoadbookUiState) {
    GisteTheme {
        RoadbookViewerContent(
            state = state,
            showImportButton = true,
            onImportClick = {},
            onImportFailureShown = {},
            onSetPartial = {},
            onScrollStopped = {},
        )
    }
}

@Preview(
    name = "Loading",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    widthDp = WIDTH,
    heightDp = SHORT,
)
@Composable
private fun RoadbookViewerLoadingPreview() {
    ScreenPreview(RoadbookUiState(roadbook = RoadbookContentState.Loading))
}

@Preview(
    name = "Empty, light",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    widthDp = WIDTH,
    heightDp = SHORT,
)
@Preview(
    name = "Empty, dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = WIDTH,
    heightDp = SHORT,
)
@Composable
private fun RoadbookViewerEmptyPreview() {
    ScreenPreview(RoadbookUiState(roadbook = RoadbookContentState.Empty))
}

@Preview(
    name = "Unreadable",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    widthDp = WIDTH,
    heightDp = SHORT,
)
@Composable
private fun RoadbookViewerUnreadablePreview() {
    ScreenPreview(RoadbookUiState(roadbook = RoadbookContentState.Unreadable))
}

@Preview(
    name = "Content, light",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    widthDp = WIDTH,
    heightDp = TALL,
)
@Preview(
    name = "Content, dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = WIDTH,
    heightDp = TALL,
)
@Composable
private fun RoadbookViewerContentPreview() {
    ScreenPreview(RoadbookUiState(roadbook = SAMPLE_CONTENT))
}

@Preview(
    name = "Content, FIA",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    widthDp = WIDTH,
    heightDp = TALL,
)
@Composable
private fun RoadbookViewerFiaPreview() {
    ScreenPreview(RoadbookUiState(style = RoadbookStyle.FIA, roadbook = SAMPLE_CONTENT))
}

@Preview(
    name = "Importing",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    widthDp = WIDTH,
    heightDp = TALL,
)
@Composable
private fun RoadbookViewerImportingPreview() {
    ScreenPreview(RoadbookUiState(roadbook = SAMPLE_CONTENT, import = ImportState.Importing))
}

@Preview(
    name = "Import failed",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    widthDp = WIDTH,
    heightDp = SHORT,
)
@Composable
private fun RoadbookViewerImportFailedPreview() {
    ScreenPreview(
        RoadbookUiState(
            roadbook = RoadbookContentState.Empty,
            import = ImportState.Failed(ImportRoadbookFailure.MALFORMED),
        ),
    )
}
