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

package dev.giste.roadbook.presentation.icon.sign

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import dev.giste.roadbook.presentation.icon.FixedIconColors
import dev.giste.roadbook.presentation.icon.IconColors

/**
 * Draws the roundabout sign, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun roundaboutIcon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Sign.Roundabout",
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
            fill = SolidColor(FixedIconColors.SIGN_BLUE),
            stroke = SolidColor(colors.onSurface),
            strokeLineWidth = 1f
        ) {
            moveTo(24f, 24f)
            moveToRelative(-23f, 0f)
            arcToRelative(23f, 23f, 0f, isMoreThanHalf = true, isPositiveArc = true, 46f, 0f)
            arcToRelative(23f, 23f, 0f, isMoreThanHalf = true, isPositiveArc = true, -46f, 0f)
        }
    }
    group(
        clipPathData = PathData {
            moveTo(40f, 24f)
            curveTo(40f, 27.379f, 38.93f, 30.671f, 36.944f, 33.405f)
            curveTo(34.958f, 36.138f, 32.158f, 38.173f, 28.944f, 39.217f)
            lineTo(27.56f, 34.956f)
            curveTo(29.874f, 34.204f, 31.89f, 32.739f, 33.32f, 30.771f)
            curveTo(34.75f, 28.803f, 35.52f, 26.433f, 35.52f, 24f)
            horizontalLineTo(40f)
            close()
        }
    ) {
        path(
            stroke = SolidColor(FixedIconColors.SIGN_WHITE),
            strokeLineWidth = 12f
        ) {
            moveTo(40f, 24f)
            curveTo(40f, 27.379f, 38.93f, 30.671f, 36.944f, 33.405f)
            curveTo(34.958f, 36.138f, 32.158f, 38.173f, 28.944f, 39.217f)
            lineTo(27.56f, 34.956f)
            curveTo(29.874f, 34.204f, 31.89f, 32.739f, 33.32f, 30.771f)
            curveTo(34.75f, 28.803f, 35.52f, 26.433f, 35.52f, 24f)
            horizontalLineTo(40f)
            close()
        }
    }
    group(
        clipPathData = PathData {
            moveTo(16f, 37.856f)
            curveTo(13.074f, 36.167f, 10.758f, 33.595f, 9.383f, 30.508f)
            curveTo(8.009f, 27.421f, 7.647f, 23.978f, 8.35f, 20.673f)
            lineTo(13.201f, 21.705f)
            curveTo(12.717f, 23.985f, 12.966f, 26.361f, 13.915f, 28.49f)
            curveTo(14.863f, 30.62f, 16.461f, 32.395f, 18.48f, 33.561f)
            lineTo(16f, 37.856f)
            close()
        }
    ) {
        path(
            stroke = SolidColor(FixedIconColors.SIGN_WHITE),
            strokeLineWidth = 12f
        ) {
            moveTo(16f, 37.856f)
            curveTo(13.074f, 36.167f, 10.758f, 33.595f, 9.383f, 30.508f)
            curveTo(8.009f, 27.421f, 7.647f, 23.978f, 8.35f, 20.673f)
            lineTo(13.201f, 21.705f)
            curveTo(12.717f, 23.985f, 12.966f, 26.361f, 13.915f, 28.49f)
            curveTo(14.863f, 30.62f, 16.461f, 32.395f, 18.48f, 33.561f)
            lineTo(16f, 37.856f)
            close()
        }
    }
    group(
        clipPathData = PathData {
            moveTo(17f, 10.144f)
            curveTo(19.926f, 8.454f, 23.312f, 7.734f, 26.673f, 8.088f)
            curveTo(30.033f, 8.441f, 33.195f, 9.849f, 35.706f, 12.11f)
            lineTo(32.387f, 15.796f)
            curveTo(30.655f, 14.236f, 28.473f, 13.264f, 26.154f, 13.021f)
            curveTo(23.835f, 12.777f, 21.499f, 13.273f, 19.48f, 14.439f)
            lineTo(17f, 10.144f)
            close()
        }
    ) {
        path(
            stroke = SolidColor(FixedIconColors.SIGN_WHITE),
            strokeLineWidth = 12f
        ) {
            moveTo(17f, 10.144f)
            curveTo(19.926f, 8.454f, 23.312f, 7.734f, 26.673f, 8.088f)
            curveTo(30.033f, 8.441f, 33.195f, 9.849f, 35.706f, 12.11f)
            lineTo(32.387f, 15.796f)
            curveTo(30.655f, 14.236f, 28.473f, 13.264f, 26.154f, 13.021f)
            curveTo(23.835f, 12.777f, 21.499f, 13.273f, 19.48f, 14.439f)
            lineTo(17f, 10.144f)
            close()
        }
    }
    path(fill = SolidColor(FixedIconColors.SIGN_WHITE)) {
        moveTo(37.5f, 17f)
        lineTo(43.995f, 24.5f)
        lineTo(31.005f, 24.5f)
        lineTo(37.5f, 17f)
        close()
    }
    path(fill = SolidColor(FixedIconColors.SIGN_WHITE)) {
        moveTo(23.41f, 39.295f)
        lineTo(13.667f, 41.17f)
        lineTo(20.163f, 29.92f)
        lineTo(23.41f, 39.295f)
        close()
    }
    path(fill = SolidColor(FixedIconColors.SIGN_WHITE)) {
        moveTo(11.75f, 15.995f)
        lineTo(14.998f, 6.62f)
        lineTo(21.493f, 17.87f)
        lineTo(11.75f, 15.995f)
        close()
    }
}.build()
