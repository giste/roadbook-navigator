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
import dev.giste.geo.Speed

/**
 * How the odometer filters location fixes before counting the distance between them.
 *
 * GPS fixes are noisy: their position wanders even when the device does not move. These limits
 * keep that noise out of the measured distance.
 *
 * Any value is valid: [Speed] and [Distance] already rule out negative and non-finite values.
 * Which values a user may choose is up to each app, e.g. the range of a settings slider.
 *
 * @property minSpeed fixes slower than this are treated as stopped and not counted, so GPS
 * jitter does not add distance while the vehicle stands still. Fixes without a known speed are
 * always counted. [Speed.ZERO] counts every fix. Defaults to [DEFAULT_MIN_SPEED].
 * @property maxHorizontalAccuracy fixes whose horizontal accuracy is worse (larger) than this, or
 * unknown, are ignored. Real fixes are never exactly accurate, so a value close to zero ignores
 * nearly all of them. Defaults to [DEFAULT_MAX_HORIZONTAL_ACCURACY].
 * @property minStepDistance a fix closer than this to the anchor (the last counted fix) is not
 * counted yet, and the anchor stays where it is. Consecutive fixes are a few meters apart from
 * GPS noise alone, and noise always makes a path longer, so counting every tiny step overcounts,
 * above all at walking speed. Short steps are not lost: they are counted in one piece once the
 * device is [minStepDistance] away from the anchor. [Distance.ZERO] counts every step. Defaults
 * to [DEFAULT_MIN_STEP_DISTANCE].
 * @property useAltitude whether the altitude change between two fixes is added to the distance.
 * GPS altitude is noisier than position, and on a 10% slope the climb only adds 0.5% to the
 * distance, so it usually adds more error than it removes. Turn it on for altitude sources
 * better than GPS, e.g. a barometer. Defaults to [DEFAULT_USE_ALTITUDE].
 * @property maxVerticalAccuracy when [useAltitude] is on, the altitude change is only added when
 * both fixes have this vertical accuracy or better. Otherwise only the horizontal distance is
 * counted. It has no effect when [useAltitude] is off. Defaults to
 * [DEFAULT_MAX_VERTICAL_ACCURACY].
 */
public data class OdometerSettings(
    public val minSpeed: Speed = DEFAULT_MIN_SPEED,
    public val maxHorizontalAccuracy: Distance = DEFAULT_MAX_HORIZONTAL_ACCURACY,
    public val minStepDistance: Distance = DEFAULT_MIN_STEP_DISTANCE,
    public val useAltitude: Boolean = DEFAULT_USE_ALTITUDE,
    public val maxVerticalAccuracy: Distance = DEFAULT_MAX_VERTICAL_ACCURACY,
) {

    /** Default values of [OdometerSettings]. */
    public companion object {

        /** Default [minSpeed]: 0.5 m/s (1.8 km/h), slower than walking. */
        public val DEFAULT_MIN_SPEED: Speed = Speed.metersPerSecond(0.5)

        /** Default [maxHorizontalAccuracy]: 20 m. */
        public val DEFAULT_MAX_HORIZONTAL_ACCURACY: Distance = Distance.meters(20.0)

        /** Default [minStepDistance]: 5 m, about the noise between two good GPS fixes. */
        public val DEFAULT_MIN_STEP_DISTANCE: Distance = Distance.meters(5.0)

        /** Default [useAltitude]: off, because GPS altitude is too noisy to help. */
        public const val DEFAULT_USE_ALTITUDE: Boolean = false

        /** Default [maxVerticalAccuracy]: 3 m, so only very good altitudes are used. */
        public val DEFAULT_MAX_VERTICAL_ACCURACY: Distance = Distance.meters(3.0)
    }
}
