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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KeyEventInterceptionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val interceptors = KeyEventInterceptors()
    private val event = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP)

    @Test
    fun intercept_noInterceptors_isNotConsumed() {
        assertFalse(interceptors.intercept(event))
    }

    @Test
    fun intercept_interceptorConsumes_isConsumed() {
        interceptors.add { true }

        assertTrue(interceptors.intercept(event))
    }

    @Test
    fun intercept_firstConsumes_laterOnesDoNotGetIt() {
        val received = mutableListOf<String>()
        interceptors.add { received += "first"; true }
        interceptors.add { received += "second"; true }

        interceptors.intercept(event)

        assertEquals(listOf("first"), received)
    }

    @Test
    fun intercept_firstDoesNotConsume_passesItOn() {
        val received = mutableListOf<String>()
        interceptors.add { received += "first"; false }
        interceptors.add { received += "second"; true }

        assertTrue(interceptors.intercept(event))
        assertEquals(listOf("first", "second"), received)
    }

    @Test
    fun intercept_removedInterceptor_getsNothing() {
        val interceptor = KeyEventInterceptor { true }
        interceptors.add(interceptor)

        interceptors.remove(interceptor)

        assertFalse(interceptors.intercept(event))
    }

    @Test
    fun interceptKeyEvents_inTheComposition_getsTheEvents() {
        val received = mutableListOf<KeyEvent>()
        composeRule.setContent {
            CompositionLocalProvider(LocalKeyEventInterceptors provides interceptors) {
                InterceptKeyEvents { received += it; true }
            }
        }
        composeRule.waitForIdle()

        assertTrue(interceptors.intercept(event))
        assertEquals(listOf(event), received)
    }

    @Test
    fun interceptKeyEvents_leavesTheComposition_getsNoMoreEvents() {
        var shown by mutableStateOf(true)
        composeRule.setContent {
            CompositionLocalProvider(LocalKeyEventInterceptors provides interceptors) {
                if (shown) InterceptKeyEvents { true }
            }
        }
        composeRule.waitForIdle()

        shown = false
        composeRule.waitForIdle()

        assertFalse(interceptors.intercept(event))
    }

    @Test
    fun interceptKeyEvents_newHandler_theLatestGetsTheEvents() {
        var consume by mutableStateOf(false)
        composeRule.setContent {
            CompositionLocalProvider(LocalKeyEventInterceptors provides interceptors) {
                val consumes = consume
                InterceptKeyEvents { consumes }
            }
        }
        composeRule.waitForIdle()

        consume = true
        composeRule.waitForIdle()

        assertTrue(interceptors.intercept(event))
    }

    @Test
    fun interceptKeyEvents_noInterceptorsProvided_doesNothing() {
        composeRule.setContent { InterceptKeyEvents { true } }

        composeRule.waitForIdle()
    }
}
