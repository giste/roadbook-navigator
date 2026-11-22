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

package dev.giste.roadbook.ui.icon.landmark

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import dev.giste.roadbook.ui.icon.IconColors

/**
 * Draws a tunnel, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun tunnelIcon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Landmark.Tunnel",
    defaultWidth = 48.dp,
    defaultHeight = 48.dp,
    viewportWidth = 48f,
    viewportHeight = 48f
).apply {
    val onSurfaceColor = SolidColor(colors.onSurface)
    path(
        fill = SolidColor(colors.muted),
        stroke = onSurfaceColor,
        strokeLineWidth = 2f
    ) {
        moveTo(21.125f, 24f)
        lineTo(1.719f, 28.133f)
        curveTo(1.719f, 24.719f, 3.156f, 22.467f, 4.414f, 21.125f)
        curveTo(7.109f, 18.25f, 9.625f, 18.43f, 9.625f, 18.43f)
        lineTo(16.992f, 17.352f)
        lineTo(21.125f, 24f)
        close()
    }
    path(fill = SolidColor(colors.muted)) {
        moveTo(22.023f, 34.422f)
        curveTo(22.023f, 25.981f, 15.411f, 17.539f, 10.71f, 18.249f)
        lineTo(39.094f, 13.938f)
        curveTo(42.867f, 13.938f, 46.281f, 18.25f, 46.281f, 22.563f)
        lineTo(22.023f, 34.422f)
        close()
    }
    path(
        stroke = onSurfaceColor,
        strokeLineWidth = 2f
    ) {
        moveTo(1.719f, 28.313f)
        curveTo(1.719f, 23.641f, 4.055f, 18.969f, 10.703f, 18.25f)
        moveTo(10.703f, 18.25f)
        curveTo(15.405f, 17.531f, 22.023f, 25.977f, 22.023f, 34.422f)
        lineTo(46.281f, 22.563f)
        curveTo(46.281f, 18.25f, 42.867f, 13.938f, 39.094f, 13.938f)
        lineTo(10.703f, 18.25f)
        close()
    }
}.build()
