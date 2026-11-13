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

/**
 * Whether one [dev.giste.roadbooknavigator.core.permission.RequiredPermission] is granted and, if
 * not, what the user can still do about it.
 */
internal sealed interface PermissionStatus {

    /** The permission is granted. */
    data object Granted : PermissionStatus

    /** Not granted, and the system dialog can be shown: never asked, or the dialog dismissed. */
    data object Denied : PermissionStatus

    /** Denied once. The system recommends explaining why the app needs it before asking again. */
    data object ShowRationale : PermissionStatus

    /**
     * Denied, and the system will most likely not show the dialog again. The user has to grant
     * it from the app settings.
     */
    data object PermanentlyDenied : PermissionStatus

    companion object {

        /**
         * Resolves the status from what Android reports.
         *
         * Android has no "permanently denied" flag. After a denial, `shouldShowRationale` becomes
         * `true`; after a second denial it goes back to `false` and the dialog is no longer
         * shown. So "not granted, no rationale" means *never asked* before any request, and
         * *permanently denied* after one. The same combination also follows a dismissed dialog,
         * so the screen for [PermanentlyDenied] still lets the user try again.
         *
         * @param isGranted whether Android reports the permission as granted.
         * @param shouldShowRationale whether Android recommends showing a rationale.
         * @param hasBeenRequested whether the app has received a request result for it.
         * @return the resolved status.
         */
        fun resolve(
            isGranted: Boolean,
            shouldShowRationale: Boolean,
            hasBeenRequested: Boolean,
        ): PermissionStatus = when {
            isGranted -> Granted
            shouldShowRationale -> ShowRationale
            hasBeenRequested -> PermanentlyDenied
            else -> Denied
        }
    }
}
