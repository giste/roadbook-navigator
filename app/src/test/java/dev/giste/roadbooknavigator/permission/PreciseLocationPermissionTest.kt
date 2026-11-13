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

package dev.giste.roadbooknavigator.permission

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Test

class PreciseLocationPermissionTest {

    @Test
    fun required_preciseLocation_isFineOnly() {
        assertEquals(
            setOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PreciseLocationPermission.required,
        )
    }

    @Test
    fun requested_preciseLocation_isFineAndCoarse() {
        assertEquals(
            setOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
            PreciseLocationPermission.requested,
        )
    }
}
