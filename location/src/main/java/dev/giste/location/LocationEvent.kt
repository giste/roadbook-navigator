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

/**
 * Something that happened to the device location, as emitted by [LocationSource.events].
 *
 * Failures are events too ([Error]), so the flow never throws.
 */
public sealed interface LocationEvent {

    /**
     * A new location fix was received.
     *
     * @property location the new fix.
     */
    public data class LocationUpdated(public val location: UserLocation) : LocationEvent

    /** The GPS provider was turned on, e.g. the user enabled location in the system settings. */
    public data object ProviderEnabled : LocationEvent

    /**
     * The GPS provider was turned off, e.g. the user disabled location in the system settings.
     * No fixes arrive until [ProviderEnabled].
     */
    public data object ProviderDisabled : LocationEvent

    /**
     * Location updates could not be started. No fixes arrive for the current request.
     *
     * [LocationSource] tries again when the settings change, or when it is collected again after
     * its upstream stopped.
     *
     * @property reason why the updates could not be started.
     */
    public data class Error(public val reason: LocationError) : LocationEvent
}

/** Why location updates could not be started. Map it to a user-facing message in the UI. */
public enum class LocationError {

    /** The app does not hold the location permission, e.g. the user revoked it. */
    PERMISSION_DENIED,

    /** The device has no GPS provider. */
    PROVIDER_UNAVAILABLE,

    /** Any other platform failure. Details are in the logs. */
    UNKNOWN,
}
