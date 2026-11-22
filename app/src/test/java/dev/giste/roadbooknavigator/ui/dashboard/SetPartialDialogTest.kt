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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.geo.Distance
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en")
class SetPartialDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val confirmed = mutableListOf<Distance>()
    private var dismissed = 0

    /** Shows the dialog, starting with [initialText], keeping the typed text like its caller does. */
    private fun show(initialText: String = "") {
        composeRule.setContent {
            var text by remember { mutableStateOf(initialText) }
            GisteTheme {
                SetPartialDialog(
                    text = text,
                    onTextChange = { text = it },
                    onConfirm = { confirmed += it },
                    onDismiss = { dismissed++ },
                )
            }
        }
    }

    private fun field() = composeRule.onNode(hasSetTextAction())

    private fun setButton() = composeRule.onNodeWithText("Set")

    @Test
    fun setPartialDialog_opened_showsTitleAndFocusesTheField() {
        show()

        composeRule.onNodeWithText("Set partial").assertIsDisplayed()
        field().assertIsFocused()
    }

    @Test
    fun setPartialDialog_empty_disablesSetWithoutError() {
        show()

        setButton().assertIsNotEnabled()
        composeRule.onNodeWithText("Enter a distance", substring = true).assertDoesNotExist()
    }

    @Test
    fun setPartialDialog_validPartialTyped_enablesSet() {
        show()

        field().performTextInput("12.35")

        setButton().assertIsEnabled()
    }

    @Test
    fun setPartialDialog_setClicked_confirmsTheTypedPartial() {
        show("12.35")

        setButton().performClick()

        assertEquals(listOf(Distance.meters(12_350.0)), confirmed)
    }

    @Test
    fun setPartialDialog_doneKeyWithValidPartial_confirmsIt() {
        show("1,5")

        field().performImeAction()

        assertEquals(listOf(Distance.meters(1_500.0)), confirmed)
    }

    @Test
    fun setPartialDialog_doneKeyWithInvalidPartial_doesNothing() {
        show("abc")

        field().performImeAction()

        assertEquals(emptyList<Distance>(), confirmed)
    }

    @Test
    fun setPartialDialog_notANumber_showsErrorAndDisablesSet() {
        show("12..3")

        composeRule.onNodeWithText("Enter a distance in kilometers, such as 12.35").assertIsDisplayed()
        setButton().assertIsNotEnabled()
    }

    @Test
    fun setPartialDialog_tooLarge_showsTheLargestPartial() {
        show("1234")

        composeRule.onNodeWithText("The partial can be at most 999.99 km").assertIsDisplayed()
        setButton().assertIsNotEnabled()
    }

    @Test
    fun setPartialDialog_tooManyDecimals_showsHowManyAreAllowed() {
        show("1.234")

        composeRule.onNodeWithText("Use at most 2 decimals").assertIsDisplayed()
        setButton().assertIsNotEnabled()
    }

    @Test
    fun setPartialDialog_cancelClicked_dismisses() {
        show("12.35")

        composeRule.onNodeWithText("Cancel").performClick()

        assertEquals(1, dismissed)
        assertEquals(emptyList<Distance>(), confirmed)
    }

    @Test
    @Config(qualifiers = "es")
    fun setPartialDialog_spanish_usesSpanishTextsAndDecimalComma() {
        show("1234")

        composeRule.onNodeWithText("Fijar parcial").assertIsDisplayed()
        composeRule.onNodeWithText("El parcial puede ser como máximo 999,99 km").assertIsDisplayed()
    }
}
