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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.geo.Distance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-w800dp-h1280dp")
class SetPartialDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val confirmed = mutableListOf<Distance>()
    private var dismissed = 0

    /** The haptic feedback the dialog asked for, in order. */
    private val haptics = mutableListOf<HapticFeedbackType>()

    /** Shows the dialog, starting with [initialText], keeping the typed text like its caller does. */
    private fun show(initialText: String = "") {
        composeRule.setContent {
            var text by remember { mutableStateOf(initialText) }
            val recordingHaptics = remember {
                object : HapticFeedback {
                    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                        haptics += hapticFeedbackType
                    }
                }
            }
            CompositionLocalProvider(LocalHapticFeedback provides recordingHaptics) {
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
    }

    private fun setButton() = composeRule.onNodeWithText("Set")

    @Test
    fun setPartialDialog_opened_showsTitleAndAllKeys() {
        show()

        composeRule.onNodeWithText("Set partial").assertIsDisplayed()
        "0123456789.".forEach { composeRule.onNode(numpadKey("$it")).assertIsDisplayed() }
        composeRule.onNodeWithTag(SetPartialTestTags.DELETE).assertIsDisplayed()
    }

    @Test
    fun setPartialDialog_empty_showsMutedZeroAndDisablesSet() {
        show()

        composeRule.onNode(shownInPartialDisplay("0 km")).assertIsDisplayed()
        setButton().assertIsNotEnabled()
    }

    @Test
    fun setPartialDialog_keysPressed_showsTheTypedPartial() {
        show()

        composeRule.pressNumpadKeys("12.35")

        composeRule.onNode(shownInPartialDisplay("12.35 km")).assertIsDisplayed()
        setButton().assertIsEnabled()
    }

    @Test
    fun setPartialDialog_deletePressed_removesTheLastCharacter() {
        show("12.35")

        composeRule.pressNumpadKeys("<")

        composeRule.onNode(shownInPartialDisplay("12.3 km")).assertIsDisplayed()
    }

    @Test
    fun setPartialDialog_keyBeyondTheLargestPartial_isIgnored() {
        show("12.35")

        composeRule.pressNumpadKeys("9")

        composeRule.onNode(shownInPartialDisplay("12.35 km")).assertIsDisplayed()
    }

    @Test
    fun setPartialDialog_keyChangesThePartial_ticks() {
        show()

        composeRule.pressNumpadKeys("1")

        assertEquals(listOf(HapticFeedbackType.VirtualKey), haptics)
    }

    @Test
    fun setPartialDialog_keyIsIgnored_rejects() {
        show("12.35")

        composeRule.pressNumpadKeys("9")

        assertEquals(listOf(HapticFeedbackType.Reject), haptics)
    }

    @Test
    fun setPartialDialog_setClicked_confirmsTheTypedPartial() {
        show()
        composeRule.pressNumpadKeys("12.35")

        setButton().performClick()

        assertEquals(listOf(Distance.meters(12_350.0)), confirmed)
    }

    @Test
    fun setPartialDialog_cancelClicked_dismisses() {
        show("12.35")

        composeRule.onNodeWithText("Cancel").performClick()

        assertEquals(1, dismissed)
        assertEquals(emptyList<Distance>(), confirmed)
    }

    @Test
    fun setPartialDialog_portrait_showsTheNumpadBelowThePartial() {
        show()

        val display = composeRule.onNodeWithTag(SetPartialTestTags.DISPLAY).getUnclippedBoundsInRoot()
        val firstKey = composeRule.onNode(numpadKey("1")).getUnclippedBoundsInRoot()

        assertTrue(firstKey.top >= display.bottom)
    }

    @Test
    @Config(qualifiers = "en-w1280dp-h800dp")
    fun setPartialDialog_landscape_showsTheNumpadRightOfThePartial() {
        show()

        val display = composeRule.onNodeWithTag(SetPartialTestTags.DISPLAY).getUnclippedBoundsInRoot()
        val firstKey = composeRule.onNode(numpadKey("1")).getUnclippedBoundsInRoot()

        assertTrue(firstKey.left >= display.right)
    }

    @Test
    @Config(qualifiers = "en-w891dp-h411dp")
    fun setPartialDialog_phoneLandscape_keysAreAtLeastTheMinimumTouchTarget() {
        show()

        val key = composeRule.onNode(numpadKey("5")).getUnclippedBoundsInRoot()

        assertTrue("Key height was ${key.height}", key.height >= 56.dp)
        assertTrue("Key width was ${key.width}", key.width >= 56.dp)
    }

    @Test
    @Config(qualifiers = "es-w800dp-h1280dp")
    fun setPartialDialog_spanish_usesTheDecimalComma() {
        show()

        composeRule.pressNumpadKeys("12,5", separator = ',')

        composeRule.onNodeWithText("Fijar parcial").assertIsDisplayed()
        composeRule.onNode(shownInPartialDisplay("12,5 km")).assertIsDisplayed()
    }
}
