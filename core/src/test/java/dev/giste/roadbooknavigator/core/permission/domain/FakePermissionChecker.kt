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

import dev.giste.roadbooknavigator.core.permission.AppPermission

/** [PermissionChecker] whose answers tests set directly. Everything starts not granted. */
internal class FakePermissionChecker : PermissionChecker {

    /** Permissions reported as granted. */
    val granted: MutableSet<AppPermission> = mutableSetOf()

    /** Permissions for which a rationale is recommended. */
    val rationale: MutableSet<AppPermission> = mutableSetOf()

    override fun isGranted(permission: AppPermission): Boolean = permission in granted

    override fun shouldShowRationale(permission: AppPermission): Boolean = permission in rationale
}
