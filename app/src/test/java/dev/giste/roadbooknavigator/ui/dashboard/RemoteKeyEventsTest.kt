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

import android.view.KeyEvent
import dev.giste.roadbooknavigator.domain.KeyPress
import dev.giste.roadbooknavigator.domain.KeyPressType
import dev.giste.roadbooknavigator.settings.RemoteKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteKeyEventsTest {

    @Test
    fun toKeyPress_firstDown_isADownAtItsTime() {
        val event = KeyEvent(1_000, 1_000, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP, 0)

        assertEquals(
            KeyPress(RemoteKey(KeyEvent.KEYCODE_DPAD_UP), KeyPressType.DOWN, repeatCount = 0, timeMillis = 1_000),
            event.toKeyPress(),
        )
    }

    @Test
    fun toKeyPress_repeat_keepsTheRepeatCountAndItsTime() {
        val event = KeyEvent(1_000, 1_550, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F6, 2)

        assertEquals(
            KeyPress(RemoteKey(KeyEvent.KEYCODE_F6), KeyPressType.DOWN, repeatCount = 2, timeMillis = 1_550),
            event.toKeyPress(),
        )
    }

    @Test
    fun toKeyPress_up_isAnUpAtItsTime() {
        val event = KeyEvent(1_000, 2_200, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_VOLUME_UP, 0)

        assertEquals(
            KeyPress(RemoteKey(KeyEvent.KEYCODE_VOLUME_UP), KeyPressType.UP, repeatCount = 0, timeMillis = 2_200),
            event.toKeyPress(),
        )
    }

    @Test
    fun toKeyPress_unknownKeyCode_isNull() {
        val event = KeyEvent(1_000, 1_000, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_UNKNOWN, 0)

        assertNull(event.toKeyPress())
    }

    @Test
    @Suppress("DEPRECATION") // ACTION_MULTIPLE is what this test is about.
    fun toKeyPress_multipleAction_isNull() {
        val event = KeyEvent(1_000, 1_000, KeyEvent.ACTION_MULTIPLE, KeyEvent.KEYCODE_DPAD_UP, 3)

        assertNull(event.toKeyPress())
    }
}
