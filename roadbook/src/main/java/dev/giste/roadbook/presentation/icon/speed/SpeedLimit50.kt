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
 * Draws the 50 km/h speed limit sign, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun speedLimit50Icon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Speed.Limit50",
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
            moveTo(17.448f, 32.219f)
            curveTo(16.469f, 32.219f, 15.589f, 32.031f, 14.808f, 31.656f)
            curveTo(14.032f, 31.276f, 13.412f, 30.755f, 12.948f, 30.094f)
            curveTo(12.485f, 29.432f, 12.238f, 28.677f, 12.206f, 27.828f)
            horizontalLineTo(14.55f)
            curveTo(14.607f, 28.516f, 14.912f, 29.081f, 15.464f, 29.523f)
            curveTo(16.016f, 29.966f, 16.678f, 30.188f, 17.448f, 30.188f)
            curveTo(18.063f, 30.188f, 18.607f, 30.047f, 19.081f, 29.766f)
            curveTo(19.56f, 29.479f, 19.935f, 29.086f, 20.206f, 28.586f)
            curveTo(20.482f, 28.086f, 20.62f, 27.516f, 20.62f, 26.875f)
            curveTo(20.62f, 26.224f, 20.48f, 25.643f, 20.198f, 25.133f)
            curveTo(19.917f, 24.622f, 19.529f, 24.221f, 19.034f, 23.93f)
            curveTo(18.545f, 23.638f, 17.982f, 23.49f, 17.347f, 23.484f)
            curveTo(16.862f, 23.484f, 16.375f, 23.568f, 15.886f, 23.734f)
            curveTo(15.396f, 23.901f, 15f, 24.12f, 14.698f, 24.391f)
            lineTo(12.488f, 24.063f)
            lineTo(13.386f, 16f)
            horizontalLineTo(22.183f)
            verticalLineTo(18.07f)
            horizontalLineTo(15.394f)
            lineTo(14.886f, 22.547f)
            horizontalLineTo(14.98f)
            curveTo(15.292f, 22.245f, 15.706f, 21.992f, 16.222f, 21.789f)
            curveTo(16.743f, 21.586f, 17.3f, 21.484f, 17.894f, 21.484f)
            curveTo(18.868f, 21.484f, 19.735f, 21.716f, 20.495f, 22.18f)
            curveTo(21.261f, 22.643f, 21.862f, 23.276f, 22.3f, 24.078f)
            curveTo(22.743f, 24.875f, 22.962f, 25.792f, 22.956f, 26.828f)
            curveTo(22.962f, 27.865f, 22.727f, 28.789f, 22.253f, 29.602f)
            curveTo(21.784f, 30.414f, 21.133f, 31.055f, 20.3f, 31.523f)
            curveTo(19.472f, 31.987f, 18.521f, 32.219f, 17.448f, 32.219f)
            close()
            moveTo(30.28f, 32.266f)
            curveTo(29.046f, 32.26f, 27.991f, 31.935f, 27.116f, 31.289f)
            curveTo(26.241f, 30.643f, 25.572f, 29.703f, 25.108f, 28.469f)
            curveTo(24.645f, 27.234f, 24.413f, 25.747f, 24.413f, 24.008f)
            curveTo(24.413f, 22.273f, 24.645f, 20.792f, 25.108f, 19.563f)
            curveTo(25.577f, 18.333f, 26.249f, 17.396f, 27.124f, 16.75f)
            curveTo(28.004f, 16.104f, 29.056f, 15.781f, 30.28f, 15.781f)
            curveTo(31.504f, 15.781f, 32.554f, 16.107f, 33.429f, 16.758f)
            curveTo(34.304f, 17.404f, 34.973f, 18.341f, 35.436f, 19.57f)
            curveTo(35.905f, 20.794f, 36.139f, 22.273f, 36.139f, 24.008f)
            curveTo(36.139f, 25.753f, 35.908f, 27.242f, 35.444f, 28.477f)
            curveTo(34.981f, 29.706f, 34.311f, 30.646f, 33.436f, 31.297f)
            curveTo(32.561f, 31.943f, 31.509f, 32.266f, 30.28f, 32.266f)
            close()
            moveTo(30.28f, 30.18f)
            curveTo(31.363f, 30.18f, 32.21f, 29.651f, 32.819f, 28.594f)
            curveTo(33.434f, 27.537f, 33.741f, 26.008f, 33.741f, 24.008f)
            curveTo(33.741f, 22.68f, 33.6f, 21.557f, 33.319f, 20.641f)
            curveTo(33.043f, 19.719f, 32.645f, 19.021f, 32.124f, 18.547f)
            curveTo(31.608f, 18.068f, 30.994f, 17.828f, 30.28f, 17.828f)
            curveTo(29.202f, 17.828f, 28.356f, 18.359f, 27.741f, 19.422f)
            curveTo(27.126f, 20.484f, 26.816f, 22.013f, 26.811f, 24.008f)
            curveTo(26.811f, 25.341f, 26.949f, 26.469f, 27.225f, 27.391f)
            curveTo(27.507f, 28.307f, 27.905f, 29.003f, 28.421f, 29.477f)
            curveTo(28.936f, 29.945f, 29.556f, 30.18f, 30.28f, 30.18f)
            close()
        }
    }
}.build()
