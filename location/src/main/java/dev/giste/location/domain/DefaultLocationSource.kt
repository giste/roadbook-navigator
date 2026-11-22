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
import dev.giste.location.MotionState
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn

/**
 * [LocationSource] that follows the current [LocationSettings].
 *
 * It restarts the [LocationRepository] request whenever the request values of the settings
 * change, and shares one request among every collector in [scope]. [motion] is derived from
 * [events] with the motion speeds of the settings, and shared the same way.
 *
 * @param repository where the location events come from.
 * @param settingsSource the settings of the request.
 * @param logger logs request changes and settings failures.
 * @param scope the scope the shared request runs in. It must outlive every collector, so it is
 * the application scope in production.
 */
internal class DefaultLocationSource @Inject constructor(
    private val repository: LocationRepository,
    settingsSource: LocationSettingsSource,
    private val logger: Logger,
    @ApplicationScope scope: CoroutineScope,
) : LocationSource {

    /**
     * The current settings. A broken settings source must not stop location updates or motion
     * detection, so it falls back to the defaults. Cold: [events] and [motion] each collect it.
     */
    private val currentSettings: Flow<LocationSettings> = settingsSource.settings
        .catch { error ->
            logger.e(error, "Location settings failed, using the defaults")
            emit(LocationSettings())
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val events: Flow<LocationEvent> = currentSettings
        // Restarting a platform request is not free, so only restart when a value of the request
        // changed. The motion speeds are not part of it.
        .distinctUntilChanged { old, new ->
            old.updateInterval == new.updateInterval &&
                old.minUpdateDistance == new.minUpdateDistance
        }
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

    @OptIn(ExperimentalCoroutinesApi::class)
    override val motion: Flow<MotionState> = currentSettings
        // A settings change resubscribes to the shared events, which replay the latest one, so it
        // is judged again with the new speeds. Sharing keeps the requests running meanwhile.
        .flatMapLatest { settings -> events.map { event -> event to settings } }
        .scan(MotionState.STOPPED) { state, (event, settings) -> state.after(event, settings) }
        .distinctUntilChanged()
        .shareIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = STOP_TIMEOUT_MILLIS,
                // Once updates stop, the last state is stale: the next collector starts again
                // from stopped.
                replayExpirationMillis = 0,
            ),
            replay = 1,
        )

    private companion object {
        /** Keeps updates running across short gaps, such as a configuration change. */
        const val STOP_TIMEOUT_MILLIS: Long = 5_000
    }
}
