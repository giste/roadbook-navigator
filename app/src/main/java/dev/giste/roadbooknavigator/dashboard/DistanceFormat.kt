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
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/** Decimals of the partial: hundredths of a kilometer (10 m), as in the roadbook. */
internal const val PARTIAL_DECIMALS: Int = 2

/** Decimals of the total: tenths of a kilometer (100 m), as in the previous app. */
internal const val TOTAL_DECIMALS: Int = 1

/**
 * This distance in kilometers with [decimals] decimals, in the number format of [locale] (e.g.
 * `1.25` in English, `1,25` in Spanish), without unit or grouping, like the roadbook distances.
 *
 * Halves are rounded up. The meters are converted as a decimal, so a value such as 1,255 m is not
 * first turned into 1.2549999… km by binary floating point.
 *
 * @param decimals how many decimals to show.
 * @param locale the locale of the user.
 * @return the formatted distance.
 */
internal fun Distance.formatKilometers(decimals: Int, locale: Locale): String {
    val kilometers = BigDecimal.valueOf(inMeters).movePointLeft(3)
    return NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
        roundingMode = RoundingMode.HALF_UP
        isGroupingUsed = false
    }.format(kilometers)
}
