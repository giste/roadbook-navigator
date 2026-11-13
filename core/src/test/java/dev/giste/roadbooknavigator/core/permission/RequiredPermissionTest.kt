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

package dev.giste.roadbooknavigator.core.permission

import android.Manifest
import org.junit.Assert.assertEquals
import org.junit.Test

class RequiredPermissionTest {

    @Test
    fun requested_notGiven_isRequired() {
        val permission = RequiredPermission(
            required = setOf(Manifest.permission.CAMERA),
            title = 1,
            message = 2,
            rationale = 3,
            permanentlyDeniedMessage = 4,
            allowAction = 5,
        )

        assertEquals(setOf(Manifest.permission.CAMERA), permission.requested)
    }

    @Test
    fun requested_supersetOfRequired_isKept() {
        val permission = TestPermissions.location

        assertEquals(
            setOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ),
            permission.requested,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_emptyRequired_throws() {
        RequiredPermission(
            required = emptySet(),
            title = 1,
            message = 2,
            rationale = 3,
            permanentlyDeniedMessage = 4,
            allowAction = 5,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun create_requestedMissingRequired_throws() {
        RequiredPermission(
            required = setOf(Manifest.permission.ACCESS_FINE_LOCATION),
            requested = setOf(Manifest.permission.ACCESS_COARSE_LOCATION),
            title = 1,
            message = 2,
            rationale = 3,
            permanentlyDeniedMessage = 4,
            allowAction = 5,
        )
    }
}
