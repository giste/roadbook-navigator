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


package dev.giste.roadbooknavigator.core.logging

import dev.giste.roadbooknavigator.core.logging.data.AutomaticTagTree
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import timber.log.Timber

class DebugLoggingTest {

    @Before
    fun setUp() {
        Timber.uprootAll()
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
    }

    @Test
    fun install_noTreePlanted_plantsAutomaticTagTree() {
        DebugLogging.install()

        assertTrue(Timber.forest().single() is AutomaticTagTree)
    }

    @Test
    fun install_calledTwice_plantsOnlyOneTree() {
        DebugLogging.install()

        DebugLogging.install()

        assertEquals(1, Timber.treeCount)
    }

    @Test
    fun install_otherTreePlanted_addsAutomaticTagTree() {
        Timber.plant(Timber.DebugTree())

        DebugLogging.install()

        assertEquals(2, Timber.treeCount)
        assertEquals(1, Timber.forest().count { it is AutomaticTagTree })
    }
}
