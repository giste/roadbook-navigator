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

package dev.giste.core.permission.presentation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.giste.core.permission.RequiredPermission
import dev.giste.core.permission.domain.PermissionChecker
import dev.giste.core.permission.domain.PermissionStatus

/**
 * A required permission that is not granted yet, and its status.
 *
 * @property permission the permission the gate is waiting for.
 * @property status why it is not granted. Never [PermissionStatus.Granted].
 */
internal data class PendingPermission(
    val permission: RequiredPermission,
    val status: PermissionStatus,
) {

    /** String resource of the explanation that fits [status]. */
    @get:StringRes
    val message: Int
        get() = when (status) {
            PermissionStatus.ShowRationale -> permission.rationale
            PermissionStatus.PermanentlyDenied -> permission.permanentlyDeniedMessage
            // Granted never reaches the screen; it shares the first-time explanation.
            PermissionStatus.Denied, PermissionStatus.Granted -> permission.message
        }
}

/**
 * State holder for [dev.giste.core.permission.PermissionGate].
 *
 * Plain Compose state instead of a ViewModel: the rationale flag needs the current `Activity`,
 * and the only state worth keeping (which manifest permissions were requested) survives
 * configuration changes and process death through [saver].
 *
 * @param permissions the required permissions, in the order they are asked for.
 * @param checker reads the current Android state of each manifest permission.
 * @param requested manifest permissions that already got a request result, restored from saved
 * state.
 */
@Stable
internal class PermissionGateState(
    private val permissions: List<RequiredPermission>,
    private val checker: PermissionChecker,
    requested: Set<String> = emptySet(),
) {

    /**
     * Manifest permissions that already got a request result. Needed to detect "permanently
     * denied".
     */
    var requested: Set<String> = requested
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
     * @param manifestPermissions the manifest permissions the result contains (its keys).
     */
    fun onRequestResult(manifestPermissions: Collection<String>) {
        requested = requested + manifestPermissions
        refresh()
    }

    /**
     * Resolves each permission in order and returns the first one that is not granted.
     *
     * A permission is granted when every one of its `required` manifest permissions is. The
     * rationale flag only matters for the missing ones, and a single request result for any of
     * its `requested` manifest permissions counts as having requested it, because they are always
     * requested together.
     */
    private fun computePending(): PendingPermission? =
        permissions.firstNotNullOfOrNull { permission ->
            val missing = permission.required.filterNot(checker::isGranted)
            val status = PermissionStatus.resolve(
                isGranted = missing.isEmpty(),
                shouldShowRationale = missing.any(checker::shouldShowRationale),
                hasBeenRequested = permission.requested.any { it in requested },
            )
            if (status == PermissionStatus.Granted) null else PendingPermission(permission, status)
        }

    companion object {

        /**
         * Saves the [requested] manifest permissions, so a denial is still recognized as
         * permanent after a configuration change or process death.
         *
         * @param permissions the required permissions of the restored state.
         * @param checker the checker of the restored state.
         * @return a saver that keeps [requested] and rebuilds everything else.
         */
        fun saver(
            permissions: List<RequiredPermission>,
            checker: PermissionChecker,
        ): Saver<PermissionGateState, List<String>> = Saver(
            save = { state -> state.requested.toList() },
            restore = { names ->
                PermissionGateState(
                    permissions = permissions,
                    checker = checker,
                    requested = names.toSet(),
                )
            },
        )
    }
}

/**
 * Remembers a [PermissionGateState] that survives configuration changes and process death.
 *
 * @param permissions the required permissions, in the order they are asked for.
 * @param checker reads the current Android state of each manifest permission.
 * @return the remembered state.
 */
@Composable
internal fun rememberPermissionGateState(
    permissions: List<RequiredPermission>,
    checker: PermissionChecker,
): PermissionGateState = rememberSaveable(
    permissions,
    checker,
    saver = PermissionGateState.saver(permissions, checker),
) {
    PermissionGateState(permissions, checker)
}
