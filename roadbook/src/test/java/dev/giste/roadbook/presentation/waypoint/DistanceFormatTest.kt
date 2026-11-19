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

package dev.giste.roadbook.presentation.waypoint

import dev.giste.geo.Distance
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceFormatTest {

    @Test
    fun formatKilometers_english_usesDotAndTwoDecimals() {
        assertEquals("1.25", Distance.meters(1_250.0).formatKilometers(Locale.ENGLISH))
    }

    @Test
    fun formatKilometers_spanish_usesComma() {
        val spanish = Locale.forLanguageTag("es-ES")

        assertEquals("1,25", Distance.meters(1_250.0).formatKilometers(spanish))
    }

    @Test
    fun formatKilometers_zero_showsTwoDecimals() {
        assertEquals("0.00", Distance.ZERO.formatKilometers(Locale.ENGLISH))
    }

    @Test
    fun formatKilometers_half_roundsUp() {
        assertEquals("1.26", Distance.meters(1_255.0).formatKilometers(Locale.ENGLISH))
    }

    @Test
    fun formatKilometers_belowHalf_roundsDown() {
        assertEquals("1.25", Distance.meters(1_254.9).formatKilometers(Locale.ENGLISH))
    }

    @Test
    fun formatKilometers_thousandsOfKilometers_haveNoGrouping() {
        assertEquals("1234.57", Distance.meters(1_234_567.0).formatKilometers(Locale.ENGLISH))
    }
}
