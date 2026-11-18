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

package dev.giste.roadbook.domain.roadbook

import dev.giste.geo.Altitude
import dev.giste.geo.Distance
import dev.giste.geo.GeoPoint
import dev.giste.roadbook.ShortDistanceThreshold
import dev.giste.roadbook.domain.drawing.NoteElement
import dev.giste.roadbook.domain.drawing.TulipElement

/**
 * One waypoint of a [Roadbook]: a point of the route with the instructions to navigate it.
 *
 * It belongs to its [Roadbook], which checks that the waypoints fit together (consecutive
 * numbers, …). On its own it only holds values that are each valid.
 *
 * @property number the number printed on the waypoint, from 1.
 * @property position where the waypoint is.
 * @property altitude the altitude of the waypoint, or `null` when the roadbook does not give it.
 * @property partial the partial at this waypoint: the distance from the last waypoint that resets
 * the partial (or from the start of the roadbook) to this one. It is the large distance shown on
 * the waypoint, the one the rider compares with the odometer.
 * @property distanceFromPrevious the distance from the previous waypoint shown, or [Distance.ZERO]
 * for the first one. Distance through waypoints that are not shown is included.
 * @property resetsPartial whether the partial starts again from zero at this waypoint. The partial
 * of this waypoint still counts up to it, and the next waypoints count from here.
 * @property dangerLevel how dangerous the waypoint is, so it can be highlighted.
 * @property tulip what is drawn in the tulip column: the junction diagram.
 * @property notes what is drawn in the notes column: signs, landmarks, texts, …. It has no roads.
 */
internal data class Waypoint(
    val number: WaypointNumber,
    val position: GeoPoint,
    val altitude: Altitude?,
    val partial: Distance,
    val distanceFromPrevious: Distance,
    val resetsPartial: Boolean = false,
    val dangerLevel: DangerLevel = DangerLevel.NONE,
    val tulip: List<TulipElement> = emptyList(),
    val notes: List<NoteElement> = emptyList(),
) {

    /**
     * The partial the odometer should show when the rider is at this waypoint, e.g. when they ask
     * to set the partial from it: [partial], or [Distance.ZERO] when the waypoint
     * [resets the partial][resetsPartial], because the partial starts again from here.
     */
    val partialToSet: Distance
        get() = if (resetsPartial) Distance.ZERO else partial

    /**
     * Whether this waypoint comes soon after the previous one: closer than [threshold], so the
     * roadbook highlights it. The first waypoint, which has no previous one, never does.
     *
     * @param threshold the short distance the user set.
     * @return `true` when the distance from the previous waypoint is above zero and below
     * [threshold].
     */
    fun comesSoonAfterPrevious(threshold: ShortDistanceThreshold): Boolean =
        distanceFromPrevious > Distance.ZERO && distanceFromPrevious < threshold.distance
}

/** How dangerous a [Waypoint] is, as marked in the roadbook. */
internal enum class DangerLevel {

    /** No danger is marked. */
    NONE,

    /** Danger level 1, the lowest marked danger. */
    LOW,

    /** Danger level 2. */
    MEDIUM,

    /** Danger level 3, the highest. The roadbook highlights it the most. */
    HIGH,
}
