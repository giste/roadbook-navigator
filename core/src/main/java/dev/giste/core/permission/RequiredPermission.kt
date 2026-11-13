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

import androidx.annotation.StringRes

/**
 * A runtime permission that [PermissionGate] waits for, described by the module that needs it.
 *
 * `:core` knows no permission in particular: the caller says which manifest permissions to
 * request and require, and gives the texts the gate shows while it is missing. The caller must
 * also declare every [requested] permission with `<uses-permission>` in its own manifest, or
 * Android denies it without showing the dialog.
 *
 * @property required manifest permissions that must all be granted for this permission to count
 * as granted, e.g. `ACCESS_FINE_LOCATION`. Never empty.
 * @property requested manifest permissions asked for together in one system dialog. Contains
 * every [required] one and may add more that Android needs alongside, e.g. `ACCESS_COARSE_LOCATION`
 * together with `ACCESS_FINE_LOCATION` on Android 12+. Defaults to [required].
 * @property title string resource of the screen title, e.g. "Precise location needed".
 * @property message string resource explaining why the app needs the permission. Shown before
 * the first request.
 * @property rationale string resource shown after the user denied it once, when Android
 * recommends explaining it again before asking.
 * @property permanentlyDeniedMessage string resource shown when Android will no longer show the
 * dialog, telling the user how to grant it from the app settings.
 * @property allowAction string resource of the button that shows the system dialog, e.g.
 * "Allow location".
 * @throws IllegalArgumentException if [required] is empty or [requested] does not contain every
 * [required] permission.
 */
public data class RequiredPermission(
    public val required: Set<String>,
    @StringRes public val title: Int,
    @StringRes public val message: Int,
    @StringRes public val rationale: Int,
    @StringRes public val permanentlyDeniedMessage: Int,
    @StringRes public val allowAction: Int,
    public val requested: Set<String> = required,
) {

    init {
        require(required.isNotEmpty()) {
            "A required permission needs at least one manifest permission"
        }
        require(requested.containsAll(required)) {
            "requested $requested must contain every required permission $required"
        }
    }
}
