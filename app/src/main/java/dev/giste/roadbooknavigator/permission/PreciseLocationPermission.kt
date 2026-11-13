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
import dev.giste.roadbooknavigator.R
import dev.giste.core.permission.RequiredPermission

/**
 * Precise location while the app is in use, which the odometer and the map need.
 *
 * Only `ACCESS_FINE_LOCATION` is required: if the user allows only approximate location, the
 * gate stays closed, because it is not accurate enough to measure distances.
 * `ACCESS_COARSE_LOCATION` is requested too, because Android 12+ ignores a fine location request
 * without it. Both are declared in this module's manifest.
 *
 * It stays in `:app`, not in `:location`, because its texts are specific to this app, and a
 * reusable module must not contain them (AGENTS.md §4.1).
 */
internal val PreciseLocationPermission: RequiredPermission = RequiredPermission(
    required = setOf(Manifest.permission.ACCESS_FINE_LOCATION),
    requested = setOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ),
    title = R.string.app_permission_location_title,
    message = R.string.app_permission_location_message,
    rationale = R.string.app_permission_location_rationale,
    permanentlyDeniedMessage = R.string.app_permission_location_permanently_denied_message,
    allowAction = R.string.app_permission_location_action_allow,
)
