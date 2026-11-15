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

package dev.giste.roadbook.domain.drawing

/**
 * A road drawn in a tulip: a line or curve from [start] to [end], drawn in the style of its
 * [type].
 *
 * @property start where the road starts, or `null` to start at the center of the canvas (the
 * junction).
 * @property end where the road ends, or `null` when the file gives no end. A road without an end
 * is not drawn.
 * @property type the kind of road, which decides how it is drawn.
 * @property handles the control points that bend the road between [start] and [end], in order.
 * Empty for a straight road.
 */
internal data class Road(
    val start: CanvasPoint?,
    val end: CanvasPoint?,
    val type: RoadType = RoadType.TRACK,
    val handles: List<CanvasPoint> = emptyList(),
) : Element

/**
 * The kind of a [Road], from the least to the most important. It decides how the road is drawn
 * (dashed, thin, double line, …).
 */
internal enum class RoadType {

    /** A track that is hard to see on the ground. */
    LOW_VISIBILITY_TRACK,

    /** Off track: no road at all, e.g. across open terrain. */
    OFF_TRACK,

    /** A small track. */
    SMALL_TRACK,

    /** A track. The default when the file gives no type. */
    TRACK,

    /** A tarmac road. */
    TARMAC_ROAD,

    /** A dual carriageway. */
    DUAL_CARRIAGEWAY,
}
