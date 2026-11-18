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

package dev.giste.roadbook.presentation.icon

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * The theme colors the roadbook symbols are drawn with, so they follow the light and dark themes.
 *
 * Colors that must look like the real world (a speed limit's red ring, a river's blue) do not
 * change with the theme: they are in [FixedIconColors].
 *
 * @property onSurface lines and shapes, drawn over [surface].
 * @property surface the background the symbol is drawn on. Parts of some symbols are filled with
 * it to cut shapes out of others, so it must match what is behind the symbol.
 * @property danger the danger marks.
 * @property muted secondary shapes, such as the walls of buildings.
 */
@Immutable
internal data class IconColors(
    val onSurface: Color,
    val surface: Color,
    val danger: Color,
    val muted: Color,
)

/**
 * The [IconColors] of the current theme, for symbols drawn on its surface.
 *
 * @param surface the background behind the symbols. Defaults to the theme's surface; pass the
 * background of the container when it is another color (e.g. a highlighted waypoint).
 * @return the colors to draw the symbols with.
 */
@Composable
@ReadOnlyComposable
internal fun iconColors(surface: Color = MaterialTheme.colorScheme.surface): IconColors =
    IconColors(
        onSurface = MaterialTheme.colorScheme.onSurface,
        surface = surface,
        danger = MaterialTheme.colorScheme.error,
        muted = MaterialTheme.colorScheme.outline,
    )

/**
 * Colors of symbols that copy real-world signs and features, so the rider recognizes them from
 * the road. They are the same in the light and dark themes, which is why they are not theme
 * tokens.
 */
internal object FixedIconColors {

    /** The red of traffic signs: speed limit rings, the stop and no entry signs. */
    val SIGN_RED: Color = Color(0xFFFF0000)

    /** The light gray face of the signs with a red ring. */
    val SIGN_FACE: Color = Color(0xFFD9D9D9)

    /** The black of the numbers on speed limit signs. */
    val SIGN_BLACK: Color = Color(0xFF000000)

    /** The white of sign marks, such as the bar of the no entry sign. */
    val SIGN_WHITE: Color = Color(0xFFFFFFFF)

    /** The blue of mandatory signs, such as the roundabout. */
    val SIGN_BLUE: Color = Color(0xFF376FDE)

    /** The blue of the fuel zone mark. */
    val FUEL_BLUE: Color = Color(0xFF3D93D0)

    /** The blue of water. */
    val WATER_BLUE: Color = Color(0xFF0088FF)

    /** The green of tree foliage. */
    val TREE_GREEN: Color = Color(0xFF008000)
}
