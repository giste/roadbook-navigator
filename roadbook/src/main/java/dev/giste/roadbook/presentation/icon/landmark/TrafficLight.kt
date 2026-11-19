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

package dev.giste.roadbook.presentation.icon.landmark

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import dev.giste.roadbook.presentation.icon.IconColors

/**
 * Draws a traffic light, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun trafficLightIcon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Landmark.TrafficLight",
    defaultWidth = 48.dp,
    defaultHeight = 48.dp,
    viewportWidth = 48f,
    viewportHeight = 48f
).apply {
    path(fill = SolidColor(colors.onSurface)) {
        moveTo(23f, 0f)
        lineTo(23f, 0f)
        arcTo(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 28f, 5f)
        lineTo(28f, 23f)
        arcTo(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 23f, 28f)
        lineTo(23f, 28f)
        arcTo(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 18f, 23f)
        lineTo(18f, 5f)
        arcTo(5f, 5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 23f, 0f)
        close()
    }
    path(fill = SolidColor(colors.surface)) {
        moveTo(23f, 5f)
        moveToRelative(-4f, 0f)
        arcToRelative(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 8f, 0f)
        arcToRelative(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, -8f, 0f)
    }
    path(fill = SolidColor(colors.surface)) {
        moveTo(23f, 14f)
        moveToRelative(-4f, 0f)
        arcToRelative(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 8f, 0f)
        arcToRelative(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, -8f, 0f)
    }
    path(fill = SolidColor(colors.surface)) {
        moveTo(23f, 23f)
        moveToRelative(-4f, 0f)
        arcToRelative(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, 8f, 0f)
        arcToRelative(4f, 4f, 0f, isMoreThanHalf = true, isPositiveArc = true, -8f, 0f)
    }
    path(
        stroke = SolidColor(colors.onSurface),
        strokeLineWidth = 2f
    ) {
        moveTo(23f, 27f)
        verticalLineTo(47f)
        moveTo(16f, 47f)
        horizontalLineTo(30f)
    }
}.build()
