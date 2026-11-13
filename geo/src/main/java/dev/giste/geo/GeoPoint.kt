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

/**
 * A position on the Earth's surface, as WGS84 latitude and longitude in decimal degrees (the
 * system used by GPS).
 *
 * It has no altitude. Both coordinates are validated on creation, so every instance is a real
 * position.
 *
 * @property latitude degrees north of the equator, in `[-90, 90]`. South is negative.
 * @property longitude degrees east of the Greenwich meridian, in `[-180, 180]`. West is
 * negative. `-180` and `180` are the same meridian but are kept as given.
 * @throws IllegalArgumentException if a coordinate is out of range or `NaN`.
 */
public data class GeoPoint(
    public val latitude: Double,
    public val longitude: Double,
) {
    init {
        // NaN fails both range checks, because every comparison with NaN is false.
        require(latitude in MIN_LATITUDE..MAX_LATITUDE) {
            "Latitude must be in [$MIN_LATITUDE, $MAX_LATITUDE], but was $latitude"
        }
        require(longitude in MIN_LONGITUDE..MAX_LONGITUDE) {
            "Longitude must be in [$MIN_LONGITUDE, $MAX_LONGITUDE], but was $longitude"
        }
    }

    private companion object {
        const val MIN_LATITUDE: Double = -90.0
        const val MAX_LATITUDE: Double = 90.0
        const val MIN_LONGITUDE: Double = -180.0
        const val MAX_LONGITUDE: Double = 180.0
    }
}
