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
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * [MapDownloadRepository] for tests: [downloads] emits [current] and each change to it, and
 * [start] and [cancel] only record what they are called with.
 */
internal class FakeMapDownloadRepository : MapDownloadRepository {

    /** The downloads to report. Set it to emit new ones. */
    val current: MutableStateFlow<List<MapDownload>> = MutableStateFlow(emptyList())

    /** The maps passed to [start] with whether mobile data was allowed, in call order. */
    val started: MutableList<Pair<RemoteMapPath, Boolean>> = mutableListOf()

    /** The maps passed to [cancel], in call order. */
    val cancelled: MutableList<RemoteMapPath> = mutableListOf()

    override val downloads: Flow<List<MapDownload>> = current

    override suspend fun start(map: RemoteMapPath, allowMobileData: Boolean) {
        started += map to allowMobileData
    }

    override suspend fun cancel(map: RemoteMapPath) {
        cancelled += map
    }
}
