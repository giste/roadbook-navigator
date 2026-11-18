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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import dev.giste.roadbook.presentation.icon.IconColors

/**
 * Draws a church, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun churchIcon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Landmark.Church",
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
        path(fill = SolidColor(colors.onSurface)) {
            moveTo(8f, 25f)
            verticalLineTo(48f)
            horizontalLineTo(40f)
            verticalLineTo(25f)
            lineTo(25.5f, 10.5f)
            verticalLineTo(6f)
            horizontalLineTo(28.5f)
            verticalLineTo(3f)
            horizontalLineTo(25.5f)
            verticalLineTo(0f)
            horizontalLineTo(22.5f)
            verticalLineTo(3f)
            horizontalLineTo(19.5f)
            verticalLineTo(6f)
            horizontalLineTo(22.5f)
            verticalLineTo(10.5f)
            lineTo(8f, 25f)
            close()
        }
        path(
            stroke = SolidColor(colors.surface),
            strokeLineWidth = 4f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(13f, 30f)
            verticalLineTo(42f)
        }
        path(
            stroke = SolidColor(colors.surface),
            strokeLineWidth = 4f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(35f, 30f)
            verticalLineTo(42f)
        }
        path(fill = SolidColor(colors.surface)) {
            moveTo(29f, 31f)
            curveTo(29f, 28.239f, 26.761f, 26f, 24f, 26f)
            curveTo(21.239f, 26f, 19f, 28.239f, 19f, 31f)
            horizontalLineTo(24f)
            horizontalLineTo(29f)
            close()
            moveTo(24f, 48f)
            horizontalLineTo(29f)
            verticalLineTo(31f)
            horizontalLineTo(24f)
            horizontalLineTo(19f)
            verticalLineTo(48f)
            horizontalLineTo(24f)
            close()
        }
        path(fill = SolidColor(colors.muted)) {
            moveTo(22f, 48f)
            verticalLineTo(50f)
            horizontalLineTo(26f)
            verticalLineTo(48f)
            horizontalLineTo(24f)
            horizontalLineTo(22f)
            close()
            moveTo(26f, 31f)
            curveTo(26f, 29.895f, 25.105f, 29f, 24f, 29f)
            curveTo(22.895f, 29f, 22f, 29.895f, 22f, 31f)
            horizontalLineTo(24f)
            horizontalLineTo(26f)
            close()
            moveTo(24f, 48f)
            horizontalLineTo(26f)
            verticalLineTo(31f)
            horizontalLineTo(24f)
            horizontalLineTo(22f)
            verticalLineTo(48f)
            horizontalLineTo(24f)
            close()
        }
        path(fill = SolidColor(colors.surface)) {
            moveTo(24f, 20f)
            moveToRelative(-3f, 0f)
            arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, 6f, 0f)
            arcToRelative(3f, 3f, 0f, isMoreThanHalf = true, isPositiveArc = true, -6f, 0f)
        }
    }
}.build()
