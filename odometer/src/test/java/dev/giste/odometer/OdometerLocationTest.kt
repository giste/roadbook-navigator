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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OdometerLocationTest {

    @Test
    fun constructor_finiteAltitude_isKept() {
        val fix = testFix(altitudeInMeters = -12.5)

        assertEquals(-12.5, fix.altitudeInMeters!!, 0.0)
    }

    @Test
    fun constructor_unknownAltitude_isAccepted() {
        val fix = testFix(altitudeInMeters = null)

        assertNull(fix.altitudeInMeters)
    }

    @Test(expected = IllegalArgumentException::class)
    fun constructor_nanAltitude_throws() {
        testFix(altitudeInMeters = Double.NaN)
    }

    @Test(expected = IllegalArgumentException::class)
    fun constructor_infiniteAltitude_throws() {
        testFix(altitudeInMeters = Double.POSITIVE_INFINITY)
    }
}
