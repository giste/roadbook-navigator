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

package dev.giste.roadbook.domain.roadbook

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [RoadbookPositionRepository] for tests.
 *
 * @param initial the position it starts with.
 */
internal class FakeRoadbookPositionRepository(
    initial: WaypointNumber = WaypointNumber.FIRST,
) : RoadbookPositionRepository {

    /** The current position. Tests read it to check what was saved. */
    override val position: MutableStateFlow<WaypointNumber> = MutableStateFlow(initial)

    /** The positions passed to [save], in call order. */
    val saved: MutableList<WaypointNumber> = mutableListOf()

    override suspend fun save(position: WaypointNumber) {
        saved += position
        this.position.value = position
    }
}
