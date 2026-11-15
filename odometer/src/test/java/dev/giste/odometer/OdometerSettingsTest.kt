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
import org.junit.Assert.assertEquals
import org.junit.Test

class OdometerSettingsTest {

    @Test
    fun constructor_noArguments_usesDefaults() {
        val settings = OdometerSettings()

        assertEquals(Speed.metersPerSecond(0.5), settings.minSpeed)
        assertEquals(Distance.meters(20.0), settings.maxHorizontalAccuracy)
        assertEquals(Distance.meters(5.0), settings.minStepDistance)
        assertEquals(false, settings.useAltitude)
        assertEquals(Distance.meters(3.0), settings.maxVerticalAccuracy)
    }

    @Test
    fun constructor_zeroValues_areAccepted() {
        val settings = OdometerSettings(
            minSpeed = Speed.ZERO,
            maxHorizontalAccuracy = Distance.ZERO,
            minStepDistance = Distance.ZERO,
            maxVerticalAccuracy = Distance.ZERO,
        )

        assertEquals(Speed.ZERO, settings.minSpeed)
        assertEquals(Distance.ZERO, settings.maxHorizontalAccuracy)
        assertEquals(Distance.ZERO, settings.minStepDistance)
        assertEquals(Distance.ZERO, settings.maxVerticalAccuracy)
    }

    @Test
    fun constructor_largeValues_areAccepted() {
        val settings = OdometerSettings(
            minSpeed = Speed.kilometersPerHour(50.0),
            maxHorizontalAccuracy = Distance.kilometers(5.0),
            minStepDistance = Distance.kilometers(1.0),
            maxVerticalAccuracy = Distance.kilometers(5.0),
        )

        assertEquals(Speed.kilometersPerHour(50.0), settings.minSpeed)
        assertEquals(Distance.kilometers(5.0), settings.maxHorizontalAccuracy)
        assertEquals(Distance.kilometers(1.0), settings.minStepDistance)
        assertEquals(Distance.kilometers(5.0), settings.maxVerticalAccuracy)
    }
}
