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

package dev.giste.roadbooknavigator.core.coroutines

import javax.inject.Qualifier

/**
 * Qualifies the [kotlinx.coroutines.CoroutineDispatcher] for CPU-bound work: parsing, distance
 * computations and other non-blocking processing.
 *
 * Inject it instead of using `Dispatchers.Default` directly, so tests can replace it with a test
 * dispatcher. In production it is `Dispatchers.Default`.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
public annotation class DefaultDispatcher
