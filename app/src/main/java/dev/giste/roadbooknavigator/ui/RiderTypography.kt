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

package dev.giste.roadbooknavigator.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified

/**
 * How much larger the app's text is than the Material 3 defaults. The app is read at arm's length
 * on a tablet mounted on the handlebars, often with vibration, so everyday text needs about
 * 18–20 sp instead of the phone-sized 14–16 sp.
 */
internal const val RIDER_TEXT_SCALE: Float = 1.25f

/**
 * The app's text styles: the Material 3 ones scaled by [RIDER_TEXT_SCALE]. `MainActivity` passes
 * them to `GisteTheme`, so every screen, including those of `:settings` and `:roadbook`, uses them.
 * Sizes stay in `sp`, so the system font size still scales them further.
 */
internal val RiderTypography: Typography = Typography().scaled(RIDER_TEXT_SCALE)

/**
 * This typography with every style's font size and line height multiplied by [factor]. Letter
 * spacing, weights and families are kept.
 *
 * @param factor how much larger the text gets, e.g. 1.25.
 * @return the scaled typography.
 */
internal fun Typography.scaled(factor: Float): Typography = Typography(
    displayLarge = displayLarge.scaled(factor),
    displayMedium = displayMedium.scaled(factor),
    displaySmall = displaySmall.scaled(factor),
    headlineLarge = headlineLarge.scaled(factor),
    headlineMedium = headlineMedium.scaled(factor),
    headlineSmall = headlineSmall.scaled(factor),
    titleLarge = titleLarge.scaled(factor),
    titleMedium = titleMedium.scaled(factor),
    titleSmall = titleSmall.scaled(factor),
    bodyLarge = bodyLarge.scaled(factor),
    bodyMedium = bodyMedium.scaled(factor),
    bodySmall = bodySmall.scaled(factor),
    labelLarge = labelLarge.scaled(factor),
    labelMedium = labelMedium.scaled(factor),
    labelSmall = labelSmall.scaled(factor),
)

/** This style with its font size and line height multiplied by [factor]. */
internal fun TextStyle.scaled(factor: Float): TextStyle =
    copy(fontSize = fontSize.scaled(factor), lineHeight = lineHeight.scaled(factor))

/** This size multiplied by [factor], or unspecified if it is unspecified. */
private fun TextUnit.scaled(factor: Float): TextUnit = if (isSpecified) this * factor else this
