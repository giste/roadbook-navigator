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

package dev.giste.roadbook.presentation.icon.cross

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import dev.giste.roadbook.presentation.icon.IconColors

/**
 * Draws the danger level 1 mark: one exclamation mark, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun danger1Icon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Cross.Danger1",
    defaultWidth = 48.dp,
    defaultHeight = 48.dp,
    viewportWidth = 48f,
    viewportHeight = 48f
).apply {
    path(fill = SolidColor(colors.danger)) {
        moveTo(18f, 34f)
        horizontalLineToRelative(12f)
        verticalLineToRelative(14f)
        horizontalLineToRelative(-12f)
        close()
    }
    path(fill = SolidColor(colors.danger)) {
        moveTo(18f, 12f)
        lineTo(20f, 32f)
        horizontalLineTo(28f)
        lineTo(30f, 12f)
        verticalLineTo(0f)
        horizontalLineTo(18f)
        verticalLineTo(12f)
        close()
    }
}.build()
