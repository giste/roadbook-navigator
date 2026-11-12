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

package dev.giste.roadbooknavigator.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import dev.giste.roadbooknavigator.core.ui.theme.presentation.LocalDimensions
import dev.giste.roadbooknavigator.core.ui.theme.presentation.beerColorScheme

/**
 * The app's Material 3 theme, with a palette based on beer colors.
 *
 * Wrap every screen, and every `@Preview`, in it. Read colors and typography through
 * [MaterialTheme] (`MaterialTheme.colorScheme.primary`, `MaterialTheme.typography.bodyLarge`) and
 * spacing through [RoadbookNavigatorTheme.dimensions]. Never hardcode colors or dimensions.
 *
 * Dynamic color (Material You) is not used: the beer palette is the app's identity, and fixed
 * colors keep the roadbook equally readable on every device.
 *
 * @param darkTheme whether to use the dark (stout) color scheme instead of the light (foam)
 * one. Defaults to the system setting.
 * @param content the composable content the theme applies to.
 */
@Composable
public fun RoadbookNavigatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalDimensions provides Dimensions()) {
        MaterialTheme(
            colorScheme = beerColorScheme(darkTheme),
            typography = Typography(),
            content = content,
        )
    }
}

/**
 * Accessors for the theme values that [MaterialTheme] does not cover.
 *
 * Read them only inside [RoadbookNavigatorTheme]. Outside it, they fall back to the defaults.
 */
public object RoadbookNavigatorTheme {

    /** The spacing and size tokens of the current theme. */
    public val dimensions: Dimensions
        @Composable
        @ReadOnlyComposable
        get() = LocalDimensions.current
}
