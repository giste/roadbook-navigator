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

package dev.giste.roadbook.ui.icon.landmark

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import dev.giste.roadbook.ui.icon.IconColors

/**
 * Draws a fort or castle, 48 by 48.
 *
 * @param colors the colors of the current theme.
 * @return the symbol as a vector.
 */
internal fun fortCastleIcon(colors: IconColors): ImageVector = ImageVector.Builder(
    name = "Landmark.FortCastle",
    defaultWidth = 48.dp,
    defaultHeight = 48.dp,
    viewportWidth = 48f,
    viewportHeight = 48f
).apply {
    path(
        fill = SolidColor(colors.muted),
        stroke = SolidColor(colors.onSurface),
        strokeLineWidth = 2f
    ) {
        moveTo(11f, 8f)
        horizontalLineTo(1f)
        verticalLineTo(18f)
        horizontalLineTo(6f)
        verticalLineTo(30f)
        horizontalLineTo(1f)
        verticalLineTo(40f)
        horizontalLineTo(11f)
        verticalLineTo(35f)
        horizontalLineTo(36f)
        verticalLineTo(40f)
        horizontalLineTo(47f)
        verticalLineTo(30f)
        horizontalLineTo(42f)
        verticalLineTo(18f)
        horizontalLineTo(47f)
        verticalLineTo(8f)
        horizontalLineTo(37f)
        verticalLineTo(13f)
        horizontalLineTo(11f)
        verticalLineTo(8f)
        close()
    }
}.build()
