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

import kotlinx.coroutines.flow.Flow

/**
 * Keeps the position of the roadbook, the [WaypointNumber] of the waypoint shown at the top, and
 * persists it, so the rider finds the roadbook where they left it after process death or a
 * restart.
 *
 * Only the waypoint is kept, not how far the screen is scrolled within it: that depends on the
 * screen, and is transient UI state.
 *
 * It does not know the roadbook, so it does not check that the number is one of its waypoints:
 * the code that uses it keeps the position within the roadbook (e.g. after importing a shorter
 * one). Implementations never throw: storage failures are logged.
 */
internal interface RoadbookPositionRepository {

    /**
     * The saved position, once it has been read from storage, followed by every change.
     * [WaypointNumber.FIRST] when nothing has been saved, or what was saved cannot be read.
     *
     * It never throws and never completes.
     */
    val position: Flow<WaypointNumber>

    /**
     * Saves [position] as the current one. [position][RoadbookPositionRepository.position] emits
     * it at once, even if writing it to storage fails, in which case it is only kept until the
     * process ends.
     *
     * @param position the number of the waypoint shown at the top.
     */
    suspend fun save(position: WaypointNumber)
}
