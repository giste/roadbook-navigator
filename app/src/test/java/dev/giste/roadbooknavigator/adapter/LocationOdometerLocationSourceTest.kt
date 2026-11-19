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

package dev.giste.roadbooknavigator.adapter

import app.cash.turbine.test
import dev.giste.location.LocationError
import dev.giste.location.LocationEvent
import dev.giste.roadbooknavigator.FakeLocationSource
import dev.giste.roadbooknavigator.fixAt
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationOdometerLocationSourceTest {

    private val locationSource = FakeLocationSource()
    private val adapter = LocationOdometerLocationSource(locationSource)

    @Test
    fun locations_locationUpdated_forwardsTheFix() = runTest {
        val event = fixAt(30.0)

        adapter.locations.test {
            locationSource.emit(event)

            assertEquals(event.location, awaitItem())
        }
    }

    @Test
    fun locations_providerAndErrorEvents_areDropped() = runTest {
        val event = fixAt(30.0)

        adapter.locations.test {
            locationSource.emit(LocationEvent.ProviderDisabled)
            locationSource.emit(LocationEvent.ProviderEnabled)
            locationSource.emit(LocationEvent.Error(LocationError.UNKNOWN))
            locationSource.emit(event)

            // Only the fix arrives.
            assertEquals(event.location, awaitItem())
        }
    }
}
