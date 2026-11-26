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

import kotlinx.coroutines.flow.Flow

/**
 * The stored [DashboardSettings]. Hilt binds it as soon as the app depends on `:settings`: inject
 * it wherever the app lays out its dashboard.
 */
public interface DashboardSettingsSource {

    /**
     * The current dashboard settings. It is cold: each collector first gets the stored settings,
     * then every change, without repeated values. Values never stored, or that cannot be read, are
     * the defaults. It never throws.
     */
    public val settings: Flow<DashboardSettings>
}
