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


package dev.giste.roadbooknavigator.core.logging.data

import android.util.Log
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import timber.log.Timber

class TimberLoggerTest {

    private val tree = RecordingTree()
    private val logger = TimberLogger()

    @Before
    fun setUp() {
        Timber.plant(tree)
    }

    @After
    fun tearDown() {
        Timber.uprootAll()
    }

    @Test
    fun v_message_isLoggedAsVerbose() {
        logger.v("verbose")

        assertEquals(RecordingTree.Line(Log.VERBOSE, null, "verbose", null), tree.lines.single())
    }

    @Test
    fun d_message_isLoggedAsDebug() {
        logger.d("debug")

        assertEquals(RecordingTree.Line(Log.DEBUG, null, "debug", null), tree.lines.single())
    }

    @Test
    fun i_message_isLoggedAsInfo() {
        logger.i("info")

        assertEquals(RecordingTree.Line(Log.INFO, null, "info", null), tree.lines.single())
    }

    @Test
    fun w_message_isLoggedAsWarning() {
        logger.w("warning")

        assertEquals(RecordingTree.Line(Log.WARN, null, "warning", null), tree.lines.single())
    }

    @Test
    fun e_message_isLoggedAsError() {
        logger.e("error")

        assertEquals(RecordingTree.Line(Log.ERROR, null, "error", null), tree.lines.single())
    }

    @Test
    fun e_withThrowable_isLoggedAsErrorWithThrowableAndStackTrace() {
        val exception = IllegalStateException("boom")

        logger.e(exception, "failed %s", "load")

        val line = tree.lines.single()
        assertEquals(Log.ERROR, line.priority)
        assertSame(exception, line.t)
        assertTrue(line.message.startsWith("failed load\n"))
        assertTrue(line.message.contains("IllegalStateException: boom"))
    }

    @Test
    fun d_withArgs_formatsMessage() {
        logger.d("loaded %d waypoints in %s", 12, "stage 1")

        assertEquals("loaded 12 waypoints in stage 1", tree.lines.single().message)
    }

    @Test
    fun d_withoutArgs_logsMessageVerbatim() {
        logger.d("100% done")

        assertEquals("100% done", tree.lines.single().message)
    }

    @Test
    fun d_untaggedLogger_leavesTagToTree() {
        logger.d("message")

        assertNull(tree.lines.single().tag)
    }

    @Test
    fun withTag_everyLevel_usesTag() {
        val tagged = logger.withTag("Tag")
        val exception = RuntimeException()

        tagged.v("v")
        tagged.d("d")
        tagged.i("i")
        tagged.w("w")
        tagged.e("e")
        tagged.e(exception, "e %s", "t")

        assertEquals(
            listOf(Log.VERBOSE, Log.DEBUG, Log.INFO, Log.WARN, Log.ERROR, Log.ERROR),
            tree.lines.map { it.priority },
        )
        assertTrue(tree.lines.all { it.tag == "Tag" })
        assertEquals(listOf("v", "d", "i", "w", "e"), tree.lines.take(5).map { it.message })
        assertTrue(tree.lines.last().message.startsWith("e t\n"))
        assertSame(exception, tree.lines.last().t)
    }

    @Test
    fun withTag_consecutiveCalls_keepTag() {
        val tagged = logger.withTag("Tag")

        tagged.d("first")
        tagged.d("second")

        assertEquals(listOf("Tag", "Tag"), tree.lines.map { it.tag })
    }

    @Test
    fun withTag_originalLogger_staysUntagged() {
        logger.withTag("Tag").d("tagged")

        logger.d("untagged")

        assertNull(tree.lines.last().tag)
    }

    @Test
    fun withTag_onTaggedLogger_usesNewTag() {
        val retagged = logger.withTag("First").withTag("Second")

        retagged.d("message")

        assertEquals("Second", tree.lines.single().tag)
    }

    @Test
    fun d_noTreePlanted_doesNothing() {
        Timber.uprootAll()

        logger.d("message")

        assertTrue(tree.lines.isEmpty())
    }
}
