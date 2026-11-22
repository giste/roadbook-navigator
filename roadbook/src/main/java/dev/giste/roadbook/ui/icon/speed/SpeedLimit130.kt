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
 * Draws the 130 km/h speed limit sign, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun speedLimit130Icon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Speed.Limit130",
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
            moveTo(13.32f, 16f)
            verticalLineTo(32f)
            horizontalLineTo(10.898f)
            verticalLineTo(18.422f)
            horizontalLineTo(10.804f)
            lineTo(6.976f, 20.922f)
            verticalLineTo(18.609f)
            lineTo(10.968f, 16f)
            horizontalLineTo(13.32f)
            close()
            moveTo(22.118f, 32.219f)
            curveTo(21.045f, 32.219f, 20.087f, 32.034f, 19.243f, 31.664f)
            curveTo(18.405f, 31.294f, 17.741f, 30.781f, 17.251f, 30.125f)
            curveTo(16.767f, 29.463f, 16.506f, 28.698f, 16.47f, 27.828f)
            horizontalLineTo(18.923f)
            curveTo(18.954f, 28.302f, 19.113f, 28.713f, 19.399f, 29.063f)
            curveTo(19.691f, 29.406f, 20.071f, 29.672f, 20.54f, 29.859f)
            curveTo(21.009f, 30.047f, 21.53f, 30.141f, 22.103f, 30.141f)
            curveTo(22.733f, 30.141f, 23.29f, 30.031f, 23.774f, 29.813f)
            curveTo(24.264f, 29.594f, 24.647f, 29.289f, 24.923f, 28.898f)
            curveTo(25.199f, 28.503f, 25.337f, 28.047f, 25.337f, 27.531f)
            curveTo(25.337f, 26.995f, 25.199f, 26.523f, 24.923f, 26.117f)
            curveTo(24.652f, 25.706f, 24.254f, 25.383f, 23.728f, 25.148f)
            curveTo(23.207f, 24.914f, 22.576f, 24.797f, 21.837f, 24.797f)
            horizontalLineTo(20.485f)
            verticalLineTo(22.828f)
            horizontalLineTo(21.837f)
            curveTo(22.431f, 22.828f, 22.951f, 22.721f, 23.399f, 22.508f)
            curveTo(23.853f, 22.294f, 24.207f, 21.997f, 24.462f, 21.617f)
            curveTo(24.717f, 21.232f, 24.845f, 20.781f, 24.845f, 20.266f)
            curveTo(24.845f, 19.771f, 24.733f, 19.341f, 24.509f, 18.977f)
            curveTo(24.29f, 18.607f, 23.978f, 18.318f, 23.571f, 18.109f)
            curveTo(23.17f, 17.901f, 22.696f, 17.797f, 22.149f, 17.797f)
            curveTo(21.629f, 17.797f, 21.142f, 17.893f, 20.688f, 18.086f)
            curveTo(20.241f, 18.273f, 19.876f, 18.544f, 19.595f, 18.898f)
            curveTo(19.313f, 19.247f, 19.162f, 19.667f, 19.142f, 20.156f)
            horizontalLineTo(16.806f)
            curveTo(16.832f, 19.292f, 17.087f, 18.531f, 17.571f, 17.875f)
            curveTo(18.061f, 17.219f, 18.707f, 16.706f, 19.509f, 16.336f)
            curveTo(20.311f, 15.966f, 21.201f, 15.781f, 22.181f, 15.781f)
            curveTo(23.207f, 15.781f, 24.092f, 15.982f, 24.837f, 16.383f)
            curveTo(25.587f, 16.779f, 26.165f, 17.307f, 26.571f, 17.969f)
            curveTo(26.983f, 18.63f, 27.186f, 19.354f, 27.181f, 20.141f)
            curveTo(27.186f, 21.037f, 26.936f, 21.797f, 26.431f, 22.422f)
            curveTo(25.931f, 23.047f, 25.264f, 23.466f, 24.431f, 23.68f)
            verticalLineTo(23.805f)
            curveTo(25.493f, 23.966f, 26.316f, 24.388f, 26.899f, 25.07f)
            curveTo(27.488f, 25.753f, 27.78f, 26.599f, 27.774f, 27.609f)
            curveTo(27.78f, 28.49f, 27.535f, 29.279f, 27.04f, 29.977f)
            curveTo(26.551f, 30.674f, 25.881f, 31.224f, 25.032f, 31.625f)
            curveTo(24.183f, 32.021f, 23.212f, 32.219f, 22.118f, 32.219f)
            close()
            moveTo(35.198f, 32.266f)
            curveTo(33.964f, 32.26f, 32.909f, 31.935f, 32.034f, 31.289f)
            curveTo(31.159f, 30.643f, 30.49f, 29.703f, 30.026f, 28.469f)
            curveTo(29.562f, 27.234f, 29.331f, 25.747f, 29.331f, 24.008f)
            curveTo(29.331f, 22.273f, 29.562f, 20.792f, 30.026f, 19.563f)
            curveTo(30.495f, 18.333f, 31.167f, 17.396f, 32.042f, 16.75f)
            curveTo(32.922f, 16.104f, 33.974f, 15.781f, 35.198f, 15.781f)
            curveTo(36.422f, 15.781f, 37.471f, 16.107f, 38.346f, 16.758f)
            curveTo(39.221f, 17.404f, 39.891f, 18.341f, 40.354f, 19.57f)
            curveTo(40.823f, 20.794f, 41.057f, 22.273f, 41.057f, 24.008f)
            curveTo(41.057f, 25.753f, 40.826f, 27.242f, 40.362f, 28.477f)
            curveTo(39.898f, 29.706f, 39.229f, 30.646f, 38.354f, 31.297f)
            curveTo(37.479f, 31.943f, 36.427f, 32.266f, 35.198f, 32.266f)
            close()
            moveTo(35.198f, 30.18f)
            curveTo(36.281f, 30.18f, 37.127f, 29.651f, 37.737f, 28.594f)
            curveTo(38.352f, 27.537f, 38.659f, 26.008f, 38.659f, 24.008f)
            curveTo(38.659f, 22.68f, 38.518f, 21.557f, 38.237f, 20.641f)
            curveTo(37.961f, 19.719f, 37.562f, 19.021f, 37.042f, 18.547f)
            curveTo(36.526f, 18.068f, 35.911f, 17.828f, 35.198f, 17.828f)
            curveTo(34.12f, 17.828f, 33.273f, 18.359f, 32.659f, 19.422f)
            curveTo(32.044f, 20.484f, 31.734f, 22.013f, 31.729f, 24.008f)
            curveTo(31.729f, 25.341f, 31.867f, 26.469f, 32.143f, 27.391f)
            curveTo(32.424f, 28.307f, 32.823f, 29.003f, 33.339f, 29.477f)
            curveTo(33.854f, 29.945f, 34.474f, 30.18f, 35.198f, 30.18f)
            close()
        }
    }
}.build()
