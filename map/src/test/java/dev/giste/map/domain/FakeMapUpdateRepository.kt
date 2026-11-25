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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * [MapUpdateRepository] for tests: [siteVersions] emits [current] and each change to it, and
 * [check] only records what it is called with.
 */
internal class FakeMapUpdateRepository : MapUpdateRepository {

    /** The site dates to report. Set it to emit new ones. */
    val current: MutableStateFlow<Map<RemoteMapPath, Instant>> = MutableStateFlow(emptyMap())

    /** The maps passed to each [check], in call order. */
    val checked: MutableList<List<RemoteMapPath>> = mutableListOf()

    override val siteVersions: Flow<Map<RemoteMapPath, Instant>> = current

    override suspend fun check(maps: Collection<RemoteMapPath>) {
        checked += maps.toList()
    }
}
