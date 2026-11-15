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

package dev.giste.odometer.data

import javax.inject.Qualifier

/**
 * Qualifies the `DataStore<Preferences>` that holds the odometer state (file `odometer_state`).
 *
 * It keeps this DataStore apart from those of other modules, which have the same type.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class OdometerStateDataStore

/**
 * Qualifies the `kotlin.time.TimeSource` the odometer measures save intervals with.
 *
 * It is monotonic in production, and tests replace it with the virtual time of `runTest`. The
 * qualifier keeps this binding from clashing with a `TimeSource` that another module provides.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class OdometerTimeSource
