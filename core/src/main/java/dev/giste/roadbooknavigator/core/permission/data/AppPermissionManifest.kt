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

/**
 * The manifest permission that decides whether this permission is granted. For
 * [AppPermission.PRECISE_LOCATION] it is the fine one: coarse alone means approximate location.
 */
internal val AppPermission.grantingPermission: String
    get() = when (this) {
        AppPermission.PRECISE_LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
    }

/**
 * The manifest permissions to request together. Android 12+ ignores a fine location request
 * that does not include coarse location.
 */
internal val AppPermission.requestedPermissions: List<String>
    get() = when (this) {
        AppPermission.PRECISE_LOCATION -> listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }

/**
 * Finds the app permissions a request result belongs to.
 *
 * @param manifestPermissions the keys of a permission request result.
 * @return every [AppPermission] that requests any of them.
 */
internal fun appPermissionsOf(manifestPermissions: Collection<String>): Set<AppPermission> =
    AppPermission.entries.filterTo(mutableSetOf()) { permission ->
        permission.requestedPermissions.any { it in manifestPermissions }
    }
