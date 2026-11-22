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
 * Both `.` and `,` are accepted as the decimal separator, whatever the locale; the numpad always
 * builds text with [PARTIAL_SEPARATOR]. Thousands separators are not accepted: a partial
 * has at most [PARTIAL_INTEGER_DIGITS] whole digits, so `1.234` could only be a mistake. Leading
 * and trailing spaces are ignored. Only values the dashboard can show exactly are valid, so the
 * partial the rider sets is the partial they see: at most [PARTIAL_INTEGER_DIGITS] whole digits
 * (leading zeros do not count) and at most [PARTIAL_DECIMALS] decimals.
 *
 * @param text what the rider typed, e.g. with [pressNumpadKey].
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

/** The decimal separator of the text the numpad builds, whatever the locale shows. */
internal const val PARTIAL_SEPARATOR: Char = '.'

/** A key of the set-partial numpad. */
internal sealed interface NumpadKey {

    /**
     * A digit key.
     *
     * @property digit the digit, from 0 to 9.
     */
    data class Digit(val digit: Int) : NumpadKey {
        init {
            require(digit in 0..9) { "A numpad digit must be from 0 to 9, but was $digit" }
        }
    }

    /** The decimal separator key. */
    data object Separator : NumpadKey

    /** The key that deletes the last character. */
    data object Delete : NumpadKey
}

/**
 * The text after pressing [key] on the numpad, with [PARTIAL_SEPARATOR] as the separator.
 *
 * The numpad never builds text that [parsePartialInput] rejects, so it needs no error messages: a
 * key that would make the partial invalid is ignored, and the text is returned unchanged.
 *
 * - A digit is appended, unless the partial would get more whole digits or decimals than the
 *   dashboard shows. A lone `0` is replaced, so the text never starts with a useless zero.
 * - The separator is appended once. On empty text it gives `0.`, so the text always starts with a
 *   digit.
 * - Delete removes the last character, if any.
 *
 * @param text the text so far, built by this function (empty at first).
 * @param key the pressed key.
 * @return the new text, or [text] itself when the key is ignored.
 */
internal fun pressNumpadKey(text: String, key: NumpadKey): String = when (key) {
    is NumpadKey.Digit -> {
        val candidate = if (text == "0") "${key.digit}" else "$text${key.digit}"
        if (parsePartialInput(candidate) is PartialInput.Valid) candidate else text
    }
    NumpadKey.Separator -> when {
        PARTIAL_SEPARATOR in text -> text
        text.isEmpty() -> "0$PARTIAL_SEPARATOR"
        else -> "$text$PARTIAL_SEPARATOR"
    }
    NumpadKey.Delete -> text.dropLast(1)
}
