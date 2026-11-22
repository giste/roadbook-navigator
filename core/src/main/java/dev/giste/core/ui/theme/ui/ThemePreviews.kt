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

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import dev.giste.core.ui.theme.Dimensions
import dev.giste.core.ui.theme.GisteTheme
import kotlin.math.max
import kotlin.math.min

// The palette of the theme, to review its colors in Android Studio. Preview-only: the labels are
// sample data, not string resources.

/** WCAG AA minimum contrast for normal-size text. */
private const val MIN_TEXT_CONTRAST: Double = 4.5

/** WCAG minimum contrast for graphics and large text, used for outlines. */
private const val MIN_GRAPHIC_CONTRAST: Double = 3.0

/**
 * One color role to show: its name, its color, the color drawn on it and the contrast it needs.
 *
 * @property name the role's name in [ColorScheme].
 * @property background the role's color.
 * @property content the color drawn on it: its "on" role, or what the role is drawn next to.
 * @property contentName the name of [content], shown with the contrast.
 * @property minContrast the contrast [content] needs on [background].
 */
private data class PaletteRole(
    val name: String,
    val background: Color,
    val content: Color,
    val contentName: String,
    val minContrast: Double = MIN_TEXT_CONTRAST,
)

/** The roles of [this] scheme in groups, with the pairs `BeerColorSchemeTest` checks. */
private fun ColorScheme.paletteGroups(): List<Pair<String, List<PaletteRole>>> = listOf(
    "Primary (beer gold)" to listOf(
        PaletteRole("primary", primary, onPrimary, "onPrimary"),
        PaletteRole("primaryContainer", primaryContainer, onPrimaryContainer, "onPrimaryContainer"),
        PaletteRole("primary on surface", surface, primary, "primary"),
        PaletteRole("inversePrimary", inverseSurface, inversePrimary, "inversePrimary"),
    ),
    "Secondary" to listOf(
        PaletteRole("secondary", secondary, onSecondary, "onSecondary"),
        PaletteRole("secondaryContainer", secondaryContainer, onSecondaryContainer, "onSecondaryContainer"),
    ),
    "Tertiary (roasted malt)" to listOf(
        PaletteRole("tertiary", tertiary, onTertiary, "onTertiary"),
        PaletteRole("tertiaryContainer", tertiaryContainer, onTertiaryContainer, "onTertiaryContainer"),
    ),
    "Error" to listOf(
        PaletteRole("error", error, onError, "onError"),
        PaletteRole("errorContainer", errorContainer, onErrorContainer, "onErrorContainer"),
    ),
    "Surfaces (foam / stout)" to listOf(
        PaletteRole("background", background, onBackground, "onBackground"),
        PaletteRole("surface", surface, onSurface, "onSurface"),
        PaletteRole("surfaceVariant", surfaceVariant, onSurfaceVariant, "onSurfaceVariant"),
        PaletteRole("surfaceDim", surfaceDim, onSurface, "onSurface"),
        PaletteRole("surfaceBright", surfaceBright, onSurface, "onSurface"),
        PaletteRole("surfaceContainerLowest", surfaceContainerLowest, onSurface, "onSurface"),
        PaletteRole("surfaceContainerLow", surfaceContainerLow, onSurface, "onSurface"),
        PaletteRole("surfaceContainer", surfaceContainer, onSurface, "onSurface"),
        PaletteRole("surfaceContainerHigh", surfaceContainerHigh, onSurface, "onSurface"),
        PaletteRole("surfaceContainerHighest", surfaceContainerHighest, onSurface, "onSurface"),
        PaletteRole("inverseSurface", inverseSurface, inverseOnSurface, "inverseOnSurface"),
    ),
    "Outlines" to listOf(
        PaletteRole("outline on surface", surface, outline, "outline", MIN_GRAPHIC_CONTRAST),
        PaletteRole("outlineVariant on surface", surface, outlineVariant, "outlineVariant", 1.0),
    ),
)

/**
 * The whole palette of the current theme: every color role with its hex value and the contrast of
 * the color drawn on it, marked against its minimum, followed by common components.
 *
 * @param modifier applied to the palette's column.
 */
@Composable
private fun ColorPalette(modifier: Modifier = Modifier) {
    val dimensions = GisteTheme.dimensions
    Column(
        modifier = modifier.padding(dimensions.spacingMedium),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
    ) {
        Text(text = "Giste theme palette", style = MaterialTheme.typography.headlineSmall)
        MaterialTheme.colorScheme.paletteGroups().forEach { (group, roles) ->
            Text(
                text = group,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = dimensions.spacingSmall),
            )
            roles.forEach { PaletteSwatch(it) }
        }
        Text(
            text = "Components",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = dimensions.spacingSmall),
        )
        ComponentSamples()
    }
}

/**
 * One role: its name in its content color over its color, then its hex value and the contrast of
 * the content, with ✓ when it reaches the minimum and ✗ when it does not.
 *
 * @param role the role to show.
 */
@Composable
private fun PaletteSwatch(role: PaletteRole) {
    val dimensions = GisteTheme.dimensions
    val ratio = contrastRatio(role.content, role.background)
    val mark = if (ratio >= role.minContrast) "✓" else "✗"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(role.background)
            .border(width = dimensions.hairline(), color = MaterialTheme.colorScheme.outlineVariant)
            .padding(dimensions.spacingSmall),
    ) {
        Text(text = role.name, color = role.content, style = MaterialTheme.typography.titleSmall)
        Text(
            text = "${role.background.hex()} · ${role.contentName} ${role.content.hex()} · " +
                "%.1f:1 %s".format(ratio, mark),
            color = role.content,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
        )
    }
}

/** Common components in their default colors: the buttons and an error text. */
@Composable
private fun ComponentSamples() {
    val dimensions = GisteTheme.dimensions
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
    ) {
        Button(onClick = {}) { Text("Button") }
        FilledTonalButton(onClick = {}) { Text("Tonal") }
        OutlinedButton(onClick = {}) { Text("Outlined") }
        TextButton(onClick = {}) { Text("Text") }
        Button(onClick = {}, enabled = false) { Text("Disabled") }
    }
    Row { Text(text = "Error text", color = MaterialTheme.colorScheme.error) }
}

/** A thin border between swatches whose colors are close: a quarter of the smallest spacing. */
private fun Dimensions.hairline(): Dp = spacingExtraSmall / 4

/** This color as `#RRGGBB`. */
private fun Color.hex(): String = "#%06X".format(toArgb() and 0xFFFFFF)

/** WCAG 2 contrast ratio between two colors: (lighter + 0.05) / (darker + 0.05), from 1 to 21. */
private fun contrastRatio(a: Color, b: Color): Double {
    val la = a.luminance().toDouble()
    val lb = b.luminance().toDouble()
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
}

@Preview(name = "Light", uiMode = Configuration.UI_MODE_NIGHT_NO, widthDp = 420, heightDp = 1900)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 420, heightDp = 1900)
@Composable
private fun ColorPalettePreview() {
    GisteTheme {
        Surface {
            ColorPalette()
        }
    }
}
