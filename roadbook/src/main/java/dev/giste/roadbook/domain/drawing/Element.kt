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
 * Something drawn in a waypoint's tulip or notes column: the [Track] the rider follows, other
 * [Road]s, an [Icon] or a [Text].
 *
 * Positions are [CanvasPoint]s relative to the center of the column's canvas. The type of element
 * is its subtype, so there is no separate type field.
 */
internal sealed interface Element

/**
 * The route through the junction of a tulip: the road the rider arrives on and the road they
 * leave on. It is drawn on top of the other roads, so the way to go stands out.
 *
 * @property roadIn the road the rider arrives on, usually from the bottom of the canvas.
 * @property roadOut the road the rider leaves on.
 */
internal data class Track(
    val roadIn: Road,
    val roadOut: Road,
) : Element
