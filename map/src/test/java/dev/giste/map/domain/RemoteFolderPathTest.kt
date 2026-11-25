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

package dev.giste.map.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteFolderPathTest {

    private val italy = RemoteFolderPath(listOf("europe", "italy"))

    @Test
    fun isRoot_noSegments_isTrue() {
        assertTrue(RemoteFolderPath.ROOT.isRoot)
    }

    @Test
    fun isRoot_segments_isFalse() {
        assertFalse(italy.isRoot)
    }

    @Test
    fun child_name_addsItAtTheEnd() {
        assertEquals(italy, RemoteFolderPath.ROOT.child("europe").child("italy"))
    }

    @Test
    fun parent_subFolder_dropsTheLastName() {
        assertEquals(RemoteFolderPath(listOf("europe")), italy.parent())
    }

    @Test
    fun parent_topFolder_isTheRoot() {
        assertEquals(RemoteFolderPath.ROOT, RemoteFolderPath(listOf("europe")).parent())
    }

    @Test
    fun parent_root_isNull() {
        assertNull(RemoteFolderPath.ROOT.parent())
    }

    @Test
    fun toString_segments_joinsThemWithSlashes() {
        assertEquals("europe/italy", italy.toString())
    }

    @Test
    fun toString_root_isEmpty() {
        assertEquals("", RemoteFolderPath.ROOT.toString())
    }

    @Test
    fun parse_text_readsBackToString() {
        assertEquals(italy, RemoteFolderPath.parse("europe/italy"))
    }

    @Test
    fun parse_empty_isTheRoot() {
        assertEquals(RemoteFolderPath.ROOT, RemoteFolderPath.parse(""))
    }

    @Test
    fun parse_parentReference_isRejected() {
        assertThrows(IllegalArgumentException::class.java) { RemoteFolderPath.parse("europe/../..") }
    }

    @Test
    fun constructor_unsafeName_isRejected() {
        assertThrows(IllegalArgumentException::class.java) { RemoteFolderPath(listOf("a/b")) }
    }

    @Test
    fun child_unsafeName_isRejected() {
        assertThrows(IllegalArgumentException::class.java) { italy.child("..") }
    }

    @Test
    fun isSafeRemoteName_plainNames_areSafe() {
        assertTrue(isSafeRemoteName("andorra.map"))
        assertTrue(isSafeRemoteName("nord-est"))
        assertTrue(isSafeRemoteName("ñandú map.map"))
    }

    @Test
    fun isSafeRemoteName_namesThatLeaveTheFolder_areNotSafe() {
        assertFalse(isSafeRemoteName("."))
        assertFalse(isSafeRemoteName(".."))
        assertFalse(isSafeRemoteName("a/b"))
        assertFalse(isSafeRemoteName("a\\b"))
    }

    @Test
    fun isSafeRemoteName_blankOrControlCharacters_areNotSafe() {
        assertFalse(isSafeRemoteName(""))
        assertFalse(isSafeRemoteName("  "))
        assertFalse(isSafeRemoteName("a\u0000.map"))
        assertFalse(isSafeRemoteName("a\n.map"))
    }
}
