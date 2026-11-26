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
import android.view.WindowInsets
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Whether this window asks for its status and navigation bars to be shown.
 *
 * Robolectric draws no system bars, so the window insets never report them as visible. Robolectric
 * runs the framework's own `InsetsController`, though, which keeps the bar types the window asks to
 * show. Its getter is not public API, so it is read by reflection; it exists on the SDK the tests
 * run on (see `robolectric.properties`).
 */
internal fun Window.requestsSystemBars(): Boolean {
    val controller = insetsController!!
    val requested = controller.javaClass.getMethod("getRequestedVisibleTypes")
        .invoke(controller) as Int
    val systemBars = WindowInsets.Type.systemBars()
    return requested and systemBars == systemBars
}

@RunWith(RobolectricTestRunner::class)
class SystemBarsTest {

    private val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()

    /** Whether the activity's window asks for its status and navigation bars to be shown. */
    private fun systemBarsRequested(): Boolean = activity.window.requestsSystemBars()

    @Test
    fun applyFullScreen_true_hidesTheSystemBars() {
        activity.window.applyFullScreen(true)

        assertFalse(systemBarsRequested())
    }

    @Test
    fun applyFullScreen_true_showsThemBrieflyOnASwipe() {
        activity.window.applyFullScreen(true)

        assertEquals(
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE,
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                .systemBarsBehavior,
        )
    }

    @Test
    fun applyFullScreen_falseAfterTrue_showsTheSystemBarsAgain() {
        activity.window.applyFullScreen(true)

        activity.window.applyFullScreen(false)

        assertTrue(systemBarsRequested())
    }
}
