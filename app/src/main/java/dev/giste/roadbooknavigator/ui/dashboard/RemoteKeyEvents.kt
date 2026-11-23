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

/**
 * This event as a [KeyPress] for the remote control rules, or `null` when it cannot be one: an
 * unknown key code, or an action other than down and up (the deprecated multiple-key events).
 */
internal fun KeyEvent.toKeyPress(): KeyPress? {
    if (keyCode <= KeyEvent.KEYCODE_UNKNOWN) return null
    val type = when (action) {
        KeyEvent.ACTION_DOWN -> KeyPressType.DOWN
        KeyEvent.ACTION_UP -> KeyPressType.UP
        else -> return null
    }
    return KeyPress(
        key = RemoteKey(keyCode),
        type = type,
        repeatCount = repeatCount,
        // The system's uptime clock, the same for every key event: no clock of our own needed.
        timeMillis = eventTime,
    )
}
