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

package dev.giste.roadbooknavigator.ui.dashboard

import dev.giste.geo.Distance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PartialInputTest {

    private fun valid(meters: Double): PartialInput = PartialInput.Valid(Distance.meters(meters))

    private fun invalid(reason: PartialInputError): PartialInput = PartialInput.Invalid(reason)

    @Test
    fun parsePartialInput_empty_isEmpty() {
        assertEquals(PartialInput.Empty, parsePartialInput(""))
    }

    @Test
    fun parsePartialInput_onlySpaces_isEmpty() {
        assertEquals(PartialInput.Empty, parsePartialInput("   "))
    }

    @Test
    fun parsePartialInput_wholeKilometers_isValid() {
        assertEquals(valid(12_000.0), parsePartialInput("12"))
    }

    @Test
    fun parsePartialInput_decimalPoint_isValid() {
        assertEquals(valid(12_350.0), parsePartialInput("12.35"))
    }

    @Test
    fun parsePartialInput_decimalComma_isValid() {
        assertEquals(valid(12_350.0), parsePartialInput("12,35"))
    }

    @Test
    fun parsePartialInput_noWholeDigits_isValid() {
        assertEquals(valid(500.0), parsePartialInput(",5"))
    }

    @Test
    fun parsePartialInput_trailingSeparator_isValid() {
        assertEquals(valid(12_000.0), parsePartialInput("12."))
    }

    @Test
    fun parsePartialInput_zero_isValid() {
        assertEquals(valid(0.0), parsePartialInput("0"))
    }

    @Test
    fun parsePartialInput_surroundingSpaces_areIgnored() {
        assertEquals(valid(1_500.0), parsePartialInput(" 1.5 "))
    }

    @Test
    fun parsePartialInput_decimalValue_isExactInMeters() {
        // 0.29 is 0.28999… as a binary Double, so a Double multiplication would give 289.99… m.
        assertEquals(valid(290.0), parsePartialInput("0.29"))
    }

    @Test
    fun parsePartialInput_largestPartial_isValid() {
        assertEquals(valid(999_990.0), parsePartialInput("999.99"))
    }

    @Test
    fun parsePartialInput_leadingZeros_doNotCountAsDigits() {
        assertEquals(valid(999_000.0), parsePartialInput("000999"))
    }

    @Test
    fun parsePartialInput_letters_isNotANumber() {
        assertEquals(invalid(PartialInputError.NOT_A_NUMBER), parsePartialInput("12a"))
    }

    @Test
    fun parsePartialInput_negative_isNotANumber() {
        assertEquals(invalid(PartialInputError.NOT_A_NUMBER), parsePartialInput("-1"))
    }

    @Test
    fun parsePartialInput_onlySeparator_isNotANumber() {
        assertEquals(invalid(PartialInputError.NOT_A_NUMBER), parsePartialInput("."))
    }

    @Test
    fun parsePartialInput_twoSeparators_isNotANumber() {
        assertEquals(invalid(PartialInputError.NOT_A_NUMBER), parsePartialInput("1.2.3"))
    }

    @Test
    fun parsePartialInput_thousandsSeparator_isNotANumber() {
        assertEquals(invalid(PartialInputError.NOT_A_NUMBER), parsePartialInput("1,234.5"))
    }

    @Test
    fun parsePartialInput_innerSpace_isNotANumber() {
        assertEquals(invalid(PartialInputError.NOT_A_NUMBER), parsePartialInput("1 2"))
    }

    @Test
    fun parsePartialInput_moreWholeDigitsThanShown_isTooLarge() {
        assertEquals(invalid(PartialInputError.TOO_LARGE), parsePartialInput("1000"))
    }

    @Test
    fun parsePartialInput_moreDecimalsThanShown_isTooManyDecimals() {
        assertEquals(invalid(PartialInputError.TOO_MANY_DECIMALS), parsePartialInput("1.234"))
    }

    @Test
    fun parsePartialInput_tooLargeAndTooManyDecimals_isTooLarge() {
        assertEquals(invalid(PartialInputError.TOO_LARGE), parsePartialInput("1000.123"))
    }

    private fun press(text: String, key: NumpadKey): String = pressNumpadKey(text, key)

    private fun digit(value: Int): NumpadKey = NumpadKey.Digit(value)

    @Test
    fun pressNumpadKey_digitOnEmpty_appendsIt() {
        assertEquals("7", press("", digit(7)))
    }

    @Test
    fun pressNumpadKey_digitAfterDigits_appendsIt() {
        assertEquals("12", press("1", digit(2)))
    }

    @Test
    fun pressNumpadKey_digitAfterLoneZero_replacesIt() {
        assertEquals("5", press("0", digit(5)))
    }

    @Test
    fun pressNumpadKey_digitAfterZeroAndSeparator_appendsIt() {
        assertEquals("0.5", press("0.", digit(5)))
    }

    @Test
    fun pressNumpadKey_digitBeyondWholeDigits_isIgnored() {
        assertEquals("999", press("999", digit(1)))
    }

    @Test
    fun pressNumpadKey_digitBeyondDecimals_isIgnored() {
        assertEquals("12.35", press("12.35", digit(1)))
    }

    @Test
    fun pressNumpadKey_separatorOnEmpty_startsWithZero() {
        assertEquals("0.", press("", NumpadKey.Separator))
    }

    @Test
    fun pressNumpadKey_separatorAfterDigits_appendsIt() {
        assertEquals("12.", press("12", NumpadKey.Separator))
    }

    @Test
    fun pressNumpadKey_secondSeparator_isIgnored() {
        assertEquals("1.2", press("1.2", NumpadKey.Separator))
    }

    @Test
    fun pressNumpadKey_delete_removesTheLastCharacter() {
        assertEquals("12.", press("12.3", NumpadKey.Delete))
    }

    @Test
    fun pressNumpadKey_deleteOnEmpty_staysEmpty() {
        assertEquals("", press("", NumpadKey.Delete))
    }

    @Test
    fun pressNumpadKey_largestPartialTyped_isValid() {
        val text = listOf(9, 9, 9).fold("") { typed, value -> press(typed, digit(value)) }
            .let { press(it, NumpadKey.Separator) }
            .let { press(it, digit(9)) }
            .let { press(it, digit(9)) }

        assertEquals(PartialInput.Valid(Distance.meters(999_990.0)), parsePartialInput(text))
    }

    @Test
    fun numpadDigit_outOfRange_throws() {
        assertThrows(IllegalArgumentException::class.java) { NumpadKey.Digit(10) }
    }
}
