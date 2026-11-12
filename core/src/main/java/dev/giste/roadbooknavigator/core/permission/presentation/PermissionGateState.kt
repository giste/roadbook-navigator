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

package dev.giste.roadbooknavigator.core.permission.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.giste.roadbooknavigator.core.permission.AppPermission
import dev.giste.roadbooknavigator.core.permission.domain.PermissionChecker
import dev.giste.roadbooknavigator.core.permission.domain.PermissionStatus

/**
 * A required permission that is not granted yet, and its status.
 *
 * @property permission the permission the gate is waiting for.
 * @property status why it is not granted. Never [PermissionStatus.Granted].
 */
internal data class PendingPermission(
    val permission: AppPermission,
    val status: PermissionStatus,
)

/**
 * State holder for [dev.giste.roadbooknavigator.core.permission.PermissionGate].
 *
 * Plain Compose state instead of a ViewModel: the rationale flag needs the current `Activity`,
 * and the only state worth keeping (which permissions were requested) survives configuration
 * changes and process death through [saver].
 *
 * @param permissions the required permissions, in the order they are asked for.
 * @param checker reads the current Android state of each permission.
 * @param requested permissions that already got a request result, restored from saved state.
 */
@Stable
internal class PermissionGateState(
    private val permissions: List<AppPermission>,
    private val checker: PermissionChecker,
    requested: Set<AppPermission> = emptySet(),
) {

    /** Permissions that already got a request result. Needed to detect "permanently denied". */
    var requested: Set<AppPermission> = requested
        private set

    /**
     * The first required permission that is not granted, or `null` when all are granted and the
     * gate shows its content. Asking one at a time lets each screen explain one permission.
     */
    var pending: PendingPermission? by mutableStateOf(computePending())
        private set

    /** Re-reads every permission, e.g. when the user comes back from the settings. */
    fun refresh() {
        pending = computePending()
    }

    /**
     * Records a request result and re-reads every permission.
     *
     * @param permissions the permissions the result belongs to.
     */
    fun onRequestResult(permissions: Set<AppPermission>) {
        requested = requested + permissions
        refresh()
    }

    private fun computePending(): PendingPermission? =
        permissions.firstNotNullOfOrNull { permission ->
            val status = PermissionStatus.resolve(
                isGranted = checker.isGranted(permission),
                shouldShowRationale = checker.shouldShowRationale(permission),
                hasBeenRequested = permission in requested,
            )
            if (status == PermissionStatus.Granted) null else PendingPermission(permission, status)
        }

    companion object {

        /**
         * Saves the [requested] permissions by name, so a denial is still recognized as
         * permanent after a configuration change or process death.
         *
         * @param permissions the required permissions of the restored state.
         * @param checker the checker of the restored state.
         * @return a saver that keeps [requested] and rebuilds everything else.
         */
        fun saver(
            permissions: List<AppPermission>,
            checker: PermissionChecker,
        ): Saver<PermissionGateState, List<String>> = Saver(
            save = { state -> state.requested.map { it.name } },
            restore = { names ->
                PermissionGateState(
                    permissions = permissions,
                    checker = checker,
                    requested = names.mapTo(mutableSetOf()) { AppPermission.valueOf(it) },
                )
            },
        )
    }
}

/**
 * Remembers a [PermissionGateState] that survives configuration changes and process death.
 *
 * @param permissions the required permissions. They are asked for in [AppPermission] order.
 * @param checker reads the current Android state of each permission.
 * @return the remembered state.
 */
@Composable
internal fun rememberPermissionGateState(
    permissions: Set<AppPermission>,
    checker: PermissionChecker,
): PermissionGateState {
    val ordered = permissions.sortedBy { it.ordinal }
    return rememberSaveable(ordered, checker, saver = PermissionGateState.saver(ordered, checker)) {
        PermissionGateState(ordered, checker)
    }
}
