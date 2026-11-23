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

package dev.giste.roadbooknavigator.settings.ui

import android.view.KeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.roadbooknavigator.settings.RemoteAction
import dev.giste.roadbooknavigator.settings.RemoteKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en")
class KeyCaptureDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val captured = mutableListOf<Int>()
    private var dismissed = 0

    private fun show(currentKeys: Set<RemoteKey> = setOf(RemoteKey(KeyEvent.KEYCODE_F6))) {
        composeRule.setContent {
            GisteTheme {
                KeyCaptureDialog(
                    action = RemoteAction.RESET_PARTIAL,
                    currentKeys = currentKeys,
                    onKeyCaptured = { captured += it.code },
                    onDismiss = { dismissed++ },
                )
            }
        }
    }

    /** Presses and releases [keyCode] on the dialog. */
    private fun press(keyCode: Int) {
        composeRule.onNodeWithText("Reset partial").performKeyInput { pressKey(Key(keyCode)) }
    }

    @Test
    fun keyCaptureDialog_shown_namesTheActionAndItsKey() {
        show()

        composeRule.onNodeWithText("Reset partial").assertExists()
        composeRule.onNodeWithText("Press the key on your remote for this action.").assertExists()
        composeRule.onNodeWithText("Now: F6").assertExists()
    }

    @Test
    fun keyCaptureDialog_noKey_saysSo() {
        show(currentKeys = emptySet())

        composeRule.onNodeWithText("Now: No key").assertExists()
    }

    @Test
    fun keyCaptureDialog_severalKeys_namesThemAll() {
        show(currentKeys = setOf(RemoteKey(KeyEvent.KEYCODE_MEDIA_PLAY), RemoteKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)))

        composeRule.onNodeWithText("Now: Play/pause, Play").assertExists()
    }

    @Test
    fun keyCaptureDialog_keyPressed_capturesItOnce() {
        show()

        press(KeyEvent.KEYCODE_F7)

        assertEquals(listOf(KeyEvent.KEYCODE_F7), captured)
    }

    @Test
    fun keyCaptureDialog_dpadKeyPressed_capturesIt() {
        show()

        press(KeyEvent.KEYCODE_DPAD_RIGHT)

        assertEquals(listOf(KeyEvent.KEYCODE_DPAD_RIGHT), captured)
    }

    @Test
    fun keyCaptureDialog_volumeKeyPressed_capturesIt() {
        show()

        press(KeyEvent.KEYCODE_VOLUME_UP)

        assertEquals(listOf(KeyEvent.KEYCODE_VOLUME_UP), captured)
    }

    @Test
    fun keyCaptureDialog_modifierKeyPressed_capturesNothing() {
        show()

        press(KeyEvent.KEYCODE_SHIFT_LEFT)

        assertEquals(emptyList<Int>(), captured)
    }

    @Test
    fun keyCaptureDialog_backPressed_capturesNothing() {
        show()

        press(KeyEvent.KEYCODE_BACK)

        assertEquals(emptyList<Int>(), captured)
    }

    @Test
    fun keyCaptureDialog_cancelPressed_dismisses() {
        show()

        composeRule.onNodeWithText("Cancel").performClick()

        assertEquals(1, dismissed)
        assertEquals(emptyList<Int>(), captured)
    }

    @Test
    fun isCapturableKey_ordinaryKeys_areCapturable() {
        listOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_F6,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_MEDIA_NEXT,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_A,
        ).forEach { assertTrue(KeyEvent.keyCodeToString(it), isCapturableKey(it)) }
    }

    @Test
    fun isCapturableKey_cancelModifierAndUnknownKeys_areNot() {
        listOf(
            KeyEvent.KEYCODE_UNKNOWN,
            KeyEvent.KEYCODE_BACK,
            KeyEvent.KEYCODE_ESCAPE,
            KeyEvent.KEYCODE_SHIFT_LEFT,
            KeyEvent.KEYCODE_ALT_LEFT,
            KeyEvent.KEYCODE_CTRL_RIGHT,
            KeyEvent.KEYCODE_META_LEFT,
        ).forEach { assertFalse(KeyEvent.keyCodeToString(it), isCapturableKey(it)) }
    }
}
