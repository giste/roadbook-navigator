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

package dev.giste.odometer

import dev.giste.geo.Distance
import kotlinx.coroutines.flow.Flow

/**
 * The odometer: measures the trip and total distance travelled, and lets the app change them.
 *
 * Hilt binds it as a singleton in `SingletonComponent`. Inject it wherever the distances are shown
 * or needed. It needs the app to bind an [OdometerLocationSource] and an [OdometerSettingsSource].
 *
 * The distances are persisted, so they survive process death and restarts. All functions are
 * main-safe. Changes made through them are applied in call order and saved at once.
 */
public interface OdometerController {

    /**
     * The current distances, followed by every change.
     *
     * It is a hot flow shared by every collector:
     *
     * - While it is collected, the odometer collects [OdometerLocationSource.locations] and adds
     *   the distance between accepted fixes to both distances. It stops 5 seconds after the last
     *   collector leaves, so a configuration change does not interrupt it. Distance travelled
     *   while it is not collected is not counted.
     * - A new collector first receives the current state, once it has been read from storage. It
     *   never emits a placeholder state before that.
     * - It never throws and never completes. Storage failures are logged, and the odometer keeps
     *   working with the distances it has in memory.
     */
    public val state: Flow<OdometerState>

    /** Sets the trip distance to zero. The total distance does not change. */
    public suspend fun resetTrip()

    /** Sets both the trip and the total distance to zero. */
    public suspend fun resetAll()

    /**
     * Sets the trip distance to [distance], e.g. to match a reference point of the route. The
     * total distance does not change.
     *
     * @param distance the new trip distance.
     */
    public suspend fun setTrip(distance: Distance)

    /**
     * Adds [by] to the trip distance, e.g. to correct it by hand. The total distance does not
     * change.
     *
     * @param by the distance to add.
     */
    public suspend fun increaseTrip(by: Distance)

    /**
     * Subtracts [by] from the trip distance, e.g. to correct it by hand. The trip distance stops at
     * zero, and the total distance does not change.
     *
     * @param by the distance to subtract.
     */
    public suspend fun decreaseTrip(by: Distance)
}
