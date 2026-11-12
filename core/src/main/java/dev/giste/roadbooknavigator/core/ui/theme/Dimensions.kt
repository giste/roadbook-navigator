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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Spacing and size tokens. Read them through [RoadbookNavigatorTheme.dimensions] instead of
 * hardcoding `dp` values in features.
 *
 * Only the theme creates instances, so every screen shares the same scale.
 *
 * @property spacingExtraSmall 4 dp. Space between tightly related items, e.g. an icon and its label.
 * @property spacingSmall 8 dp. Space between items in the same group.
 * @property spacingMedium 16 dp. Default content padding and space between groups.
 * @property spacingLarge 24 dp. Space between sections.
 * @property spacingExtraLarge 32 dp. Space around the main blocks of a screen.
 * @property minTouchTarget 56 dp. Minimum size of anything the user taps. Larger than the
 * Material 48 dp minimum because the app is used while driving, often with gloves.
 */
@Immutable
public class Dimensions internal constructor(
    public val spacingExtraSmall: Dp = 4.dp,
    public val spacingSmall: Dp = 8.dp,
    public val spacingMedium: Dp = 16.dp,
    public val spacingLarge: Dp = 24.dp,
    public val spacingExtraLarge: Dp = 32.dp,
    public val minTouchTarget: Dp = 56.dp,
)
