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

package dev.giste.roadbooknavigator

import android.app.Application
import android.content.pm.ApplicationInfo
import dagger.hilt.android.HiltAndroidApp
import dev.giste.core.logging.DebugLogging
import dev.giste.roadbooknavigator.coordination.RoadbookOdometerCoordinator
import javax.inject.Inject

/**
 * The application: the root of the Hilt dependency graph.
 *
 * It turns on logging in debuggable builds only, so release builds never write logs, and starts
 * the coordinators that apply the cross-module rules (AGENTS.md §4.3).
 */
@HiltAndroidApp
class RoadbookNavigatorApplication : Application() {

    /** Applies the roadbook's events to the odometer. Injected by Hilt in [onCreate]. */
    @Inject
    internal lateinit var roadbookOdometerCoordinator: RoadbookOdometerCoordinator

    override fun onCreate() {
        // Hilt injects the fields here.
        super.onCreate()
        // The debuggable flag comes from the build type, so no BuildConfig field is needed.
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            DebugLogging.install()
        }
        roadbookOdometerCoordinator.start()
    }
}
