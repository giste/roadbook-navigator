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
import java.math.BigDecimal

/** A number with optional decimals, after either decimal separator: `12`, `12.3`, `,5`, `12,`. */
private val PARTIAL_INPUT_PATTERN = Regex("""(\d*)(?:[.,](\d*))?""")

/** What the rider typed as the new partial, in kilometers. */
internal sealed interface PartialInput {

    /** Nothing typed yet. Not an error, but there is nothing to set either. */
    data object Empty : PartialInput

    /**
     * Something that is not a partial the dashboard can show.
     *
     * @property reason why it is rejected.
     */
    data class Invalid(val reason: PartialInputError) : PartialInput

    /**
     * A partial that can be set.
     *
     * @property partial the typed distance.
     */
    data class Valid(val partial: Distance) : PartialInput
}

/** Why a typed partial is rejected. */
internal enum class PartialInputError {

    /** Not a number, e.g. letters, a sign, a lone separator or two separators. */
    NOT_A_NUMBER,

    /** More whole kilometers than the partial shows (see [PARTIAL_INTEGER_DIGITS]). */
    TOO_LARGE,

    /** More decimals than the partial shows (see [PARTIAL_DECIMALS]). */
    TOO_MANY_DECIMALS,
}

/**
 * Reads [text] as a partial in kilometers.
 *
 * Both `.` and `,` are accepted as the decimal separator, whatever the locale, because decimal
 * keyboards do not always offer the locale's one. Thousands separators are not accepted: a partial
 * has at most [PARTIAL_INTEGER_DIGITS] whole digits, so `1.234` could only be a mistake. Leading
 * and trailing spaces are ignored. Only values the dashboard can show exactly are valid, so the
 * partial the rider sets is the partial they see: at most [PARTIAL_INTEGER_DIGITS] whole digits
 * (leading zeros do not count) and at most [PARTIAL_DECIMALS] decimals.
 *
 * @param text what the rider typed.
 * @return [PartialInput.Empty] for blank text, otherwise the parsed partial or why it is rejected.
 */
internal fun parsePartialInput(text: String): PartialInput {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return PartialInput.Empty

    val match = PARTIAL_INPUT_PATTERN.matchEntire(trimmed)
    val integerPart = match?.groupValues?.get(1).orEmpty()
    val decimalPart = match?.groupValues?.get(2).orEmpty()
    return when {
        match == null || (integerPart.isEmpty() && decimalPart.isEmpty()) ->
            PartialInput.Invalid(PartialInputError.NOT_A_NUMBER)
        integerPart.trimStart('0').length > PARTIAL_INTEGER_DIGITS ->
            PartialInput.Invalid(PartialInputError.TOO_LARGE)
        decimalPart.length > PARTIAL_DECIMALS ->
            PartialInput.Invalid(PartialInputError.TOO_MANY_DECIMALS)
        else -> {
            // Decimal arithmetic, so 0.29 km is exactly 290 m and not 289.99999… m.
            val kilometers = BigDecimal("${integerPart.ifEmpty { "0" }}.${decimalPart.ifEmpty { "0" }}")
            PartialInput.Valid(Distance.meters(kilometers.movePointRight(3).toDouble()))
        }
    }
}
