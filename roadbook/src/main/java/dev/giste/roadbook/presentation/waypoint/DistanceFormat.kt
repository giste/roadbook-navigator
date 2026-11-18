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
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/** Decimals of the distances shown in the roadbook: hundredths of a kilometer, 10 m. */
private const val KILOMETER_DECIMALS: Int = 2

/**
 * This distance in kilometers with two decimals, as roadbooks show it, in the number format of
 * [locale] (e.g. `1.25` in English, `1,25` in Spanish). It has no unit and no grouping, so it fits
 * the narrow distance boxes.
 *
 * Halves are rounded up (1,255 m is `1.26`). The meters are converted to kilometers as a decimal,
 * so a value such as 1.255 is not first turned into 1.2549999… by binary floating point.
 *
 * @param locale the locale of the user.
 * @return the formatted distance.
 */
internal fun Distance.formatKilometers(locale: Locale): String {
    val kilometers = BigDecimal.valueOf(inMeters).movePointLeft(3)
    return NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = KILOMETER_DECIMALS
        maximumFractionDigits = KILOMETER_DECIMALS
        roundingMode = RoundingMode.HALF_UP
        isGroupingUsed = false
    }.format(kilometers)
}
