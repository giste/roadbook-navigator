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

package dev.giste.roadbook.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import dev.giste.roadbook.RoadbookStyle
import dev.giste.roadbook.presentation.icon.IconColors
import org.junit.Assert.assertEquals
import org.junit.Test

class RoadbookColorsTest {

    private val light = lightColorScheme()
    private val dark = darkColorScheme()

    @Test
    fun roadbookColors_appTheme_takesColorsFromTheme() {
        val colors = roadbookColors(RoadbookStyle.APP_THEME, light)

        assertEquals(
            RoadbookColors(
                surface = light.surface,
                onSurface = light.onSurface,
                track = light.primary,
                trackSecondary = light.secondary,
                roadSecondary = light.onSurface.copy(alpha = 0.5f),
                danger = light.error,
                muted = light.outline,
                shortDistanceSurface = light.tertiaryContainer,
                onShortDistanceSurface = light.onTertiaryContainer,
                waypointNumberSurface = light.inverseSurface,
                onWaypointNumberSurface = light.inverseOnSurface,
            ),
            colors,
        )
    }

    @Test
    fun roadbookColors_appTheme_followsDarkMode() {
        val colors = roadbookColors(RoadbookStyle.APP_THEME, dark)

        assertEquals(dark.surface, colors.surface)
        assertEquals(dark.onSurface, colors.onSurface)
    }

    @Test
    fun roadbookColors_fia_isFiaPalette() {
        assertEquals(FIA_ROADBOOK_COLORS, roadbookColors(RoadbookStyle.FIA, light))
    }

    @Test
    fun roadbookColors_fia_isSameInDarkMode() {
        assertEquals(
            roadbookColors(RoadbookStyle.FIA, light),
            roadbookColors(RoadbookStyle.FIA, dark),
        )
    }

    @Test
    fun fiaColors_areBlackOnWhiteWithBlueTrackAndRedDanger() {
        assertEquals(Color.White, FIA_ROADBOOK_COLORS.surface)
        assertEquals(Color.Black, FIA_ROADBOOK_COLORS.onSurface)
        assertEquals(Color.Blue, FIA_ROADBOOK_COLORS.track)
        assertEquals(Color.Red, FIA_ROADBOOK_COLORS.danger)
    }

    @Test
    fun iconColors_noBackground_drawsOnSurface() {
        assertEquals(
            IconColors(
                onSurface = Color.Black,
                surface = Color.White,
                danger = Color.Red,
                muted = FIA_ROADBOOK_COLORS.muted,
            ),
            FIA_ROADBOOK_COLORS.iconColors(),
        )
    }

    @Test
    fun iconColors_highlightedBackground_cutsShapesOutOfIt() {
        val colors = FIA_ROADBOOK_COLORS.iconColors(background = Color.Green)

        assertEquals(Color.Green, colors.surface)
    }
}
