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
 * A point on the drawing canvas of a tulip or notes column, in the units of the roadbook file.
 *
 * It is not a geographic position (that is a `GeoPoint`). As on a screen, x grows to the right
 * and y grows downwards. Where it is measured from depends on what it positions, as in `.rn2`
 * files:
 *
 * - The points of a [Road], and so of a [Track], are relative to the junction of the tulip, the
 *   point where its roads meet. The tulip canvas is 200 by 135 units, and its junction is 100
 *   from its left edge and 85 from its top edge. Negative values are left of and above the
 *   junction.
 * - The centers of [Icon]s and [Text]s are measured from the top-left corner of the canvas, so a
 *   symbol drawn on the junction of a tulip is at (100, 85).
 *
 * @property x horizontal coordinate. Must be finite.
 * @property y vertical coordinate, downwards. Must be finite.
 * @throws IllegalArgumentException if a coordinate is `NaN` or infinite.
 */
internal data class CanvasPoint(val x: Double, val y: Double) {

    init {
        require(x.isFinite() && y.isFinite()) {
            "A canvas point must have finite coordinates, but was ($x, $y)"
        }
    }

    /** Constants for [CanvasPoint]. */
    companion object {

        /**
         * (0, 0): the junction, for the points of a road, or the top-left corner of the canvas,
         * for the center of an icon or a text.
         */
        val CENTER: CanvasPoint = CanvasPoint(0.0, 0.0)
    }
}
