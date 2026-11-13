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

import dev.giste.geo.Distance
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LocationSettingsTest {

    @Test
    fun constructor_noArguments_usesDefaults() {
        val settings = LocationSettings()

        assertEquals(500.milliseconds, settings.updateInterval)
        assertEquals(Distance.meters(2.0), settings.minUpdateDistance)
    }

    @Test
    fun constructor_positiveInterval_keepsValues() {
        val settings = LocationSettings(updateInterval = 2.seconds, minUpdateDistance = Distance.ZERO)

        assertEquals(2.seconds, settings.updateInterval)
        assertEquals(Distance.ZERO, settings.minUpdateDistance)
    }

    @Test
    fun constructor_zeroInterval_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            LocationSettings(updateInterval = Duration.ZERO)
        }
    }

    @Test
    fun constructor_negativeInterval_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            LocationSettings(updateInterval = (-1).seconds)
        }
    }

    @Test
    fun constructor_infiniteInterval_throws() {
        assertThrows(IllegalArgumentException::class.java) {
            LocationSettings(updateInterval = Duration.INFINITE)
        }
    }
}
