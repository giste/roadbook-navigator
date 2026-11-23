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

package dev.giste.map.domain

import dev.giste.geo.Bearing
import dev.giste.geo.GeoPoint
import dev.giste.geo.LocationFix

/**
 * Where the map is centered, and which way it is turned.
 *
 * @property position the point at the map's center: the device location.
 * @property bearing the direction shown up on the map, or `null` while no fix has had one yet,
 * in which case north is up.
 */
internal data class MapCenter(val position: GeoPoint, val bearing: Bearing?)

/**
 * Returns where the map is centered after [fix], coming from this center (`null` before the
 * first fix).
 *
 * The map moves to the fix's position. A fix without a bearing (the device reports none, e.g.
 * while stopped) keeps the previous one, so the map does not snap back to north at every stop.
 *
 * @param fix the new location fix.
 * @return the new center.
 */
internal fun MapCenter?.follow(fix: LocationFix): MapCenter =
    MapCenter(position = fix.position, bearing = fix.bearing ?: this?.bearing)
