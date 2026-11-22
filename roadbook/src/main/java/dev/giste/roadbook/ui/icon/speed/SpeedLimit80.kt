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
 * Draws the 80 km/h speed limit sign, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun speedLimit80Icon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Speed.Limit80",
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
            moveTo(17.444f, 32.219f)
            curveTo(16.325f, 32.219f, 15.335f, 32.026f, 14.476f, 31.641f)
            curveTo(13.622f, 31.255f, 12.952f, 30.724f, 12.468f, 30.047f)
            curveTo(11.984f, 29.37f, 11.744f, 28.599f, 11.749f, 27.734f)
            curveTo(11.744f, 27.057f, 11.882f, 26.435f, 12.163f, 25.867f)
            curveTo(12.45f, 25.294f, 12.838f, 24.818f, 13.327f, 24.438f)
            curveTo(13.817f, 24.052f, 14.364f, 23.807f, 14.968f, 23.703f)
            verticalLineTo(23.609f)
            curveTo(14.171f, 23.417f, 13.533f, 22.99f, 13.054f, 22.328f)
            curveTo(12.575f, 21.667f, 12.338f, 20.906f, 12.343f, 20.047f)
            curveTo(12.338f, 19.229f, 12.554f, 18.5f, 12.991f, 17.859f)
            curveTo(13.434f, 17.213f, 14.041f, 16.706f, 14.812f, 16.336f)
            curveTo(15.583f, 15.966f, 16.46f, 15.781f, 17.444f, 15.781f)
            curveTo(18.419f, 15.781f, 19.288f, 15.969f, 20.054f, 16.344f)
            curveTo(20.825f, 16.713f, 21.431f, 17.221f, 21.874f, 17.867f)
            curveTo(22.317f, 18.508f, 22.541f, 19.234f, 22.546f, 20.047f)
            curveTo(22.541f, 20.906f, 22.296f, 21.667f, 21.812f, 22.328f)
            curveTo(21.327f, 22.99f, 20.697f, 23.417f, 19.921f, 23.609f)
            verticalLineTo(23.703f)
            curveTo(20.52f, 23.807f, 21.059f, 24.052f, 21.538f, 24.438f)
            curveTo(22.023f, 24.818f, 22.408f, 25.294f, 22.694f, 25.867f)
            curveTo(22.986f, 26.435f, 23.135f, 27.057f, 23.14f, 27.734f)
            curveTo(23.135f, 28.599f, 22.89f, 29.37f, 22.406f, 30.047f)
            curveTo(21.921f, 30.724f, 21.249f, 31.255f, 20.39f, 31.641f)
            curveTo(19.536f, 32.026f, 18.554f, 32.219f, 17.444f, 32.219f)
            close()
            moveTo(17.444f, 30.242f)
            curveTo(18.106f, 30.242f, 18.679f, 30.133f, 19.163f, 29.914f)
            curveTo(19.648f, 29.69f, 20.023f, 29.38f, 20.288f, 28.984f)
            curveTo(20.554f, 28.583f, 20.689f, 28.115f, 20.694f, 27.578f)
            curveTo(20.689f, 27.021f, 20.544f, 26.529f, 20.257f, 26.102f)
            curveTo(19.976f, 25.674f, 19.593f, 25.338f, 19.109f, 25.094f)
            curveTo(18.624f, 24.849f, 18.069f, 24.727f, 17.444f, 24.727f)
            curveTo(16.814f, 24.727f, 16.254f, 24.849f, 15.765f, 25.094f)
            curveTo(15.275f, 25.338f, 14.89f, 25.674f, 14.609f, 26.102f)
            curveTo(14.327f, 26.529f, 14.189f, 27.021f, 14.194f, 27.578f)
            curveTo(14.189f, 28.115f, 14.317f, 28.583f, 14.577f, 28.984f)
            curveTo(14.843f, 29.38f, 15.221f, 29.69f, 15.71f, 29.914f)
            curveTo(16.2f, 30.133f, 16.778f, 30.242f, 17.444f, 30.242f)
            close()
            moveTo(17.444f, 22.797f)
            curveTo(17.976f, 22.797f, 18.447f, 22.69f, 18.859f, 22.477f)
            curveTo(19.27f, 22.263f, 19.593f, 21.966f, 19.827f, 21.586f)
            curveTo(20.067f, 21.206f, 20.189f, 20.76f, 20.194f, 20.25f)
            curveTo(20.189f, 19.75f, 20.069f, 19.313f, 19.835f, 18.938f)
            curveTo(19.606f, 18.563f, 19.286f, 18.273f, 18.874f, 18.07f)
            curveTo(18.463f, 17.862f, 17.986f, 17.758f, 17.444f, 17.758f)
            curveTo(16.892f, 17.758f, 16.408f, 17.862f, 15.991f, 18.07f)
            curveTo(15.58f, 18.273f, 15.26f, 18.563f, 15.031f, 18.938f)
            curveTo(14.801f, 19.313f, 14.689f, 19.75f, 14.694f, 20.25f)
            curveTo(14.689f, 20.76f, 14.804f, 21.206f, 15.038f, 21.586f)
            curveTo(15.273f, 21.966f, 15.596f, 22.263f, 16.007f, 22.477f)
            curveTo(16.424f, 22.69f, 16.903f, 22.797f, 17.444f, 22.797f)
            close()
            moveTo(30.409f, 32.266f)
            curveTo(29.175f, 32.26f, 28.12f, 31.935f, 27.245f, 31.289f)
            curveTo(26.37f, 30.643f, 25.701f, 29.703f, 25.237f, 28.469f)
            curveTo(24.774f, 27.234f, 24.542f, 25.747f, 24.542f, 24.008f)
            curveTo(24.542f, 22.273f, 24.774f, 20.792f, 25.237f, 19.563f)
            curveTo(25.706f, 18.333f, 26.378f, 17.396f, 27.253f, 16.75f)
            curveTo(28.133f, 16.104f, 29.185f, 15.781f, 30.409f, 15.781f)
            curveTo(31.633f, 15.781f, 32.682f, 16.107f, 33.557f, 16.758f)
            curveTo(34.432f, 17.404f, 35.102f, 18.341f, 35.565f, 19.57f)
            curveTo(36.034f, 20.794f, 36.268f, 22.273f, 36.268f, 24.008f)
            curveTo(36.268f, 25.753f, 36.037f, 27.242f, 35.573f, 28.477f)
            curveTo(35.11f, 29.706f, 34.44f, 30.646f, 33.565f, 31.297f)
            curveTo(32.69f, 31.943f, 31.638f, 32.266f, 30.409f, 32.266f)
            close()
            moveTo(30.409f, 30.18f)
            curveTo(31.492f, 30.18f, 32.339f, 29.651f, 32.948f, 28.594f)
            curveTo(33.563f, 27.537f, 33.87f, 26.008f, 33.87f, 24.008f)
            curveTo(33.87f, 22.68f, 33.729f, 21.557f, 33.448f, 20.641f)
            curveTo(33.172f, 19.719f, 32.774f, 19.021f, 32.253f, 18.547f)
            curveTo(31.737f, 18.068f, 31.122f, 17.828f, 30.409f, 17.828f)
            curveTo(29.331f, 17.828f, 28.485f, 18.359f, 27.87f, 19.422f)
            curveTo(27.255f, 20.484f, 26.945f, 22.013f, 26.94f, 24.008f)
            curveTo(26.94f, 25.341f, 27.078f, 26.469f, 27.354f, 27.391f)
            curveTo(27.635f, 28.307f, 28.034f, 29.003f, 28.55f, 29.477f)
            curveTo(29.065f, 29.945f, 29.685f, 30.18f, 30.409f, 30.18f)
            close()
        }
    }
}.build()
