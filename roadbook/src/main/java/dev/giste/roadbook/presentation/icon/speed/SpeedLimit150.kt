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
 * Draws the 150 km/h speed limit sign, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun speedLimit150Icon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Speed.Limit150",
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
            moveTo(13.588f, 16f)
            verticalLineTo(32f)
            horizontalLineTo(11.166f)
            verticalLineTo(18.422f)
            horizontalLineTo(11.073f)
            lineTo(7.245f, 20.922f)
            verticalLineTo(18.609f)
            lineTo(11.237f, 16f)
            horizontalLineTo(13.588f)
            close()
            moveTo(22.098f, 32.219f)
            curveTo(21.118f, 32.219f, 20.238f, 32.031f, 19.457f, 31.656f)
            curveTo(18.681f, 31.276f, 18.061f, 30.755f, 17.598f, 30.094f)
            curveTo(17.134f, 29.432f, 16.887f, 28.677f, 16.855f, 27.828f)
            horizontalLineTo(19.199f)
            curveTo(19.257f, 28.516f, 19.561f, 29.081f, 20.113f, 29.523f)
            curveTo(20.665f, 29.966f, 21.327f, 30.188f, 22.098f, 30.188f)
            curveTo(22.712f, 30.188f, 23.257f, 30.047f, 23.73f, 29.766f)
            curveTo(24.21f, 29.479f, 24.585f, 29.086f, 24.855f, 28.586f)
            curveTo(25.132f, 28.086f, 25.27f, 27.516f, 25.27f, 26.875f)
            curveTo(25.27f, 26.224f, 25.129f, 25.643f, 24.848f, 25.133f)
            curveTo(24.566f, 24.622f, 24.178f, 24.221f, 23.684f, 23.93f)
            curveTo(23.194f, 23.638f, 22.632f, 23.49f, 21.996f, 23.484f)
            curveTo(21.512f, 23.484f, 21.025f, 23.568f, 20.535f, 23.734f)
            curveTo(20.046f, 23.901f, 19.65f, 24.12f, 19.348f, 24.391f)
            lineTo(17.137f, 24.063f)
            lineTo(18.035f, 16f)
            horizontalLineTo(26.832f)
            verticalLineTo(18.07f)
            horizontalLineTo(20.043f)
            lineTo(19.535f, 22.547f)
            horizontalLineTo(19.629f)
            curveTo(19.941f, 22.245f, 20.355f, 21.992f, 20.871f, 21.789f)
            curveTo(21.392f, 21.586f, 21.949f, 21.484f, 22.543f, 21.484f)
            curveTo(23.517f, 21.484f, 24.384f, 21.716f, 25.145f, 22.18f)
            curveTo(25.91f, 22.643f, 26.512f, 23.276f, 26.949f, 24.078f)
            curveTo(27.392f, 24.875f, 27.611f, 25.792f, 27.605f, 26.828f)
            curveTo(27.611f, 27.865f, 27.376f, 28.789f, 26.902f, 29.602f)
            curveTo(26.434f, 30.414f, 25.783f, 31.055f, 24.949f, 31.523f)
            curveTo(24.121f, 31.987f, 23.171f, 32.219f, 22.098f, 32.219f)
            close()
            moveTo(34.929f, 32.266f)
            curveTo(33.695f, 32.26f, 32.64f, 31.935f, 31.765f, 31.289f)
            curveTo(30.89f, 30.643f, 30.221f, 29.703f, 29.757f, 28.469f)
            curveTo(29.294f, 27.234f, 29.062f, 25.747f, 29.062f, 24.008f)
            curveTo(29.062f, 22.273f, 29.294f, 20.792f, 29.757f, 19.563f)
            curveTo(30.226f, 18.333f, 30.898f, 17.396f, 31.773f, 16.75f)
            curveTo(32.653f, 16.104f, 33.705f, 15.781f, 34.929f, 15.781f)
            curveTo(36.153f, 15.781f, 37.203f, 16.107f, 38.078f, 16.758f)
            curveTo(38.953f, 17.404f, 39.622f, 18.341f, 40.085f, 19.57f)
            curveTo(40.554f, 20.794f, 40.789f, 22.273f, 40.789f, 24.008f)
            curveTo(40.789f, 25.753f, 40.557f, 27.242f, 40.093f, 28.477f)
            curveTo(39.63f, 29.706f, 38.96f, 30.646f, 38.085f, 31.297f)
            curveTo(37.21f, 31.943f, 36.159f, 32.266f, 34.929f, 32.266f)
            close()
            moveTo(34.929f, 30.18f)
            curveTo(36.013f, 30.18f, 36.859f, 29.651f, 37.468f, 28.594f)
            curveTo(38.083f, 27.537f, 38.39f, 26.008f, 38.39f, 24.008f)
            curveTo(38.39f, 22.68f, 38.25f, 21.557f, 37.968f, 20.641f)
            curveTo(37.692f, 19.719f, 37.294f, 19.021f, 36.773f, 18.547f)
            curveTo(36.257f, 18.068f, 35.643f, 17.828f, 34.929f, 17.828f)
            curveTo(33.851f, 17.828f, 33.005f, 18.359f, 32.39f, 19.422f)
            curveTo(31.776f, 20.484f, 31.466f, 22.013f, 31.461f, 24.008f)
            curveTo(31.461f, 25.341f, 31.599f, 26.469f, 31.875f, 27.391f)
            curveTo(32.156f, 28.307f, 32.554f, 29.003f, 33.07f, 29.477f)
            curveTo(33.585f, 29.945f, 34.205f, 30.18f, 34.929f, 30.18f)
            close()
        }
    }
}.build()
