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

import dev.giste.roadbooknavigator.settings.RemoteAction
import dev.giste.roadbooknavigator.settings.RemoteKey
import dev.giste.roadbooknavigator.settings.RemoteKeys

/** How long the reset key must be held before it resets the partial, in milliseconds. */
internal const val RESET_HOLD_MILLIS: Long = 1_000

/**
 * One key event from a remote control (or any keyboard), free of Android types.
 *
 * @property key the key.
 * @property type whether the key went down (first press or a repeat while held) or up.
 * @property repeatCount 0 for the first [KeyPressType.DOWN] of a press; 1, 2, … for the repeats
 * the system sends while the key is held. 0 for [KeyPressType.UP].
 * @property timeMillis when the event happened, in milliseconds on a monotonic clock (the same
 * for every event).
 */
internal data class KeyPress(
    val key: RemoteKey,
    val type: KeyPressType,
    val repeatCount: Int,
    val timeMillis: Long,
)

/** Whether a [KeyPress] is the key going down or coming up. */
internal enum class KeyPressType {

    /** The key went down, or is still down and the system repeated it. */
    DOWN,

    /** The key was released. */
    UP,
}

/** What to do with a [KeyPress], decided by [RemoteKeyInterpreter]. */
internal sealed interface RemoteKeyOutcome {

    /** The key is bound to no action: the system handles it as usual (e.g. the volume). */
    data object NotBound : RemoteKeyOutcome

    /**
     * The key is bound to an action, but nothing happens on this event, e.g. the release of a key
     * or a reset key not held long enough yet. It is still consumed, so the system does not act on
     * it either (no volume change, no music skipping).
     */
    data object Consumed : RemoteKeyOutcome

    /**
     * Perform [action] now. The event is consumed.
     *
     * @property action what the rider asked for.
     */
    data class Perform(val action: RemoteAction) : RemoteKeyOutcome
}

/**
 * Turns the key presses of a remote control into [RemoteAction]s, following the keys the rider
 * configured. An application rule: how the app reacts to its remote.
 *
 * - Moving between waypoints and changing the partial act on every [KeyPressType.DOWN], including
 *   the repeats while a key is held, so holding a key keeps scrolling or counting.
 * - Resetting the partial cannot be undone, so it only acts once the key has been held for
 *   [RESET_HOLD_MILLIS], and only once per hold. A bump on the remote does nothing. It acts on the
 *   repeat that crosses the threshold, so the rider sees the reset while still holding the key; or
 *   on the release, for a keyboard that sends no repeats.
 *
 * The hold is timed from the first [KeyPressType.DOWN] of the press, not from the down time each
 * event carries, because some senders (`adb shell input`, for one) give their releases no down
 * time of their own.
 *
 * It remembers the reset keys being held, so use one instance per stream of key events, from one
 * thread.
 */
internal class RemoteKeyInterpreter {

    /** When each held reset key went down, in [KeyPress.timeMillis]. */
    private val holdStarts = mutableMapOf<RemoteKey, Long>()

    /** The held keys that already reset the partial during their current hold. */
    private val actedDuringHold = mutableSetOf<RemoteKey>()

    /**
     * Decides what to do with [press].
     *
     * @param press the key event.
     * @param keys the keys bound to each action.
     * @return [RemoteKeyOutcome.NotBound] when [press]'s key is bound to no action; otherwise the
     * action to perform now, if any.
     */
    fun interpret(press: KeyPress, keys: RemoteKeys): RemoteKeyOutcome {
        val action = keys.actionFor(press.key) ?: return RemoteKeyOutcome.NotBound
        val acts = if (action == RemoteAction.RESET_PARTIAL) {
            actsAfterHold(press)
        } else {
            press.type == KeyPressType.DOWN
        }
        return if (acts) RemoteKeyOutcome.Perform(action) else RemoteKeyOutcome.Consumed
    }

    /**
     * Whether [press], of a key that needs holding, acts now: the first event of its hold to reach
     * [RESET_HOLD_MILLIS].
     */
    private fun actsAfterHold(press: KeyPress): Boolean {
        val key = press.key
        if (press.type == KeyPressType.DOWN && press.repeatCount == 0) {
            // A new press starts a new hold, even if the release of the last one was missed.
            holdStarts[key] = press.timeMillis
            actedDuringHold -= key
        }
        // A repeat or a release whose first press went elsewhere (e.g. to a dialog) starts timing
        // here, so it never resets at once.
        val heldMillis = press.timeMillis - holdStarts.getOrPut(key) { press.timeMillis }
        val acts = key !in actedDuringHold && heldMillis >= RESET_HOLD_MILLIS
        if (press.type == KeyPressType.UP) {
            // The hold ends here, so it is forgotten.
            holdStarts -= key
            actedDuringHold -= key
        } else if (acts) {
            actedDuringHold += key
        }
        return acts
    }
}
