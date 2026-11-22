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

package dev.giste.roadbooknavigator.ui.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.giste.core.ui.theme.GisteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en")
class ResetAllDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var confirmed = 0
    private var dismissed = 0

    private fun show() {
        composeRule.setContent {
            GisteTheme {
                ResetAllDialog(onConfirm = { confirmed++ }, onDismiss = { dismissed++ })
            }
        }
    }

    @Test
    fun resetAllDialog_shown_explainsWhatIsReset() {
        show()

        composeRule.onNodeWithText("Reset distances?").assertIsDisplayed()
        composeRule.onNodeWithText("The partial and the total will be set to zero.").assertIsDisplayed()
    }

    @Test
    fun resetAllDialog_resetClicked_confirms() {
        show()

        composeRule.onNodeWithText("Reset").performClick()

        assertEquals(1, confirmed)
        assertEquals(0, dismissed)
    }

    @Test
    fun resetAllDialog_cancelClicked_dismisses() {
        show()

        composeRule.onNodeWithText("Cancel").performClick()

        assertEquals(0, confirmed)
        assertEquals(1, dismissed)
    }

    @Test
    @Config(qualifiers = "es")
    fun resetAllDialog_spanish_usesSpanishTexts() {
        show()

        composeRule.onNodeWithText("¿Poner las distancias a cero?").assertIsDisplayed()
        composeRule.onNodeWithText("Poner a cero").assertIsDisplayed()
    }
}
