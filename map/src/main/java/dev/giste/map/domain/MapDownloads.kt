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

import kotlinx.coroutines.flow.Flow

/**
 * A map file on the download site: the folder that holds it and its name.
 *
 * A downloaded map keeps the site's folders, so this is also where it goes on the device,
 * relative to the map folder.
 *
 * @property folder the folder of the download site that holds it.
 * @property name its file name, a safe name (see [isSafeRemoteName]), e.g. `centro.map`.
 */
internal data class RemoteMapPath(val folder: RemoteFolderPath, val name: String) {

    init {
        require(isSafeRemoteName(name)) { "Unsafe map file name: $name" }
    }

    /**
     * The folder names and the file name with `/` between them, e.g. `europe/italy/centro.map`:
     * the path of the map below the download site's maps folder, and below the map folder on the
     * device.
     */
    override fun toString(): String = if (folder.isRoot) name else "$folder/$name"

    /** Reading paths back from text. */
    companion object {

        /**
         * Reads a path written by [toString].
         *
         * @param path folder names and the file name, with `/` between them.
         * @return the path.
         * @throws IllegalArgumentException if a name in it is not safe.
         */
        fun parse(path: String): RemoteMapPath {
            val folder = path.substringBeforeLast('/', missingDelimiterValue = "")
            return RemoteMapPath(RemoteFolderPath.parse(folder), path.substringAfterLast('/'))
        }
    }
}

/** A map being downloaded, waiting to be, or that could not be. */
internal sealed interface MapDownload {

    /** The map. */
    val map: RemoteMapPath

    /**
     * Waiting to start: another map is being downloaded, or the device is not on Wi-Fi.
     *
     * @property map the map.
     */
    data class Queued(override val map: RemoteMapPath) : MapDownload

    /**
     * Being downloaded.
     *
     * @property map the map.
     * @property downloaded how much of it is on the device, including what earlier tries left.
     * @property total its whole size, or `null` while the site has not said it.
     */
    data class Running(
        override val map: RemoteMapPath,
        val downloaded: FileSize,
        val total: FileSize?,
    ) : MapDownload {

        /** How much is done, from 0 to 1, or `null` while [total] is unknown. */
        val fraction: Float?
            get() = total?.takeIf { it.bytes > 0 }?.let { (downloaded.bytes.toFloat() / it.bytes).coerceIn(0f, 1f) }
    }

    /**
     * Stopped for good: downloading it again may work once the reason is gone.
     *
     * @property map the map.
     * @property reason why.
     */
    data class Failed(override val map: RemoteMapPath, val reason: MapDownloadFailure) : MapDownload
}

/**
 * Why a map download stopped for good. Failures that may go away by themselves, such as a lost
 * connection or a busy server, are not here: those downloads wait and try again.
 */
internal enum class MapDownloadFailure {

    /** The download site does not have the map, or refuses to send it. */
    SERVER_ERROR,

    /** The device has not enough free space for the map. */
    NOT_ENOUGH_SPACE,

    /** The map was downloaded, but the renderer cannot read it. It was not kept. */
    UNREADABLE_MAP,

    /** The map could not be written to the device. */
    STORAGE_ERROR,
}

/** Downloads maps from the download site into the map folder, one at a time. */
internal interface MapDownloadRepository {

    /**
     * The downloads that are waiting, running or failed, at most one per map. Maps downloaded
     * successfully are left out: they are installed.
     *
     * A cold flow that emits the current downloads at once and again after every change, and
     * never completes. It never throws.
     */
    val downloads: Flow<List<MapDownload>>

    /**
     * Downloads [map] into the map folder, in the folder it has on the site, after the maps
     * already queued. Nothing is queued twice: if [map] is already queued or being downloaded,
     * only the mobile data answer applies. Main-safe. It never throws, except to cancel.
     *
     * @param map the map to download.
     * @param allowMobileData whether the rider agreed to use mobile data. It applies to the whole
     * queue: every map already queued may then download on mobile data too. If `false`, [map]
     * waits for Wi-Fi, and the maps already queued keep what they were allowed.
     */
    suspend fun start(map: RemoteMapPath, allowMobileData: Boolean)

    /**
     * Stops the download of [map], whether it is waiting or running, and throws away what was
     * downloaded of it. The maps queued after it keep their order. Nothing happens if [map] is not
     * queued. Main-safe. It never throws, except to cancel.
     *
     * @param map the map whose download to stop.
     */
    suspend fun cancel(map: RemoteMapPath)
}

/** The device's network connection, to know whether a download would use mobile data. */
internal fun interface NetworkConnection {

    /**
     * Whether the device is on a network that does not charge by the amount of data, such as
     * Wi-Fi. `false` on mobile data, on a hotspot that says it is metered, and with no network.
     *
     * @return `true` on an unmetered network.
     */
    fun isUnmetered(): Boolean
}
