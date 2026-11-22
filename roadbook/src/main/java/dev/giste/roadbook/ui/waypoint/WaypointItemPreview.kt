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

package dev.giste.roadbook.ui.waypoint

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.geo.Distance
import dev.giste.geo.GeoPoint
import dev.giste.roadbook.RoadbookStyle
import dev.giste.roadbook.ShortDistanceThreshold
import dev.giste.roadbook.domain.drawing.CanvasPoint
import dev.giste.roadbook.domain.drawing.Icon
import dev.giste.roadbook.domain.drawing.IconType
import dev.giste.roadbook.domain.drawing.Road
import dev.giste.roadbook.domain.drawing.RoadType
import dev.giste.roadbook.domain.drawing.Text
import dev.giste.roadbook.domain.drawing.Track
import dev.giste.roadbook.domain.roadbook.DangerLevel
import dev.giste.roadbook.domain.roadbook.Waypoint
import dev.giste.roadbook.domain.roadbook.WaypointNumber
import dev.giste.roadbook.ui.theme.RoadbookTheme

// Preview-only sample data: one waypoint per state the row can show.

/** A waypoint numbered [number], turning right off a tarmac road. */
private fun sampleWaypoint(
    number: Int,
    partialMeters: Double,
    fromPreviousMeters: Double,
    resetsPartial: Boolean = false,
    dangerLevel: DangerLevel = DangerLevel.NONE,
): Waypoint = Waypoint(
    number = WaypointNumber(number),
    position = GeoPoint(latitude = 37.1, longitude = -2.5),
    altitude = null,
    partial = Distance.meters(partialMeters),
    distanceFromPrevious = Distance.meters(fromPreviousMeters),
    resetsPartial = resetsPartial,
    dangerLevel = dangerLevel,
    tulip = listOf(
        Road(start = null, end = CanvasPoint(-70.0, -40.0), type = RoadType.TARMAC_ROAD),
        Track(
            roadIn = Road(start = null, end = CanvasPoint(0.0, 45.0), type = RoadType.TARMAC_ROAD),
            roadOut = Road(start = null, end = CanvasPoint(75.0, -10.0), type = RoadType.TRACK),
        ),
    ),
    notes = listOf(
        Icon(type = IconType.CHURCH, width = 40.0, height = 40.0, center = CanvasPoint(40.0, 40.0)),
        Text(
            text = "Village",
            lineHeight = 1.0,
            width = 100.0,
            height = 30.0,
            maxWidth = 120.0,
            maxHeight = 40.0,
            center = CanvasPoint(130.0, 40.0),
        ),
    ),
)

/** One waypoint of each state: plain, reset, coming soon, and each danger level. */
private val SAMPLE_WAYPOINTS: List<Waypoint> = listOf(
    sampleWaypoint(number = 12, partialMeters = 25_430.0, fromPreviousMeters = 1_270.0),
    sampleWaypoint(
        number = 13,
        partialMeters = 26_100.0,
        fromPreviousMeters = 670.0,
        resetsPartial = true,
    ),
    sampleWaypoint(number = 14, partialMeters = 120.0, fromPreviousMeters = 120.0),
    sampleWaypoint(
        number = 15,
        partialMeters = 2_450.0,
        fromPreviousMeters = 2_330.0,
        dangerLevel = DangerLevel.MEDIUM,
    ),
    sampleWaypoint(
        number = 16,
        partialMeters = 3_010.0,
        fromPreviousMeters = 560.0,
        dangerLevel = DangerLevel.HIGH,
    ),
)

/** Every sample waypoint, one row under the other. */
@Composable
private fun SampleRows() {
    Surface(color = RoadbookTheme.colors.surface) {
        Column(
            modifier = Modifier.padding(GisteTheme.dimensions.spacingSmall),
            verticalArrangement = Arrangement.spacedBy(GisteTheme.dimensions.spacingSmall),
        ) {
            SAMPLE_WAYPOINTS.forEach { waypoint ->
                WaypointItem(
                    waypoint = waypoint,
                    shortDistanceThreshold = ShortDistanceThreshold.DEFAULT,
                    onSetPartial = {},
                )
            }
        }
    }
}

@Preview(name = "App theme, light", uiMode = Configuration.UI_MODE_NIGHT_NO, widthDp = 420)
@Preview(name = "App theme, dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 420)
@Composable
private fun WaypointItemAppThemePreview() {
    GisteTheme {
        RoadbookTheme(style = RoadbookStyle.APP_THEME) {
            SampleRows()
        }
    }
}

@Preview(name = "FIA, light", uiMode = Configuration.UI_MODE_NIGHT_NO, widthDp = 420)
@Preview(name = "FIA, dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 420)
@Composable
private fun WaypointItemFiaPreview() {
    GisteTheme {
        RoadbookTheme(style = RoadbookStyle.FIA) {
            SampleRows()
        }
    }
}
