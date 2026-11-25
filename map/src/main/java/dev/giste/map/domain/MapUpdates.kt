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

import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * When the maps on the download site last changed, as last checked, to find the installed maps
 * that have an update.
 */
internal interface MapUpdateRepository {

    /**
     * When each map last changed on the download site, for the maps [check] found a date for.
     * Maps never checked, or whose date the site did not give, are left out.
     *
     * A hot flow that emits the current dates at once to each collector, then again after each
     * [check] that changed them, and never completes. It never throws. It is kept in memory only:
     * it starts empty each time the app starts.
     */
    val siteVersions: Flow<Map<RemoteMapPath, Instant>>

    /**
     * Asks the download site when each of [maps] last changed, and keeps the answers in
     * [siteVersions]. A map the site no longer has, or sends no date for, is dropped from it; a map
     * the site could not be asked about (e.g. no connection) keeps what was known. Main-safe. It
     * never throws, except to cancel.
     *
     * @param maps the maps to ask about.
     */
    suspend fun check(maps: Collection<RemoteMapPath>)
}

/**
 * Where this file would be on the download site: downloaded maps keep the site's folders, so it is
 * the same sub-folder and name. Files copied by hand may have names the site could not have.
 *
 * @return the path, or `null` if a name in it is not a safe remote name (see [isSafeRemoteName]).
 */
internal fun InstalledMapFile.remotePath(): RemoteMapPath? {
    val folders = if (folder.isEmpty()) emptyList() else folder.split('/')
    if (!isSafeRemoteName(name) || !folders.all(::isSafeRemoteName)) return null
    return RemoteMapPath(RemoteFolderPath(folders), name)
}

/**
 * Whether the download site has a newer version of this file: the site's date for its path in
 * [siteVersions] is after the file's own, compared to the second (HTTP dates have no fractions,
 * and a downloaded map's modification time is the site's date).
 *
 * A downloaded map that is up to date has the site's very date, so it is not outdated. A map
 * copied by hand has the time it was copied, so it is outdated only if the site changed it after
 * that.
 *
 * @param siteVersions when the maps last changed on the site, as [MapUpdateRepository] knows them.
 * @return `true` if an update is available; `false` if not, or if the site's date is unknown.
 */
internal fun InstalledMapFile.isOutdated(siteVersions: Map<RemoteMapPath, Instant>): Boolean {
    val siteVersion = remotePath()?.let(siteVersions::get) ?: return false
    return siteVersion.epochSecond > modified.epochSecond
}

/**
 * Asks the download site about every installed map file, so [MapUpdateRepository.siteVersions]
 * tells which ones have an update. Files whose names the site could not have are left out.
 *
 * @param files the installed map files.
 * @param updates asks the site and keeps its answers.
 */
internal class CheckMapUpdatesUseCase @Inject constructor(
    private val files: MapFileRepository,
    private val updates: MapUpdateRepository,
) {

    /**
     * Checks the files installed now. Nothing is asked if the map folder cannot be read or holds
     * no map. Main-safe. It never throws, except to cancel.
     */
    suspend operator fun invoke() {
        val installed = files.installedFiles.first() as? InstalledMapFiles.Listed ?: return
        val maps = installed.files.mapNotNull { it.remotePath() }
        if (maps.isNotEmpty()) updates.check(maps)
    }
}
