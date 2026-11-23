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

/**
 * Which keys trigger each [RemoteAction]. An action may have several keys (a remote may send
 * either), or none.
 *
 * A key triggers at most one action, so pressing it is never ambiguous. An action that is missing
 * and one with no keys are the same: two [RemoteKeys] are equal when every action has the same
 * keys.
 *
 * @param bindings the keys of each action. A missing action has no keys.
 * @throws IllegalArgumentException if a key is bound to more than one action.
 */
public class RemoteKeys(bindings: Map<RemoteAction, Set<RemoteKey>>) {

    /** The keys of each action that has any. Actions without keys are left out. */
    public val bindings: Map<RemoteAction, Set<RemoteKey>> =
        bindings.filterValues { it.isNotEmpty() }

    init {
        val duplicated = this.bindings.values.flatten()
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
        require(duplicated.isEmpty()) { "Keys bound to more than one action: ${duplicated.keys}" }
    }

    /**
     * The keys of [action].
     *
     * @param action the action to look up.
     * @return its keys, empty if it has none.
     */
    public fun keysFor(action: RemoteAction): Set<RemoteKey> = bindings[action].orEmpty()

    /**
     * The action [key] triggers.
     *
     * @param key the pressed key.
     * @return its action, or `null` if the key triggers none.
     */
    public fun actionFor(key: RemoteKey): RemoteAction? =
        bindings.entries.firstOrNull { key in it.value }?.key

    override fun equals(other: Any?): Boolean = other is RemoteKeys && bindings == other.bindings

    override fun hashCode(): Int = bindings.hashCode()

    override fun toString(): String = "RemoteKeys($bindings)"

    /** The keys of the remote models with fixed keys. */
    public companion object {

        /**
         * The DND2 remote: D-pad up and down move the roadbook, right and left change the
         * partial, and F6 resets it.
         */
        public val DND2: RemoteKeys = RemoteKeys(
            mapOf(
                RemoteAction.NEXT_WAYPOINT to keys(KeyEvent.KEYCODE_DPAD_UP),
                RemoteAction.PREVIOUS_WAYPOINT to keys(KeyEvent.KEYCODE_DPAD_DOWN),
                RemoteAction.INCREASE_PARTIAL to keys(KeyEvent.KEYCODE_DPAD_RIGHT),
                RemoteAction.DECREASE_PARTIAL to keys(KeyEvent.KEYCODE_DPAD_LEFT),
                RemoteAction.RESET_PARTIAL to keys(KeyEvent.KEYCODE_F6),
            ),
        )

        /**
         * The Terra Pirata remote: next and previous track move the roadbook, volume up and down
         * change the partial, and play/pause resets it. Some units send play instead of
         * play/pause, so both reset.
         */
        public val TERRA_PIRATA: RemoteKeys = RemoteKeys(
            mapOf(
                RemoteAction.NEXT_WAYPOINT to keys(KeyEvent.KEYCODE_MEDIA_NEXT),
                RemoteAction.PREVIOUS_WAYPOINT to keys(KeyEvent.KEYCODE_MEDIA_PREVIOUS),
                RemoteAction.INCREASE_PARTIAL to keys(KeyEvent.KEYCODE_VOLUME_UP),
                RemoteAction.DECREASE_PARTIAL to keys(KeyEvent.KEYCODE_VOLUME_DOWN),
                RemoteAction.RESET_PARTIAL to
                    keys(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY),
            ),
        )

        /** The [RemoteKey]s of [codes]. */
        private fun keys(vararg codes: Int): Set<RemoteKey> = codes.map(::RemoteKey).toSet()
    }
}
