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

package dev.giste.roadbooknavigator.core.coroutines.di

import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CoroutinesModuleTest {

    private val dispatcher = StandardTestDispatcher()
    private val applicationScope: CoroutineScope =
        CoroutinesModule.provideApplicationScope(dispatcher)

    @After
    fun tearDown() {
        applicationScope.cancel()
    }

    @Test
    fun provideIoDispatcher_returnsDispatchersIo() {
        assertSame(Dispatchers.IO, CoroutinesModule.provideIoDispatcher())
    }

    @Test
    fun provideDefaultDispatcher_returnsDispatchersDefault() {
        assertSame(Dispatchers.Default, CoroutinesModule.provideDefaultDispatcher())
    }

    @Test
    fun provideApplicationScope_givenDispatcher_runsOnIt() {
        assertSame(dispatcher, applicationScope.coroutineContext[ContinuationInterceptor])
    }

    @Test
    fun provideApplicationScope_childFails_scopeStaysActive() {
        // The handler keeps the expected failure away from the test's uncaught-exception check.
        applicationScope.launch(CoroutineExceptionHandler { _, _ -> }) { error("child failure") }

        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(applicationScope.isActive)
    }

    @Test
    fun provideApplicationScope_childFails_siblingKeepsRunning() {
        var siblingCompleted = false
        val sibling = applicationScope.launch {
            yield()
            siblingCompleted = true
        }
        applicationScope.launch(CoroutineExceptionHandler { _, _ -> }) { error("child failure") }

        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(sibling.isCancelled)
        assertTrue(siblingCompleted)
    }
}
