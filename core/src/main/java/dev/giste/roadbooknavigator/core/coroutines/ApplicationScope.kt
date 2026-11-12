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
 * Qualifies the application-wide [kotlinx.coroutines.CoroutineScope].
 *
 * Use it for work that must outlive any screen, such as collecting events between modules or
 * finishing a write after the user leaves a screen. Never use `GlobalScope` instead.
 *
 * The scope lives as long as the process and is never cancelled. It runs on the
 * [DefaultDispatcher] and uses a `SupervisorJob`, so a failing child does not cancel its
 * siblings or the scope. A child that fails with an uncaught exception still crashes the app:
 * code launched here must model expected failures as sealed states, as for any flow.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
public annotation class ApplicationScope
