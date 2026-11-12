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

import androidx.compose.runtime.saveable.SaverScope
import dev.giste.roadbooknavigator.core.permission.AppPermission
import dev.giste.roadbooknavigator.core.permission.domain.FakePermissionChecker
import dev.giste.roadbooknavigator.core.permission.domain.PermissionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PermissionGateStateTest {

    private val location = AppPermission.PRECISE_LOCATION
    private val checker = FakePermissionChecker()

    private fun createState(requested: Set<AppPermission> = emptySet()) =
        PermissionGateState(listOf(location), checker, requested)

    @Test
    fun pending_allGranted_isNull() {
        checker.granted += location

        val state = createState()

        assertNull(state.pending)
    }

    @Test
    fun pending_neverRequested_isDenied() {
        val state = createState()

        assertEquals(PendingPermission(location, PermissionStatus.Denied), state.pending)
    }

    @Test
    fun pending_noRequiredPermissions_isNull() {
        val state = PermissionGateState(emptyList(), checker)

        assertNull(state.pending)
    }

    @Test
    fun onRequestResult_deniedWithRationale_isShowRationale() {
        val state = createState()
        checker.rationale += location

        state.onRequestResult(setOf(location))

        assertEquals(PendingPermission(location, PermissionStatus.ShowRationale), state.pending)
    }

    @Test
    fun onRequestResult_deniedWithoutRationale_isPermanentlyDenied() {
        val state = createState()

        state.onRequestResult(setOf(location))

        assertEquals(PendingPermission(location, PermissionStatus.PermanentlyDenied), state.pending)
    }

    @Test
    fun onRequestResult_granted_isNull() {
        val state = createState()
        checker.granted += location

        state.onRequestResult(setOf(location))

        assertNull(state.pending)
    }

    @Test
    fun onRequestResult_emptyResult_keepsDenied() {
        val state = createState()

        state.onRequestResult(emptySet())

        assertEquals(PendingPermission(location, PermissionStatus.Denied), state.pending)
    }

    @Test
    fun refresh_grantedInSettings_isNull() {
        val state = createState(requested = setOf(location))
        checker.granted += location

        state.refresh()

        assertNull(state.pending)
    }

    @Test
    fun refresh_revokedWhileAway_isDenied() {
        checker.granted += location
        val state = createState()
        checker.granted -= location

        state.refresh()

        assertEquals(PendingPermission(location, PermissionStatus.Denied), state.pending)
    }

    @Test
    fun saver_roundTrip_keepsRequestedPermissions() {
        val saver = PermissionGateState.saver(listOf(location), checker)
        val state = createState()
        state.onRequestResult(setOf(location))

        val saved = with(saver) { SaverScope { true }.save(state) }
        val restored = saver.restore(checkNotNull(saved))

        assertEquals(setOf(location), restored?.requested)
        assertEquals(
            PendingPermission(location, PermissionStatus.PermanentlyDenied),
            restored?.pending,
        )
    }
}
