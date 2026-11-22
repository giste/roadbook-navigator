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

package dev.giste.roadbook.ui.icon.speed

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import dev.giste.roadbook.ui.icon.FixedIconColors
import dev.giste.roadbook.ui.icon.IconColors

/**
 * Draws the 40 km/h speed limit sign, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun speedLimit40Icon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Speed.Limit40",
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
            arcToRelative(
                23.5f,
                23.5f,
                0f,
                isMoreThanHalf = true,
                isPositiveArc = true,
                47f,
                0f
            )
            arcToRelative(
                23.5f,
                23.5f,
                0f,
                isMoreThanHalf = true,
                isPositiveArc = true,
                -47f,
                0f
            )
        }
        path(fill = SolidColor(FixedIconColors.SIGN_FACE)) {
            moveTo(24f, 24f)
            moveToRelative(-19f, 0f)
            arcToRelative(19f, 19f, 0f, isMoreThanHalf = true, isPositiveArc = true, 38f, 0f)
            arcToRelative(19f, 19f, 0f, isMoreThanHalf = true, isPositiveArc = true, -38f, 0f)
        }
        path(fill = SolidColor(FixedIconColors.SIGN_BLACK)) {
            moveTo(11.455f, 28.875f)
            verticalLineTo(26.922f)
            lineTo(18.369f, 16f)
            horizontalLineTo(19.908f)
            verticalLineTo(18.875f)
            horizontalLineTo(18.932f)
            lineTo(13.986f, 26.703f)
            verticalLineTo(26.828f)
            horizontalLineTo(23.463f)
            verticalLineTo(28.875f)
            horizontalLineTo(11.455f)
            close()
            moveTo(19.041f, 32f)
            verticalLineTo(28.281f)
            lineTo(19.057f, 27.391f)
            verticalLineTo(16f)
            horizontalLineTo(21.346f)
            verticalLineTo(32f)
            horizontalLineTo(19.041f)
            close()
            moveTo(30.656f, 32.266f)
            curveTo(29.422f, 32.26f, 28.367f, 31.935f, 27.492f, 31.289f)
            curveTo(26.617f, 30.643f, 25.948f, 29.703f, 25.484f, 28.469f)
            curveTo(25.021f, 27.234f, 24.789f, 25.747f, 24.789f, 24.008f)
            curveTo(24.789f, 22.273f, 25.021f, 20.792f, 25.484f, 19.563f)
            curveTo(25.953f, 18.333f, 26.625f, 17.396f, 27.5f, 16.75f)
            curveTo(28.38f, 16.104f, 29.432f, 15.781f, 30.656f, 15.781f)
            curveTo(31.88f, 15.781f, 32.93f, 16.107f, 33.805f, 16.758f)
            curveTo(34.68f, 17.404f, 35.349f, 18.341f, 35.812f, 19.57f)
            curveTo(36.281f, 20.794f, 36.515f, 22.273f, 36.515f, 24.008f)
            curveTo(36.515f, 25.753f, 36.284f, 27.242f, 35.82f, 28.477f)
            curveTo(35.357f, 29.706f, 34.687f, 30.646f, 33.812f, 31.297f)
            curveTo(32.937f, 31.943f, 31.885f, 32.266f, 30.656f, 32.266f)
            close()
            moveTo(30.656f, 30.18f)
            curveTo(31.739f, 30.18f, 32.586f, 29.651f, 33.195f, 28.594f)
            curveTo(33.81f, 27.537f, 34.117f, 26.008f, 34.117f, 24.008f)
            curveTo(34.117f, 22.68f, 33.976f, 21.557f, 33.695f, 20.641f)
            curveTo(33.419f, 19.719f, 33.021f, 19.021f, 32.5f, 18.547f)
            curveTo(31.984f, 18.068f, 31.37f, 17.828f, 30.656f, 17.828f)
            curveTo(29.578f, 17.828f, 28.732f, 18.359f, 28.117f, 19.422f)
            curveTo(27.502f, 20.484f, 27.192f, 22.013f, 27.187f, 24.008f)
            curveTo(27.187f, 25.341f, 27.325f, 26.469f, 27.601f, 27.391f)
            curveTo(27.883f, 28.307f, 28.281f, 29.003f, 28.797f, 29.477f)
            curveTo(29.312f, 29.945f, 29.932f, 30.18f, 30.656f, 30.18f)
            close()
        }
    }
}.build()
