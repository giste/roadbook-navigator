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

package dev.giste.roadbook.data.persistence

import javax.inject.Qualifier

/**
 * Qualifies the `DataStore<Preferences>` that holds the roadbook state (file `roadbook_state`).
 *
 * It keeps this DataStore apart from those of other modules, which have the same type.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class RoadbookStateDataStore
