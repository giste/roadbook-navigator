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

package dev.giste.roadbook.ui.icon

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
import dev.giste.roadbook.RoadbookStyle
import dev.giste.roadbook.domain.drawing.IconType
import dev.giste.roadbook.ui.theme.RoadbookTheme

/**
 * Shows every symbol with the name of its type, so they can be reviewed in Android Studio in
 * every roadbook style and mode. Preview-only: the labels are sample data, not resources.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconGallery() {
    val dimensions = GisteTheme.dimensions
    val colors = RoadbookTheme.colors.iconColors()
    Surface(color = RoadbookTheme.colors.surface) {
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
                    Text(
                        text = type.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = RoadbookTheme.colors.onSurface,
                    )
                }
            }
        }
    }
}

@Preview(name = "App theme, light", uiMode = Configuration.UI_MODE_NIGHT_NO, widthDp = 420)
@Preview(name = "App theme, dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 420)
@Composable
private fun IconGalleryAppThemePreview() {
    GisteTheme {
        RoadbookTheme(style = RoadbookStyle.APP_THEME) {
            IconGallery()
        }
    }
}

@Preview(name = "FIA, light", uiMode = Configuration.UI_MODE_NIGHT_NO, widthDp = 420)
@Preview(name = "FIA, dark", uiMode = Configuration.UI_MODE_NIGHT_YES, widthDp = 420)
@Composable
private fun IconGalleryFiaPreview() {
    GisteTheme {
        RoadbookTheme(style = RoadbookStyle.FIA) {
            IconGallery()
        }
    }
}
