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

package dev.giste.core.permission

import android.Manifest

/** Sample [RequiredPermission]s for the JVM tests. The resource IDs are arbitrary. */
internal object TestPermissions {

    /** Like the app's precise location: requires fine, requests fine and coarse. */
    val location: RequiredPermission = RequiredPermission(
        required = setOf(Manifest.permission.ACCESS_FINE_LOCATION),
        requested = setOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ),
        title = 1,
        message = 2,
        rationale = 3,
        permanentlyDeniedMessage = 4,
        allowAction = 5,
    )

    /** Requests exactly what it requires. */
    val camera: RequiredPermission = RequiredPermission(
        required = setOf(Manifest.permission.CAMERA),
        title = 11,
        message = 12,
        rationale = 13,
        permanentlyDeniedMessage = 14,
        allowAction = 15,
    )

    /** Requires two manifest permissions at once. */
    val bluetooth: RequiredPermission = RequiredPermission(
        required = setOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        ),
        title = 21,
        message = 22,
        rationale = 23,
        permanentlyDeniedMessage = 24,
        allowAction = 25,
    )
}
