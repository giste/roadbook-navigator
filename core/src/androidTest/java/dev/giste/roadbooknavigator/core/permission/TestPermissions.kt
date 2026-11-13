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
import dev.giste.roadbooknavigator.core.test.R

/** Sample [RequiredPermission] for the instrumented tests, with test-only string resources. */
internal object TestPermissions {

    /** Like the app's precise location: requires fine, requests fine and coarse. */
    val location: RequiredPermission = RequiredPermission(
        required = setOf(Manifest.permission.ACCESS_FINE_LOCATION),
        requested = setOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ),
        title = R.string.core_test_permission_title,
        message = R.string.core_test_permission_message,
        rationale = R.string.core_test_permission_rationale,
        permanentlyDeniedMessage = R.string.core_test_permission_permanently_denied,
        allowAction = R.string.core_test_permission_allow,
    )
}
