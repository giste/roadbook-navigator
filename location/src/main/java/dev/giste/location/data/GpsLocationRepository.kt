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

import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import dev.giste.core.logging.Logger
import dev.giste.location.LocationEvent
import dev.giste.location.LocationSettings
import dev.giste.location.domain.LocationRepository
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * [LocationRepository] that reads the GPS provider of the platform [LocationManager].
 *
 * It asks for high-accuracy fixes, because GPS is the only provider precise enough to measure
 * distances along a route.
 *
 * @param locationManager the platform location service.
 * @param logger logs request start, stop, failures and dropped fixes. Never coordinates.
 */
internal class GpsLocationRepository @Inject constructor(
    private val locationManager: LocationManager,
    private val logger: Logger,
) : LocationRepository {

    // The permission is checked by the platform: a SecurityException becomes an Error event.
    @SuppressLint("MissingPermission")
    override fun events(settings: LocationSettings): Flow<LocationEvent> = callbackFlow {
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val userLocation = location.toUserLocation()
                if (userLocation == null) {
                    logger.w("Dropped a location fix with invalid values")
                } else {
                    trySend(LocationEvent.LocationUpdated(userLocation))
                }
            }

            override fun onProviderEnabled(provider: String) {
                logger.i("Location provider %s enabled", provider)
                trySend(LocationEvent.ProviderEnabled)
            }

            override fun onProviderDisabled(provider: String) {
                logger.w("Location provider %s disabled", provider)
                trySend(LocationEvent.ProviderDisabled)
            }
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                settings.toLocationRequest(),
                DirectExecutor,
                listener,
            )
        } catch (error: RuntimeException) {
            val reason = error.toLocationError()
            logger.e(error, "Could not request location updates: %s", reason)
            trySend(LocationEvent.Error(reason))
            // Complete normally: the failure has been reported as an event, not as an exception.
            close()
        }

        awaitClose {
            logger.d("Removing location updates")
            locationManager.removeUpdates(listener)
        }
    }

    private companion object {
        /**
         * Runs listener callbacks on the platform thread that delivers them. `trySend` is
         * thread-safe and never blocks, so there is no need to hop to the main thread.
         */
        val DirectExecutor: Executor = Executor { command -> command.run() }
    }
}

/** Converts the settings to a high-accuracy platform request. */
internal fun LocationSettings.toLocationRequest(): LocationRequest =
    LocationRequest.Builder(updateInterval.inWholeMilliseconds)
        .setQuality(LocationRequest.QUALITY_HIGH_ACCURACY)
        .setMinUpdateDistanceMeters(minUpdateDistance.inMeters.toFloat())
        .build()
