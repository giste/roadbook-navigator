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

package dev.giste.roadbooknavigator.settings

import dev.giste.location.LocationSettings
import dev.giste.location.LocationSettingsSource
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Temporary [LocationSettingsSource] that always uses the default [LocationSettings].
 *
 * `:location` needs a settings source, but `:settings` does not exist yet. Delete this class and
 * its binding when `:settings` provides the real one.
 */
internal class DefaultLocationSettingsSource @Inject constructor() : LocationSettingsSource {

    /** Emits the default [LocationSettings] once. They never change. */
    override val settings: Flow<LocationSettings> = flowOf(LocationSettings())
}
