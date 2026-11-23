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

import kotlinx.coroutines.flow.Flow

/**
 * Input port: the [MapSettings] that `:map` uses.
 *
 * `:map` does not store its settings. The app implements this interface (usually from its
 * settings storage) and binds it with Hilt in `SingletonComponent`.
 */
public interface MapSettingsSource {

    /**
     * The current settings, followed by every change.
     *
     * It must emit at least once, and emit [MapSettings] with its defaults when the user has not
     * set anything. Every new value is applied to the map at once. If it throws, the map logs the
     * error and uses the default [MapSettings].
     */
    public val settings: Flow<MapSettings>
}
