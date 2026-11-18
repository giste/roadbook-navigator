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
 * Draws the 90 km/h speed limit sign, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun speedLimit90Icon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Speed.Limit90",
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
            moveTo(17.316f, 15.781f)
            curveTo(18.029f, 15.786f, 18.732f, 15.917f, 19.425f, 16.172f)
            curveTo(20.118f, 16.427f, 20.743f, 16.844f, 21.3f, 17.422f)
            curveTo(21.862f, 18f, 22.31f, 18.781f, 22.644f, 19.766f)
            curveTo(22.982f, 20.745f, 23.154f, 21.963f, 23.159f, 23.422f)
            curveTo(23.159f, 24.823f, 23.019f, 26.07f, 22.737f, 27.164f)
            curveTo(22.456f, 28.253f, 22.053f, 29.172f, 21.527f, 29.922f)
            curveTo(21.006f, 30.672f, 20.373f, 31.242f, 19.628f, 31.633f)
            curveTo(18.883f, 32.023f, 18.045f, 32.219f, 17.112f, 32.219f)
            curveTo(16.159f, 32.219f, 15.313f, 32.031f, 14.573f, 31.656f)
            curveTo(13.834f, 31.281f, 13.232f, 30.763f, 12.769f, 30.102f)
            curveTo(12.305f, 29.435f, 12.016f, 28.669f, 11.902f, 27.805f)
            horizontalLineTo(14.284f)
            curveTo(14.441f, 28.492f, 14.758f, 29.049f, 15.238f, 29.477f)
            curveTo(15.722f, 29.898f, 16.347f, 30.109f, 17.112f, 30.109f)
            curveTo(18.284f, 30.109f, 19.198f, 29.599f, 19.855f, 28.578f)
            curveTo(20.511f, 27.552f, 20.842f, 26.12f, 20.847f, 24.281f)
            horizontalLineTo(20.722f)
            curveTo(20.451f, 24.729f, 20.112f, 25.115f, 19.706f, 25.438f)
            curveTo(19.305f, 25.76f, 18.855f, 26.01f, 18.355f, 26.188f)
            curveTo(17.855f, 26.365f, 17.321f, 26.453f, 16.753f, 26.453f)
            curveTo(15.831f, 26.453f, 14.993f, 26.227f, 14.238f, 25.773f)
            curveTo(13.482f, 25.32f, 12.881f, 24.698f, 12.433f, 23.906f)
            curveTo(11.985f, 23.115f, 11.761f, 22.211f, 11.761f, 21.195f)
            curveTo(11.761f, 20.185f, 11.99f, 19.268f, 12.448f, 18.445f)
            curveTo(12.912f, 17.622f, 13.558f, 16.971f, 14.386f, 16.492f)
            curveTo(15.219f, 16.008f, 16.196f, 15.771f, 17.316f, 15.781f)
            close()
            moveTo(17.323f, 17.813f)
            curveTo(16.714f, 17.813f, 16.165f, 17.963f, 15.675f, 18.266f)
            curveTo(15.191f, 18.563f, 14.808f, 18.966f, 14.527f, 19.477f)
            curveTo(14.245f, 19.982f, 14.105f, 20.544f, 14.105f, 21.164f)
            curveTo(14.105f, 21.784f, 14.24f, 22.346f, 14.511f, 22.852f)
            curveTo(14.787f, 23.352f, 15.162f, 23.75f, 15.636f, 24.047f)
            curveTo(16.115f, 24.338f, 16.662f, 24.484f, 17.277f, 24.484f)
            curveTo(17.735f, 24.484f, 18.162f, 24.396f, 18.558f, 24.219f)
            curveTo(18.954f, 24.042f, 19.3f, 23.797f, 19.597f, 23.484f)
            curveTo(19.894f, 23.167f, 20.125f, 22.807f, 20.292f, 22.406f)
            curveTo(20.459f, 22.005f, 20.542f, 21.583f, 20.542f, 21.141f)
            curveTo(20.542f, 20.552f, 20.402f, 20.005f, 20.12f, 19.5f)
            curveTo(19.844f, 18.995f, 19.464f, 18.588f, 18.98f, 18.281f)
            curveTo(18.495f, 17.969f, 17.943f, 17.813f, 17.323f, 17.813f)
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
