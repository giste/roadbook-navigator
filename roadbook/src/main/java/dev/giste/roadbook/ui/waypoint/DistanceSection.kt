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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import dev.giste.core.ui.component.AutoSizeText
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.geo.Distance
import dev.giste.roadbook.domain.roadbook.DangerLevel
import dev.giste.roadbook.domain.roadbook.Waypoint
import dev.giste.roadbook.ui.theme.RoadbookTheme
import java.util.Locale

// Share of the height of the distance section taken by each of its parts, from top to bottom,
// as in the previous app.

/** The partial, as large as possible. */
private const val PARTIAL_WEIGHT: Float = 0.33f

/** The line under the partial of a waypoint that resets it. */
private const val RESET_LINE_WEIGHT: Float = 0.02f

/** The bar of a waypoint with danger level 2. */
private const val DANGER_BAR_WEIGHT: Float = 0.05f

/** The new partial, 0.00, of a waypoint that resets it. */
private const val RESET_PARTIAL_WEIGHT: Float = 0.28f

/** Space between the partials and the bottom line. */
private const val GAP_WEIGHT: Float = 0.12f

/** The bottom line: distance from the previous waypoint and waypoint number. */
private const val BOTTOM_WEIGHT: Float = 0.2f

/** Share of the width of the bottom line taken by the distance from the previous waypoint. */
private const val FROM_PREVIOUS_WEIGHT: Float = 0.5f

/** Share of the width of the bottom line left between the distance and the number. */
private const val BOTTOM_GAP_WEIGHT: Float = 0.25f

/** Share of the width of the bottom line taken by the waypoint number. */
private const val NUMBER_WEIGHT: Float = 0.25f

/** Share of the height of the bottom line left empty above the waypoint number. */
private const val NUMBER_TOP_GAP_WEIGHT: Float = 0.3f

/** Share of the height of the bottom line taken by the waypoint number. */
private const val NUMBER_HEIGHT_WEIGHT: Float = 0.7f

/**
 * The distances of a waypoint: its partial in large figures, the new partial (0.00) when it
 * resets it, its distance from the previous waypoint in a frame, and its number. It also shows
 * the reset line, and the bar of danger level 2.
 *
 * It has no background: the row draws it (highlighted when the waypoint comes soon) under its
 * border, so this content stays visible over a thick border.
 *
 * @param waypoint the waypoint.
 * @param comesSoon whether it comes soon after the previous one, which changes the ink color to
 * the one for the highlighted background.
 * @param locale the locale to format the distances in.
 * @param modifier the modifier applied to the section.
 */
@Composable
internal fun DistanceSection(
    waypoint: Waypoint,
    comesSoon: Boolean,
    locale: Locale,
    modifier: Modifier = Modifier,
) {
    val colors = RoadbookTheme.colors
    val dimensions = GisteTheme.dimensions
    val typography = MaterialTheme.typography
    val ink = if (comesSoon) colors.onShortDistanceSurface else colors.onSurface

    Column(modifier = modifier) {
        AutoSizeText(
            text = waypoint.partial.formatKilometers(locale),
            color = ink,
            style = typography.displaySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(PARTIAL_WEIGHT)
                .fillMaxWidth(),
        )
        Mark(
            color = ink,
            isShown = waypoint.resetsPartial,
            modifier = Modifier
                .weight(RESET_LINE_WEIGHT)
                .padding(horizontal = dimensions.spacingMedium),
        )
        Mark(
            color = colors.danger,
            isShown = waypoint.dangerLevel == DangerLevel.MEDIUM,
            modifier = Modifier
                .weight(DANGER_BAR_WEIGHT)
                .padding(horizontal = dimensions.spacingSmall),
        )
        if (waypoint.resetsPartial) {
            AutoSizeText(
                text = Distance.ZERO.formatKilometers(locale),
                color = ink,
                style = typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .weight(RESET_PARTIAL_WEIGHT)
                    .fillMaxWidth(),
            )
        } else {
            Spacer(modifier = Modifier.weight(RESET_PARTIAL_WEIGHT))
        }
        Spacer(modifier = Modifier.weight(GAP_WEIGHT))
        Row(
            modifier = Modifier
                .weight(BOTTOM_WEIGHT)
                .fillMaxWidth(),
        ) {
            AutoSizeText(
                text = waypoint.distanceFromPrevious.formatKilometers(locale),
                color = ink,
                style = typography.titleLarge,
                modifier = Modifier
                    .weight(FROM_PREVIOUS_WEIGHT)
                    .fillMaxSize()
                    .border(width = ROADBOOK_LINE_WIDTH, color = ink)
                    .padding(horizontal = ROADBOOK_LINE_WIDTH),
            )
            Spacer(modifier = Modifier.weight(BOTTOM_GAP_WEIGHT))
            Column(modifier = Modifier.weight(NUMBER_WEIGHT)) {
                Spacer(modifier = Modifier.weight(NUMBER_TOP_GAP_WEIGHT))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(NUMBER_HEIGHT_WEIGHT)
                        .fillMaxWidth()
                        .background(colors.waypointNumberSurface),
                ) {
                    AutoSizeText(
                        text = waypoint.number.value.toString(),
                        color = colors.onWaypointNumberSurface,
                        style = typography.titleMedium,
                        modifier = Modifier.padding(horizontal = ROADBOOK_LINE_WIDTH),
                    )
                }
            }
        }
    }
}

/** A horizontal bar of [color] filling its box, or an empty box when it is not [isShown]. */
@Composable
private fun Mark(color: Color, isShown: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(if (isShown) color else Color.Transparent),
    )
}
