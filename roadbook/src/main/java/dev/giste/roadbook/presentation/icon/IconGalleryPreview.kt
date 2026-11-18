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

package dev.giste.roadbook.presentation.icon

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.roadbook.domain.drawing.IconType

/**
 * Shows every symbol with the name of its type, so they can be reviewed in Android Studio in both
 * themes. Preview-only: the labels are sample data, not resources.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconGallery() {
    val dimensions = GisteTheme.dimensions
    val colors = iconColors()
    Surface {
        FlowRow(
            modifier = Modifier.padding(dimensions.spacingMedium),
            horizontalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
            verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
        ) {
            IconType.entries.forEach { type ->
                val vector = rememberIconVector(type, colors) ?: return@forEach
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        imageVector = vector,
                        contentDescription = null,
                        modifier = Modifier.size(dimensions.minTouchTarget),
                    )
                    Text(text = type.name, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Preview(name = "Light", uiMode = Configuration.UI_MODE_NIGHT_NO, widthDp = 420)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 420)
@Composable
private fun IconGalleryPreview() {
    GisteTheme {
        IconGallery()
    }
}
