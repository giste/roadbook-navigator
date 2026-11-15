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
 * @property center where the center of the symbol is drawn.
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
) : Element {

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

    // Cross: marks that apply to the whole waypoint.

    /** Danger level 1. Sets the waypoint's danger level to low. */
    DANGER_1,

    /** Danger level 2. Sets the waypoint's danger level to medium. */
    DANGER_2,

    /** Danger level 3. Sets the waypoint's danger level to high. */
    DANGER_3,

    /** A fuel zone. */
    FUEL_ZONE,

    /** The partial starts again from zero at this waypoint. */
    RESET_PARTIAL,

    // Landmarks.

    /** The route passes over a bridge. */
    ABOVE_BRIDGE,

    /** A church. */
    CHURCH,

    /** A fort or castle. */
    FORT_CASTLE,

    /** A house. */
    HOUSE,

    /** A traffic light. */
    TRAFFIC_LIGHT,

    /** A tree. */
    TREE,

    /** A tunnel. */
    TUNNEL,

    /** The route passes under a bridge. */
    UNDER_BRIDGE,

    // Signs.

    /** A warning sign. */
    ALERT,

    /** A no-entry sign. */
    DO_NOT_ENTER,

    /** A roundabout. */
    ROUNDABOUT,

    /** A stop sign. */
    STOP,

    // Speed limits, in km/h.

    /** Speed limit of 10 km/h. */
    SPEED_LIMIT_10,

    /** Speed limit of 20 km/h. */
    SPEED_LIMIT_20,

    /** Speed limit of 30 km/h. */
    SPEED_LIMIT_30,

    /** Speed limit of 40 km/h. */
    SPEED_LIMIT_40,

    /** Speed limit of 50 km/h. */
    SPEED_LIMIT_50,

    /** Speed limit of 60 km/h. */
    SPEED_LIMIT_60,

    /** Speed limit of 70 km/h. */
    SPEED_LIMIT_70,

    /** Speed limit of 80 km/h. */
    SPEED_LIMIT_80,

    /** Speed limit of 90 km/h. */
    SPEED_LIMIT_90,

    /** Speed limit of 100 km/h. */
    SPEED_LIMIT_100,

    /** Speed limit of 110 km/h. */
    SPEED_LIMIT_110,

    /** Speed limit of 120 km/h. */
    SPEED_LIMIT_120,

    /** Speed limit of 130 km/h. */
    SPEED_LIMIT_130,

    /** Speed limit of 140 km/h. */
    SPEED_LIMIT_140,

    /** Speed limit of 150 km/h. */
    SPEED_LIMIT_150,

    // Terrain.

    /** A river or water crossing. */
    RIVER_WATER,

    /** A symbol the app does not know. It is shown by its [Icon.originalName] or [Icon.originalId]. */
    UNKNOWN,
}
