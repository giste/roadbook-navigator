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

package dev.giste.roadbooknavigator.core.ui.theme.presentation

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Palette based on beer colours, built from four seeds:
//
// - Primary: beer gold (#F28E1C), nudged towards amber (#FFB300) so tone 80 reads as gold, not
//   peach.
// - Secondary: hops green (#558B2F).
// - Tertiary: roasted malt (#8D6E63), shifted to a coffee-brown hue with a little more chroma so
//   it stays clearly distinct from the error reds, which share its reddish hue.
// - Neutrals: warm foam cream (light) and stout (dark), with a slight gold tint.
// - Error: the Material 3 default reds, so warnings never blend with the beer palette.
//
// Each seed gives a tonal palette (CIELAB lightness = Material "tone", seed hue and chroma kept
// within the sRGB gamut), and every role uses the Material 3 baseline tone for it: e.g. primary
// is tone 40 in light and tone 80 in dark. Every role is set, so no default purple-tinted colour
// leaks in. Light surfaces show the bright gold through primaryContainer (tone 80) because white
// text on the bright gold itself would be unreadable.
//
// BeerColorSchemeTest checks that every content colour reaches WCAG AA contrast (4.5:1) on its
// background, that every brand colour stays clearly distinct from its error counterpart, and that
// no role is left at its Material default.

/** Light scheme: dark gold, hops and malt on foam cream. */
internal val LightBeerColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF815500),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFB950),
    onPrimaryContainer = Color(0xFF271900),
    inversePrimary = Color(0xFFFFB950),
    secondary = Color(0xFF346B0D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFBBF28F),
    onSecondaryContainer = Color(0xFF131F00),
    tertiary = Color(0xFF725A3C),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFDDDBA),
    onTertiaryContainer = Color(0xFF271900),
    background = Color(0xFFFFF8F3),
    onBackground = Color(0xFF1F1B16),
    surface = Color(0xFFFFF8F3),
    onSurface = Color(0xFF1F1B16),
    surfaceVariant = Color(0xFFEFE0D2),
    onSurfaceVariant = Color(0xFF50453A),
    surfaceTint = Color(0xFF815500),
    inverseSurface = Color(0xFF352F2B),
    inverseOnSurface = Color(0xFFF7EFE9),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF827569),
    outlineVariant = Color(0xFFD3C4B7),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFF8F3),
    surfaceDim = Color(0xFFE0D9D3),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAF2EC),
    surfaceContainer = Color(0xFFF4EDE7),
    surfaceContainerHigh = Color(0xFFEEE7E1),
    surfaceContainerHighest = Color(0xFFE8E1DB),
)

/** Dark scheme: beer gold, hops and malt on stout. */
internal val DarkBeerColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFFFB950),
    onPrimary = Color(0xFF442C00),
    primaryContainer = Color(0xFF624000),
    onPrimaryContainer = Color(0xFFFFDDB5),
    inversePrimary = Color(0xFF815500),
    secondary = Color(0xFF9FD675),
    onSecondary = Color(0xFF183800),
    secondaryContainer = Color(0xFF225100),
    onSecondaryContainer = Color(0xFFBBF28F),
    tertiary = Color(0xFFE0C19F),
    onTertiary = Color(0xFF402D11),
    tertiaryContainer = Color(0xFF594326),
    onTertiaryContainer = Color(0xFFFDDDBA),
    background = Color(0xFF18120D),
    onBackground = Color(0xFFE8E1DB),
    surface = Color(0xFF18120D),
    onSurface = Color(0xFFE8E1DB),
    surfaceVariant = Color(0xFF50453A),
    onSurfaceVariant = Color(0xFFD3C4B7),
    surfaceTint = Color(0xFFFFB950),
    inverseSurface = Color(0xFFE8E1DB),
    inverseOnSurface = Color(0xFF352F2B),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF9C8E82),
    outlineVariant = Color(0xFF50453A),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF3E3834),
    surfaceDim = Color(0xFF18120D),
    surfaceContainerLowest = Color(0xFF140D05),
    surfaceContainerLow = Color(0xFF1F1B16),
    surfaceContainer = Color(0xFF231F1A),
    surfaceContainerHigh = Color(0xFF2E2925),
    surfaceContainerHighest = Color(0xFF39342F),
)

/**
 * Selects the beer color scheme for the requested mode.
 *
 * @param darkTheme `true` for the dark (stout) scheme, `false` for the light (foam) one.
 * @return [DarkBeerColorScheme] or [LightBeerColorScheme].
 */
internal fun beerColorScheme(darkTheme: Boolean): ColorScheme =
    if (darkTheme) DarkBeerColorScheme else LightBeerColorScheme
