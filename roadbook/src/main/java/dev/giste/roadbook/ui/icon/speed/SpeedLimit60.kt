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
 * Draws the 60 km/h speed limit sign, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun speedLimit60Icon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Speed.Limit60",
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
            moveTo(17.581f, 32.219f)
            curveTo(16.868f, 32.208f, 16.165f, 32.078f, 15.472f, 31.828f)
            curveTo(14.784f, 31.578f, 14.159f, 31.162f, 13.597f, 30.578f)
            curveTo(13.034f, 29.995f, 12.584f, 29.211f, 12.245f, 28.227f)
            curveTo(11.912f, 27.242f, 11.745f, 26.013f, 11.745f, 24.539f)
            curveTo(11.745f, 23.143f, 11.883f, 21.904f, 12.159f, 20.82f)
            curveTo(12.441f, 19.737f, 12.844f, 18.823f, 13.37f, 18.078f)
            curveTo(13.896f, 17.328f, 14.532f, 16.758f, 15.277f, 16.367f)
            curveTo(16.021f, 15.977f, 16.857f, 15.781f, 17.784f, 15.781f)
            curveTo(18.737f, 15.781f, 19.584f, 15.969f, 20.323f, 16.344f)
            curveTo(21.063f, 16.719f, 21.662f, 17.237f, 22.12f, 17.898f)
            curveTo(22.584f, 18.56f, 22.878f, 19.313f, 23.003f, 20.156f)
            horizontalLineTo(20.62f)
            curveTo(20.459f, 19.49f, 20.139f, 18.945f, 19.659f, 18.523f)
            curveTo(19.18f, 18.102f, 18.555f, 17.891f, 17.784f, 17.891f)
            curveTo(16.612f, 17.891f, 15.698f, 18.401f, 15.042f, 19.422f)
            curveTo(14.391f, 20.443f, 14.063f, 21.862f, 14.058f, 23.68f)
            horizontalLineTo(14.175f)
            curveTo(14.451f, 23.227f, 14.79f, 22.841f, 15.191f, 22.523f)
            curveTo(15.597f, 22.201f, 16.05f, 21.953f, 16.55f, 21.781f)
            curveTo(17.055f, 21.604f, 17.587f, 21.516f, 18.144f, 21.516f)
            curveTo(19.071f, 21.516f, 19.909f, 21.742f, 20.659f, 22.195f)
            curveTo(21.415f, 22.643f, 22.016f, 23.263f, 22.464f, 24.055f)
            curveTo(22.912f, 24.846f, 23.136f, 25.753f, 23.136f, 26.773f)
            curveTo(23.136f, 27.794f, 22.904f, 28.719f, 22.441f, 29.547f)
            curveTo(21.982f, 30.375f, 21.337f, 31.031f, 20.503f, 31.516f)
            curveTo(19.67f, 31.995f, 18.696f, 32.229f, 17.581f, 32.219f)
            close()
            moveTo(17.573f, 30.188f)
            curveTo(18.188f, 30.188f, 18.737f, 30.037f, 19.222f, 29.734f)
            curveTo(19.706f, 29.432f, 20.089f, 29.026f, 20.37f, 28.516f)
            curveTo(20.652f, 28.005f, 20.792f, 27.435f, 20.792f, 26.805f)
            curveTo(20.792f, 26.19f, 20.654f, 25.63f, 20.378f, 25.125f)
            curveTo(20.107f, 24.62f, 19.732f, 24.219f, 19.253f, 23.922f)
            curveTo(18.779f, 23.625f, 18.237f, 23.477f, 17.628f, 23.477f)
            curveTo(17.165f, 23.477f, 16.735f, 23.565f, 16.339f, 23.742f)
            curveTo(15.948f, 23.919f, 15.605f, 24.164f, 15.308f, 24.477f)
            curveTo(15.011f, 24.789f, 14.777f, 25.148f, 14.605f, 25.555f)
            curveTo(14.438f, 25.956f, 14.355f, 26.38f, 14.355f, 26.828f)
            curveTo(14.355f, 27.427f, 14.493f, 27.982f, 14.769f, 28.492f)
            curveTo(15.05f, 29.003f, 15.433f, 29.414f, 15.917f, 29.727f)
            curveTo(16.407f, 30.034f, 16.959f, 30.188f, 17.573f, 30.188f)
            close()
            moveTo(30.452f, 32.266f)
            curveTo(29.218f, 32.26f, 28.163f, 31.935f, 27.288f, 31.289f)
            curveTo(26.413f, 30.643f, 25.744f, 29.703f, 25.28f, 28.469f)
            curveTo(24.816f, 27.234f, 24.585f, 25.747f, 24.585f, 24.008f)
            curveTo(24.585f, 22.273f, 24.816f, 20.792f, 25.28f, 19.563f)
            curveTo(25.749f, 18.333f, 26.421f, 17.396f, 27.296f, 16.75f)
            curveTo(28.176f, 16.104f, 29.228f, 15.781f, 30.452f, 15.781f)
            curveTo(31.676f, 15.781f, 32.725f, 16.107f, 33.6f, 16.758f)
            curveTo(34.475f, 17.404f, 35.145f, 18.341f, 35.608f, 19.57f)
            curveTo(36.077f, 20.794f, 36.311f, 22.273f, 36.311f, 24.008f)
            curveTo(36.311f, 25.753f, 36.08f, 27.242f, 35.616f, 28.477f)
            curveTo(35.153f, 29.706f, 34.483f, 30.646f, 33.608f, 31.297f)
            curveTo(32.733f, 31.943f, 31.681f, 32.266f, 30.452f, 32.266f)
            close()
            moveTo(30.452f, 30.18f)
            curveTo(31.535f, 30.18f, 32.382f, 29.651f, 32.991f, 28.594f)
            curveTo(33.606f, 27.537f, 33.913f, 26.008f, 33.913f, 24.008f)
            curveTo(33.913f, 22.68f, 33.772f, 21.557f, 33.491f, 20.641f)
            curveTo(33.215f, 19.719f, 32.817f, 19.021f, 32.296f, 18.547f)
            curveTo(31.78f, 18.068f, 31.166f, 17.828f, 30.452f, 17.828f)
            curveTo(29.374f, 17.828f, 28.528f, 18.359f, 27.913f, 19.422f)
            curveTo(27.298f, 20.484f, 26.988f, 22.013f, 26.983f, 24.008f)
            curveTo(26.983f, 25.341f, 27.121f, 26.469f, 27.397f, 27.391f)
            curveTo(27.678f, 28.307f, 28.077f, 29.003f, 28.593f, 29.477f)
            curveTo(29.108f, 29.945f, 29.728f, 30.18f, 30.452f, 30.18f)
            close()
        }
    }
}.build()
