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

package dev.giste.roadbooknavigator.domain

import android.view.KeyEvent
import dev.giste.roadbooknavigator.settings.RemoteAction
import dev.giste.roadbooknavigator.settings.RemoteKey
import dev.giste.roadbooknavigator.settings.RemoteKeys
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteKeyInterpreterTest {

    private val interpreter = RemoteKeyInterpreter()
    private val keys = RemoteKeys.DND2

    private val up = RemoteKey(KeyEvent.KEYCODE_DPAD_UP)
    private val right = RemoteKey(KeyEvent.KEYCODE_DPAD_RIGHT)
    private val reset = RemoteKey(KeyEvent.KEYCODE_F6)
    private val otherReset = RemoteKey(KeyEvent.KEYCODE_F7)

    private fun down(key: RemoteKey, repeatCount: Int = 0, atMillis: Long = 0) =
        interpreter.interpret(KeyPress(key, KeyPressType.DOWN, repeatCount, atMillis), keys)

    private fun release(key: RemoteKey, atMillis: Long = 0) =
        interpreter.interpret(KeyPress(key, KeyPressType.UP, 0, atMillis), keys)

    private fun perform(action: RemoteAction) = RemoteKeyOutcome.Perform(action)

    // The first press of each test happens at 0 ms, so a later event's time is how long the key
    // has been held.

    @Test
    fun interpret_unboundKey_isNotBound() {
        assertEquals(RemoteKeyOutcome.NotBound, down(RemoteKey(KeyEvent.KEYCODE_VOLUME_UP)))
    }

    @Test
    fun interpret_unboundKeyReleased_isNotBound() {
        assertEquals(RemoteKeyOutcome.NotBound, release(RemoteKey(KeyEvent.KEYCODE_VOLUME_UP)))
    }

    @Test
    fun interpret_noKeysBound_isNotBound() {
        val outcome = interpreter.interpret(KeyPress(up, KeyPressType.DOWN, 0, 0), RemoteKeys(emptyMap()))

        assertEquals(RemoteKeyOutcome.NotBound, outcome)
    }

    @Test
    fun interpret_waypointKeyDown_performsItsAction() {
        assertEquals(perform(RemoteAction.NEXT_WAYPOINT), down(up))
    }

    @Test
    fun interpret_partialKeyDown_performsItsAction() {
        assertEquals(perform(RemoteAction.INCREASE_PARTIAL), down(right))
    }

    @Test
    fun interpret_partialKeyRepeated_performsItsActionOnEveryRepeat() {
        down(right)

        assertEquals(perform(RemoteAction.INCREASE_PARTIAL), down(right, repeatCount = 1, atMillis = 500))
        assertEquals(perform(RemoteAction.INCREASE_PARTIAL), down(right, repeatCount = 2, atMillis = 550))
    }

    @Test
    fun interpret_actionKeyReleased_isConsumedWithoutAction() {
        down(up)

        assertEquals(RemoteKeyOutcome.Consumed, release(up, atMillis = 100))
    }

    @Test
    fun interpret_resetKeyDown_isConsumedWithoutAction() {
        assertEquals(RemoteKeyOutcome.Consumed, down(reset))
    }

    @Test
    fun interpret_resetKeyReleasedBeforeTheHold_isConsumedWithoutAction() {
        down(reset)

        assertEquals(RemoteKeyOutcome.Consumed, release(reset, atMillis = RESET_HOLD_MILLIS - 1))
    }

    @Test
    fun interpret_resetKeyRepeatedBeforeTheHold_isConsumedWithoutAction() {
        down(reset)

        assertEquals(RemoteKeyOutcome.Consumed, down(reset, repeatCount = 1, atMillis = RESET_HOLD_MILLIS - 1))
    }

    @Test
    fun interpret_resetKeyRepeatReachesTheHold_resetsThePartial() {
        down(reset)

        assertEquals(perform(RemoteAction.RESET_PARTIAL), down(reset, repeatCount = 10, atMillis = RESET_HOLD_MILLIS))
    }

    @Test
    fun interpret_resetKeyHeldLonger_resetsOnlyOnce() {
        down(reset)
        down(reset, repeatCount = 10, atMillis = RESET_HOLD_MILLIS)

        assertEquals(RemoteKeyOutcome.Consumed, down(reset, repeatCount = 11, atMillis = RESET_HOLD_MILLIS + 50))
        assertEquals(RemoteKeyOutcome.Consumed, release(reset, atMillis = RESET_HOLD_MILLIS + 60))
    }

    @Test
    fun interpret_resetKeyReleasedAfterTheHoldWithoutRepeats_resetsThePartial() {
        down(reset)

        assertEquals(perform(RemoteAction.RESET_PARTIAL), release(reset, atMillis = RESET_HOLD_MILLIS))
    }

    @Test
    fun interpret_resetKeyHeldAgainAfterAReset_resetsAgain() {
        down(reset)
        down(reset, repeatCount = 10, atMillis = RESET_HOLD_MILLIS)
        release(reset, atMillis = RESET_HOLD_MILLIS + 10)

        down(reset)

        assertEquals(perform(RemoteAction.RESET_PARTIAL), down(reset, repeatCount = 10, atMillis = RESET_HOLD_MILLIS))
    }

    @Test
    fun interpret_newPressAfterAMissedRelease_startsANewHold() {
        // The release of the first hold never arrived, e.g. it went to another window.
        down(reset)
        down(reset, repeatCount = 10, atMillis = RESET_HOLD_MILLIS)

        down(reset)

        assertEquals(perform(RemoteAction.RESET_PARTIAL), down(reset, repeatCount = 10, atMillis = RESET_HOLD_MILLIS))
    }

    @Test
    fun interpret_twoResetKeys_holdsAreKeptApart() {
        val keys = RemoteKeys(mapOf(RemoteAction.RESET_PARTIAL to setOf(reset, otherReset)))
        fun downWithBoth(key: RemoteKey, repeatCount: Int, atMillis: Long) =
            interpreter.interpret(KeyPress(key, KeyPressType.DOWN, repeatCount, atMillis), keys)
        downWithBoth(reset, 0, 0)
        downWithBoth(reset, 10, RESET_HOLD_MILLIS)

        downWithBoth(otherReset, 0, 0)

        assertEquals(perform(RemoteAction.RESET_PARTIAL), downWithBoth(otherReset, 10, RESET_HOLD_MILLIS))
    }

    @Test
    fun interpret_resetKeyHeldLater_isTimedFromItsFirstPress() {
        down(reset, atMillis = 5_000)

        assertEquals(RemoteKeyOutcome.Consumed, down(reset, repeatCount = 1, atMillis = 5_000 + RESET_HOLD_MILLIS - 1))
        assertEquals(perform(RemoteAction.RESET_PARTIAL), down(reset, repeatCount = 2, atMillis = 5_000 + RESET_HOLD_MILLIS))
    }

    @Test
    fun interpret_resetKeyRepeatWithoutItsFirstPress_startsTimingThere() {
        // The first press went elsewhere, e.g. to a dialog that has just closed.
        assertEquals(RemoteKeyOutcome.Consumed, down(reset, repeatCount = 20, atMillis = 3_000))

        assertEquals(perform(RemoteAction.RESET_PARTIAL), down(reset, repeatCount = 40, atMillis = 3_000 + RESET_HOLD_MILLIS))
    }

    @Test
    fun interpret_resetKeyReleaseWithoutItsFirstPress_isConsumedWithoutAction() {
        assertEquals(RemoteKeyOutcome.Consumed, release(reset, atMillis = 3_000))
    }
}
