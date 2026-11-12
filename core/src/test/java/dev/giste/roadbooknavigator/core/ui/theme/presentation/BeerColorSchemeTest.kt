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
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class BeerColorSchemeTest {

    @Test
    fun beerColorScheme_darkTheme_returnsDarkScheme() {
        val scheme = beerColorScheme(darkTheme = true)

        assertSame(DarkBeerColorScheme, scheme)
    }

    @Test
    fun beerColorScheme_lightTheme_returnsLightScheme() {
        val scheme = beerColorScheme(darkTheme = false)

        assertSame(LightBeerColorScheme, scheme)
    }

    @Test
    fun lightBeerColorScheme_contentColors_meetWcagAaContrast() {
        val failures = LightBeerColorScheme.contrastFailures()

        assertTrue("Pairs below $MIN_CONTRAST:1: $failures", failures.isEmpty())
    }

    @Test
    fun darkBeerColorScheme_contentColors_meetWcagAaContrast() {
        val failures = DarkBeerColorScheme.contrastFailures()

        assertTrue("Pairs below $MIN_CONTRAST:1: $failures", failures.isEmpty())
    }

    @Test
    fun lightBeerColorScheme_brandColors_areDistinctFromErrorColors() {
        val failures = LightBeerColorScheme.errorLookalikes()

        assertTrue("Pairs closer than ΔE $MIN_ERROR_DISTANCE: $failures", failures.isEmpty())
    }

    @Test
    fun darkBeerColorScheme_brandColors_areDistinctFromErrorColors() {
        val failures = DarkBeerColorScheme.errorLookalikes()

        assertTrue("Pairs closer than ΔE $MIN_ERROR_DISTANCE: $failures", failures.isEmpty())
    }

    @Test
    fun lightBeerColorScheme_everyBrandRole_overridesMaterialDefault() {
        val leaks = LightBeerColorScheme.rolesLeftAtDefault(lightColorScheme())

        assertTrue("Roles left at the Material default: $leaks", leaks.isEmpty())
    }

    @Test
    fun darkBeerColorScheme_everyBrandRole_overridesMaterialDefault() {
        val leaks = DarkBeerColorScheme.rolesLeftAtDefault(darkColorScheme())

        assertTrue("Roles left at the Material default: $leaks", leaks.isEmpty())
    }

    /**
     * Returns every brand/error pair whose color distance is below [MIN_ERROR_DISTANCE], i.e.
     * where a brand color could be mistaken for an error at a glance.
     */
    private fun ColorScheme.errorLookalikes(): List<String> = listOf(
        "primary/error" to (primary to error),
        "secondary/error" to (secondary to error),
        "tertiary/error" to (tertiary to error),
        "primaryContainer/errorContainer" to (primaryContainer to errorContainer),
        "secondaryContainer/errorContainer" to (secondaryContainer to errorContainer),
        "tertiaryContainer/errorContainer" to (tertiaryContainer to errorContainer),
    ).mapNotNull { (name, pair) ->
        val distance = labDistance(pair.first, pair.second)
        if (distance < MIN_ERROR_DISTANCE) "$name=%.1f".format(distance) else null
    }

    /**
     * Returns the roles that still hold the Material baseline value of [defaults]. Error roles and
     * the scrim keep the Material values on purpose, and pure white or black can legitimately
     * match, so they are excluded.
     */
    private fun ColorScheme.rolesLeftAtDefault(defaults: ColorScheme): List<String> {
        val defaultRoles = defaults.roles()
        return roles().filter { (name, color) ->
            name !in INTENTIONAL_DEFAULTS &&
                color != Color.White &&
                color != Color.Black &&
                color == defaultRoles.getValue(name)
        }.keys.toList()
    }

    /** Every color role of the scheme, by name. */
    private fun ColorScheme.roles(): Map<String, Color> = mapOf(
        "primary" to primary,
        "onPrimary" to onPrimary,
        "primaryContainer" to primaryContainer,
        "onPrimaryContainer" to onPrimaryContainer,
        "inversePrimary" to inversePrimary,
        "secondary" to secondary,
        "onSecondary" to onSecondary,
        "secondaryContainer" to secondaryContainer,
        "onSecondaryContainer" to onSecondaryContainer,
        "tertiary" to tertiary,
        "onTertiary" to onTertiary,
        "tertiaryContainer" to tertiaryContainer,
        "onTertiaryContainer" to onTertiaryContainer,
        "background" to background,
        "onBackground" to onBackground,
        "surface" to surface,
        "onSurface" to onSurface,
        "surfaceVariant" to surfaceVariant,
        "onSurfaceVariant" to onSurfaceVariant,
        "surfaceTint" to surfaceTint,
        "inverseSurface" to inverseSurface,
        "inverseOnSurface" to inverseOnSurface,
        "error" to error,
        "onError" to onError,
        "errorContainer" to errorContainer,
        "onErrorContainer" to onErrorContainer,
        "outline" to outline,
        "outlineVariant" to outlineVariant,
        "scrim" to scrim,
        "surfaceBright" to surfaceBright,
        "surfaceDim" to surfaceDim,
        "surfaceContainerLowest" to surfaceContainerLowest,
        "surfaceContainerLow" to surfaceContainerLow,
        "surfaceContainer" to surfaceContainer,
        "surfaceContainerHigh" to surfaceContainerHigh,
        "surfaceContainerHighest" to surfaceContainerHighest,
    )

    /** CIE76 color difference: Euclidean distance in CIELAB. About 2.3 is just noticeable. */
    private fun labDistance(a: Color, b: Color): Double {
        val labA = a.convert(ColorSpaces.CieLab)
        val labB = b.convert(ColorSpaces.CieLab)
        val dl = (labA.red - labB.red).toDouble()
        val da = (labA.green - labB.green).toDouble()
        val db = (labA.blue - labB.blue).toDouble()
        return sqrt(dl * dl + da * da + db * db)
    }

    /** Returns a description of every text/background pair below [MIN_CONTRAST]. */
    private fun ColorScheme.contrastFailures(): List<String> = listOf(
        "onPrimary/primary" to (onPrimary to primary),
        "onPrimaryContainer/primaryContainer" to (onPrimaryContainer to primaryContainer),
        "onSecondary/secondary" to (onSecondary to secondary),
        "onSecondaryContainer/secondaryContainer" to (onSecondaryContainer to secondaryContainer),
        "onTertiary/tertiary" to (onTertiary to tertiary),
        "onTertiaryContainer/tertiaryContainer" to (onTertiaryContainer to tertiaryContainer),
        "onError/error" to (onError to error),
        "onErrorContainer/errorContainer" to (onErrorContainer to errorContainer),
        "onBackground/background" to (onBackground to background),
        "onSurface/surface" to (onSurface to surface),
        "onSurfaceVariant/surfaceVariant" to (onSurfaceVariant to surfaceVariant),
        "inverseOnSurface/inverseSurface" to (inverseOnSurface to inverseSurface),
        "inversePrimary/inverseSurface" to (inversePrimary to inverseSurface),
        "primary/surface" to (primary to surface),
        "onSurface/surfaceDim" to (onSurface to surfaceDim),
        "onSurface/surfaceBright" to (onSurface to surfaceBright),
        "onSurface/surfaceContainerLowest" to (onSurface to surfaceContainerLowest),
        "onSurface/surfaceContainerLow" to (onSurface to surfaceContainerLow),
        "onSurface/surfaceContainer" to (onSurface to surfaceContainer),
        "onSurface/surfaceContainerHigh" to (onSurface to surfaceContainerHigh),
        "onSurface/surfaceContainerHighest" to (onSurface to surfaceContainerHighest),
        "onSurfaceVariant/surfaceContainerHighest" to
            (onSurfaceVariant to surfaceContainerHighest),
    ).mapNotNull { (name, pair) ->
        val ratio = contrastRatio(pair.first, pair.second)
        if (ratio < MIN_CONTRAST) "$name=%.2f".format(ratio) else null
    }

    /** WCAG 2 contrast ratio: (lighter + 0.05) / (darker + 0.05), from 1 to 21. */
    private fun contrastRatio(a: Color, b: Color): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    private companion object {
        /** WCAG AA minimum for normal-size text. */
        const val MIN_CONTRAST = 4.5

        /**
         * Minimum CIE76 distance between a brand color and its error counterpart. The pale
         * containers are inherently close to each other, so this is lower than for vivid roles,
         * but still far above the ~2.3 noticeable difference.
         */
        const val MIN_ERROR_DISTANCE = 15.0

        /** Roles that keep the Material 3 value on purpose. */
        val INTENTIONAL_DEFAULTS =
            setOf("error", "onError", "errorContainer", "onErrorContainer", "scrim")
    }
}
