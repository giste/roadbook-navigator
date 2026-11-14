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

package dev.giste.odometer.domain

import dev.giste.core.coroutines.ApplicationScope
import dev.giste.core.logging.Logger
import dev.giste.geo.Distance
import dev.giste.odometer.OdometerController
import dev.giste.odometer.OdometerLocation
import dev.giste.odometer.OdometerLocationSource
import dev.giste.odometer.OdometerSettings
import dev.giste.odometer.OdometerSettingsSource
import dev.giste.odometer.OdometerState
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * [OdometerController] that measures distance from an [OdometerLocationSource] and keeps it in an
 * [OdometerStateRepository].
 *
 * @param repository keeps and persists the distances.
 * @param locationSource the fixes to measure distance from.
 * @param settingsSource how fixes are filtered.
 * @param logger logs commands, ignored fixes and failures. Never positions.
 * @param scope the scope the shared state runs in. It must outlive every collector, so it is the
 * application scope in production.
 */
internal class DefaultOdometerController @Inject constructor(
    private val repository: OdometerStateRepository,
    private val locationSource: OdometerLocationSource,
    settingsSource: OdometerSettingsSource,
    private val logger: Logger,
    @ApplicationScope scope: CoroutineScope,
) : OdometerController {

    /** The settings, falling back to the defaults if the source fails. */
    private val settings: Flow<OdometerSettings> = settingsSource.settings
        .catch { error ->
            logger.e(error, "Odometer settings failed, using the defaults")
            emit(OdometerSettings())
        }

    override val state: Flow<OdometerState> = channelFlow {
        // Measuring runs next to the state, so it only runs while someone collects the state.
        launch { measureDistance() }
        repository.state.collect { send(it) }
    }.shareIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(
            stopTimeoutMillis = STOP_TIMEOUT_MILLIS,
            // The repository replays the current state to a new collection anyway, so a stale
            // replay would only show an outdated value first.
            replayExpirationMillis = 0,
        ),
        replay = 1,
    )

    override suspend fun resetTrip() {
        logger.i("Resetting the trip distance")
        repository.update { it.copy(trip = Distance.ZERO) }
    }

    override suspend fun resetAll() {
        logger.i("Resetting all distances")
        repository.update { OdometerState.ZERO }
    }

    override suspend fun setTrip(distance: Distance) {
        logger.i("Setting the trip distance to %s", distance)
        repository.update { it.copy(trip = distance) }
    }

    override suspend fun increaseTrip(by: Distance) {
        logger.d("Increasing the trip distance by %s", by)
        repository.update { it.copy(trip = it.trip + by) }
    }

    override suspend fun decreaseTrip(by: Distance) {
        logger.d("Decreasing the trip distance by %s", by)
        // Distance cannot be negative, so the trip stops at zero.
        repository.update { it.copy(trip = if (by >= it.trip) Distance.ZERO else it.trip - by) }
    }

    /**
     * Collects the fixes and adds the distance between accepted ones to the repository.
     *
     * The anchor starts empty on every collection, so distance travelled while the state was not
     * collected is never counted. It suspends until the settings source emits for the first time.
     */
    private suspend fun measureDistance(): Unit = coroutineScope {
        // Settings changes must not reprocess a fix, so the latest value is read per fix instead
        // of combining both flows.
        val currentSettings = settings.stateIn(this)
        var anchor: OdometerLocation? = null

        locationSource.locations
            .catch { error -> logger.e(error, "Odometer locations failed, measuring stopped") }
            .collect { fix ->
                when (val result = filterFix(anchor, fix, currentSettings.value)) {
                    is FixResult.Ignored -> logger.v("Fix ignored: %s", result.reason)
                    FixResult.Anchored -> {
                        logger.d("First accurate fix received, measuring distance")
                        anchor = fix
                    }
                    is FixResult.Travelled -> {
                        anchor = fix
                        if (result.distance > Distance.ZERO) {
                            repository.addTravelled(result.distance)
                        }
                    }
                }
            }
    }

    private companion object {
        /** Keeps measuring across short gaps, such as a configuration change. */
        const val STOP_TIMEOUT_MILLIS: Long = 5_000
    }
}
