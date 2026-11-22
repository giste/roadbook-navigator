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
 * Draws the 100 km/h speed limit sign, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun speedLimit100Icon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Speed.Limit100",
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
            moveTo(13.309f, 16f)
            verticalLineTo(32f)
            horizontalLineTo(10.887f)
            verticalLineTo(18.422f)
            horizontalLineTo(10.793f)
            lineTo(6.965f, 20.922f)
            verticalLineTo(18.609f)
            lineTo(10.957f, 16f)
            horizontalLineTo(13.309f)
            close()
            moveTo(22.107f, 32.266f)
            curveTo(20.873f, 32.26f, 19.818f, 31.935f, 18.943f, 31.289f)
            curveTo(18.068f, 30.643f, 17.399f, 29.703f, 16.935f, 28.469f)
            curveTo(16.472f, 27.234f, 16.24f, 25.747f, 16.24f, 24.008f)
            curveTo(16.24f, 22.273f, 16.472f, 20.792f, 16.935f, 19.563f)
            curveTo(17.404f, 18.333f, 18.076f, 17.396f, 18.951f, 16.75f)
            curveTo(19.831f, 16.104f, 20.883f, 15.781f, 22.107f, 15.781f)
            curveTo(23.331f, 15.781f, 24.381f, 16.107f, 25.256f, 16.758f)
            curveTo(26.131f, 17.404f, 26.8f, 18.341f, 27.264f, 19.57f)
            curveTo(27.732f, 20.794f, 27.967f, 22.273f, 27.967f, 24.008f)
            curveTo(27.967f, 25.753f, 27.735f, 27.242f, 27.271f, 28.477f)
            curveTo(26.808f, 29.706f, 26.139f, 30.646f, 25.264f, 31.297f)
            curveTo(24.389f, 31.943f, 23.337f, 32.266f, 22.107f, 32.266f)
            close()
            moveTo(22.107f, 30.18f)
            curveTo(23.191f, 30.18f, 24.037f, 29.651f, 24.646f, 28.594f)
            curveTo(25.261f, 27.537f, 25.568f, 26.008f, 25.568f, 24.008f)
            curveTo(25.568f, 22.68f, 25.428f, 21.557f, 25.146f, 20.641f)
            curveTo(24.87f, 19.719f, 24.472f, 19.021f, 23.951f, 18.547f)
            curveTo(23.435f, 18.068f, 22.821f, 17.828f, 22.107f, 17.828f)
            curveTo(21.029f, 17.828f, 20.183f, 18.359f, 19.568f, 19.422f)
            curveTo(18.954f, 20.484f, 18.644f, 22.013f, 18.639f, 24.008f)
            curveTo(18.639f, 25.341f, 18.777f, 26.469f, 19.053f, 27.391f)
            curveTo(19.334f, 28.307f, 19.732f, 29.003f, 20.248f, 29.477f)
            curveTo(20.764f, 29.945f, 21.383f, 30.18f, 22.107f, 30.18f)
            close()
            moveTo(35.209f, 32.266f)
            curveTo(33.974f, 32.26f, 32.919f, 31.935f, 32.044f, 31.289f)
            curveTo(31.17f, 30.643f, 30.5f, 29.703f, 30.037f, 28.469f)
            curveTo(29.573f, 27.234f, 29.341f, 25.747f, 29.341f, 24.008f)
            curveTo(29.341f, 22.273f, 29.573f, 20.792f, 30.037f, 19.563f)
            curveTo(30.506f, 18.333f, 31.177f, 17.396f, 32.052f, 16.75f)
            curveTo(32.933f, 16.104f, 33.985f, 15.781f, 35.209f, 15.781f)
            curveTo(36.433f, 15.781f, 37.482f, 16.107f, 38.357f, 16.758f)
            curveTo(39.232f, 17.404f, 39.901f, 18.341f, 40.365f, 19.57f)
            curveTo(40.834f, 20.794f, 41.068f, 22.273f, 41.068f, 24.008f)
            curveTo(41.068f, 25.753f, 40.836f, 27.242f, 40.373f, 28.477f)
            curveTo(39.909f, 29.706f, 39.24f, 30.646f, 38.365f, 31.297f)
            curveTo(37.49f, 31.943f, 36.438f, 32.266f, 35.209f, 32.266f)
            close()
            moveTo(35.209f, 30.18f)
            curveTo(36.292f, 30.18f, 37.138f, 29.651f, 37.748f, 28.594f)
            curveTo(38.362f, 27.537f, 38.669f, 26.008f, 38.669f, 24.008f)
            curveTo(38.669f, 22.68f, 38.529f, 21.557f, 38.248f, 20.641f)
            curveTo(37.972f, 19.719f, 37.573f, 19.021f, 37.052f, 18.547f)
            curveTo(36.537f, 18.068f, 35.922f, 17.828f, 35.209f, 17.828f)
            curveTo(34.131f, 17.828f, 33.284f, 18.359f, 32.669f, 19.422f)
            curveTo(32.055f, 20.484f, 31.745f, 22.013f, 31.74f, 24.008f)
            curveTo(31.74f, 25.341f, 31.878f, 26.469f, 32.154f, 27.391f)
            curveTo(32.435f, 28.307f, 32.834f, 29.003f, 33.349f, 29.477f)
            curveTo(33.865f, 29.945f, 34.485f, 30.18f, 35.209f, 30.18f)
            close()
        }
    }
}.build()
