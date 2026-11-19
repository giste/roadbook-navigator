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

package dev.giste.roadbook.presentation.icon.speed

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import dev.giste.roadbook.presentation.icon.FixedIconColors
import dev.giste.roadbook.presentation.icon.IconColors

/**
 * Draws the 110 km/h speed limit sign, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun speedLimit110Icon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Speed.Limit110",
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
            fill = SolidColor(FixedIconColors.SIGN_RED),
            stroke = SolidColor(colors.onSurface),
            strokeLineWidth = 1f
        ) {
            moveTo(24f, 24f)
            moveToRelative(-23.5f, 0f)
            arcToRelative(23.5f, 23.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, 47f, 0f)
            arcToRelative(23.5f, 23.5f, 0f, isMoreThanHalf = true, isPositiveArc = true, -47f, 0f)
        }
        path(fill = SolidColor(FixedIconColors.SIGN_FACE)) {
            moveTo(24f, 24f)
            moveToRelative(-19f, 0f)
            arcToRelative(19f, 19f, 0f, isMoreThanHalf = true, isPositiveArc = true, 38f, 0f)
            arcToRelative(19f, 19f, 0f, isMoreThanHalf = true, isPositiveArc = true, -38f, 0f)
        }
        path(fill = SolidColor(FixedIconColors.SIGN_BLACK)) {
            moveTo(15.21f, 16f)
            verticalLineTo(32f)
            horizontalLineTo(12.788f)
            verticalLineTo(18.422f)
            horizontalLineTo(12.695f)
            lineTo(8.867f, 20.922f)
            verticalLineTo(18.609f)
            lineTo(12.859f, 16f)
            horizontalLineTo(15.21f)
            close()
            moveTo(24.509f, 16f)
            verticalLineTo(32f)
            horizontalLineTo(22.087f)
            verticalLineTo(18.422f)
            horizontalLineTo(21.993f)
            lineTo(18.165f, 20.922f)
            verticalLineTo(18.609f)
            lineTo(22.157f, 16f)
            horizontalLineTo(24.509f)
            close()
            moveTo(33.307f, 32.266f)
            curveTo(32.073f, 32.26f, 31.018f, 31.935f, 30.143f, 31.289f)
            curveTo(29.268f, 30.643f, 28.599f, 29.703f, 28.135f, 28.469f)
            curveTo(27.672f, 27.234f, 27.44f, 25.747f, 27.44f, 24.008f)
            curveTo(27.44f, 22.273f, 27.672f, 20.792f, 28.135f, 19.563f)
            curveTo(28.604f, 18.333f, 29.276f, 17.396f, 30.151f, 16.75f)
            curveTo(31.031f, 16.104f, 32.083f, 15.781f, 33.307f, 15.781f)
            curveTo(34.531f, 15.781f, 35.581f, 16.107f, 36.456f, 16.758f)
            curveTo(37.331f, 17.404f, 38f, 18.341f, 38.464f, 19.57f)
            curveTo(38.932f, 20.794f, 39.167f, 22.273f, 39.167f, 24.008f)
            curveTo(39.167f, 25.753f, 38.935f, 27.242f, 38.471f, 28.477f)
            curveTo(38.008f, 29.706f, 37.339f, 30.646f, 36.464f, 31.297f)
            curveTo(35.589f, 31.943f, 34.536f, 32.266f, 33.307f, 32.266f)
            close()
            moveTo(33.307f, 30.18f)
            curveTo(34.391f, 30.18f, 35.237f, 29.651f, 35.846f, 28.594f)
            curveTo(36.461f, 27.537f, 36.768f, 26.008f, 36.768f, 24.008f)
            curveTo(36.768f, 22.68f, 36.627f, 21.557f, 36.346f, 20.641f)
            curveTo(36.07f, 19.719f, 35.672f, 19.021f, 35.151f, 18.547f)
            curveTo(34.635f, 18.068f, 34.021f, 17.828f, 33.307f, 17.828f)
            curveTo(32.229f, 17.828f, 31.383f, 18.359f, 30.768f, 19.422f)
            curveTo(30.154f, 20.484f, 29.844f, 22.013f, 29.838f, 24.008f)
            curveTo(29.838f, 25.341f, 29.976f, 26.469f, 30.253f, 27.391f)
            curveTo(30.534f, 28.307f, 30.932f, 29.003f, 31.448f, 29.477f)
            curveTo(31.963f, 29.945f, 32.583f, 30.18f, 33.307f, 30.18f)
            close()
        }
    }
}.build()
