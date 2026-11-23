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

package dev.giste.roadbooknavigator.settings

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteKeysTest {

    private val up = RemoteKey(KeyEvent.KEYCODE_DPAD_UP)
    private val play = RemoteKey(KeyEvent.KEYCODE_MEDIA_PLAY)
    private val playPause = RemoteKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)

    @Test(expected = IllegalArgumentException::class)
    fun remoteKey_zero_isRejected() {
        RemoteKey(KeyEvent.KEYCODE_UNKNOWN)
    }

    @Test(expected = IllegalArgumentException::class)
    fun remoteKey_negative_isRejected() {
        RemoteKey(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun remoteKeys_keyBoundToTwoActions_isRejected() {
        RemoteKeys(
            mapOf(
                RemoteAction.NEXT_WAYPOINT to setOf(up),
                RemoteAction.RESET_PARTIAL to setOf(up, play),
            ),
        )
    }

    @Test
    fun keysFor_actionWithSeveralKeys_isAllOfThem() {
        assertEquals(
            setOf(play, playPause),
            RemoteKeys.TERRA_PIRATA.keysFor(RemoteAction.RESET_PARTIAL),
        )
    }

    @Test
    fun keysFor_missingAction_isEmpty() {
        val keys = RemoteKeys(mapOf(RemoteAction.NEXT_WAYPOINT to setOf(up)))

        assertTrue(keys.keysFor(RemoteAction.RESET_PARTIAL).isEmpty())
    }

    @Test
    fun equals_missingActionAndActionWithoutKeys_areEqual() {
        val missing = RemoteKeys(mapOf(RemoteAction.NEXT_WAYPOINT to setOf(up)))
        val empty = RemoteKeys(
            mapOf(RemoteAction.NEXT_WAYPOINT to setOf(up), RemoteAction.RESET_PARTIAL to emptySet()),
        )

        assertEquals(missing, empty)
        assertEquals(missing.hashCode(), empty.hashCode())
    }

    @Test
    fun equals_differentKeys_areNotEqual() {
        assertNotEquals(RemoteKeys.DND2, RemoteKeys.TERRA_PIRATA)
    }

    @Test
    fun actionFor_boundKey_isItsAction() {
        assertEquals(RemoteAction.RESET_PARTIAL, RemoteKeys.TERRA_PIRATA.actionFor(play))
    }

    @Test
    fun actionFor_unboundKey_isNull() {
        assertNull(RemoteKeys.TERRA_PIRATA.actionFor(up))
    }

    @Test
    fun dnd2_bindsTheDpadAndF6() {
        val expected = mapOf(
            RemoteAction.NEXT_WAYPOINT to KeyEvent.KEYCODE_DPAD_UP,
            RemoteAction.PREVIOUS_WAYPOINT to KeyEvent.KEYCODE_DPAD_DOWN,
            RemoteAction.INCREASE_PARTIAL to KeyEvent.KEYCODE_DPAD_RIGHT,
            RemoteAction.DECREASE_PARTIAL to KeyEvent.KEYCODE_DPAD_LEFT,
            RemoteAction.RESET_PARTIAL to KeyEvent.KEYCODE_F6,
        )

        expected.forEach { (action, code) ->
            assertEquals(action.name, setOf(RemoteKey(code)), RemoteKeys.DND2.keysFor(action))
        }
    }

    @Test
    fun terraPirata_bindsTheMediaAndVolumeKeys() {
        val expected = mapOf(
            RemoteAction.NEXT_WAYPOINT to setOf(KeyEvent.KEYCODE_MEDIA_NEXT),
            RemoteAction.PREVIOUS_WAYPOINT to setOf(KeyEvent.KEYCODE_MEDIA_PREVIOUS),
            RemoteAction.INCREASE_PARTIAL to setOf(KeyEvent.KEYCODE_VOLUME_UP),
            RemoteAction.DECREASE_PARTIAL to setOf(KeyEvent.KEYCODE_VOLUME_DOWN),
            RemoteAction.RESET_PARTIAL to
                setOf(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY),
        )

        expected.forEach { (action, codes) ->
            assertEquals(
                action.name,
                codes.map(::RemoteKey).toSet(),
                RemoteKeys.TERRA_PIRATA.keysFor(action),
            )
        }
    }
}
