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

package dev.giste.core.ui.theme.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.luminance
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
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

        assertTrue("Pairs too close to their error color: $failures", failures.isEmpty())
    }

    @Test
    fun darkBeerColorScheme_brandColors_areDistinctFromErrorColors() {
        val failures = DarkBeerColorScheme.errorLookalikes()

        assertTrue("Pairs too close to their error color: $failures", failures.isEmpty())
    }

    @Test
    fun deltaE2000_referencePairs_matchPublishedValues() {
        // Test data of Sharma, Wu and Dalal (2005), "The CIEDE2000 color-difference formula:
        // implementation notes, supplementary test data, and mathematical observations", pairs 1,
        // 7, 17 and 25.
        val pairs = listOf(
            Triple(Lab(50.0, 2.6772, -79.7751), Lab(50.0, 0.0, -82.7485), 2.0425),
            Triple(Lab(50.0, 0.0, 0.0), Lab(50.0, -1.0, 2.0), 2.3669),
            Triple(Lab(50.0, 2.5, 0.0), Lab(73.0, 25.0, -18.0), 27.1492),
            Triple(Lab(60.2574, -34.0099, 36.2677), Lab(60.4626, -34.1751, 39.4387), 1.2644),
        )

        pairs.forEach { (first, second, expected) ->
            assertEquals(expected, deltaE2000(first, second), 0.0001)
        }
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
     * Returns every brand/error pair whose CIEDE2000 distance is below its minimum, i.e. where a
     * brand color could be mistaken for an error at a glance. Vivid roles need
     * [MIN_ERROR_DISTANCE]; the pale containers need [MIN_CONTAINER_ERROR_DISTANCE].
     */
    private fun ColorScheme.errorLookalikes(): List<String> = listOf(
        Triple("primary/error", primary to error, MIN_ERROR_DISTANCE),
        Triple("secondary/error", secondary to error, MIN_ERROR_DISTANCE),
        Triple("tertiary/error", tertiary to error, MIN_ERROR_DISTANCE),
        Triple(
            "primaryContainer/errorContainer",
            primaryContainer to errorContainer,
            MIN_CONTAINER_ERROR_DISTANCE,
        ),
        Triple(
            "secondaryContainer/errorContainer",
            secondaryContainer to errorContainer,
            MIN_CONTAINER_ERROR_DISTANCE,
        ),
        Triple(
            "tertiaryContainer/errorContainer",
            tertiaryContainer to errorContainer,
            MIN_CONTAINER_ERROR_DISTANCE,
        ),
    ).mapNotNull { (name, pair, minimum) ->
        val distance = deltaE2000(pair.first.toLab(), pair.second.toLab())
        if (distance < minimum) "$name=%.1f (min %.0f)".format(distance, minimum) else null
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

    /** A color in CIELAB: lightness [l] (0–100) and the opponent axes [a] and [b]. */
    private data class Lab(val l: Double, val a: Double, val b: Double)

    /** This color in Compose's CIELAB space (D50 white point, Bradford adaptation). */
    private fun Color.toLab(): Lab {
        val lab = convert(ColorSpaces.CieLab)
        return Lab(lab.red.toDouble(), lab.green.toDouble(), lab.blue.toDouble())
    }

    /**
     * CIEDE2000 color difference (CIE 142-2001), with the reference conditions kL = kC = kH = 1.
     *
     * Unlike the plain CIELAB distance (CIE76), it scales chroma and hue differences down for
     * saturated colors, as the eye does: two vivid colors a few degrees of hue apart look alike
     * even though they are far apart in CIELAB. About 1 is just noticeable; below about 10 two
     * colors read as the same color family at a glance.
     */
    private fun deltaE2000(first: Lab, second: Lab): Double {
        val chromaMean = (hypot(first.a, first.b) + hypot(second.a, second.b)) / 2
        // Stretches the a axis of near-neutral colors, where CIELAB hue is least uniform.
        val g = 0.5 * (1 - sqrt(chromaMean.pow(7) / (chromaMean.pow(7) + 25.0.pow(7))))
        val a1 = first.a * (1 + g)
        val a2 = second.a * (1 + g)
        val c1 = hypot(a1, first.b)
        val c2 = hypot(a2, second.b)
        val h1 = hueDegrees(first.b, a1)
        val h2 = hueDegrees(second.b, a2)

        val deltaL = second.l - first.l
        val deltaC = c2 - c1
        val hueStep = when {
            c1 * c2 == 0.0 -> 0.0
            abs(h2 - h1) <= 180 -> h2 - h1
            h2 - h1 > 180 -> h2 - h1 - 360
            else -> h2 - h1 + 360
        }
        val deltaH = 2 * sqrt(c1 * c2) * sin(Math.toRadians(hueStep / 2))

        val lMean = (first.l + second.l) / 2
        val cMean = (c1 + c2) / 2
        val hMean = when {
            c1 * c2 == 0.0 -> h1 + h2
            abs(h1 - h2) <= 180 -> (h1 + h2) / 2
            h1 + h2 < 360 -> (h1 + h2 + 360) / 2
            else -> (h1 + h2 - 360) / 2
        }
        val t = 1 - 0.17 * cos(Math.toRadians(hMean - 30)) +
            0.24 * cos(Math.toRadians(2 * hMean)) +
            0.32 * cos(Math.toRadians(3 * hMean + 6)) -
            0.20 * cos(Math.toRadians(4 * hMean - 63))
        val lightnessWeight = 1 + 0.015 * (lMean - 50).pow(2) / sqrt(20 + (lMean - 50).pow(2))
        val chromaWeight = 1 + 0.045 * cMean
        val hueWeight = 1 + 0.015 * cMean * t
        // Rotates the chroma/hue ellipses in the blue region, where CIELAB is least uniform.
        val rotation = -2 * sqrt(cMean.pow(7) / (cMean.pow(7) + 25.0.pow(7))) *
            sin(Math.toRadians(60 * exp(-((hMean - 275) / 25).pow(2))))

        val l = deltaL / lightnessWeight
        val c = deltaC / chromaWeight
        val h = deltaH / hueWeight
        return sqrt(l * l + c * c + h * h + rotation * c * h)
    }

    /** The hue angle of ([a], [b]) in degrees, from 0 to 360. */
    private fun hueDegrees(b: Double, a: Double): Double {
        if (a == 0.0 && b == 0.0) return 0.0
        val degrees = Math.toDegrees(atan2(b, a))
        return if (degrees < 0) degrees + 360 else degrees
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
         * Minimum CIEDE2000 distance between a vivid brand color (primary, secondary, tertiary)
         * and the error color: clearly a different color at a glance.
         */
        const val MIN_ERROR_DISTANCE = 15.0

        /**
         * Minimum CIEDE2000 distance between a brand container and the error container. Pale
         * colors are inherently close to each other, so this is lower than for vivid roles, but
         * still well above the ~1 noticeable difference.
         */
        const val MIN_CONTAINER_ERROR_DISTANCE = 10.0

        /** Roles that keep the Material 3 value on purpose. */
        val INTENTIONAL_DEFAULTS =
            setOf("error", "onError", "errorContainer", "onErrorContainer", "scrim")
    }
}
