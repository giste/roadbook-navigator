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

/** Reads what Android reports about a permission. Implemented over an `Activity` in `data`. */
internal interface PermissionChecker {

    /**
     * @param permission the permission to check.
     * @return whether [permission] is granted.
     */
    fun isGranted(permission: AppPermission): Boolean

    /**
     * @param permission the permission to check.
     * @return whether Android recommends explaining why the app needs [permission].
     */
    fun shouldShowRationale(permission: AppPermission): Boolean
}
