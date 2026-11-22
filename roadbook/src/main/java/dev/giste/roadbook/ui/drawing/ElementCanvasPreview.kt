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

package dev.giste.roadbook.ui.drawing

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.roadbook.RoadbookStyle
import dev.giste.roadbook.domain.drawing.CanvasPoint
import dev.giste.roadbook.domain.drawing.Icon
import dev.giste.roadbook.domain.drawing.IconType
import dev.giste.roadbook.domain.drawing.NoteElement
import dev.giste.roadbook.domain.drawing.Road
import dev.giste.roadbook.domain.drawing.RoadType
import dev.giste.roadbook.domain.drawing.Text
import dev.giste.roadbook.domain.drawing.Track
import dev.giste.roadbook.domain.drawing.TulipElement
import dev.giste.roadbook.ui.theme.RoadbookTheme

// Preview-only sample data: a tulip with every kind of road and a curved track, and a notes
// column with symbols (one the app cannot draw) and a text.

/** A tulip that turns right off a dual carriageway, with roads of every other kind. */
private val SAMPLE_TULIP: List<TulipElement> = listOf(
    Road(start = null, end = CanvasPoint(-80.0, -40.0), type = RoadType.TARMAC_ROAD),
    Road(start = null, end = CanvasPoint(-60.0, 40.0), type = RoadType.OFF_TRACK),
    Road(start = null, end = CanvasPoint(0.0, -70.0), type = RoadType.LOW_VISIBILITY_TRACK),
    Road(start = null, end = CanvasPoint(40.0, 45.0), type = RoadType.SMALL_TRACK),
    Track(
        roadIn = Road(
            start = null,
            end = CanvasPoint(0.0, 45.0),
            type = RoadType.DUAL_CARRIAGEWAY,
        ),
        roadOut = Road(
            start = null,
            end = CanvasPoint(75.0, -20.0),
            type = RoadType.TRACK,
            handles = listOf(CanvasPoint(25.0, -5.0), CanvasPoint(50.0, -20.0)),
        ),
    ),
    Icon(
        type = IconType.ROUNDABOUT,
        width = 30.0,
        height = 30.0,
        center = CanvasPoint(100.0, 85.0),
    ),
    Text(
        text = "CAP 120",
        lineHeight = 1.0,
        width = 60.0,
        height = 20.0,
        maxWidth = 80.0,
        maxHeight = 30.0,
        center = CanvasPoint(155.0, 115.0),
    ),
)

/** A notes column with a danger mark, a mirrored symbol, an unknown symbol and a text. */
private val SAMPLE_NOTES: List<NoteElement> = listOf(
    Icon(type = IconType.DANGER_2, width = 40.0, height = 40.0, center = CanvasPoint(30.0, 30.0)),
    Icon(
        type = IconType.TREE,
        width = 40.0,
        height = 40.0,
        center = CanvasPoint(80.0, 30.0),
        scaleX = -1.0,
    ),
    Icon(
        type = IconType.UNKNOWN,
        width = 50.0,
        height = 40.0,
        center = CanvasPoint(140.0, 30.0),
        originalName = "Camel",
    ),
    Text(
        text = "Sandy, keep left",
        lineHeight = 1.0,
        width = 160.0,
        height = 40.0,
        maxWidth = 180.0,
        maxHeight = 60.0,
        center = CanvasPoint(100.0, 95.0),
    ),
)

/** The sample tulip and notes column side by side, on the roadbook paper. */
@Composable
private fun SampleColumns() {
    Surface(color = RoadbookTheme.colors.surface) {
        Row(
            modifier = Modifier.padding(GisteTheme.dimensions.spacingSmall),
            horizontalArrangement = Arrangement.spacedBy(GisteTheme.dimensions.spacingSmall),
        ) {
            TulipCanvas(elements = SAMPLE_TULIP, modifier = Modifier.width(PREVIEW_COLUMN_WIDTH))
            NotesCanvas(elements = SAMPLE_NOTES, modifier = Modifier.width(PREVIEW_COLUMN_WIDTH))
        }
    }
}

/** Width of each column in the previews, about the size they have on a phone. */
private val PREVIEW_COLUMN_WIDTH = 180.dp

@Preview(name = "App theme, light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "App theme, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ElementCanvasAppThemePreview() {
    GisteTheme {
        RoadbookTheme(style = RoadbookStyle.APP_THEME) {
            SampleColumns()
        }
    }
}

@Preview(name = "FIA, light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "FIA, dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ElementCanvasFiaPreview() {
    GisteTheme {
        RoadbookTheme(style = RoadbookStyle.FIA) {
            SampleColumns()
        }
    }
}
