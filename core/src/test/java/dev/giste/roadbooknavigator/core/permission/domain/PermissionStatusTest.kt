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

package dev.giste.roadbooknavigator.core.permission.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PermissionStatusTest {

    @Test
    fun resolve_granted_isGranted() {
        val status = PermissionStatus.resolve(
            isGranted = true,
            shouldShowRationale = false,
            hasBeenRequested = true,
        )

        assertEquals(PermissionStatus.Granted, status)
    }

    @Test
    fun resolve_grantedWithStaleRationale_isGranted() {
        val status = PermissionStatus.resolve(
            isGranted = true,
            shouldShowRationale = true,
            hasBeenRequested = false,
        )

        assertEquals(PermissionStatus.Granted, status)
    }

    @Test
    fun resolve_neverRequested_isDenied() {
        val status = PermissionStatus.resolve(
            isGranted = false,
            shouldShowRationale = false,
            hasBeenRequested = false,
        )

        assertEquals(PermissionStatus.Denied, status)
    }

    @Test
    fun resolve_rationaleRecommended_isShowRationale() {
        val status = PermissionStatus.resolve(
            isGranted = false,
            shouldShowRationale = true,
            hasBeenRequested = true,
        )

        assertEquals(PermissionStatus.ShowRationale, status)
    }

    @Test
    fun resolve_rationaleRecommendedBeforeRequestThisSession_isShowRationale() {
        val status = PermissionStatus.resolve(
            isGranted = false,
            shouldShowRationale = true,
            hasBeenRequested = false,
        )

        assertEquals(PermissionStatus.ShowRationale, status)
    }

    @Test
    fun resolve_requestedAndNoRationale_isPermanentlyDenied() {
        val status = PermissionStatus.resolve(
            isGranted = false,
            shouldShowRationale = false,
            hasBeenRequested = true,
        )

        assertEquals(PermissionStatus.PermanentlyDenied, status)
    }
}
