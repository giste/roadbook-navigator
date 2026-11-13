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

package dev.giste.location

import kotlinx.coroutines.flow.Flow

/**
 * Input port: the [LocationSettings] that `:location` uses.
 *
 * `:location` does not store its settings. The app implements this interface (usually from its
 * settings storage) and binds it with Hilt in `SingletonComponent`.
 */
public interface LocationSettingsSource {

    /**
     * The current settings, followed by every change.
     *
     * It should emit at least once, and emit [LocationSettings] with its defaults when the user
     * has not set anything. [LocationSource] collects it for as long as location updates run and
     * restarts the updates on every distinct value. If it throws, [LocationSource] logs the error
     * and falls back to the default [LocationSettings].
     */
    public val settings: Flow<LocationSettings>
}
