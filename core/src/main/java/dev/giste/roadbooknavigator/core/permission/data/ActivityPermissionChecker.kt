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

import android.app.Activity
import android.content.pm.PackageManager
import dev.giste.roadbooknavigator.core.permission.AppPermission
import dev.giste.roadbooknavigator.core.permission.domain.PermissionChecker

/**
 * [PermissionChecker] backed by an [Activity]: the rationale flag is only available there.
 *
 * Only keep it as long as the activity, e.g. `remember(activity)` in a composable.
 */
internal class ActivityPermissionChecker(private val activity: Activity) : PermissionChecker {

    override fun isGranted(permission: AppPermission): Boolean =
        activity.checkSelfPermission(permission.grantingPermission) ==
            PackageManager.PERMISSION_GRANTED

    override fun shouldShowRationale(permission: AppPermission): Boolean =
        activity.shouldShowRequestPermissionRationale(permission.grantingPermission)
}
