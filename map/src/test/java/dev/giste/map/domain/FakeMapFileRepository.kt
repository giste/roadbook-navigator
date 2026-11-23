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
import kotlinx.coroutines.flow.flow

/**
 * [MapFileRepository] for tests: each collection emits [result] and completes, like the real one,
 * and is counted.
 *
 * @param result the installed maps to report.
 */
internal class FakeMapFileRepository(var result: InstalledMaps) : MapFileRepository {

    /** How many times [installedMaps] was collected. */
    var collections: Int = 0
        private set

    override val installedMaps: Flow<InstalledMaps> = flow {
        collections++
        emit(result)
    }
}

/** A readable map file for tests. */
internal fun testMapFile(name: String = "spain.map"): MapFile = MapFile(name = name, path = "/maps/$name")
