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

package dev.giste.map.data

import dev.giste.map.MapDownloadServer
import dev.giste.map.domain.RemoteFolderPath
import dev.giste.map.domain.RemoteMapPath
import java.util.Optional
import org.junit.Assert.assertEquals
import org.junit.Test

class MapSiteTest {

    private val site = MapSite(Optional.of(MapDownloadServer("https://mirror.example.org/maps/v5/")))
    private val italy = RemoteFolderPath.ROOT.child("europe").child("italy")

    @Test
    fun url_mapsFolder_isTheServerAddress() {
        assertEquals("https://mirror.example.org/maps/v5/", site.url(RemoteFolderPath.ROOT).toString())
    }

    @Test
    fun url_subFolder_endsInASlash() {
        assertEquals("https://mirror.example.org/maps/v5/europe/italy/", site.url(italy).toString())
    }

    @Test
    fun url_mapInTheMapsFolder_isBesideTheFolders() {
        assertEquals(
            "https://mirror.example.org/maps/v5/world.map",
            site.url(RemoteMapPath(RemoteFolderPath.ROOT, "world.map")).toString(),
        )
    }

    @Test
    fun url_mapInASubFolder_hasItsFolders() {
        assertEquals(
            "https://mirror.example.org/maps/v5/europe/italy/centro.map",
            site.url(RemoteMapPath(italy, "centro.map")).toString(),
        )
    }

    @Test
    fun url_namesWithSpaces_areEncoded() {
        assertEquals(
            "https://mirror.example.org/maps/v5/north%20america/new%20york.map",
            site.url(RemoteMapPath(RemoteFolderPath.ROOT.child("north america"), "new york.map")).toString(),
        )
    }

    @Test
    fun url_noServerProvided_usesTheDefault() {
        assertEquals(MapDownloadServer.DEFAULT_URL, MapSite(Optional.empty()).url(RemoteFolderPath.ROOT).toString())
    }
}
