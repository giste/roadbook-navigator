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
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import dev.giste.roadbook.ui.icon.IconColors

/**
 * Draws a house, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun houseIcon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Landmark.House",
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
            fill = SolidColor(colors.muted),
            stroke = SolidColor(colors.onSurface),
            strokeLineWidth = 2f
        ) {
            moveTo(35f, 10f)
            horizontalLineToRelative(6f)
            verticalLineToRelative(9f)
            horizontalLineToRelative(-6f)
            close()
        }
        path(
            fill = SolidColor(colors.surface),
            stroke = SolidColor(colors.onSurface),
            strokeLineWidth = 2f
        ) {
            moveTo(5f, 25f)
            horizontalLineToRelative(38f)
            verticalLineToRelative(22f)
            horizontalLineToRelative(-38f)
            close()
        }
        path(
            fill = SolidColor(colors.muted),
            stroke = SolidColor(colors.onSurface),
            strokeLineWidth = 2f
        ) {
            moveTo(2.5f, 25f)
            lineTo(24f, 2f)
            lineTo(45.5f, 25f)
            horizontalLineTo(2.5f)
            close()
        }
        path(
            fill = SolidColor(colors.surface),
            stroke = SolidColor(colors.onSurface),
            strokeLineWidth = 2f
        ) {
            moveTo(19f, 33f)
            horizontalLineToRelative(10f)
            verticalLineToRelative(14f)
            horizontalLineToRelative(-10f)
            close()
        }
    }
}.build()
