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

package dev.giste.odometer.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import dev.giste.core.logging.Logger
import dev.giste.geo.Distance
import dev.giste.odometer.OdometerState
import dev.giste.odometer.domain.OdometerStateRepository
import java.io.IOException
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeMark
import kotlin.time.TimeSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * [OdometerStateRepository] that keeps the state in memory and persists it in a DataStore.
 *
 * The state is read from [dataStore] once, on first use. After that the copy in memory is the
 * source of truth, because this repository is its only writer. A fix arrives every second or so,
 * so travelled distance is not written on every call: it is saved once [SAVE_DISTANCE] has
 * accumulated, or once [SAVE_INTERVAL] has passed since the last save, whichever comes first.
 * Explicit changes ([update]) are saved at once.
 *
 * Storage failures never escape. A failed read starts from [OdometerState.ZERO], an invalid
 * stored value is read as zero, and a failed write keeps the state in memory and is retried on
 * the next change. All are logged.
 *
 * It must be a singleton: two instances would each keep their own copy of the state.
 *
 * @param dataStore the odometer DataStore. Nothing else may write to it.
 * @param timeSource measures the time since the last save.
 * @param logger logs storage failures.
 */
internal class DataStoreOdometerStateRepository @Inject constructor(
    @OdometerStateDataStore private val dataStore: DataStore<Preferences>,
    @OdometerTimeSource private val timeSource: TimeSource,
    private val logger: Logger,
) : OdometerStateRepository {

    /** Serializes loading and every change, so no change is lost or applied out of order. */
    private val mutex = Mutex()

    /** The current state, or `null` until it has been read from [dataStore]. */
    private val current = MutableStateFlow<OdometerState?>(null)

    /** Travelled distance added since the last successful save. Guarded by [mutex]. */
    private var unsavedDistance = Distance.ZERO

    /** When the state was last saved, or loaded. Guarded by [mutex]. */
    private var lastSave: TimeMark? = null

    override val state: Flow<OdometerState> = current
        .onStart { mutex.withLock { loadIfNeeded() } }
        .filterNotNull()

    override suspend fun addTravelled(distance: Distance) {
        mutex.withLock {
            val state = loadIfNeeded()
            val newState = OdometerState(trip = state.trip + distance, total = state.total + distance)
            current.value = newState
            unsavedDistance += distance

            val sinceLastSave = lastSave?.elapsedNow() ?: Duration.INFINITE
            if (unsavedDistance >= SAVE_DISTANCE || sinceLastSave >= SAVE_INTERVAL) {
                save(newState)
            }
        }
    }

    override suspend fun update(transform: (OdometerState) -> OdometerState) {
        mutex.withLock {
            val newState = transform(loadIfNeeded())
            current.value = newState
            save(newState)
        }
    }

    /**
     * Returns the current state, reading it from [dataStore] the first time. Call it with [mutex]
     * held.
     */
    private suspend fun loadIfNeeded(): OdometerState {
        current.value?.let { return it }

        val preferences = try {
            dataStore.data.first()
        } catch (error: IOException) {
            logger.e(error, "Could not read the odometer state, starting from zero")
            emptyPreferences()
        }
        val state = OdometerState(
            trip = preferences.distance(TRIP_KEY),
            total = preferences.distance(TOTAL_KEY),
        )
        current.value = state
        lastSave = timeSource.markNow()
        return state
    }

    /** Writes [state] to [dataStore]. Call it with [mutex] held. */
    private suspend fun save(state: OdometerState) {
        try {
            dataStore.edit { preferences ->
                preferences[TRIP_KEY] = state.trip.inMeters
                preferences[TOTAL_KEY] = state.total.inMeters
            }
            unsavedDistance = Distance.ZERO
            lastSave = timeSource.markNow()
        } catch (error: IOException) {
            // The state in memory is still right. unsavedDistance and lastSave are left as they
            // are, so the next change tries again.
            logger.e(error, "Could not save the odometer state")
        }
    }

    /** Reads the distance stored under [key], or zero if it is missing or invalid. */
    private fun Preferences.distance(key: Preferences.Key<Double>): Distance {
        val meters = this[key] ?: return Distance.ZERO
        if (!meters.isFinite() || meters < 0.0) {
            logger.w("Invalid stored %s: %f, using zero", key.name, meters)
            return Distance.ZERO
        }
        return Distance.meters(meters)
    }

    internal companion object {

        /** Name of the DataStore file. Prefixed with the module name (AGENTS.md §7). */
        const val FILE_NAME: String = "odometer_state"

        /** Unsaved travelled distance that triggers a save. */
        val SAVE_DISTANCE: Distance = Distance.meters(500.0)

        /** Time since the last save after which travelled distance is saved anyway. */
        val SAVE_INTERVAL: Duration = 1.minutes

        /** Trip distance in meters. */
        val TRIP_KEY: Preferences.Key<Double> = doublePreferencesKey("trip_distance_meters")

        /** Total distance in meters. */
        val TOTAL_KEY: Preferences.Key<Double> = doublePreferencesKey("total_distance_meters")
    }
}
