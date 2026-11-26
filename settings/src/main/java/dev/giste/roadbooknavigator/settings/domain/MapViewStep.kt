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

import dev.giste.map.MapSettings
import dev.giste.map.MapTilt
import dev.giste.map.MapZoomLevel

/*
 * The map zoom levels and tilts this app offers. :map accepts VTM's whole ranges (zoom 2-20,
 * tilt 0-65 degrees), but on a rally the rider looks at the map to see the next turn: farther than
 * zoom 15 the turn cannot be made out, and past 60 degrees the road ahead flattens out of view.
 * Both ranges are within :map's, so every value in them is a valid MapZoomLevel or MapTilt.
 */

/** The zoom levels the settings screen offers. */
internal val MAP_ZOOM_RANGE: IntRange = 15..20

/** The tilts the settings screen offers, in degrees. */
internal val MAP_TILT_RANGE: ClosedFloatingPointRange<Double> = 0.0..60.0

/** How much one press of the settings stepper changes the tilt, in degrees. */
internal const val MAP_TILT_STEP_DEGREES: Double = 5.0

/**
 * This zoom level moved by [steps] levels (negative steps show the map farther), kept within
 * [MAP_ZOOM_RANGE].
 *
 * @param steps how many levels to move it.
 * @return the new zoom level.
 */
internal fun MapZoomLevel.stepped(steps: Int): MapZoomLevel =
    MapZoomLevel.of((value + steps).coerceIn(MAP_ZOOM_RANGE))

/**
 * This tilt moved by [steps] of [MAP_TILT_STEP_DEGREES] (negative steps look more straight down),
 * kept within [MAP_TILT_RANGE].
 *
 * @param steps how many steps to move it.
 * @return the new tilt.
 */
internal fun MapTilt.stepped(steps: Int): MapTilt =
    MapTilt.of((inDegrees + steps * MAP_TILT_STEP_DEGREES).coerceIn(MAP_TILT_RANGE))

/**
 * These settings with the zoom level brought into [MAP_ZOOM_RANGE] and the tilt into
 * [MAP_TILT_RANGE], e.g. for a value stored by a version that offered more.
 *
 * @return the settings within the ranges this app offers.
 */
internal fun MapSettings.withinOfferedRanges(): MapSettings = MapSettings(
    zoomLevel = MapZoomLevel.of(zoomLevel.value.coerceIn(MAP_ZOOM_RANGE)),
    tilt = MapTilt.of(tilt.inDegrees.coerceIn(MAP_TILT_RANGE)),
)
