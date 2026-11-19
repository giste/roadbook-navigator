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

package dev.giste.roadbooknavigator.dashboard

import dev.giste.geo.Distance
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceFormatTest {

    private val spanish = Locale.forLanguageTag("es-ES")

    @Test
    fun formatKilometers_partialInEnglish_hasTwoDecimalsWithAPoint() {
        assertEquals("12.35", Distance.meters(12_345.0).formatKilometers(PARTIAL_DECIMALS, Locale.US))
    }

    @Test
    fun formatKilometers_totalInSpanish_hasOneDecimalWithAComma() {
        assertEquals("234,6", Distance.meters(234_567.0).formatKilometers(TOTAL_DECIMALS, spanish))
    }

    @Test
    fun formatKilometers_half_roundsUp() {
        // 1.255 is not exact in binary floating point: it must still round up.
        assertEquals("1.26", Distance.meters(1_255.0).formatKilometers(2, Locale.US))
    }

    @Test
    fun formatKilometers_zero_keepsTheDecimals() {
        assertEquals("0.00", Distance.ZERO.formatKilometers(2, Locale.US))
    }

    @Test
    fun formatKilometers_thousandsOfKilometers_hasNoGrouping() {
        assertEquals("1234,6", Distance.meters(1_234_567.0).formatKilometers(1, spanish))
    }
}
