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

package dev.giste.roadbooknavigator.coordination

import dev.giste.core.coroutines.ApplicationScope
import dev.giste.odometer.OdometerController
import dev.giste.roadbook.RoadbookController
import dev.giste.roadbook.RoadbookEvent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Applies the roadbook's events to the odometer (AGENTS.md §4.3):
 *
 * - [RoadbookEvent.Loaded] resets both the partial and the total, as the previous app did, so a
 *   new roadbook starts from zero.
 * - [RoadbookEvent.SetPartialRequested] sets the partial (the odometer's trip distance) to the
 *   requested value.
 *
 * It needs no location, so it runs from app start, independently of the permission gate.
 *
 * @param roadbook emits the events.
 * @param odometer receives the changes.
 * @param scope the application scope: the coordinator lives as long as the process.
 */
@Singleton
internal class RoadbookOdometerCoordinator @Inject constructor(
    private val roadbook: RoadbookController,
    private val odometer: OdometerController,
    @param:ApplicationScope private val scope: CoroutineScope,
) {

    /**
     * Starts collecting the roadbook's events. Call it once, from `Application.onCreate`.
     *
     * Events are handled one at a time, in order. Neither the events flow nor the odometer
     * functions throw, so the collection only ends with the scope.
     *
     * @return the collecting job, so tests can cancel it.
     */
    fun start(): Job = scope.launch {
        roadbook.events.collect { event ->
            when (event) {
                RoadbookEvent.Loaded -> odometer.resetAll()
                is RoadbookEvent.SetPartialRequested -> odometer.setTrip(event.partial)
            }
        }
    }
}
