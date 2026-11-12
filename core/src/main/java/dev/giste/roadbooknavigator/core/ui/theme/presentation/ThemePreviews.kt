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

package dev.giste.roadbooknavigator.core.ui.theme.presentation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import dev.giste.roadbooknavigator.core.ui.theme.RoadbookNavigatorTheme

/**
 * Shows the main colour roles of the theme as swatches with their "on" colour, so the palette
 * can be reviewed in Android Studio. Preview-only: labels are sample data, not resources.
 */
@Composable
private fun ColorRolesSample(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val dimensions = RoadbookNavigatorTheme.dimensions
    val roles = listOf(
        Triple("primary", colors.primary, colors.onPrimary),
        Triple("primaryContainer", colors.primaryContainer, colors.onPrimaryContainer),
        Triple("secondary", colors.secondary, colors.onSecondary),
        Triple("secondaryContainer", colors.secondaryContainer, colors.onSecondaryContainer),
        Triple("tertiary", colors.tertiary, colors.onTertiary),
        Triple("tertiaryContainer", colors.tertiaryContainer, colors.onTertiaryContainer),
        Triple("error", colors.error, colors.onError),
        Triple("surfaceContainerHighest", colors.surfaceContainerHighest, colors.onSurface),
    )
    Column(
        modifier = modifier.padding(dimensions.spacingMedium),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingSmall),
    ) {
        Text(text = "Roadbook Navigator", style = MaterialTheme.typography.headlineSmall)
        roles.forEach { (name, background, content) ->
            Swatch(name = name, background = background, content = content)
        }
    }
}

/** One colour role: its name written in its "on" colour over its background. */
@Composable
private fun Swatch(name: String, background: Color, content: Color) {
    val dimensions = RoadbookNavigatorTheme.dimensions
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(dimensions.spacingSmall),
    ) {
        Text(text = name, color = content)
    }
}

@Preview(name = "Light", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ColorRolesPreview() {
    RoadbookNavigatorTheme {
        Surface {
            ColorRolesSample()
        }
    }
}
