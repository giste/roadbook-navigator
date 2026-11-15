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
 * A point on the drawing canvas of a tulip or notes column, relative to the center of the
 * canvas, in the units of the roadbook file.
 *
 * It is not a geographic position (that is a `GeoPoint`). As on a screen, x grows to the right
 * and y grows downwards, so negative values are left of and above the center.
 *
 * @property x horizontal offset from the center. Must be finite.
 * @property y vertical offset from the center, downwards. Must be finite.
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

        /** The center of the canvas. */
        val CENTER: CanvasPoint = CanvasPoint(0.0, 0.0)
    }
}
