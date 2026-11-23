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

package dev.giste.map

import dev.giste.geo.LocationFix
import kotlinx.coroutines.flow.Flow

/**
 * Input port: the device location that the map follows.
 *
 * `:map` does not read the location itself. The app implements this interface (e.g. from its
 * location provider) and binds it with Hilt in `SingletonComponent`.
 */
public interface MapLocationSource {

    /**
     * The location fixes, in order.
     *
     * The map collects it only while [MapViewer] is shown and has maps to show. Each fix moves
     * the map's center to [LocationFix.position], and turns the map so that
     * [LocationFix.bearing] points up. A fix without a bearing keeps the last one. Every other
     * field is ignored. It should never throw: if it does, the map logs the error and stops
     * following the location until it is shown again.
     */
    public val locations: Flow<LocationFix>
}
