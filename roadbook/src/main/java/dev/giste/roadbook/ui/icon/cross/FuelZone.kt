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

package dev.giste.roadbook.ui.icon.cross

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import dev.giste.roadbook.ui.icon.FixedIconColors
import dev.giste.roadbook.ui.icon.IconColors

/**
 * Draws the fuel zone mark, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun fuelZoneIcon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Cross.FuelZone",
    defaultWidth = 48.dp,
    defaultHeight = 48.dp,
    viewportWidth = 48f,
    viewportHeight = 48f
).apply {
    val onBackgroundColor = SolidColor(colors.onSurface)

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
            fill = SolidColor(FixedIconColors.FUEL_BLUE),
            stroke = onBackgroundColor,
            strokeLineWidth = 3f
        ) {
            moveTo(24f, 24f)
            moveToRelative(-22.5f, 0f)
            arcToRelative(
                22.5f,
                22.5f,
                0f,
                isMoreThanHalf = true,
                isPositiveArc = true,
                45f,
                0f
            )
            arcToRelative(
                22.5f,
                22.5f,
                0f,
                isMoreThanHalf = true,
                isPositiveArc = true,
                -45f,
                0f
            )
        }
        path(fill = SolidColor(FixedIconColors.SIGN_BLACK)) {
            moveTo(32f, 27f)
            lineTo(32.711f, 26.763f)
            lineTo(32f, 27f)
            close()
            moveTo(35.192f, 17.712f)
            curveTo(35.033f, 17.329f, 34.594f, 17.148f, 34.211f, 17.308f)
            curveTo(33.829f, 17.467f, 33.648f, 17.906f, 33.808f, 18.288f)
            lineTo(34.5f, 18f)
            lineTo(35.192f, 17.712f)
            close()
            moveTo(30f, 26f)
            verticalLineTo(26.75f)
            curveTo(30.486f, 26.75f, 30.785f, 26.791f, 30.976f, 26.872f)
            curveTo(31.115f, 26.931f, 31.214f, 27.014f, 31.288f, 27.237f)
            lineTo(32f, 27f)
            lineTo(32.711f, 26.763f)
            curveTo(32.503f, 26.137f, 32.102f, 25.72f, 31.562f, 25.491f)
            curveTo(31.074f, 25.285f, 30.514f, 25.25f, 30f, 25.25f)
            verticalLineTo(26f)
            close()
            moveTo(32f, 27f)
            lineTo(31.288f, 27.237f)
            curveTo(31.495f, 27.858f, 31.496f, 28.291f, 31.442f, 28.803f)
            curveTo(31.386f, 29.322f, 31.25f, 30.041f, 31.25f, 31f)
            horizontalLineTo(32f)
            horizontalLineTo(32.75f)
            curveTo(32.75f, 30.146f, 32.863f, 29.616f, 32.933f, 28.962f)
            curveTo(33.004f, 28.303f, 33.005f, 27.642f, 32.711f, 26.763f)
            lineTo(32f, 27f)
            close()
            moveTo(32f, 31f)
            horizontalLineTo(31.25f)
            curveTo(31.25f, 31.91f, 31.188f, 32.903f, 31.219f, 33.716f)
            curveTo(31.253f, 34.591f, 31.39f, 35.457f, 31.829f, 36.335f)
            lineTo(32.5f, 36f)
            lineTo(33.171f, 35.665f)
            curveTo(32.86f, 35.043f, 32.747f, 34.409f, 32.718f, 33.659f)
            curveTo(32.687f, 32.847f, 32.75f, 32.09f, 32.75f, 31f)
            horizontalLineTo(32f)
            close()
            moveTo(32.5f, 36f)
            lineTo(31.829f, 36.335f)
            curveTo(32.26f, 37.198f, 33.237f, 37.5f, 34f, 37.5f)
            curveTo(34.763f, 37.5f, 35.74f, 37.198f, 36.171f, 36.335f)
            lineTo(35.5f, 36f)
            lineTo(34.829f, 35.665f)
            curveTo(34.76f, 35.802f, 34.487f, 36f, 34f, 36f)
            curveTo(33.513f, 36f, 33.24f, 35.802f, 33.171f, 35.665f)
            lineTo(32.5f, 36f)
            close()
            moveTo(35.5f, 36f)
            lineTo(36.171f, 36.335f)
            curveTo(36.519f, 35.64f, 36.765f, 34.492f, 36.921f, 33.156f)
            curveTo(37.081f, 31.791f, 37.156f, 30.14f, 37.115f, 28.378f)
            curveTo(37.034f, 24.868f, 36.493f, 20.833f, 35.192f, 17.712f)
            lineTo(34.5f, 18f)
            lineTo(33.808f, 18.288f)
            curveTo(35.007f, 21.167f, 35.536f, 24.992f, 35.615f, 28.412f)
            curveTo(35.655f, 30.115f, 35.582f, 31.696f, 35.431f, 32.982f)
            curveTo(35.278f, 34.297f, 35.052f, 35.22f, 34.829f, 35.665f)
            lineTo(35.5f, 36f)
            close()
        }
        path(
            stroke = SolidColor(FixedIconColors.SIGN_BLACK),
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Round
        ) {
            moveTo(34.848f, 19.774f)
            curveTo(34.71f, 19.866f, 34.361f, 20.057f, 34.071f, 20.247f)
            curveTo(33.826f, 20.408f, 33.832f, 20.747f, 33.775f, 21.009f)
            curveTo(33.709f, 21.31f, 33.684f, 21.613f, 33.683f, 22.217f)
            curveTo(33.682f, 22.565f, 33.952f, 22.791f, 34.164f, 23.097f)
            curveTo(34.401f, 23.44f, 34.735f, 23.478f, 35.039f, 23.51f)
            curveTo(35.179f, 23.557f, 35.303f, 23.621f, 35.428f, 23.656f)
            curveTo(35.496f, 23.66f, 35.574f, 23.635f, 35.653f, 23.609f)
        }
        path(fill = SolidColor(FixedIconColors.SIGN_BLACK)) {
            moveTo(16f, 24f)
            horizontalLineToRelative(14f)
            verticalLineToRelative(14f)
            horizontalLineToRelative(-14f)
            close()
        }
        path(fill = SolidColor(FixedIconColors.SIGN_BLACK)) {
            moveTo(14f, 12f)
            horizontalLineToRelative(18f)
            verticalLineToRelative(12f)
            horizontalLineToRelative(-18f)
            close()
        }
        path(fill = SolidColor(FixedIconColors.SIGN_WHITE)) {
            moveTo(17f, 14f)
            lineTo(27f, 14f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 28f, 15f)
            lineTo(28f, 21f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 27f, 22f)
            lineTo(17f, 22f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 16f, 21f)
            lineTo(16f, 15f)
            arcTo(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 17f, 14f)
            close()
        }
    }
}.build()
