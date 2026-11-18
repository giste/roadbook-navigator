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
 * Draws the 30 km/h speed limit sign, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun speedLimit30Icon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Speed.Limit30",
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
            moveTo(17.469f, 32.219f)
            curveTo(16.396f, 32.219f, 15.438f, 32.034f, 14.594f, 31.664f)
            curveTo(13.755f, 31.294f, 13.091f, 30.781f, 12.602f, 30.125f)
            curveTo(12.117f, 29.463f, 11.857f, 28.698f, 11.821f, 27.828f)
            horizontalLineTo(14.274f)
            curveTo(14.305f, 28.302f, 14.464f, 28.713f, 14.75f, 29.063f)
            curveTo(15.042f, 29.406f, 15.422f, 29.672f, 15.891f, 29.859f)
            curveTo(16.36f, 30.047f, 16.88f, 30.141f, 17.453f, 30.141f)
            curveTo(18.083f, 30.141f, 18.641f, 30.031f, 19.125f, 29.813f)
            curveTo(19.615f, 29.594f, 19.998f, 29.289f, 20.274f, 28.898f)
            curveTo(20.55f, 28.503f, 20.688f, 28.047f, 20.688f, 27.531f)
            curveTo(20.688f, 26.995f, 20.55f, 26.523f, 20.274f, 26.117f)
            curveTo(20.003f, 25.706f, 19.604f, 25.383f, 19.078f, 25.148f)
            curveTo(18.558f, 24.914f, 17.927f, 24.797f, 17.188f, 24.797f)
            horizontalLineTo(15.836f)
            verticalLineTo(22.828f)
            horizontalLineTo(17.188f)
            curveTo(17.781f, 22.828f, 18.302f, 22.721f, 18.75f, 22.508f)
            curveTo(19.203f, 22.294f, 19.558f, 21.997f, 19.813f, 21.617f)
            curveTo(20.068f, 21.232f, 20.195f, 20.781f, 20.195f, 20.266f)
            curveTo(20.195f, 19.771f, 20.083f, 19.341f, 19.86f, 18.977f)
            curveTo(19.641f, 18.607f, 19.328f, 18.318f, 18.922f, 18.109f)
            curveTo(18.521f, 17.901f, 18.047f, 17.797f, 17.5f, 17.797f)
            curveTo(16.979f, 17.797f, 16.492f, 17.893f, 16.039f, 18.086f)
            curveTo(15.591f, 18.273f, 15.227f, 18.544f, 14.946f, 18.898f)
            curveTo(14.664f, 19.247f, 14.513f, 19.667f, 14.492f, 20.156f)
            horizontalLineTo(12.156f)
            curveTo(12.182f, 19.292f, 12.438f, 18.531f, 12.922f, 17.875f)
            curveTo(13.412f, 17.219f, 14.057f, 16.706f, 14.86f, 16.336f)
            curveTo(15.662f, 15.966f, 16.552f, 15.781f, 17.531f, 15.781f)
            curveTo(18.558f, 15.781f, 19.443f, 15.982f, 20.188f, 16.383f)
            curveTo(20.938f, 16.779f, 21.516f, 17.307f, 21.922f, 17.969f)
            curveTo(22.333f, 18.63f, 22.537f, 19.354f, 22.531f, 20.141f)
            curveTo(22.537f, 21.037f, 22.287f, 21.797f, 21.781f, 22.422f)
            curveTo(21.281f, 23.047f, 20.615f, 23.466f, 19.781f, 23.68f)
            verticalLineTo(23.805f)
            curveTo(20.844f, 23.966f, 21.667f, 24.388f, 22.25f, 25.07f)
            curveTo(22.839f, 25.753f, 23.13f, 26.599f, 23.125f, 27.609f)
            curveTo(23.13f, 28.49f, 22.886f, 29.279f, 22.391f, 29.977f)
            curveTo(21.901f, 30.674f, 21.232f, 31.224f, 20.383f, 31.625f)
            curveTo(19.534f, 32.021f, 18.563f, 32.219f, 17.469f, 32.219f)
            close()
            moveTo(30.549f, 32.266f)
            curveTo(29.314f, 32.26f, 28.26f, 31.935f, 27.385f, 31.289f)
            curveTo(26.51f, 30.643f, 25.84f, 29.703f, 25.377f, 28.469f)
            curveTo(24.913f, 27.234f, 24.681f, 25.747f, 24.681f, 24.008f)
            curveTo(24.681f, 22.273f, 24.913f, 20.792f, 25.377f, 19.563f)
            curveTo(25.846f, 18.333f, 26.517f, 17.396f, 27.392f, 16.75f)
            curveTo(28.273f, 16.104f, 29.325f, 15.781f, 30.549f, 15.781f)
            curveTo(31.773f, 15.781f, 32.822f, 16.107f, 33.697f, 16.758f)
            curveTo(34.572f, 17.404f, 35.241f, 18.341f, 35.705f, 19.57f)
            curveTo(36.174f, 20.794f, 36.408f, 22.273f, 36.408f, 24.008f)
            curveTo(36.408f, 25.753f, 36.176f, 27.242f, 35.713f, 28.477f)
            curveTo(35.249f, 29.706f, 34.58f, 30.646f, 33.705f, 31.297f)
            curveTo(32.83f, 31.943f, 31.778f, 32.266f, 30.549f, 32.266f)
            close()
            moveTo(30.549f, 30.18f)
            curveTo(31.632f, 30.18f, 32.478f, 29.651f, 33.088f, 28.594f)
            curveTo(33.702f, 27.537f, 34.01f, 26.008f, 34.01f, 24.008f)
            curveTo(34.01f, 22.68f, 33.869f, 21.557f, 33.588f, 20.641f)
            curveTo(33.312f, 19.719f, 32.913f, 19.021f, 32.392f, 18.547f)
            curveTo(31.877f, 18.068f, 31.262f, 17.828f, 30.549f, 17.828f)
            curveTo(29.471f, 17.828f, 28.624f, 18.359f, 28.01f, 19.422f)
            curveTo(27.395f, 20.484f, 27.085f, 22.013f, 27.08f, 24.008f)
            curveTo(27.08f, 25.341f, 27.218f, 26.469f, 27.494f, 27.391f)
            curveTo(27.775f, 28.307f, 28.174f, 29.003f, 28.689f, 29.477f)
            curveTo(29.205f, 29.945f, 29.825f, 30.18f, 30.549f, 30.18f)
            close()
        }
    }
}.build()
