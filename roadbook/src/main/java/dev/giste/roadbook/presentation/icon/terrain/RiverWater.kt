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

package dev.giste.roadbook.presentation.icon.terrain

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import dev.giste.roadbook.presentation.icon.FixedIconColors
import dev.giste.roadbook.presentation.icon.IconColors

/**
 * Draws a river or water crossing, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun riverWaterIcon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Terrain.RiverWater",
    defaultWidth = 48.dp,
    defaultHeight = 48.dp,
    viewportWidth = 48f,
    viewportHeight = 48f
).apply {
    group(
        clipPathData = PathData {
            moveTo(0f, 0f)
            horizontalLineToRelative(48f)
            verticalLineToRelative(48f)
            horizontalLineToRelative(-48f)
            close()
        }
    ) {
        path(
            fill = SolidColor(FixedIconColors.WATER_BLUE),
            stroke = SolidColor(colors.onSurface),
            strokeLineWidth = 2f
        ) {
            moveTo(0f, 10f)
            curveTo(-3f, 10f, -6f, 8f, -6f, 8f)
            verticalLineTo(38f)
            curveTo(-6f, 38f, -2.47f, 40f, 0f, 40f)
            curveTo(2.47f, 40f, 3.53f, 38f, 6f, 38f)
            curveTo(8.47f, 38f, 9.53f, 40f, 12f, 40f)
            curveTo(14.47f, 40f, 15.53f, 38f, 18f, 38f)
            curveTo(20.47f, 38f, 21.53f, 40f, 24f, 40f)
            curveTo(26.47f, 40f, 27.53f, 38f, 30f, 38f)
            curveTo(32.47f, 38f, 33.53f, 40f, 36f, 40f)
            curveTo(38.47f, 40f, 39.53f, 38f, 42f, 38f)
            curveTo(44.47f, 38f, 45.53f, 40f, 48f, 40f)
            curveTo(50.47f, 40f, 54f, 38f, 54f, 38f)
            verticalLineTo(8f)
            curveTo(54f, 8f, 50.47f, 10f, 48f, 10f)
            curveTo(45.53f, 10f, 44.47f, 8f, 42f, 8f)
            curveTo(39.53f, 8f, 38.47f, 10f, 36f, 10f)
            curveTo(33.53f, 10f, 32.47f, 8f, 30f, 8f)
            curveTo(27.53f, 8f, 26.47f, 10f, 24f, 10f)
            curveTo(21.53f, 10f, 20.47f, 8f, 18f, 8f)
            curveTo(15.53f, 8f, 14.47f, 10f, 12f, 10f)
            curveTo(9.53f, 10f, 8.47f, 8f, 6f, 8f)
            curveTo(3.53f, 8f, 3f, 10f, 0f, 10f)
            close()
        }
    }
}.build()
