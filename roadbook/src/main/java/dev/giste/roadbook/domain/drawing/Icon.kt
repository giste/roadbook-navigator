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
 * A symbol drawn in a tulip or notes column, e.g. a danger mark, a fuel zone or a speed limit.
 *
 * @property type which symbol it is.
 * @property width the width of the symbol before scaling, in canvas units. Must be finite and
 * positive.
 * @property height the height of the symbol before scaling, in canvas units. Must be finite and
 * positive.
 * @property center where the center of the symbol is drawn, measured from the top-left corner of
 * the canvas (see [CanvasPoint]).
 * @property angle the rotation of the symbol, in degrees clockwise.
 * @property scaleX the horizontal scale. Must be finite and not zero. A negative scale mirrors the
 * symbol.
 * @property scaleY the vertical scale. Must be finite and not zero. A negative scale mirrors the
 * symbol.
 * @property originalId the id of the symbol in the roadbook file, if any. It is shown in place of
 * an [IconType.UNKNOWN] symbol, and helps to add support for it.
 * @property originalName the name of the symbol in the roadbook file, if any. Shown like
 * [originalId], and preferred to it.
 * @throws IllegalArgumentException if a size or a scale is out of range.
 */
internal data class Icon(
    val type: IconType,
    val width: Double,
    val height: Double,
    val center: CanvasPoint,
    val angle: Int = 0,
    val scaleX: Double = 1.0,
    val scaleY: Double = 1.0,
    val originalId: String? = null,
    val originalName: String? = null,
) : TulipElement, NoteElement {

    init {
        require(width.isFinite() && width > 0.0) {
            "An icon's width must be finite and positive, but was $width"
        }
        require(height.isFinite() && height > 0.0) {
            "An icon's height must be finite and positive, but was $height"
        }
        require(scaleX.isFinite() && scaleX != 0.0) {
            "An icon's horizontal scale must be finite and not zero, but was $scaleX"
        }
        require(scaleY.isFinite() && scaleY != 0.0) {
            "An icon's vertical scale must be finite and not zero, but was $scaleY"
        }
    }
}

/** The symbols the roadbook can draw. Symbols the app does not know are [UNKNOWN]. */
internal enum class IconType {
    // Cross.
    /** Danger level 1. Sets the waypoint's danger level to low. */
    DANGER_1,
    /** Danger level 2. Sets the waypoint's danger level to medium. */
    DANGER_2,
    /** Danger level 3. Sets the waypoint's danger level to high. */
    DANGER_3,
    FUEL_ZONE,
    /** The partial starts again from zero at this waypoint. */
    RESET_PARTIAL,

    // Landmarks.
    /** The route passes over a bridge. */
    ABOVE_BRIDGE,
    CHURCH,
    FORT_CASTLE,
    HOUSE,
    TRAFFIC_LIGHT,
    TREE,
    TUNNEL,
    /** The route passes under a bridge. */
    UNDER_BRIDGE,

    // Signs.
    /** A warning sign. */
    ALERT,
    DO_NOT_ENTER,
    ROUNDABOUT,
    STOP,

    // Speed limits, in km/h.
    SPEED_LIMIT_10,
    SPEED_LIMIT_20,
    SPEED_LIMIT_30,
    SPEED_LIMIT_40,
    SPEED_LIMIT_50,
    SPEED_LIMIT_60,
    SPEED_LIMIT_70,
    SPEED_LIMIT_80,
    SPEED_LIMIT_90,
    SPEED_LIMIT_100,
    SPEED_LIMIT_110,
    SPEED_LIMIT_120,
    SPEED_LIMIT_130,
    SPEED_LIMIT_140,
    SPEED_LIMIT_150,

    // Terrain.
    RIVER_WATER,

    /** A symbol the app does not know. It is shown by its [Icon.originalName] or [Icon.originalId]. */
    UNKNOWN,
}
