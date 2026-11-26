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

package dev.giste.roadbooknavigator.ui

import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Hides the status and navigation bars of this window when [fullScreen], and shows them otherwise.
 *
 * Hidden bars come back for a moment when the rider swipes from the edge of the screen, over the
 * app and without resizing it, and hide again by themselves. The app is drawn edge to edge, so
 * the screens' insets grow and shrink with the bars, and nothing is hidden under them.
 *
 * @param fullScreen `true` to hide the bars, `false` to show them.
 */
internal fun Window.applyFullScreen(fullScreen: Boolean) {
    val controller = WindowCompat.getInsetsController(this, decorView)
    if (fullScreen) {
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    } else {
        controller.show(WindowInsetsCompat.Type.systemBars())
    }
}
