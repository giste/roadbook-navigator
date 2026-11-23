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

package dev.giste.roadbooknavigator.integration

import android.os.SystemClock
import android.view.KeyEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.pressKey
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dev.giste.geo.Distance
import dev.giste.odometer.OdometerController
import dev.giste.roadbooknavigator.MainActivity
import dev.giste.roadbooknavigator.domain.RESET_HOLD_MILLIS
import dev.giste.roadbooknavigator.ui.dashboard.DashboardTestTags
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * A remote control on the real app: key events sent to [MainActivity], as a Bluetooth remote's
 * would, with the real Hilt graph, the real settings and the real odometer.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "en")
class RemoteKeysIntegrationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val grantLocationRule = GrantLocationRule()

    @get:Rule(order = 2)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var odometer: OdometerController

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    /** Sends a key event to the activity, and returns whether it was consumed. */
    private fun dispatch(action: Int, keyCode: Int, repeatCount: Int = 0, heldMillis: Long = 0): Boolean {
        val downTime = SystemClock.uptimeMillis()
        val event = KeyEvent(downTime, downTime + heldMillis, action, keyCode, repeatCount)
        var consumed = false
        composeRule.runOnUiThread { consumed = composeRule.activity.dispatchKeyEvent(event) }
        composeRule.waitForIdle()
        return consumed
    }

    /** Presses and releases [keyCode], quickly. */
    private fun tap(keyCode: Int) {
        dispatch(KeyEvent.ACTION_DOWN, keyCode)
        dispatch(KeyEvent.ACTION_UP, keyCode)
    }

    /**
     * Waits until the dashboard shows the partial [partial] and has read the remote keys, which
     * [keyCode] (a key of the chosen model) is bound to. A release does nothing, so it can be sent
     * until it is consumed.
     */
    private fun awaitDashboard(keyCode: Int, partial: String = "0.00") {
        composeRule.awaitPartial(partial)
        composeRule.waitUntil(TIMEOUT_MILLIS) { dispatch(KeyEvent.ACTION_UP, keyCode) }
    }

    // Terra Pirata is the default remote: volume keys change the partial, play/pause resets it.

    @Test
    fun increasePartialKey_onTheDashboard_increasesThePartial() {
        awaitDashboard(KeyEvent.KEYCODE_VOLUME_UP)

        tap(KeyEvent.KEYCODE_VOLUME_UP)

        composeRule.awaitPartial("0.01")
    }

    @Test
    fun resetKey_heldOnTheDashboard_resetsThePartial() {
        runBlocking { odometer.setTrip(Distance.meters(500.0)) }
        awaitDashboard(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, partial = "0.50")

        dispatch(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        dispatch(
            KeyEvent.ACTION_DOWN,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            repeatCount = 10,
            heldMillis = RESET_HOLD_MILLIS,
        )

        composeRule.awaitPartial("0.00")
    }

    @Test
    fun dashboard_shown_holdsTheFocus() {
        composeRule.awaitPartial("0.00")

        // A focused view keeps a D-pad key from being spent on leaving touch mode.
        composeRule.onNode(isFocused()).assertExists()
    }

    @Test
    fun dashboard_backFromTheSettings_takesTheFocusAgain() {
        composeRule.awaitPartial("0.00")
        openSettings()

        goBack()
        composeRule.awaitPartial("0.00")

        composeRule.onNode(isFocused()).assertExists()
    }

    @Test
    fun unboundKey_onTheDashboard_reachesTheSystem() {
        awaitDashboard(KeyEvent.KEYCODE_VOLUME_UP)

        assertFalse(dispatch(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_F6))
    }

    @Test
    fun remoteKey_inTheSettings_doesNotChangeThePartial() {
        awaitDashboard(KeyEvent.KEYCODE_VOLUME_UP)
        openSettings()

        tap(KeyEvent.KEYCODE_VOLUME_UP)
        goBack()
        composeRule.awaitPartial("0.00")
        tap(KeyEvent.KEYCODE_VOLUME_UP)

        // Only the press on the dashboard counted.
        composeRule.awaitPartial("0.01")
    }

    @Test
    fun otherModelPicked_itsKeysControlTheDashboard() {
        awaitDashboard(KeyEvent.KEYCODE_VOLUME_UP)
        openSettings()
        composeRule.onNode(hasText("DND2") and hasClickAction()).performScrollTo().performClick()
        goBack()
        awaitDashboard(KeyEvent.KEYCODE_DPAD_RIGHT)

        tap(KeyEvent.KEYCODE_DPAD_RIGHT)

        composeRule.awaitPartial("0.01")
    }

    @Test
    fun customKeyAssigned_controlsTheDashboard() {
        awaitDashboard(KeyEvent.KEYCODE_VOLUME_UP)
        openSettings()
        composeRule.onNode(hasText("Custom") and hasClickAction()).performScrollTo().performClick()
        // Custom starts with Terra Pirata's keys.
        composeRule.onNode(hasText("Volume up") and hasClickAction()).performScrollTo().performClick()
        composeRule.onNodeWithText("Now: Volume up").performKeyInput { pressKey(Key(KeyEvent.KEYCODE_F7)) }
        composeRule.awaitText("Now: Volume up", exists = false)
        goBack()
        awaitDashboard(KeyEvent.KEYCODE_F7)

        tap(KeyEvent.KEYCODE_F7)

        composeRule.awaitPartial("0.01")
    }

    /** Opens the settings from the dashboard, with the button shown while stopped. */
    private fun openSettings() {
        composeRule.onNodeWithTag(DashboardTestTags.SETTINGS).performClick()
        composeRule.awaitText("Settings")
    }

    /** Leaves the settings with the system back gesture. */
    private fun goBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
    }
}
