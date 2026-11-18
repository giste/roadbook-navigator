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
 * Draws the 20 km/h speed limit sign, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun speedLimit20Icon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Speed.Limit20",
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
            moveTo(12.242f, 32f)
            verticalLineTo(30.25f)
            lineTo(17.656f, 24.641f)
            curveTo(18.235f, 24.031f, 18.711f, 23.497f, 19.086f, 23.039f)
            curveTo(19.466f, 22.576f, 19.75f, 22.135f, 19.938f, 21.719f)
            curveTo(20.125f, 21.302f, 20.219f, 20.859f, 20.219f, 20.391f)
            curveTo(20.219f, 19.859f, 20.094f, 19.401f, 19.844f, 19.016f)
            curveTo(19.594f, 18.625f, 19.253f, 18.326f, 18.82f, 18.117f)
            curveTo(18.388f, 17.904f, 17.901f, 17.797f, 17.36f, 17.797f)
            curveTo(16.787f, 17.797f, 16.287f, 17.914f, 15.86f, 18.148f)
            curveTo(15.432f, 18.383f, 15.104f, 18.713f, 14.875f, 19.141f)
            curveTo(14.646f, 19.568f, 14.531f, 20.068f, 14.531f, 20.641f)
            horizontalLineTo(12.227f)
            curveTo(12.227f, 19.667f, 12.451f, 18.815f, 12.899f, 18.086f)
            curveTo(13.347f, 17.357f, 13.961f, 16.792f, 14.742f, 16.391f)
            curveTo(15.524f, 15.984f, 16.412f, 15.781f, 17.406f, 15.781f)
            curveTo(18.412f, 15.781f, 19.297f, 15.982f, 20.063f, 16.383f)
            curveTo(20.833f, 16.779f, 21.435f, 17.32f, 21.867f, 18.008f)
            curveTo(22.3f, 18.69f, 22.516f, 19.461f, 22.516f, 20.32f)
            curveTo(22.516f, 20.914f, 22.404f, 21.495f, 22.18f, 22.063f)
            curveTo(21.961f, 22.63f, 21.578f, 23.263f, 21.031f, 23.961f)
            curveTo(20.485f, 24.654f, 19.724f, 25.495f, 18.75f, 26.484f)
            lineTo(15.571f, 29.813f)
            verticalLineTo(29.93f)
            horizontalLineTo(22.774f)
            verticalLineTo(32f)
            horizontalLineTo(12.242f)
            close()
            moveTo(30.205f, 32.266f)
            curveTo(28.971f, 32.26f, 27.916f, 31.935f, 27.041f, 31.289f)
            curveTo(26.166f, 30.643f, 25.497f, 29.703f, 25.033f, 28.469f)
            curveTo(24.569f, 27.234f, 24.338f, 25.747f, 24.338f, 24.008f)
            curveTo(24.338f, 22.273f, 24.569f, 20.792f, 25.033f, 19.563f)
            curveTo(25.502f, 18.333f, 26.174f, 17.396f, 27.049f, 16.75f)
            curveTo(27.929f, 16.104f, 28.981f, 15.781f, 30.205f, 15.781f)
            curveTo(31.429f, 15.781f, 32.478f, 16.107f, 33.353f, 16.758f)
            curveTo(34.228f, 17.404f, 34.898f, 18.341f, 35.361f, 19.57f)
            curveTo(35.83f, 20.794f, 36.064f, 22.273f, 36.064f, 24.008f)
            curveTo(36.064f, 25.753f, 35.833f, 27.242f, 35.369f, 28.477f)
            curveTo(34.905f, 29.706f, 34.236f, 30.646f, 33.361f, 31.297f)
            curveTo(32.486f, 31.943f, 31.434f, 32.266f, 30.205f, 32.266f)
            close()
            moveTo(30.205f, 30.18f)
            curveTo(31.288f, 30.18f, 32.135f, 29.651f, 32.744f, 28.594f)
            curveTo(33.359f, 27.537f, 33.666f, 26.008f, 33.666f, 24.008f)
            curveTo(33.666f, 22.68f, 33.525f, 21.557f, 33.244f, 20.641f)
            curveTo(32.968f, 19.719f, 32.569f, 19.021f, 32.049f, 18.547f)
            curveTo(31.533f, 18.068f, 30.918f, 17.828f, 30.205f, 17.828f)
            curveTo(29.127f, 17.828f, 28.28f, 18.359f, 27.666f, 19.422f)
            curveTo(27.051f, 20.484f, 26.741f, 22.013f, 26.736f, 24.008f)
            curveTo(26.736f, 25.341f, 26.874f, 26.469f, 27.15f, 27.391f)
            curveTo(27.431f, 28.307f, 27.83f, 29.003f, 28.346f, 29.477f)
            curveTo(28.861f, 29.945f, 29.481f, 30.18f, 30.205f, 30.18f)
            close()
        }
    }
}.build()
