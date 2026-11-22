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

package dev.giste.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GisteThemeTypographyTest {

    @get:Rule
    val composeRule = createComposeRule()

    /** The typography [MaterialTheme] provides inside the theme that [theme] sets up. */
    private fun typographyIn(theme: @Composable (@Composable () -> Unit) -> Unit): Typography {
        var typography: Typography? = null
        composeRule.setContent { theme { typography = MaterialTheme.typography } }
        composeRule.waitForIdle()
        return checkNotNull(typography)
    }

    @Test
    fun gisteTheme_noTypography_usesTheMaterialDefaults() {
        val typography = typographyIn { content -> GisteTheme(content = content) }

        assertEquals(Typography(), typography)
    }

    @Test
    fun gisteTheme_typographyGiven_providesIt() {
        val custom = Typography(bodyLarge = TextStyle(fontSize = 20.sp, lineHeight = 30.sp))

        val typography = typographyIn { content -> GisteTheme(typography = custom, content = content) }

        assertEquals(custom, typography)
    }
}
