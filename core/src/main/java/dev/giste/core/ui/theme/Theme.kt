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

package dev.giste.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import dev.giste.core.ui.theme.ui.LocalDimensions
import dev.giste.core.ui.theme.ui.beerColorScheme

/**
 * The shared Material 3 theme of every app that uses `:core`, with a palette based on beer colors.
 *
 * Wrap every screen, and every `@Preview`, in it. Read colors and typography through
 * [MaterialTheme] (`MaterialTheme.colorScheme.primary`, `MaterialTheme.typography.bodyLarge`) and
 * spacing through [GisteTheme.dimensions]. Never hardcode colors or dimensions.
 *
 * Dynamic color (Material You) is not used: the beer palette is the apps' identity, and fixed
 * colors keep content equally readable on every device.
 *
 * @param darkTheme whether to use the dark (stout) color scheme instead of the light (foam)
 * one. Defaults to the system setting.
 * @param typography the text styles. Defaults to the Material 3 ones. An app that needs other
 * sizes, e.g. larger text for a screen read at arm's length, passes its own, so every screen,
 * including those of other modules, uses them.
 * @param content the composable content the theme applies to.
 */
@Composable
public fun GisteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    typography: Typography = Typography(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalDimensions provides Dimensions()) {
        MaterialTheme(
            colorScheme = beerColorScheme(darkTheme),
            typography = typography,
            content = content,
        )
    }
}

/**
 * Accessors for the theme values that [MaterialTheme] does not cover.
 *
 * Read them only inside [GisteTheme]. Outside it, they fall back to the defaults.
 */
public object GisteTheme {

    /** The spacing and size tokens of the current theme. */
    public val dimensions: Dimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalDimensions.current
}
