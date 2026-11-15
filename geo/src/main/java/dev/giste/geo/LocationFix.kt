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

package dev.giste.geo

import kotlin.time.Duration
import kotlin.time.Instant

/**
 * One location fix of a device, free of platform types.
 *
 * It is the common currency between modules that produce locations (e.g. a GPS provider) and
 * modules that consume them (e.g. an odometer or a map), so neither depends on the other.
 *
 * Only [position], [time] and [elapsedRealtime] are always known. Every other value is `null`
 * when the source did not provide it for this fix, so callers can tell "unknown" apart from a
 * real zero (e.g. a stopped vehicle has a speed of [Speed.ZERO], not `null`).
 *
 * @property position where the device was, as WGS84 latitude and longitude.
 * @property horizontalAccuracy radius of 68% confidence around [position], or `null` if unknown.
 * Smaller is better.
 * @property altitude height above the WGS84 reference ellipsoid, or `null` if unknown. It can be
 * negative.
 * @property verticalAccuracy 68% confidence of [altitude], or `null` if unknown.
 * @property speed ground speed, or `null` if unknown.
 * @property bearing direction of travel, or `null` if unknown. Usually unknown while stopped.
 * @property time wall-clock time of the fix (UTC). It can jump if the device clock changes, so
 * use [elapsedRealtime] to measure intervals between fixes.
 * @property elapsedRealtime time of the fix since the device booted. It is monotonic, so it is
 * the right value to compute the time between two fixes.
 * @throws IllegalArgumentException if [elapsedRealtime] is negative or infinite.
 */
public data class LocationFix(
    public val position: GeoPoint,
    public val horizontalAccuracy: Distance?,
    public val altitude: Altitude?,
    public val verticalAccuracy: Distance?,
    public val speed: Speed?,
    public val bearing: Bearing?,
    public val time: Instant,
    public val elapsedRealtime: Duration,
) {

    init {
        require(elapsedRealtime.isFinite() && !elapsedRealtime.isNegative()) {
            "The elapsed realtime must be finite and non-negative, but was $elapsedRealtime"
        }
    }
}
