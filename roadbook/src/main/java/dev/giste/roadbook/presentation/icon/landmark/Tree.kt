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

package dev.giste.roadbook.presentation.icon.landmark

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import dev.giste.roadbook.presentation.icon.FixedIconColors
import dev.giste.roadbook.presentation.icon.IconColors

/**
 * Draws a tree, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun treeIcon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Landmark.Tree",
    defaultWidth = 48.dp,
    defaultHeight = 48.dp,
    viewportWidth = 48f,
    viewportHeight = 48f
).apply {
    // Foliage
    path(
        fill = SolidColor(FixedIconColors.TREE_GREEN),
        stroke = SolidColor(colors.onSurface),
        strokeLineWidth = 2f
    ) {
        moveTo(24f, 4f)
        curveTo(18f, 4f, 14f, 8f, 12f, 13f)
        curveTo(9f, 14f, 8f, 18f, 8f, 22f)
        curveTo(8f, 28f, 12f, 32f, 16f, 32f)
        horizontalLineTo(32f)
        curveTo(36f, 32f, 40f, 28f, 40f, 22f)
        curveTo(40f, 18f, 39f, 14f, 36f, 13f)
        curveTo(34f, 8f, 30f, 4f, 24f, 4f)
        close()
    }
    // Foliage detail (texture)
    path(
        stroke = SolidColor(colors.onSurface),
        strokeLineWidth = 2f
    ) {
        moveTo(16f, 18f)
        curveTo(16f, 18f, 18f, 16f, 21f, 16f)
        moveTo(27f, 22f)
        curveTo(27f, 22f, 30f, 20f, 33f, 21f)
        moveTo(14f, 25f)
        curveTo(14f, 25f, 17f, 24f, 20f, 25f)
    }
    // Trunk
    path(
        stroke = SolidColor(colors.onSurface),
        strokeLineWidth = 4f
    ) {
        moveTo(24f, 32f)
        lineTo(24f, 46f)
    }
}.build()
