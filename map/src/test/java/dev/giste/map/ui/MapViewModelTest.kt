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

package dev.giste.map.ui

import app.cash.turbine.test
import dev.giste.geo.Bearing
import dev.giste.geo.GeoPoint
import dev.giste.map.FakeLogger
import dev.giste.map.FakeMapLocationSource
import dev.giste.map.FakeMapSettingsSource
import dev.giste.map.MainDispatcherRule
import dev.giste.map.MapSettings
import dev.giste.map.MapTilt
import dev.giste.map.MapZoomLevel
import dev.giste.map.domain.FakeMapFileRepository
import dev.giste.map.domain.InstalledMapFiles
import dev.giste.map.domain.MapCenter
import dev.giste.map.domain.testInstalledFile
import dev.giste.map.domain.testMapFile
import dev.giste.map.testFix
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MapViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val files = listOf(testMapFile("north.map"), testMapFile("south.map"))
    private val repository = FakeMapFileRepository(
        InstalledMapFiles.Listed(
            listOf(
                testInstalledFile("damaged.map", readable = false),
                testInstalledFile("north.map"),
                testInstalledFile("south.map"),
            ),
        ),
    )
    private val settingsSource = FakeMapSettingsSource()
    private val locationSource = FakeMapLocationSource()
    private val logger = FakeLogger()

    private fun viewModel() = MapViewModel(repository, settingsSource, locationSource, logger)

    @Test
    fun uiState_beforeTheMapsAreFound_isLoading() {
        assertEquals(MapUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun uiState_noMaps_isNoMapsAndDoesNotFollowTheLocation() = runTest {
        repository.result = InstalledMapFiles.Listed(emptyList())
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(MapUiState.Loading, awaitItem())
            assertEquals(MapUiState.NoMaps, awaitItem())
            assertEquals(0, locationSource.collectors)
        }
    }

    @Test
    fun uiState_unreadableMaps_isUnreadableMapsAndDoesNotFollowTheLocation() = runTest {
        repository.result = InstalledMapFiles.FolderUnreadable
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(MapUiState.Loading, awaitItem())
            assertEquals(MapUiState.UnreadableMaps, awaitItem())
            assertEquals(0, locationSource.collectors)
        }
    }

    @Test
    fun uiState_onlyUnreadableFiles_isUnreadableMaps() = runTest {
        repository.result = InstalledMapFiles.Listed(listOf(testInstalledFile("damaged.map", readable = false)))
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(MapUiState.Loading, awaitItem())
            assertEquals(MapUiState.UnreadableMaps, awaitItem())
        }
    }

    @Test
    fun uiState_availableMaps_isReadyWithoutCenterUntilTheFirstFix() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            assertEquals(MapUiState.Loading, awaitItem())
            assertEquals(MapUiState.Ready(files, MapSettings(), center = null), awaitItem())
            assertEquals(1, locationSource.collectors)
        }
    }

    @Test
    fun uiState_fix_centersTheMapOnItWithItsBearing() = runTest {
        val viewModel = viewModel()
        val position = GeoPoint(latitude = 41.0, longitude = 2.0)

        viewModel.uiState.test {
            skipItems(2)
            locationSource.emit(testFix(position, Bearing.degrees(90.0)))

            assertEquals(MapCenter(position, Bearing.degrees(90.0)), (awaitItem() as MapUiState.Ready).center)
        }
    }

    @Test
    fun uiState_fixWithoutBearing_keepsThePreviousBearing() = runTest {
        val viewModel = viewModel()
        val stop = GeoPoint(latitude = 41.0, longitude = 2.0)

        viewModel.uiState.test {
            skipItems(2)
            locationSource.emit(testFix(bearing = Bearing.degrees(270.0)))
            skipItems(1)
            locationSource.emit(testFix(stop, bearing = null))

            assertEquals(MapCenter(stop, Bearing.degrees(270.0)), (awaitItem() as MapUiState.Ready).center)
        }
    }

    @Test
    fun uiState_settingsChange_isApplied() = runTest {
        val viewModel = viewModel()
        val newSettings = MapSettings(zoomLevel = MapZoomLevel.of(15), tilt = MapTilt.of(30.0))

        viewModel.uiState.test {
            skipItems(2)
            settingsSource.current.value = newSettings

            assertEquals(newSettings, (awaitItem() as MapUiState.Ready).settings)
        }
    }

    @Test
    fun uiState_settingsFail_usesTheDefaultsAndLogs() = runTest {
        val failure = IOException("settings broken")
        settingsSource.current.value = MapSettings(zoomLevel = MapZoomLevel.MIN)
        settingsSource.failure = failure
        val viewModel = viewModel()

        viewModel.uiState.test {
            skipItems(1)
            assertEquals(MapSettings(), (awaitItem() as MapUiState.Ready).settings)
            assertEquals(listOf(failure), logger.loggedErrors)
        }
    }

    @Test
    fun uiState_locationFails_staysReadyWithoutCenterAndLogs() = runTest {
        val failure = IllegalStateException("location broken")
        locationSource.failure = failure
        val viewModel = viewModel()

        viewModel.uiState.test {
            skipItems(1)
            assertEquals(MapUiState.Ready(files, MapSettings(), center = null), awaitItem())
            assertEquals(listOf(failure), logger.loggedErrors)
            expectNoEvents()
        }
    }

    @Test
    fun uiState_settingsChange_keepsTheCenter() = runTest {
        val viewModel = viewModel()
        val position = GeoPoint(latitude = 41.0, longitude = 2.0)

        viewModel.uiState.test {
            skipItems(2)
            locationSource.emit(testFix(position))
            skipItems(1)
            settingsSource.current.value = MapSettings(tilt = MapTilt.MIN)

            assertEquals(MapCenter(position, bearing = null), (awaitItem() as MapUiState.Ready).center)
        }
    }

    @Test
    fun uiState_resubscribedAfterTheTimeout_looksForTheMapsAgain() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test { skipItems(2) }
        testScheduler.advanceTimeBy(6_000)
        repository.result = InstalledMapFiles.Listed(emptyList())

        viewModel.uiState.test {
            // The stale value is replayed first, then the new search replaces it.
            skipItems(1)
            assertEquals(MapUiState.NoMaps, awaitItem())
        }
        assertEquals(2, repository.collections)
    }

    @Test
    fun uiState_resubscribedWithinTheTimeout_keepsTheMaps() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test { skipItems(2) }
        testScheduler.advanceTimeBy(1_000)

        viewModel.uiState.test {
            assertEquals(MapUiState.Ready(files, MapSettings(), center = null), awaitItem())
        }
        assertEquals(1, repository.collections)
    }
}
