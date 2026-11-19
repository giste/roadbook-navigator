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
 * Draws the 120 km/h speed limit sign, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun speedLimit120Icon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Speed.Limit120",
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
            moveTo(13.663f, 16f)
            verticalLineTo(32f)
            horizontalLineTo(11.242f)
            verticalLineTo(18.422f)
            horizontalLineTo(11.148f)
            lineTo(7.32f, 20.922f)
            verticalLineTo(18.609f)
            lineTo(11.312f, 16f)
            horizontalLineTo(13.663f)
            close()
            moveTo(16.892f, 32f)
            verticalLineTo(30.25f)
            lineTo(22.306f, 24.641f)
            curveTo(22.884f, 24.031f, 23.36f, 23.497f, 23.735f, 23.039f)
            curveTo(24.116f, 22.576f, 24.399f, 22.135f, 24.587f, 21.719f)
            curveTo(24.774f, 21.302f, 24.868f, 20.859f, 24.868f, 20.391f)
            curveTo(24.868f, 19.859f, 24.743f, 19.401f, 24.493f, 19.016f)
            curveTo(24.243f, 18.625f, 23.902f, 18.326f, 23.47f, 18.117f)
            curveTo(23.037f, 17.904f, 22.551f, 17.797f, 22.009f, 17.797f)
            curveTo(21.436f, 17.797f, 20.936f, 17.914f, 20.509f, 18.148f)
            curveTo(20.082f, 18.383f, 19.754f, 18.713f, 19.524f, 19.141f)
            curveTo(19.295f, 19.568f, 19.181f, 20.068f, 19.181f, 20.641f)
            horizontalLineTo(16.876f)
            curveTo(16.876f, 19.667f, 17.1f, 18.815f, 17.548f, 18.086f)
            curveTo(17.996f, 17.357f, 18.61f, 16.792f, 19.392f, 16.391f)
            curveTo(20.173f, 15.984f, 21.061f, 15.781f, 22.056f, 15.781f)
            curveTo(23.061f, 15.781f, 23.946f, 15.982f, 24.712f, 16.383f)
            curveTo(25.483f, 16.779f, 26.084f, 17.32f, 26.517f, 18.008f)
            curveTo(26.949f, 18.69f, 27.165f, 19.461f, 27.165f, 20.32f)
            curveTo(27.165f, 20.914f, 27.053f, 21.495f, 26.829f, 22.063f)
            curveTo(26.61f, 22.63f, 26.228f, 23.263f, 25.681f, 23.961f)
            curveTo(25.134f, 24.654f, 24.373f, 25.495f, 23.399f, 26.484f)
            lineTo(20.22f, 29.813f)
            verticalLineTo(29.93f)
            horizontalLineTo(27.423f)
            verticalLineTo(32f)
            horizontalLineTo(16.892f)
            close()
            moveTo(34.854f, 32.266f)
            curveTo(33.62f, 32.26f, 32.565f, 31.935f, 31.69f, 31.289f)
            curveTo(30.815f, 30.643f, 30.146f, 29.703f, 29.682f, 28.469f)
            curveTo(29.219f, 27.234f, 28.987f, 25.747f, 28.987f, 24.008f)
            curveTo(28.987f, 22.273f, 29.219f, 20.792f, 29.682f, 19.563f)
            curveTo(30.151f, 18.333f, 30.823f, 17.396f, 31.698f, 16.75f)
            curveTo(32.578f, 16.104f, 33.63f, 15.781f, 34.854f, 15.781f)
            curveTo(36.078f, 15.781f, 37.127f, 16.107f, 38.002f, 16.758f)
            curveTo(38.877f, 17.404f, 39.547f, 18.341f, 40.01f, 19.57f)
            curveTo(40.479f, 20.794f, 40.714f, 22.273f, 40.714f, 24.008f)
            curveTo(40.714f, 25.753f, 40.482f, 27.242f, 40.018f, 28.477f)
            curveTo(39.555f, 29.706f, 38.885f, 30.646f, 38.01f, 31.297f)
            curveTo(37.135f, 31.943f, 36.083f, 32.266f, 34.854f, 32.266f)
            close()
            moveTo(34.854f, 30.18f)
            curveTo(35.937f, 30.18f, 36.784f, 29.651f, 37.393f, 28.594f)
            curveTo(38.008f, 27.537f, 38.315f, 26.008f, 38.315f, 24.008f)
            curveTo(38.315f, 22.68f, 38.174f, 21.557f, 37.893f, 20.641f)
            curveTo(37.617f, 19.719f, 37.219f, 19.021f, 36.698f, 18.547f)
            curveTo(36.182f, 18.068f, 35.568f, 17.828f, 34.854f, 17.828f)
            curveTo(33.776f, 17.828f, 32.93f, 18.359f, 32.315f, 19.422f)
            curveTo(31.701f, 20.484f, 31.391f, 22.013f, 31.385f, 24.008f)
            curveTo(31.385f, 25.341f, 31.523f, 26.469f, 31.799f, 27.391f)
            curveTo(32.081f, 28.307f, 32.479f, 29.003f, 32.995f, 29.477f)
            curveTo(33.51f, 29.945f, 34.13f, 30.18f, 34.854f, 30.18f)
            close()
        }
    }
}.build()
