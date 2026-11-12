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

package dev.giste.roadbooknavigator.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.giste.roadbooknavigator.core.ui.theme.presentation.DarkBeerColorScheme
import dev.giste.roadbooknavigator.core.ui.theme.presentation.LightBeerColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoadbookNavigatorThemeTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun roadbookNavigatorTheme_darkTheme_providesDarkBeerColorScheme() {
        var scheme: ColorScheme? = null

        composeRule.setContent {
            RoadbookNavigatorTheme(darkTheme = true) { scheme = MaterialTheme.colorScheme }
        }

        composeRule.runOnIdle { assertSame(DarkBeerColorScheme, scheme) }
    }

    @Test
    fun roadbookNavigatorTheme_lightTheme_providesLightBeerColorScheme() {
        var scheme: ColorScheme? = null

        composeRule.setContent {
            RoadbookNavigatorTheme(darkTheme = false) { scheme = MaterialTheme.colorScheme }
        }

        composeRule.runOnIdle { assertSame(LightBeerColorScheme, scheme) }
    }

    @Test
    fun roadbookNavigatorTheme_insideTheme_providesGloveFriendlyTouchTarget() {
        var dimensions: Dimensions? = null

        composeRule.setContent {
            RoadbookNavigatorTheme { dimensions = RoadbookNavigatorTheme.dimensions }
        }

        composeRule.runOnIdle { assertEquals(56.dp, dimensions?.minTouchTarget) }
    }
}
