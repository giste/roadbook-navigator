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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * [OdometerStateRepository] for tests: keeps the state in memory, and records every travelled
 * distance it receives.
 *
 * @param initial the state it starts with.
 */
internal class FakeOdometerStateRepository(
    initial: OdometerState = OdometerState.ZERO,
) : OdometerStateRepository {

    private val _state = MutableStateFlow(initial)

    /** The distances passed to [addTravelled], in call order. */
    val travelled: MutableList<Distance> = mutableListOf()

    override val state: StateFlow<OdometerState> = _state

    override suspend fun addTravelled(distance: Distance) {
        travelled += distance
        _state.update { OdometerState(trip = it.trip + distance, total = it.total + distance) }
    }

    override suspend fun update(transform: (OdometerState) -> OdometerState) {
        _state.update(transform)
    }
}
