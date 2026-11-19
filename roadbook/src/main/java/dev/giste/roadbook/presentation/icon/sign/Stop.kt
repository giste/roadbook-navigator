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
 * Draws the stop sign, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun stopIcon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Sign.Stop",
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
            strokeLineWidth = 1f,
        ) {
            moveTo(14.433f, 0.903f)
            lineTo(33.567f, 0.903f)
            lineTo(47.097f, 14.433f)
            lineTo(47.097f, 33.567f)
            lineTo(33.567f, 47.097f)
            lineTo(14.433f, 47.097f)
            lineTo(0.903f, 33.567f)
            lineTo(0.903f, 14.433f)
            lineTo(14.433f, 0.903f)
            close()
        }
//                path(
//                    stroke = SolidColor(FixedIconColors.SIGN_WHITE),
//                    strokeLineWidth = 1f
//                ) {
//                    moveTo(32.977f, 2.326f)
//                    lineTo(45.672f, 15.023f)
//                    lineTo(45.673f, 32.977f)
//                    lineTo(32.977f, 45.672f)
//                    lineTo(15.022f, 45.674f)
//                    lineTo(2.327f, 32.977f)
//                    lineTo(2.326f, 15.022f)
//                    lineTo(15.023f, 2.327f)
//                    lineTo(32.977f, 2.326f)
//                    close()
//                }
        path(fill = SolidColor(FixedIconColors.SIGN_WHITE)) {
            moveTo(10.609f, 21.273f)
            curveTo(10.54f, 20.697f, 10.264f, 20.25f, 9.779f, 19.932f)
            curveTo(9.294f, 19.614f, 8.7f, 19.455f, 7.995f, 19.455f)
            curveTo(7.48f, 19.455f, 7.029f, 19.538f, 6.643f, 19.705f)
            curveTo(6.26f, 19.871f, 5.961f, 20.1f, 5.745f, 20.392f)
            curveTo(5.533f, 20.684f, 5.427f, 21.015f, 5.427f, 21.386f)
            curveTo(5.427f, 21.697f, 5.501f, 21.964f, 5.648f, 22.188f)
            curveTo(5.8f, 22.407f, 5.993f, 22.591f, 6.228f, 22.739f)
            curveTo(6.463f, 22.883f, 6.709f, 23.002f, 6.967f, 23.097f)
            curveTo(7.224f, 23.188f, 7.461f, 23.261f, 7.677f, 23.318f)
            lineTo(8.859f, 23.636f)
            curveTo(9.162f, 23.716f, 9.499f, 23.826f, 9.87f, 23.966f)
            curveTo(10.245f, 24.106f, 10.603f, 24.297f, 10.944f, 24.54f)
            curveTo(11.289f, 24.778f, 11.573f, 25.085f, 11.796f, 25.46f)
            curveTo(12.02f, 25.835f, 12.131f, 26.295f, 12.131f, 26.841f)
            curveTo(12.131f, 27.47f, 11.967f, 28.038f, 11.637f, 28.545f)
            curveTo(11.311f, 29.053f, 10.834f, 29.456f, 10.205f, 29.756f)
            curveTo(9.58f, 30.055f, 8.821f, 30.205f, 7.927f, 30.205f)
            curveTo(7.094f, 30.205f, 6.372f, 30.07f, 5.762f, 29.801f)
            curveTo(5.156f, 29.532f, 4.679f, 29.157f, 4.33f, 28.676f)
            curveTo(3.986f, 28.195f, 3.79f, 27.636f, 3.745f, 27f)
            horizontalLineTo(5.2f)
            curveTo(5.237f, 27.439f, 5.385f, 27.803f, 5.643f, 28.091f)
            curveTo(5.904f, 28.375f, 6.234f, 28.587f, 6.631f, 28.727f)
            curveTo(7.033f, 28.864f, 7.465f, 28.932f, 7.927f, 28.932f)
            curveTo(8.465f, 28.932f, 8.948f, 28.845f, 9.376f, 28.67f)
            curveTo(9.804f, 28.492f, 10.143f, 28.246f, 10.393f, 27.932f)
            curveTo(10.643f, 27.614f, 10.768f, 27.242f, 10.768f, 26.818f)
            curveTo(10.768f, 26.432f, 10.66f, 26.117f, 10.444f, 25.875f)
            curveTo(10.228f, 25.633f, 9.944f, 25.436f, 9.592f, 25.284f)
            curveTo(9.239f, 25.133f, 8.859f, 25f, 8.45f, 24.886f)
            lineTo(7.018f, 24.477f)
            curveTo(6.109f, 24.216f, 5.389f, 23.843f, 4.859f, 23.358f)
            curveTo(4.328f, 22.873f, 4.063f, 22.239f, 4.063f, 21.455f)
            curveTo(4.063f, 20.803f, 4.239f, 20.235f, 4.592f, 19.75f)
            curveTo(4.948f, 19.261f, 5.425f, 18.883f, 6.023f, 18.614f)
            curveTo(6.626f, 18.341f, 7.298f, 18.205f, 8.04f, 18.205f)
            curveTo(8.79f, 18.205f, 9.457f, 18.339f, 10.04f, 18.608f)
            curveTo(10.624f, 18.873f, 11.086f, 19.237f, 11.427f, 19.699f)
            curveTo(11.771f, 20.161f, 11.953f, 20.686f, 11.972f, 21.273f)
            horizontalLineTo(10.609f)
            close()
            moveTo(13.812f, 19.614f)
            verticalLineTo(18.364f)
            horizontalLineTo(22.539f)
            verticalLineTo(19.614f)
            horizontalLineTo(18.88f)
            verticalLineTo(30f)
            horizontalLineTo(17.471f)
            verticalLineTo(19.614f)
            horizontalLineTo(13.812f)
            close()
            moveTo(34.048f, 24.182f)
            curveTo(34.048f, 25.409f, 33.826f, 26.47f, 33.383f, 27.364f)
            curveTo(32.94f, 28.258f, 32.332f, 28.947f, 31.559f, 29.432f)
            curveTo(30.786f, 29.917f, 29.904f, 30.159f, 28.911f, 30.159f)
            curveTo(27.919f, 30.159f, 27.036f, 29.917f, 26.264f, 29.432f)
            curveTo(25.491f, 28.947f, 24.883f, 28.258f, 24.44f, 27.364f)
            curveTo(23.996f, 26.47f, 23.775f, 25.409f, 23.775f, 24.182f)
            curveTo(23.775f, 22.955f, 23.996f, 21.894f, 24.44f, 21f)
            curveTo(24.883f, 20.106f, 25.491f, 19.417f, 26.264f, 18.932f)
            curveTo(27.036f, 18.447f, 27.919f, 18.205f, 28.911f, 18.205f)
            curveTo(29.904f, 18.205f, 30.786f, 18.447f, 31.559f, 18.932f)
            curveTo(32.332f, 19.417f, 32.94f, 20.106f, 33.383f, 21f)
            curveTo(33.826f, 21.894f, 34.048f, 22.955f, 34.048f, 24.182f)
            close()
            moveTo(32.684f, 24.182f)
            curveTo(32.684f, 23.174f, 32.515f, 22.324f, 32.178f, 21.631f)
            curveTo(31.845f, 20.938f, 31.392f, 20.413f, 30.82f, 20.057f)
            curveTo(30.252f, 19.701f, 29.616f, 19.523f, 28.911f, 19.523f)
            curveTo(28.207f, 19.523f, 27.568f, 19.701f, 26.996f, 20.057f)
            curveTo(26.428f, 20.413f, 25.976f, 20.938f, 25.639f, 21.631f)
            curveTo(25.305f, 22.324f, 25.139f, 23.174f, 25.139f, 24.182f)
            curveTo(25.139f, 25.189f, 25.305f, 26.04f, 25.639f, 26.733f)
            curveTo(25.976f, 27.426f, 26.428f, 27.951f, 26.996f, 28.307f)
            curveTo(27.568f, 28.663f, 28.207f, 28.841f, 28.911f, 28.841f)
            curveTo(29.616f, 28.841f, 30.252f, 28.663f, 30.82f, 28.307f)
            curveTo(31.392f, 27.951f, 31.845f, 27.426f, 32.178f, 26.733f)
            curveTo(32.515f, 26.04f, 32.684f, 25.189f, 32.684f, 24.182f)
            close()
            moveTo(36.417f, 30f)
            verticalLineTo(18.364f)
            horizontalLineTo(40.349f)
            curveTo(41.262f, 18.364f, 42.008f, 18.528f, 42.587f, 18.858f)
            curveTo(43.171f, 19.184f, 43.603f, 19.625f, 43.883f, 20.182f)
            curveTo(44.163f, 20.739f, 44.303f, 21.36f, 44.303f, 22.045f)
            curveTo(44.303f, 22.731f, 44.163f, 23.354f, 43.883f, 23.915f)
            curveTo(43.606f, 24.475f, 43.178f, 24.922f, 42.599f, 25.256f)
            curveTo(42.019f, 25.585f, 41.277f, 25.75f, 40.371f, 25.75f)
            horizontalLineTo(37.553f)
            verticalLineTo(24.5f)
            horizontalLineTo(40.326f)
            curveTo(40.951f, 24.5f, 41.453f, 24.392f, 41.832f, 24.176f)
            curveTo(42.21f, 23.96f, 42.485f, 23.669f, 42.655f, 23.301f)
            curveTo(42.83f, 22.93f, 42.917f, 22.511f, 42.917f, 22.045f)
            curveTo(42.917f, 21.58f, 42.83f, 21.163f, 42.655f, 20.795f)
            curveTo(42.485f, 20.428f, 42.209f, 20.14f, 41.826f, 19.932f)
            curveTo(41.443f, 19.72f, 40.936f, 19.614f, 40.303f, 19.614f)
            horizontalLineTo(37.826f)
            verticalLineTo(30f)
            horizontalLineTo(36.417f)
            close()
        }
    }
}.build()
