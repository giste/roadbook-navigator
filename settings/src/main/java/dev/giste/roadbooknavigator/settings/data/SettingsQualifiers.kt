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

package dev.giste.roadbooknavigator.settings.data

import javax.inject.Qualifier

/** Qualifies the DataStore of the `:location` settings (file `settings_location`). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class LocationSettingsDataStore

/** Qualifies the DataStore of the `:odometer` settings (file `settings_odometer`). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class OdometerSettingsDataStore

/** Qualifies the DataStore of the `:roadbook` settings (file `settings_roadbook`). */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class RoadbookSettingsDataStore
