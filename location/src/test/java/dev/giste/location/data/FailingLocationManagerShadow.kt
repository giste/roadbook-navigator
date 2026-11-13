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

package dev.giste.location.data

import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Build
import java.util.concurrent.Executor
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowLocationManager

/**
 * Robolectric shadow of [LocationManager] whose request for updates always throws [failure].
 *
 * Robolectric's own [ShadowLocationManager] never checks permissions, so this shadow stands in
 * for the platform failures [GpsLocationRepository] must report. Use it with
 * `@Config(shadows = [FailingLocationManagerShadow::class])`.
 */
@Implements(LocationManager::class)
class FailingLocationManagerShadow : ShadowLocationManager() {

    @Implementation(minSdk = Build.VERSION_CODES.S)
    override fun requestLocationUpdates(
        provider: String,
        request: LocationRequest,
        executor: Executor,
        listener: LocationListener,
    ) {
        throw failure
    }

    companion object {
        /** The exception thrown by every request. Tests set it before collecting. */
        var failure: RuntimeException = SecurityException("Location permission missing")
    }
}
