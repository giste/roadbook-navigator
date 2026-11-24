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

package dev.giste.map

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MapDownloadServerTest {

    @Test
    fun url_default_isTheMapsforgeServer() {
        assertEquals("https://download.mapsforge.org/maps/v5/", MapDownloadServer().url)
    }

    @Test
    fun constructor_httpFolder_isAccepted() {
        assertEquals("http://mirror.example.org/maps/", MapDownloadServer("http://mirror.example.org/maps/").url)
    }

    @Test
    fun constructor_noTrailingSlash_isRejected() {
        assertThrows(IllegalArgumentException::class.java) { MapDownloadServer("https://example.org/maps") }
    }

    @Test
    fun constructor_otherScheme_isRejected() {
        assertThrows(IllegalArgumentException::class.java) { MapDownloadServer("ftp://example.org/maps/") }
    }

    @Test
    fun constructor_noHost_isRejected() {
        assertThrows(IllegalArgumentException::class.java) { MapDownloadServer("https:///maps/") }
    }

    @Test
    fun constructor_notAnAddress_isRejected() {
        assertThrows(IllegalArgumentException::class.java) { MapDownloadServer("not an address/") }
    }
}
