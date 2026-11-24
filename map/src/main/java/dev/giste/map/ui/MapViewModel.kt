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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.giste.core.logging.Logger
import dev.giste.geo.LocationFix
import dev.giste.map.MapLocationSource
import dev.giste.map.MapSettings
import dev.giste.map.MapSettingsSource
import dev.giste.map.domain.InstalledMaps
import dev.giste.map.domain.MapCenter
import dev.giste.map.domain.MapFileRepository
import dev.giste.map.domain.follow
import dev.giste.map.domain.toInstalledMaps
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn

/**
 * The state of the map viewer.
 *
 * It looks for the installed maps once per subscription. Only when there are maps to draw does it
 * collect the settings and the location, so a viewer without maps does not keep the location
 * running for nothing.
 *
 * @param mapFileRepository the installed maps.
 * @param settingsSource the zoom level and tilt.
 * @param locationSource the location the map follows.
 * @param logger logs failures of the settings and the location.
 */
@HiltViewModel
internal class MapViewModel @Inject constructor(
    mapFileRepository: MapFileRepository,
    settingsSource: MapSettingsSource,
    locationSource: MapLocationSource,
    private val logger: Logger,
) : ViewModel() {

    /** The settings, falling back to the defaults if the source fails. */
    private val settings: Flow<MapSettings> = settingsSource.settings.catch { error ->
        logger.e(error, "Map settings failed, using the defaults")
        emit(MapSettings())
    }

    /**
     * Where the map is centered: `null` at once, then after every fix. If the location fails,
     * the last center stays.
     */
    private val centers: Flow<MapCenter?> = locationSource.locations
        .scan(null as MapCenter?) { center, fix: LocationFix -> center.follow(fix) }
        .catch { error -> logger.e(error, "Map location failed, the map stops following it") }

    /**
     * The state of the viewer. It starts as [MapUiState.Loading], and is shared while the viewer
     * is shown and for [STOP_TIMEOUT_MILLIS] after, so a configuration change does not look for
     * the maps again.
     */
    // flatMapLatest is still experimental in kotlinx.coroutines.
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MapUiState> = mapFileRepository.installedFiles
        .map { it.toInstalledMaps() }
        .flatMapLatest { maps ->
            when (maps) {
                InstalledMaps.None -> flowOf(MapUiState.NoMaps)
                InstalledMaps.Unreadable -> flowOf(MapUiState.UnreadableMaps)
                is InstalledMaps.Available -> combine(settings, centers) { settings, center ->
                    MapUiState.Ready(files = maps.files, settings = settings, center = center)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = MapUiState.Loading,
        )

    private companion object {
        /** Keeps the state across short gaps, such as a configuration change. */
        const val STOP_TIMEOUT_MILLIS: Long = 5_000
    }
}
