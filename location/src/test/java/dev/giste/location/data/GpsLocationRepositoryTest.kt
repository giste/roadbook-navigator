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

import android.Manifest
import android.app.Application
import android.location.Location
import android.location.LocationManager
import android.location.LocationRequest
import app.cash.turbine.test
import dev.giste.geo.Distance
import dev.giste.geo.GeoPoint
import dev.giste.location.FakeLogger
import dev.giste.location.LocationError
import dev.giste.location.LocationEvent
import dev.giste.location.LocationSettings
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLocationManager

/**
 * Tests [GpsLocationRepository] against Robolectric's [LocationManager], which records requests
 * and delivers simulated fixes without a real GPS.
 */
// runCurrent() is still experimental in kotlinx-coroutines-test.
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class GpsLocationRepositoryTest {

    private lateinit var application: Application
    private lateinit var locationManager: LocationManager
    private lateinit var shadowLocationManager: ShadowLocationManager
    private lateinit var repository: GpsLocationRepository

    private val settings = LocationSettings(
        updateInterval = 2.seconds,
        minUpdateDistance = Distance.meters(10.0),
    )

    @Before
    fun setUp() {
        application = RuntimeEnvironment.getApplication()
        shadowOf(application).grantPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
        locationManager = application.getSystemService(LocationManager::class.java)
        shadowLocationManager = shadowOf(locationManager)
        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        repository = GpsLocationRepository(locationManager, FakeLogger())
    }

    private fun gpsRequests(): List<LocationRequest> =
        shadowLocationManager.getLocationRequests(LocationManager.GPS_PROVIDER)

    /**
     * Creates a fix [secondsAfterStart] seconds into the route. Robolectric drops fixes that come
     * sooner than the request interval after the previous one, so consecutive fixes in a test
     * must be at least [settings]' interval apart.
     */
    private fun gpsFix(latitude: Double, secondsAfterStart: Long = 0): Location =
        Location(LocationManager.GPS_PROVIDER).apply {
            this.latitude = latitude
            longitude = -3.7038
            accuracy = 5f
            time = 1_700_000_000_000 + secondsAfterStart * 1_000
            elapsedRealtimeNanos = (1 + secondsAfterStart) * 1_000_000_000
        }

    @Test
    fun events_collected_requestsGpsUpdatesWithSettings() = runTest {
        repository.events(settings).test {
            val request = gpsRequests().single()

            assertEquals(2_000L, request.intervalMillis)
            assertEquals(10f, request.minUpdateDistanceMeters, 0f)
            assertEquals(LocationRequest.QUALITY_HIGH_ACCURACY, request.quality)
        }
    }

    @Test
    fun events_collectionCancelled_removesUpdates() = runTest {
        val collector = launch { repository.events(settings).collect {} }
        runCurrent()

        collector.cancel()
        runCurrent()

        assertTrue(gpsRequests().isEmpty())
    }

    @Test
    fun events_gpsFix_emitsLocationUpdated() = runTest {
        repository.events(settings).test {
            shadowLocationManager.simulateLocation(gpsFix(latitude = 40.4168))

            val event = awaitItem() as LocationEvent.LocationUpdated
            assertEquals(GeoPoint(latitude = 40.4168, longitude = -3.7038), event.location.position)
        }
    }

    @Test
    fun events_invalidFix_isDropped() = runTest {
        repository.events(settings).test {
            shadowLocationManager.simulateLocation(gpsFix(latitude = 91.0))
            shadowLocationManager.simulateLocation(gpsFix(latitude = 41.0, secondsAfterStart = 10))

            val event = awaitItem() as LocationEvent.LocationUpdated
            assertEquals(41.0, event.location.position.latitude, 0.0)
        }
    }

    @Test
    fun events_providerDisabled_emitsProviderDisabled() = runTest {
        repository.events(settings).test {
            shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, false)

            assertEquals(LocationEvent.ProviderDisabled, awaitItem())
        }
    }

    @Test
    fun events_providerEnabledAgain_emitsProviderEnabled() = runTest {
        repository.events(settings).test {
            shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, false)
            skipItems(1)
            shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, true)

            assertEquals(LocationEvent.ProviderEnabled, awaitItem())
        }
    }

    @Test
    @Config(shadows = [FailingLocationManagerShadow::class])
    fun events_permissionMissing_emitsPermissionDeniedAndCompletes() = runTest {
        FailingLocationManagerShadow.failure = SecurityException("Location permission missing")

        repository.events(settings).test {
            assertEquals(LocationEvent.Error(LocationError.PERMISSION_DENIED), awaitItem())
            awaitComplete()
        }
    }

    @Test
    @Config(shadows = [FailingLocationManagerShadow::class])
    fun events_unexpectedFailure_emitsUnknownAndCompletes() = runTest {
        FailingLocationManagerShadow.failure = IllegalStateException("Location service crashed")

        repository.events(settings).test {
            assertEquals(LocationEvent.Error(LocationError.UNKNOWN), awaitItem())
            awaitComplete()
        }
    }
}
