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

import java.net.URI

/**
 * Where [MapDownloader] finds the maps: a web server that lists its folders as plain directory
 * listings (as Apache's autoindex does), with mapsforge `.map` files and sub-folders.
 *
 * `:map` uses [DEFAULT_URL], the official mapsforge server. To use another one, such as a mirror,
 * an app provides its own instance through Hilt, e.g.
 * `@Provides fun mapDownloadServer() = MapDownloadServer("https://mirror.example.org/maps/v5/")`
 * in a module installed in the `SingletonComponent`.
 *
 * @property url the address of the folder that holds the maps, with `http` or `https` and a
 * trailing `/`.
 * @throws IllegalArgumentException if [url] is not such an address.
 */
public data class MapDownloadServer(public val url: String = DEFAULT_URL) {

    init {
        val uri = runCatching { URI(url) }.getOrNull()
        require(uri != null && uri.scheme in SCHEMES && !uri.host.isNullOrEmpty() && url.endsWith("/")) {
            "Not an http(s) folder address ending in '/': $url"
        }
    }

    /** The default server. */
    public companion object {

        /** The folder of the official mapsforge server with the maps for the current file format. */
        public const val DEFAULT_URL: String = "https://download.mapsforge.org/maps/v5/"

        private val SCHEMES = setOf("http", "https")
    }
}
