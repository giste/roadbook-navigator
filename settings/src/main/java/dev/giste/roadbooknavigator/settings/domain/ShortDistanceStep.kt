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

/** How much one press of the settings stepper changes the short-distance threshold. */
internal val SHORT_DISTANCE_STEP: Distance = Distance.meters(50.0)

/**
 * This threshold moved by [steps] of [SHORT_DISTANCE_STEP] (negative steps make it shorter),
 * kept within [ShortDistanceThreshold.RANGE].
 *
 * @param steps how many steps to move it.
 * @return the new threshold.
 */
internal fun ShortDistanceThreshold.stepped(steps: Int): ShortDistanceThreshold {
    val meters = distance.inMeters + steps * SHORT_DISTANCE_STEP.inMeters
    return ShortDistanceThreshold.coerce(Distance.meters(meters.coerceAtLeast(0.0)))
}
