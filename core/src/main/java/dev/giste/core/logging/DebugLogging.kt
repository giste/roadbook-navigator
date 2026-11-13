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

package dev.giste.core.logging

import dev.giste.core.logging.data.AutomaticTagTree
import timber.log.Timber

/**
 * Turns on the output of [Logger] to Logcat.
 *
 * Until [install] is called, every [Logger] call is silently discarded. `:app` calls it once at
 * start-up, and only in debuggable builds, so release builds never write logs.
 */
public object DebugLogging {

    /**
     * Starts writing every [Logger] call to Logcat, tagged with the calling class name.
     *
     * Call it from `Application.onCreate`, only in debuggable builds. It is idempotent: calling
     * it again does nothing, so log lines are never duplicated. Safe to call from any thread.
     */
    public fun install() {
        // Synchronized so two concurrent calls cannot both see an empty forest and plant twice.
        synchronized(this) {
            if (Timber.forest().none { it is AutomaticTagTree }) {
                Timber.plant(AutomaticTagTree())
            }
        }
    }
}
