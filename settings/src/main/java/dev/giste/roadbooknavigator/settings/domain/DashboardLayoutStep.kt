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

package dev.giste.roadbooknavigator.settings.domain

import dev.giste.roadbooknavigator.settings.DashboardSettings
import dev.giste.roadbooknavigator.settings.ScreenShare

/*
 * The dashboard shares this app offers. In landscape, a side column narrower than 25 % leaves the
 * distances too small to read at a glance, and one wider than 40 % squeezes the roadbook. In
 * portrait, the distances' digits are as large as their boxes allow, and from about 12 % of the
 * height the width limits them on every screen, so a taller row would only take space from the
 * map and the roadbook. The defaults (30 % and 8 %) are within the ranges and on their steps.
 */

/** The side column shares the settings screen offers, in percent. */
internal val LANDSCAPE_SIDE_COLUMN_RANGE: IntRange = 25..40

/** How much one press of the settings stepper changes the side column share, in percent. */
internal const val LANDSCAPE_SIDE_COLUMN_STEP: Int = 5

/** The distances row shares the settings screen offers, in percent. */
internal val PORTRAIT_DISTANCES_RANGE: IntRange = 8..12

/** How much one press of the settings stepper changes the distances row share, in percent. */
internal const val PORTRAIT_DISTANCES_STEP: Int = 1

/**
 * These settings with the side column share moved by [steps] of [LANDSCAPE_SIDE_COLUMN_STEP]
 * (negative steps make it narrower), kept within [LANDSCAPE_SIDE_COLUMN_RANGE].
 *
 * @param steps how many steps to move it.
 * @return the new settings.
 */
internal fun DashboardSettings.withLandscapeSideColumnStepped(steps: Int): DashboardSettings =
    copy(
        landscapeSideColumn = offered(
            percent = landscapeSideColumn.percent + steps * LANDSCAPE_SIDE_COLUMN_STEP,
            range = LANDSCAPE_SIDE_COLUMN_RANGE,
            step = LANDSCAPE_SIDE_COLUMN_STEP,
        ),
    )

/**
 * These settings with the distances row share moved by [steps] of [PORTRAIT_DISTANCES_STEP]
 * (negative steps make it shorter), kept within [PORTRAIT_DISTANCES_RANGE].
 *
 * @param steps how many steps to move it.
 * @return the new settings.
 */
internal fun DashboardSettings.withPortraitDistancesStepped(steps: Int): DashboardSettings =
    copy(
        portraitDistances = offered(
            percent = portraitDistances.percent + steps * PORTRAIT_DISTANCES_STEP,
            range = PORTRAIT_DISTANCES_RANGE,
            step = PORTRAIT_DISTANCES_STEP,
        ),
    )

/**
 * These settings with each share brought into its offered range and onto its steps, e.g. for a
 * value a damaged file holds.
 *
 * @return the settings the screen can show.
 */
internal fun DashboardSettings.withinOfferedRanges(): DashboardSettings = DashboardSettings(
    landscapeSideColumn = offered(
        landscapeSideColumn.percent,
        LANDSCAPE_SIDE_COLUMN_RANGE,
        LANDSCAPE_SIDE_COLUMN_STEP,
    ),
    portraitDistances = offered(
        portraitDistances.percent,
        PORTRAIT_DISTANCES_RANGE,
        PORTRAIT_DISTANCES_STEP,
    ),
)

/**
 * The offered share nearest to [percent]: within [range], and a whole number of [step]s from its
 * start, rounding half up. [range] must start and end on a step, and lie within the valid shares.
 */
private fun offered(percent: Int, range: IntRange, step: Int): ScreenShare {
    val steps = (percent.coerceIn(range) - range.first + step / 2) / step
    return ScreenShare.of((range.first + steps * step).coerceAtMost(range.last))
}
