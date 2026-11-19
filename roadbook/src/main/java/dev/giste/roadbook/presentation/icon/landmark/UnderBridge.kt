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
 * Draws a bridge the route passes under, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun underBridgeIcon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Landmark.UnderBridge",
    defaultWidth = 48.dp,
    defaultHeight = 48.dp,
    viewportWidth = 48f,
    viewportHeight = 48f
).apply {
    // Bridge floor
    path(fill = SolidColor(colors.surface)) {
        moveTo(7f, 16f)
        horizontalLineToRelative(34f)
        verticalLineToRelative(16f)
        horizontalLineToRelative(-34f)
        close()
    }
    // Bridge structure - Down
    path(
        stroke = SolidColor(colors.onSurface),
        strokeLineWidth = 3f
    ) {
        moveTo(1f, 38f)
        lineToRelative(6f, -6f)
        horizontalLineTo(41f)
        lineToRelative(6f, 6f)
    }
    // Bridge structure - Up
    path(
        stroke = SolidColor(colors.onSurface),
        strokeLineWidth = 3f
    ) {
        moveTo(1f, 10f)
        lineToRelative(6f, 6f)
        horizontalLineTo(41f)
        lineToRelative(6f, -6f)
    }
}.build()
