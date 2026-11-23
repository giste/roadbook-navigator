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

import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Handles a key event before the views and the system see it.
 *
 * @return whether the event was consumed. A consumed event goes nowhere else, so the system does
 * not act on it either (e.g. no volume change).
 */
internal fun interface KeyEventInterceptor {

    /**
     * Handles [event].
     *
     * @param event the key event.
     * @return whether [event] was consumed.
     */
    fun intercept(event: KeyEvent): Boolean
}

/**
 * The interceptors of an activity's key events, which composables add while they are shown (see
 * [InterceptKeyEvents]).
 *
 * The activity calls [intercept] for every key event, before passing it on. Compose's own key
 * modifiers would need a focused node, and touching the screen takes the focus away, so a remote
 * control would stop working after the first touch.
 *
 * Only for the main thread, where key events and the composition run.
 */
internal class KeyEventInterceptors {

    private val interceptors = mutableListOf<KeyEventInterceptor>()

    /** Starts passing key events to [interceptor], after the ones added before it. */
    fun add(interceptor: KeyEventInterceptor) {
        interceptors += interceptor
    }

    /** Stops passing key events to [interceptor]. */
    fun remove(interceptor: KeyEventInterceptor) {
        interceptors -= interceptor
    }

    /**
     * Passes [event] to the interceptors, in the order they were added, until one consumes it.
     *
     * @param event the key event.
     * @return whether an interceptor consumed [event]. If not, the caller passes it on as usual.
     */
    fun intercept(event: KeyEvent): Boolean = interceptors.toList().any { it.intercept(event) }
}

/**
 * The [KeyEventInterceptors] of the activity hosting the composition, or `null` where no activity
 * provides them (previews and tests of single screens), where [InterceptKeyEvents] does nothing.
 */
internal val LocalKeyEventInterceptors = staticCompositionLocalOf<KeyEventInterceptors?> { null }

/**
 * Passes the activity's key events to [onKeyEvent] while this composable is in the composition,
 * before the views and the system get them. Other windows, such as dialogs, receive their own key
 * events, so [onKeyEvent] gets none while one is in front.
 *
 * @param onKeyEvent handles a key event and returns whether it consumed it. The latest one is
 * called, so it may change between recompositions.
 */
@Composable
internal fun InterceptKeyEvents(onKeyEvent: (KeyEvent) -> Boolean) {
    val interceptors = LocalKeyEventInterceptors.current
    val currentOnKeyEvent by rememberUpdatedState(onKeyEvent)
    DisposableEffect(interceptors) {
        val interceptor = KeyEventInterceptor { currentOnKeyEvent(it) }
        interceptors?.add(interceptor)
        onDispose { interceptors?.remove(interceptor) }
    }
}
