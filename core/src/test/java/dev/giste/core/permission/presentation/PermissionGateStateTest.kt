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

import android.Manifest
import androidx.compose.runtime.saveable.SaverScope
import dev.giste.core.permission.RequiredPermission
import dev.giste.core.permission.TestPermissions
import dev.giste.core.permission.domain.FakePermissionChecker
import dev.giste.core.permission.domain.PermissionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PermissionGateStateTest {

    private val location = TestPermissions.location
    private val camera = TestPermissions.camera
    private val bluetooth = TestPermissions.bluetooth
    private val fine = Manifest.permission.ACCESS_FINE_LOCATION
    private val coarse = Manifest.permission.ACCESS_COARSE_LOCATION
    private val checker = FakePermissionChecker()

    private fun createState(
        permissions: List<RequiredPermission> = listOf(location),
        requested: Set<String> = emptySet(),
    ) = PermissionGateState(permissions, checker, requested)

    @Test
    fun pending_allGranted_isNull() {
        checker.granted += fine

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
        val state = createState(permissions = emptyList())

        assertNull(state.pending)
    }

    @Test
    fun pending_onlyOptionalRequestedGranted_isDenied() {
        // Approximate location only: coarse is requested but fine is the required one.
        checker.granted += coarse

        val state = createState()

        assertEquals(PendingPermission(location, PermissionStatus.Denied), state.pending)
    }

    @Test
    fun pending_someRequiredMissing_isDenied() {
        checker.granted += Manifest.permission.BLUETOOTH_SCAN

        val state = createState(permissions = listOf(bluetooth))

        assertEquals(PendingPermission(bluetooth, PermissionStatus.Denied), state.pending)
    }

    @Test
    fun pending_everyRequiredGranted_isNull() {
        checker.granted += bluetooth.required

        val state = createState(permissions = listOf(bluetooth))

        assertNull(state.pending)
    }

    @Test
    fun pending_severalMissing_isFirstInListOrder() {
        val state = createState(permissions = listOf(camera, location))

        assertEquals(PendingPermission(camera, PermissionStatus.Denied), state.pending)
    }

    @Test
    fun pending_firstGranted_isNextMissing() {
        checker.granted += Manifest.permission.CAMERA

        val state = createState(permissions = listOf(camera, location))

        assertEquals(PendingPermission(location, PermissionStatus.Denied), state.pending)
    }

    @Test
    fun pending_rationaleOnlyForGrantedRequired_isDenied() {
        // A stale rationale flag on a granted permission must not count.
        checker.granted += Manifest.permission.BLUETOOTH_SCAN
        checker.rationale += Manifest.permission.BLUETOOTH_SCAN

        val state = createState(permissions = listOf(bluetooth))

        assertEquals(PendingPermission(bluetooth, PermissionStatus.Denied), state.pending)
    }

    @Test
    fun pending_rationaleForMissingRequired_isShowRationale() {
        checker.granted += Manifest.permission.BLUETOOTH_SCAN
        checker.rationale += Manifest.permission.BLUETOOTH_CONNECT

        val state = createState(permissions = listOf(bluetooth))

        assertEquals(PendingPermission(bluetooth, PermissionStatus.ShowRationale), state.pending)
    }

    @Test
    fun onRequestResult_deniedWithRationale_isShowRationale() {
        val state = createState()
        checker.rationale += fine

        state.onRequestResult(location.requested)

        assertEquals(PendingPermission(location, PermissionStatus.ShowRationale), state.pending)
    }

    @Test
    fun onRequestResult_deniedWithoutRationale_isPermanentlyDenied() {
        val state = createState()

        state.onRequestResult(location.requested)

        assertEquals(PendingPermission(location, PermissionStatus.PermanentlyDenied), state.pending)
    }

    @Test
    fun onRequestResult_resultWithOnlyOptionalRequested_countsAsRequested() {
        val state = createState()

        state.onRequestResult(listOf(coarse))

        assertEquals(PendingPermission(location, PermissionStatus.PermanentlyDenied), state.pending)
    }

    @Test
    fun onRequestResult_otherPermissionResult_keepsDenied() {
        val state = createState()

        state.onRequestResult(listOf(Manifest.permission.CAMERA))

        assertEquals(PendingPermission(location, PermissionStatus.Denied), state.pending)
    }

    @Test
    fun onRequestResult_granted_isNull() {
        val state = createState()
        checker.granted += fine

        state.onRequestResult(location.requested)

        assertNull(state.pending)
    }

    @Test
    fun onRequestResult_emptyResult_keepsDenied() {
        val state = createState()

        state.onRequestResult(emptySet())

        assertEquals(PendingPermission(location, PermissionStatus.Denied), state.pending)
    }

    @Test
    fun onRequestResult_secondResult_keepsEarlierRequested() {
        val state = createState()
        state.onRequestResult(listOf(fine))

        state.onRequestResult(listOf(Manifest.permission.CAMERA))

        assertEquals(setOf(fine, Manifest.permission.CAMERA), state.requested)
    }

    @Test
    fun refresh_grantedInSettings_isNull() {
        val state = createState(requested = location.requested)
        checker.granted += fine

        state.refresh()

        assertNull(state.pending)
    }

    @Test
    fun refresh_revokedWhileAway_isDenied() {
        checker.granted += fine
        val state = createState()
        checker.granted -= fine

        state.refresh()

        assertEquals(PendingPermission(location, PermissionStatus.Denied), state.pending)
    }

    @Test
    fun saver_roundTrip_keepsRequestedPermissions() {
        val saver = PermissionGateState.saver(listOf(location), checker)
        val state = createState()
        state.onRequestResult(location.requested)

        val saved = with(saver) { SaverScope { true }.save(state) }
        val restored = saver.restore(checkNotNull(saved))

        assertEquals(location.requested, restored?.requested)
        assertEquals(
            PendingPermission(location, PermissionStatus.PermanentlyDenied),
            restored?.pending,
        )
    }
}
