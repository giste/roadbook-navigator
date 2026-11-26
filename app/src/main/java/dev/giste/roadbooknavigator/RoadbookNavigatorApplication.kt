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
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import dev.giste.core.logging.DebugLogging
import dev.giste.roadbooknavigator.domain.coordination.RoadbookOdometerCoordinator
import javax.inject.Inject

/**
 * The application: the root of the Hilt dependency graph.
 *
 * It turns on logging in debuggable builds only, so release builds never write logs, and starts
 * the coordinators that apply the cross-module rules (AGENTS.md §4.3).
 *
 * It also configures WorkManager with Hilt's worker factory, so the modules' workers (such as
 * `:map`'s downloads) are injected. WorkManager's default start-up is removed in the manifest, so
 * WorkManager is created on first use with this configuration.
 */
@HiltAndroidApp
class RoadbookNavigatorApplication : Application(), Configuration.Provider {

    /** Creates the workers of every module with their dependencies. Injected by Hilt in [onCreate]. */
    @Inject
    internal lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

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
