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
import org.junit.Test

class InstalledMapsTest {

    @Test
    fun available_withFiles_keepsThem() {
        val files = listOf(testMapFile())

        assertEquals(files, InstalledMaps.Available(files).files)
    }

    @Test(expected = IllegalArgumentException::class)
    fun available_withoutFiles_isRejected() {
        InstalledMaps.Available(emptyList())
    }
}
