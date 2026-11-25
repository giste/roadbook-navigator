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

package dev.giste.map.data.remote

import dev.giste.map.MapDownloadServer
import dev.giste.map.domain.RemoteFolderPath
import dev.giste.map.domain.RemoteMapPath
import java.util.Optional
import javax.inject.Inject
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * The addresses of the download site's folders and maps.
 *
 * @param server the server an app provides, if any; otherwise [MapDownloadServer]'s default.
 */
internal class MapSite @Inject constructor(server: Optional<MapDownloadServer>) {

    /** The address of the maps folder. [MapDownloadServer] has checked it is a valid one. */
    private val mapsUrl: HttpUrl = server.orElseGet { MapDownloadServer() }.url.toHttpUrl()

    /**
     * The address of a folder: the maps folder, then its names, with a trailing `/`.
     *
     * @param folder the folder.
     * @return its address.
     */
    fun url(folder: RemoteFolderPath): HttpUrl {
        if (folder.isRoot) return mapsUrl
        // The final empty segment adds the trailing "/" back.
        return below(folder).addPathSegment("").build()
    }

    /**
     * The address of a map file: the maps folder, then its folder names and its file name.
     *
     * @param map the map.
     * @return its address.
     */
    fun url(map: RemoteMapPath): HttpUrl = below(map.folder).addPathSegment(map.name).build()

    /**
     * A builder of the maps folder's address with [folder]'s names added. The maps folder ends in
     * "/", so its last path segment is empty: the first name added takes its place.
     */
    private fun below(folder: RemoteFolderPath): HttpUrl.Builder =
        mapsUrl.newBuilder().apply { folder.segments.forEach { addPathSegment(it) } }
}
