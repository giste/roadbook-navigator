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

package dev.giste.roadbook.presentation.waypoint

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.giste.geo.Distance
import dev.giste.roadbook.R
import dev.giste.roadbook.ShortDistanceThreshold
import dev.giste.roadbook.domain.roadbook.DangerLevel
import dev.giste.roadbook.domain.roadbook.Waypoint
import dev.giste.roadbook.presentation.drawing.CANVAS_HEIGHT
import dev.giste.roadbook.presentation.drawing.CANVAS_WIDTH
import dev.giste.roadbook.presentation.drawing.NotesCanvas
import dev.giste.roadbook.presentation.drawing.TulipCanvas
import dev.giste.roadbook.presentation.theme.RoadbookTheme

/**
 * Width of the lines of the roadbook: the borders of a waypoint, and the lines between its
 * columns. A hairline, as on paper.
 */
internal val ROADBOOK_LINE_WIDTH: Dp = 1.dp

/** Width of the red border of a waypoint with danger level 3, which must stand out. */
internal val HIGH_DANGER_BORDER_WIDTH: Dp = 8.dp

/**
 * Width of a waypoint row for its height: the distance section (as wide as the row is high) and
 * the tulip and notes canvases.
 */
private const val ROW_ASPECT_RATIO: Float = (CANVAS_HEIGHT + 2 * CANVAS_WIDTH) / CANVAS_HEIGHT

/**
 * One waypoint of the roadbook, as on a paper roadbook: its distances and number, its tulip and
 * its notes, side by side.
 *
 * The distances are highlighted when the waypoint comes soon after the previous one, and a
 * waypoint with danger level 3 gets a thick red border. Long-pressing the distances asks to set
 * the partial to the one the roadbook gives at this waypoint. For accessibility services, the
 * distances are one element, read as its visible figures, with a labeled long-press action.
 *
 * @param waypoint the waypoint to show.
 * @param shortDistanceThreshold below which distance from the previous waypoint it is
 * highlighted.
 * @param onSetPartial called with `Waypoint.partialToSet` when the user long-presses the
 * distances.
 * @param modifier the modifier applied to the row.
 */
@Composable
internal fun WaypointItem(
    waypoint: Waypoint,
    shortDistanceThreshold: ShortDistanceThreshold,
    onSetPartial: (Distance) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RoadbookTheme.colors
    val comesSoon = waypoint.comesSoonAfterPrevious(shortDistanceThreshold)
    val isHighDanger = waypoint.dangerLevel == DangerLevel.HIGH

    // Three layers, as in the previous app: the backgrounds, the border over them, and the content
    // over the border, so a thick danger border does not hide distances or drawings.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ROW_ASPECT_RATIO)
            .background(colors.surface),
    ) {
        if (comesSoon) {
            ShortDistanceHighlight(Modifier.matchParentSize())
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(
                    width = if (isHighDanger) HIGH_DANGER_BORDER_WIDTH else ROADBOOK_LINE_WIDTH,
                    color = if (isHighDanger) colors.danger else colors.onSurface,
                ),
        )
        WaypointContent(
            waypoint = waypoint,
            comesSoon = comesSoon,
            onSetPartial = onSetPartial,
            modifier = Modifier.matchParentSize(),
        )
    }
}

/**
 * The columns of a waypoint row: its distances, tulip and notes, with lines between them. They
 * have no background, so whatever is under them shows through.
 *
 * @param waypoint the waypoint to show.
 * @param comesSoon whether it comes soon after the previous one.
 * @param onSetPartial called with `Waypoint.partialToSet` when the user long-presses the
 * distances.
 * @param modifier the modifier applied to the columns.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WaypointContent(
    waypoint: Waypoint,
    comesSoon: Boolean,
    onSetPartial: (Distance) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = RoadbookTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    val haptics = LocalHapticFeedback.current
    val setPartialLabel = stringResource(R.string.roadbook_waypoint_action_set_partial)

    Row(modifier = modifier) {
        DistanceSection(
            waypoint = waypoint,
            comesSoon = comesSoon,
            locale = locale,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .combinedClickable(
                    onLongClickLabel = setPartialLabel,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSetPartial(waypoint.partialToSet)
                    },
                    // Only the long press does something: a tap is how riders touch the screen
                    // by accident.
                    onClick = {},
                ),
        )
        VerticalDivider(thickness = ROADBOOK_LINE_WIDTH, color = colors.onSurface)
        TulipCanvas(elements = waypoint.tulip, modifier = Modifier.fillMaxHeight())
        VerticalDivider(thickness = ROADBOOK_LINE_WIDTH, color = colors.onSurface)
        NotesCanvas(elements = waypoint.notes, modifier = Modifier.fillMaxHeight())
    }
}

/**
 * The highlight behind the distances of a waypoint that comes soon after the previous one. It
 * spaces the tulip and notes like [WaypointContent], so it covers the distances exactly.
 *
 * @param modifier the modifier applied to the highlight's row.
 */
@Composable
private fun ShortDistanceHighlight(modifier: Modifier = Modifier) {
    Row(modifier = modifier) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(RoadbookTheme.colors.shortDistanceSurface),
        )
        Spacer(modifier = Modifier.width(ROADBOOK_LINE_WIDTH))
        Spacer(modifier = Modifier.fillMaxHeight().aspectRatio(CANVAS_WIDTH / CANVAS_HEIGHT))
        Spacer(modifier = Modifier.width(ROADBOOK_LINE_WIDTH))
        Spacer(modifier = Modifier.fillMaxHeight().aspectRatio(CANVAS_WIDTH / CANVAS_HEIGHT))
    }
}
