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
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/** Decimals of the partial: hundredths of a kilometer (10 m), as in the roadbook. */
internal const val PARTIAL_DECIMALS: Int = 2

/** Whole-kilometer digits of the partial: it shows at most 999.99 km. */
internal const val PARTIAL_INTEGER_DIGITS: Int = 3

/** Decimals of the total: tenths of a kilometer (100 m), as in the previous app. */
internal const val TOTAL_DECIMALS: Int = 1

/** Whole-kilometer digits of the total: it shows at most 9,999.9 km. */
internal const val TOTAL_INTEGER_DIGITS: Int = 4

/**
 * This distance in kilometers with [decimals] decimals and at most [integerDigits] whole digits,
 * in the number format of [locale], with its decimal and thousands separators (e.g. `1,234.5` in
 * English, `1.234,5` in Spanish) and without unit.
 *
 * Halves are rounded up. The meters are converted as a decimal, so a value such as 1,255 m is not
 * first turned into 1.2549999… km by binary floating point.
 *
 * A distance above the largest value that fits is capped to it (e.g. `999.99` for 3 whole digits
 * and 2 decimals), so the text never gets wider than [maxKilometersText]. The cap is applied after
 * rounding, so 999.996 km shows `999.99`, not `1000.00`.
 *
 * @param decimals how many decimals to show.
 * @param integerDigits how many whole-kilometer digits there may be at most.
 * @param locale the locale of the user.
 * @return the formatted distance.
 */
internal fun Distance.formatKilometers(decimals: Int, integerDigits: Int, locale: Locale): String {
    val kilometers = BigDecimal.valueOf(inMeters).movePointLeft(3)
        .setScale(decimals, RoundingMode.HALF_UP)
    return kilometers.min(maxKilometers(decimals, integerDigits)).format(decimals, locale)
}

/**
 * The largest distance [Distance.formatKilometers] shows for these [decimals] and
 * [integerDigits], formatted the same way (e.g. `999.99`). With tabular digits it is the widest
 * text the distance can have, so it is used to size the text.
 *
 * @param decimals how many decimals to show.
 * @param integerDigits how many whole-kilometer digits there may be at most.
 * @param locale the locale of the user.
 * @return the formatted largest distance.
 */
internal fun maxKilometersText(decimals: Int, integerDigits: Int, locale: Locale): String =
    maxKilometers(decimals, integerDigits).format(decimals, locale)

/** The largest value with [integerDigits] whole digits and [decimals] decimals: 10^d − 10^−dec. */
private fun maxKilometers(decimals: Int, integerDigits: Int): BigDecimal =
    BigDecimal.TEN.pow(integerDigits) - BigDecimal.ONE.movePointLeft(decimals)

/** This number with exactly [decimals] decimals, in the format of [locale], with its separators. */
private fun BigDecimal.format(decimals: Int, locale: Locale): String =
    NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
        roundingMode = RoundingMode.HALF_UP
        isGroupingUsed = true
    }.format(this)
