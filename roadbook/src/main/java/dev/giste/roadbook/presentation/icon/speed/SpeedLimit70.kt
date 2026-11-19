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
 * Draws the 70 km/h speed limit sign, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun speedLimit70Icon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Speed.Limit70",
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
            moveTo(13.329f, 32f)
            lineTo(20.314f, 18.188f)
            verticalLineTo(18.07f)
            horizontalLineTo(12.236f)
            verticalLineTo(16f)
            horizontalLineTo(22.814f)
            verticalLineTo(18.141f)
            lineTo(15.853f, 32f)
            horizontalLineTo(13.329f)
            close()
            moveTo(29.657f, 32.266f)
            curveTo(28.423f, 32.26f, 27.368f, 31.935f, 26.493f, 31.289f)
            curveTo(25.618f, 30.643f, 24.949f, 29.703f, 24.485f, 28.469f)
            curveTo(24.022f, 27.234f, 23.79f, 25.747f, 23.79f, 24.008f)
            curveTo(23.79f, 22.273f, 24.022f, 20.792f, 24.485f, 19.563f)
            curveTo(24.954f, 18.333f, 25.626f, 17.396f, 26.501f, 16.75f)
            curveTo(27.381f, 16.104f, 28.433f, 15.781f, 29.657f, 15.781f)
            curveTo(30.881f, 15.781f, 31.931f, 16.107f, 32.806f, 16.758f)
            curveTo(33.681f, 17.404f, 34.35f, 18.341f, 34.813f, 19.57f)
            curveTo(35.282f, 20.794f, 35.516f, 22.273f, 35.516f, 24.008f)
            curveTo(35.516f, 25.753f, 35.285f, 27.242f, 34.821f, 28.477f)
            curveTo(34.358f, 29.706f, 33.688f, 30.646f, 32.813f, 31.297f)
            curveTo(31.938f, 31.943f, 30.886f, 32.266f, 29.657f, 32.266f)
            close()
            moveTo(29.657f, 30.18f)
            curveTo(30.74f, 30.18f, 31.587f, 29.651f, 32.196f, 28.594f)
            curveTo(32.811f, 27.537f, 33.118f, 26.008f, 33.118f, 24.008f)
            curveTo(33.118f, 22.68f, 32.977f, 21.557f, 32.696f, 20.641f)
            curveTo(32.42f, 19.719f, 32.022f, 19.021f, 31.501f, 18.547f)
            curveTo(30.985f, 18.068f, 30.371f, 17.828f, 29.657f, 17.828f)
            curveTo(28.579f, 17.828f, 27.733f, 18.359f, 27.118f, 19.422f)
            curveTo(26.503f, 20.484f, 26.194f, 22.013f, 26.188f, 24.008f)
            curveTo(26.188f, 25.341f, 26.326f, 26.469f, 26.602f, 27.391f)
            curveTo(26.884f, 28.307f, 27.282f, 29.003f, 27.798f, 29.477f)
            curveTo(28.313f, 29.945f, 28.933f, 30.18f, 29.657f, 30.18f)
            close()
        }
    }
}.build()
