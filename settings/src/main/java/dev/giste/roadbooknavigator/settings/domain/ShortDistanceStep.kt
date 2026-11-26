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

import dev.giste.geo.Distance
import dev.giste.roadbook.ShortDistanceThreshold

/*
 * The short-distance thresholds this app offers. :roadbook accepts 50 m to 1 km, but this app
 * offers a narrower range, 100 m to 500 m: the thresholds riders use, in a few presses. It is
 * within :roadbook's range, so every value in it is a valid ShortDistanceThreshold.
 */

/** How much one press of the settings stepper changes the short-distance threshold. */
internal val SHORT_DISTANCE_STEP: Distance = Distance.meters(50.0)

/** The shortest threshold the settings screen offers. */
internal val SHORT_DISTANCE_MIN: ShortDistanceThreshold =
    ShortDistanceThreshold.of(Distance.meters(100.0))

/** The longest threshold the settings screen offers. */
internal val SHORT_DISTANCE_MAX: ShortDistanceThreshold =
    ShortDistanceThreshold.of(Distance.meters(500.0))

/** The distances the settings screen offers, from [SHORT_DISTANCE_MIN] to [SHORT_DISTANCE_MAX]. */
private val OFFERED_DISTANCES: ClosedRange<Distance> =
    SHORT_DISTANCE_MIN.distance..SHORT_DISTANCE_MAX.distance

/**
 * This threshold moved by [steps] of [SHORT_DISTANCE_STEP] (negative steps make it shorter),
 * kept within the range the settings screen offers.
 *
 * @param steps how many steps to move it.
 * @return the new threshold.
 */
internal fun ShortDistanceThreshold.stepped(steps: Int): ShortDistanceThreshold {
    // Clamped in meters first: many steps down would be negative, which no Distance can be.
    val meters = (distance.inMeters + steps * SHORT_DISTANCE_STEP.inMeters).coerceIn(
        SHORT_DISTANCE_MIN.distance.inMeters,
        SHORT_DISTANCE_MAX.distance.inMeters,
    )
    return ShortDistanceThreshold.of(Distance.meters(meters))
}

/**
 * This threshold brought into the range the settings screen offers, e.g. for a value stored by a
 * version that offered more.
 *
 * @return [SHORT_DISTANCE_MIN] if it is shorter, [SHORT_DISTANCE_MAX] if it is longer, or itself.
 */
internal fun ShortDistanceThreshold.withinOfferedRange(): ShortDistanceThreshold =
    ShortDistanceThreshold.of(distance.coerceIn(OFFERED_DISTANCES))
