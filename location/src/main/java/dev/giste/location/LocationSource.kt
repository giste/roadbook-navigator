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

package dev.giste.location

import kotlinx.coroutines.flow.Flow

/**
 * The device location, as a stream of [LocationEvent]s.
 *
 * Hilt binds it as a singleton in `SingletonComponent`. Inject it wherever location is needed.
 * It needs the app to bind a [LocationSettingsSource].
 */
public interface LocationSource {

    /**
     * Location fixes, provider status changes and errors, from the GPS provider.
     *
     * It is a hot flow shared by every collector:
     *
     * - Location updates start when the first collector arrives, and stop 5 seconds after the
     *   last one leaves, so a configuration change does not restart them.
     * - A new collector first receives the latest event, if updates are running.
     * - Updates restart with the new values whenever [LocationSettingsSource.settings] emits
     *   different settings.
     * - It never throws and never completes. Failures are emitted as [LocationEvent.Error].
     *
     * The app must hold `ACCESS_FINE_LOCATION` before collecting it. Otherwise it emits
     * [LocationEvent.Error] with [LocationError.PERMISSION_DENIED].
     */
    public val events: Flow<LocationEvent>

    /**
     * Whether the device is moving, from the speed of the fixes in [events].
     *
     * Two speeds from [LocationSettings] decide it, with a gap between them (hysteresis) so GPS
     * jitter around a single speed does not flip the state:
     *
     * - A fix faster than [LocationSettings.movingSpeed] means [MotionState.MOVING], and one slower
     *   than [LocationSettings.stoppedSpeed] means [MotionState.STOPPED]. In between, both limits
     *   included, the state does not change.
     * - Without a known speed it counts as stopped, so an app never locks the user out of a
     *   stopped-only action because of a missing value: a fix without a speed, a disabled provider
     *   and a location error mean [MotionState.STOPPED]. [LocationEvent.ProviderEnabled] brings no
     *   speed, so it changes nothing.
     *
     * It is a hot flow shared by every collector, which collects [events] (so it starts and stops
     * location updates the same way). It starts as [MotionState.STOPPED], emits only changes, and
     * gives a new collector the current state while it runs. New speeds from
     * [LocationSettingsSource.settings] apply from the next event. It never throws and never
     * completes.
     */
    public val motion: Flow<MotionState>
}
