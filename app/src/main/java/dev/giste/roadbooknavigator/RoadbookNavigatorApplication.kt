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

/**
 * The application: the root of the Hilt dependency graph.
 *
 * It turns on logging in debuggable builds only, so release builds never write logs.
 */
@HiltAndroidApp
class RoadbookNavigatorApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // The debuggable flag comes from the build type, so no BuildConfig field is needed.
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            DebugLogging.install()
        }
    }
}
