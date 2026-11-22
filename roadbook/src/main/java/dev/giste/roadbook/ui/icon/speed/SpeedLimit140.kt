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
 * Draws the 140 km/h speed limit sign, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun speedLimit140Icon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Speed.Limit140",
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
            moveTo(13.212f, 16f)
            verticalLineTo(32f)
            horizontalLineTo(10.79f)
            verticalLineTo(18.422f)
            horizontalLineTo(10.697f)
            lineTo(6.869f, 20.922f)
            verticalLineTo(18.609f)
            lineTo(10.861f, 16f)
            horizontalLineTo(13.212f)
            close()
            moveTo(16.104f, 28.875f)
            verticalLineTo(26.922f)
            lineTo(23.019f, 16f)
            horizontalLineTo(24.558f)
            verticalLineTo(18.875f)
            horizontalLineTo(23.581f)
            lineTo(18.636f, 26.703f)
            verticalLineTo(26.828f)
            horizontalLineTo(28.112f)
            verticalLineTo(28.875f)
            horizontalLineTo(16.104f)
            close()
            moveTo(23.69f, 32f)
            verticalLineTo(28.281f)
            lineTo(23.706f, 27.391f)
            verticalLineTo(16f)
            horizontalLineTo(25.995f)
            verticalLineTo(32f)
            horizontalLineTo(23.69f)
            close()
            moveTo(35.305f, 32.266f)
            curveTo(34.071f, 32.26f, 33.016f, 31.935f, 32.141f, 31.289f)
            curveTo(31.266f, 30.643f, 30.597f, 29.703f, 30.133f, 28.469f)
            curveTo(29.67f, 27.234f, 29.438f, 25.747f, 29.438f, 24.008f)
            curveTo(29.438f, 22.273f, 29.67f, 20.792f, 30.133f, 19.563f)
            curveTo(30.602f, 18.333f, 31.274f, 17.396f, 32.149f, 16.75f)
            curveTo(33.029f, 16.104f, 34.081f, 15.781f, 35.305f, 15.781f)
            curveTo(36.529f, 15.781f, 37.579f, 16.107f, 38.454f, 16.758f)
            curveTo(39.329f, 17.404f, 39.998f, 18.341f, 40.461f, 19.57f)
            curveTo(40.93f, 20.794f, 41.165f, 22.273f, 41.165f, 24.008f)
            curveTo(41.165f, 25.753f, 40.933f, 27.242f, 40.469f, 28.477f)
            curveTo(40.006f, 29.706f, 39.336f, 30.646f, 38.461f, 31.297f)
            curveTo(37.586f, 31.943f, 36.534f, 32.266f, 35.305f, 32.266f)
            close()
            moveTo(35.305f, 30.18f)
            curveTo(36.389f, 30.18f, 37.235f, 29.651f, 37.844f, 28.594f)
            curveTo(38.459f, 27.537f, 38.766f, 26.008f, 38.766f, 24.008f)
            curveTo(38.766f, 22.68f, 38.626f, 21.557f, 38.344f, 20.641f)
            curveTo(38.068f, 19.719f, 37.67f, 19.021f, 37.149f, 18.547f)
            curveTo(36.633f, 18.068f, 36.019f, 17.828f, 35.305f, 17.828f)
            curveTo(34.227f, 17.828f, 33.381f, 18.359f, 32.766f, 19.422f)
            curveTo(32.152f, 20.484f, 31.842f, 22.013f, 31.837f, 24.008f)
            curveTo(31.837f, 25.341f, 31.975f, 26.469f, 32.251f, 27.391f)
            curveTo(32.532f, 28.307f, 32.93f, 29.003f, 33.446f, 29.477f)
            curveTo(33.961f, 29.945f, 34.581f, 30.18f, 35.305f, 30.18f)
            close()
        }
    }
}.build()
