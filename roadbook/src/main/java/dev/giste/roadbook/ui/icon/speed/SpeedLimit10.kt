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
 * Draws the 10 km/h speed limit sign, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun speedLimit10Icon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Speed.Limit10",
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
            moveTo(19.86f, 16f)
            verticalLineTo(32f)
            horizontalLineTo(17.438f)
            verticalLineTo(18.422f)
            horizontalLineTo(17.344f)
            lineTo(13.516f, 20.922f)
            verticalLineTo(18.609f)
            lineTo(17.508f, 16f)
            horizontalLineTo(19.86f)
            close()
            moveTo(28.658f, 32.266f)
            curveTo(27.424f, 32.26f, 26.369f, 31.935f, 25.494f, 31.289f)
            curveTo(24.619f, 30.643f, 23.95f, 29.703f, 23.486f, 28.469f)
            curveTo(23.023f, 27.234f, 22.791f, 25.747f, 22.791f, 24.008f)
            curveTo(22.791f, 22.273f, 23.023f, 20.792f, 23.486f, 19.563f)
            curveTo(23.955f, 18.333f, 24.627f, 17.396f, 25.502f, 16.75f)
            curveTo(26.382f, 16.104f, 27.434f, 15.781f, 28.658f, 15.781f)
            curveTo(29.882f, 15.781f, 30.931f, 16.107f, 31.806f, 16.758f)
            curveTo(32.681f, 17.404f, 33.351f, 18.341f, 33.814f, 19.57f)
            curveTo(34.283f, 20.794f, 34.517f, 22.273f, 34.517f, 24.008f)
            curveTo(34.517f, 25.753f, 34.286f, 27.242f, 33.822f, 28.477f)
            curveTo(33.359f, 29.706f, 32.689f, 30.646f, 31.814f, 31.297f)
            curveTo(30.939f, 31.943f, 29.887f, 32.266f, 28.658f, 32.266f)
            close()
            moveTo(28.658f, 30.18f)
            curveTo(29.741f, 30.18f, 30.588f, 29.651f, 31.197f, 28.594f)
            curveTo(31.812f, 27.537f, 32.119f, 26.008f, 32.119f, 24.008f)
            curveTo(32.119f, 22.68f, 31.978f, 21.557f, 31.697f, 20.641f)
            curveTo(31.421f, 19.719f, 31.023f, 19.021f, 30.502f, 18.547f)
            curveTo(29.986f, 18.068f, 29.372f, 17.828f, 28.658f, 17.828f)
            curveTo(27.58f, 17.828f, 26.733f, 18.359f, 26.119f, 19.422f)
            curveTo(25.504f, 20.484f, 25.194f, 22.013f, 25.189f, 24.008f)
            curveTo(25.189f, 25.341f, 25.327f, 26.469f, 25.603f, 27.391f)
            curveTo(25.885f, 28.307f, 26.283f, 29.003f, 26.799f, 29.477f)
            curveTo(27.314f, 29.945f, 27.934f, 30.18f, 28.658f, 30.18f)
            close()
        }
    }
}.build()
