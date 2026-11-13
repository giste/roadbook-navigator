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

package dev.giste.location.domain

import dev.giste.core.coroutines.ApplicationScope
import dev.giste.core.logging.Logger
import dev.giste.location.LocationEvent
import dev.giste.location.LocationSettings
import dev.giste.location.LocationSettingsSource
import dev.giste.location.LocationSource
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.shareIn

/**
 * [LocationSource] that follows the current [LocationSettings].
 *
 * It restarts the [LocationRepository] request on every distinct settings value, and shares one
 * request among every collector in [scope].
 *
 * @param repository where the location events come from.
 * @param settingsSource the settings of the request.
 * @param logger logs settings changes and settings failures.
 * @param scope the scope the shared request runs in. It must outlive every collector, so it is
 * the application scope in production.
 */
internal class DefaultLocationSource @Inject constructor(
    private val repository: LocationRepository,
    settingsSource: LocationSettingsSource,
    private val logger: Logger,
    @ApplicationScope scope: CoroutineScope,
) : LocationSource {

    @OptIn(ExperimentalCoroutinesApi::class)
    override val events: Flow<LocationEvent> = settingsSource.settings
        // A broken settings source must not stop location updates: fall back to the defaults.
        .catch { error ->
            logger.e(error, "Location settings failed, using the defaults")
            emit(LocationSettings())
        }
        // Restarting a platform request is not free, so only restart when something changed.
        .distinctUntilChanged()
        .flatMapLatest { settings ->
            logger.d("Requesting location updates with %s", settings)
            repository.events(settings)
        }
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = STOP_TIMEOUT_MILLIS,
                // Once updates stop, the last event is stale: do not replay it to the next
                // collector.
                replayExpirationMillis = 0,
            ),
            replay = 1,
        )

    private companion object {
        /** Keeps updates running across short gaps, such as a configuration change. */
        const val STOP_TIMEOUT_MILLIS: Long = 5_000
    }
}
