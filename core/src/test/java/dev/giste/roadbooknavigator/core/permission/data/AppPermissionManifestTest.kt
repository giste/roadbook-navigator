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

package dev.giste.roadbooknavigator.core.permission.data

import android.Manifest
import dev.giste.roadbooknavigator.core.permission.AppPermission
import org.junit.Assert.assertEquals
import org.junit.Test

class AppPermissionManifestTest {

    @Test
    fun grantingPermission_preciseLocation_isFineLocation() {
        assertEquals(
            Manifest.permission.ACCESS_FINE_LOCATION,
            AppPermission.PRECISE_LOCATION.grantingPermission,
        )
    }

    @Test
    fun requestedPermissions_preciseLocation_includesFineAndCoarse() {
        assertEquals(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
            AppPermission.PRECISE_LOCATION.requestedPermissions,
        )
    }

    @Test
    fun appPermissionsOf_locationResult_isPreciseLocation() {
        val permissions = appPermissionsOf(
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
        )

        assertEquals(setOf(AppPermission.PRECISE_LOCATION), permissions)
    }

    @Test
    fun appPermissionsOf_coarseOnly_isPreciseLocation() {
        val permissions = appPermissionsOf(listOf(Manifest.permission.ACCESS_COARSE_LOCATION))

        assertEquals(setOf(AppPermission.PRECISE_LOCATION), permissions)
    }

    @Test
    fun appPermissionsOf_unknownPermission_isEmpty() {
        val permissions = appPermissionsOf(listOf(Manifest.permission.CAMERA))

        assertEquals(emptySet<AppPermission>(), permissions)
    }

    @Test
    fun appPermissionsOf_emptyResult_isEmpty() {
        val permissions = appPermissionsOf(emptyList())

        assertEquals(emptySet<AppPermission>(), permissions)
    }
}
