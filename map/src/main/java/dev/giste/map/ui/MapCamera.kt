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

package dev.giste.map.ui

import dev.giste.map.MapSettings
import dev.giste.map.domain.MapCenter
import org.oscim.core.MapPosition

/*
 * Conversions from the map's state to VTM's camera, the MapPosition. They are plain functions on
 * VTM's pure-Java core, so they are tested on the JVM, unlike the OpenGL view that uses them.
 */

/**
 * Sets the zoom level and the tilt of [settings] on this position, leaving the center and the
 * rotation as they are.
 *
 * VTM measures the zoom as a scale: zoom level `n` is a scale of `2ⁿ`.
 *
 * @param settings the zoom level and tilt to show.
 * @return this position, changed.
 */
internal fun MapPosition.applySettings(settings: MapSettings): MapPosition = apply {
    setZoomLevel(settings.zoomLevel.value)
    setTilt(settings.tilt.inDegrees.toFloat())
}

/**
 * Centers this position on [center] and, when it has a bearing, turns the map so that the
 * bearing points up. The zoom and the tilt stay as they are, so the user's gestures are kept.
 *
 * VTM's bearing is the map's rotation, the opposite of the direction shown up: heading east
 * (90°) turns the map by -90°.
 *
 * @param center the location to show.
 * @return this position, changed.
 */
internal fun MapPosition.follow(center: MapCenter): MapPosition = apply {
    setPosition(center.position.latitude, center.position.longitude)
    center.bearing?.let { setBearing(-it.inDegrees.toFloat()) }
}
