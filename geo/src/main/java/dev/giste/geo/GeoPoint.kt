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

import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

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

    /**
     * Returns the shortest distance from this point to [other] along the surface of the WGS84
     * ellipsoid (the geodesic distance), with Vincenty's inverse formula.
     *
     * It is accurate to well under a millimeter. For nearly antipodal points, where Vincenty's
     * formula does not converge, it falls back to the spherical haversine distance, which is
     * within about 0.5 % there. That never happens between nearby points such as consecutive
     * location fixes or waypoints.
     *
     * @param other the point to measure to.
     * @return the distance, the same in both directions. [Distance.ZERO] for the same position.
     */
    public fun distanceTo(other: GeoPoint): Distance =
        Distance.meters(inverseGeodesic(this, other).distanceInMeters)

    /**
     * Returns the direction in which to leave this point to reach [other] along the shortest path
     * on the WGS84 ellipsoid (the initial bearing of the geodesic), with Vincenty's inverse
     * formula.
     *
     * The bearing generally changes along the way, so the bearing from [other] back to this point
     * is not the exact opposite. For nearly antipodal points it falls back to the spherical
     * initial bearing, like [distanceTo]. For exactly antipodal points every direction is a
     * shortest path, so the result is one of them. From a pole, every direction points away from
     * it, so the result depends on the longitude given for the pole.
     *
     * @param other the point to head to.
     * @return the initial bearing, or `null` when both points are the same position, because no
     * direction leads from a point to itself.
     */
    public fun bearingTo(other: GeoPoint): Bearing? =
        inverseGeodesic(this, other).initialBearingInDegrees?.let(Bearing::degrees)

    private companion object {
        const val MIN_LATITUDE: Double = -90.0
        const val MAX_LATITUDE: Double = 90.0
        const val MIN_LONGITUDE: Double = -180.0
        const val MAX_LONGITUDE: Double = 180.0
    }
}

/** WGS84 semi-major axis (equatorial radius) in meters. */
private const val WGS84_SEMI_MAJOR_AXIS_METERS: Double = 6_378_137.0

/** WGS84 flattening. */
private const val WGS84_FLATTENING: Double = 1.0 / 298.257223563

/** WGS84 semi-minor axis (polar radius) in meters, derived from the axis and the flattening. */
private const val WGS84_SEMI_MINOR_AXIS_METERS: Double =
    (1.0 - WGS84_FLATTENING) * WGS84_SEMI_MAJOR_AXIS_METERS

/**
 * Mean Earth radius in meters (IUGG R1, the mean of the WGS84 axes), used by the spherical
 * fallback.
 */
private const val MEAN_EARTH_RADIUS_METERS: Double = 6_371_008.8

/**
 * The iteration on λ stops when it changes by less than this, in radians: about 0.006 mm on the
 * ground.
 */
private const val CONVERGENCE_TOLERANCE_RADIANS: Double = 1e-12

/** Iterations after which Vincenty's formula is considered not to converge. Usual cases take 2 to 5. */
private const val MAX_ITERATIONS: Int = 200

/**
 * The result of solving the inverse geodesic problem between two points.
 *
 * @property distanceInMeters the length of the geodesic. Never negative.
 * @property initialBearingInDegrees the initial bearing, clockwise from north (any finite angle,
 * not yet wrapped), or `null` when both points are the same position.
 */
private class Geodesic(val distanceInMeters: Double, val initialBearingInDegrees: Double?)

/**
 * Solves the inverse geodesic problem from [from] to [to]: Vincenty's formula on the WGS84
 * ellipsoid, or the spherical solution when it does not converge.
 */
private fun inverseGeodesic(from: GeoPoint, to: GeoPoint): Geodesic =
    vincentyInverse(from, to) ?: sphericalInverse(from, to)

/**
 * Vincenty's inverse formula (T. Vincenty, "Direct and inverse solutions of geodesics on the
 * ellipsoid with application of nested equations", Survey Review, 1975).
 *
 * The points are moved to an auxiliary sphere, where the latitudes become "reduced latitudes"
 * (U). There, λ is the difference in longitude, which starts as the difference on the ellipsoid
 * (L) and is refined by iteration until it stops changing. The geodesic length then follows from
 * the angular distance on the auxiliary sphere (σ) with a series in the ellipsoid's eccentricity.
 *
 * @return the geodesic, or `null` when the iteration does not converge. That only happens for
 * nearly antipodal points: λ then grows past π or keeps oscillating.
 */
@Suppress("LocalVariableName") // Single-letter names follow Vincenty's paper.
private fun vincentyInverse(from: GeoPoint, to: GeoPoint): Geodesic? {
    val f = WGS84_FLATTENING
    val a = WGS84_SEMI_MAJOR_AXIS_METERS
    val b = WGS84_SEMI_MINOR_AXIS_METERS

    // Wrap the difference into [-π, π], so crossing the antimeridian (179° to -179°) takes the
    // short way round.
    val L = Math.IEEEremainder(Math.toRadians(to.longitude - from.longitude), 2 * Math.PI)
    val U1 = atan((1 - f) * tan(Math.toRadians(from.latitude)))
    val U2 = atan((1 - f) * tan(Math.toRadians(to.latitude)))
    val sinU1 = sin(U1)
    val cosU1 = cos(U1)
    val sinU2 = sin(U2)
    val cosU2 = cos(U2)

    var lambda = L
    repeat(MAX_ITERATIONS) {
        val sinLambda = sin(lambda)
        val cosLambda = cos(lambda)
        val bearingY = cosU2 * sinLambda
        val bearingX = cosU1 * sinU2 - sinU1 * cosU2 * cosLambda
        val sinSigma = sqrt(bearingY * bearingY + bearingX * bearingX)
        // Only identical positions give exactly zero, and they have no bearing.
        if (sinSigma == 0.0) return Geodesic(distanceInMeters = 0.0, initialBearingInDegrees = null)
        val cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda
        val sigma = atan2(sinSigma, cosSigma)
        val sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma
        val cosSquaredAlpha = 1 - sinAlpha * sinAlpha
        // On the equator cos²α is zero and the term is defined as zero.
        val cos2SigmaM = if (cosSquaredAlpha != 0.0) {
            cosSigma - 2 * sinU1 * sinU2 / cosSquaredAlpha
        } else {
            0.0
        }
        val C = f / 16 * cosSquaredAlpha * (4 + f * (4 - 3 * cosSquaredAlpha))
        val previousLambda = lambda
        lambda = L + (1 - C) * f * sinAlpha *
            (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM)))

        // Past π the iteration is diverging, which happens for nearly antipodal points.
        if (abs(lambda) > Math.PI) return null
        if (abs(lambda - previousLambda) < CONVERGENCE_TOLERANCE_RADIANS) {
            val uSquared = cosSquaredAlpha * (a * a - b * b) / (b * b)
            val A = 1 + uSquared / 16384 * (4096 + uSquared * (-768 + uSquared * (320 - 175 * uSquared)))
            val B = uSquared / 1024 * (256 + uSquared * (-128 + uSquared * (74 - 47 * uSquared)))
            val deltaSigma = B * sinSigma * (
                cos2SigmaM + B / 4 * (
                    cosSigma * (-1 + 2 * cos2SigmaM * cos2SigmaM) -
                        B / 6 * cos2SigmaM * (-3 + 4 * sinSigma * sinSigma) *
                        (-3 + 4 * cos2SigmaM * cos2SigmaM)
                    )
                )
            // The final λ gives the bearing. bearingX/Y above used the previous one.
            val finalSinLambda = sin(lambda)
            val finalCosLambda = cos(lambda)
            val initialBearing = atan2(
                cosU2 * finalSinLambda,
                cosU1 * sinU2 - sinU1 * cosU2 * finalCosLambda,
            )
            return Geodesic(
                // Rounding can leave a tiny negative length for nearly identical positions.
                distanceInMeters = (b * A * (sigma - deltaSigma)).coerceAtLeast(0.0),
                initialBearingInDegrees = Math.toDegrees(initialBearing),
            )
        }
    }
    return null
}

/**
 * The spherical solution: the haversine distance and the initial great-circle bearing on a
 * sphere of [MEAN_EARTH_RADIUS_METERS]. It always has a result, so it backs up
 * [vincentyInverse].
 */
private fun sphericalInverse(from: GeoPoint, to: GeoPoint): Geodesic {
    val fromLatitude = Math.toRadians(from.latitude)
    val toLatitude = Math.toRadians(to.latitude)
    val deltaLatitude = toLatitude - fromLatitude
    val deltaLongitude = Math.toRadians(to.longitude - from.longitude)

    val halfChordSquared = sin(deltaLatitude / 2) * sin(deltaLatitude / 2) +
        cos(fromLatitude) * cos(toLatitude) * sin(deltaLongitude / 2) * sin(deltaLongitude / 2)
    // Rounding can push the value slightly outside [0, 1] for antipodal points, and
    // sqrt(1 - h) would then be NaN.
    val h = halfChordSquared.coerceIn(0.0, 1.0)
    val centralAngle = 2 * atan2(sqrt(h), sqrt(1 - h))

    val initialBearing = atan2(
        sin(deltaLongitude) * cos(toLatitude),
        cos(fromLatitude) * sin(toLatitude) -
            sin(fromLatitude) * cos(toLatitude) * cos(deltaLongitude),
    )
    return Geodesic(
        distanceInMeters = MEAN_EARTH_RADIUS_METERS * centralAngle,
        initialBearingInDegrees = Math.toDegrees(initialBearing),
    )
}
