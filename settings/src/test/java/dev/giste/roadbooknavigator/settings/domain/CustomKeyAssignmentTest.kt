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

package dev.giste.roadbooknavigator.settings.domain

import android.view.KeyEvent
import dev.giste.roadbooknavigator.settings.RemoteAction
import dev.giste.roadbooknavigator.settings.RemoteKey
import dev.giste.roadbooknavigator.settings.RemoteKeys
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomKeyAssignmentTest {

    private val f7 = RemoteKey(KeyEvent.KEYCODE_F7)
    private val up = RemoteKey(KeyEvent.KEYCODE_DPAD_UP)

    @Test
    fun withOnlyKey_newKey_replacesTheActionsKeys() {
        val keys = RemoteKeys.DND2.withOnlyKey(RemoteAction.NEXT_WAYPOINT, f7)

        assertEquals(setOf(f7), keys.keysFor(RemoteAction.NEXT_WAYPOINT))
    }

    @Test
    fun withOnlyKey_newKey_keepsTheOtherActions() {
        val keys = RemoteKeys.DND2.withOnlyKey(RemoteAction.NEXT_WAYPOINT, f7)

        RemoteAction.entries.filter { it != RemoteAction.NEXT_WAYPOINT }.forEach { action ->
            assertEquals(RemoteKeys.DND2.keysFor(action), keys.keysFor(action))
        }
    }

    @Test
    fun withOnlyKey_keyOfAnotherAction_movesIt() {
        val keys = RemoteKeys.DND2.withOnlyKey(RemoteAction.RESET_PARTIAL, up)

        assertEquals(setOf(up), keys.keysFor(RemoteAction.RESET_PARTIAL))
        assertEquals(emptySet<RemoteKey>(), keys.keysFor(RemoteAction.NEXT_WAYPOINT))
    }

    @Test
    fun withOnlyKey_actionWithSeveralKeys_keepsOnlyTheNewOne() {
        val keys = RemoteKeys.TERRA_PIRATA.withOnlyKey(RemoteAction.RESET_PARTIAL, f7)

        assertEquals(setOf(f7), keys.keysFor(RemoteAction.RESET_PARTIAL))
    }

    @Test
    fun withOnlyKey_itsOwnKey_changesNothing() {
        assertEquals(RemoteKeys.DND2, RemoteKeys.DND2.withOnlyKey(RemoteAction.NEXT_WAYPOINT, up))
    }

    @Test
    fun withOnlyKey_noKeysYet_assignsIt() {
        val keys = RemoteKeys(emptyMap()).withOnlyKey(RemoteAction.NEXT_WAYPOINT, f7)

        assertEquals(RemoteKeys(mapOf(RemoteAction.NEXT_WAYPOINT to setOf(f7))), keys)
    }
}
