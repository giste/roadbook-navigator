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

import dev.giste.geo.Distance
import dev.giste.odometer.OdometerState
import kotlinx.coroutines.flow.Flow

/**
 * Keeps the [OdometerState] and persists it.
 *
 * It is the single source of truth of the distances. Implementations never throw: storage
 * failures are logged, and the state in memory stays correct.
 */
internal interface OdometerStateRepository {

    /**
     * The current state, once it has been read from storage, followed by every change.
     *
     * It never throws and never completes.
     */
    val state: Flow<OdometerState>

    /**
     * Adds [distance] to both the trip and the total distance.
     *
     * It is called for every accepted fix, so implementations may delay saving it to limit disk
     * writes. A process death can then lose the most recent distance.
     *
     * @param distance the distance travelled since the previous call.
     */
    suspend fun addTravelled(distance: Distance)

    /**
     * Replaces the state with the result of [transform], and saves it at once.
     *
     * Calls are serialized with [addTravelled], so [transform] always receives the latest state.
     *
     * @param transform computes the new state from the current one. It must be fast and pure.
     */
    suspend fun update(transform: (OdometerState) -> OdometerState)
}
