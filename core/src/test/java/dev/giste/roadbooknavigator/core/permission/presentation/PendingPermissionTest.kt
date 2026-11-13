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

import dev.giste.roadbooknavigator.core.permission.TestPermissions
import dev.giste.roadbooknavigator.core.permission.domain.PermissionStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class PendingPermissionTest {

    private val permission = TestPermissions.location

    @Test
    fun message_denied_isFirstTimeMessage() {
        val pending = PendingPermission(permission, PermissionStatus.Denied)

        assertEquals(permission.message, pending.message)
    }

    @Test
    fun message_showRationale_isRationale() {
        val pending = PendingPermission(permission, PermissionStatus.ShowRationale)

        assertEquals(permission.rationale, pending.message)
    }

    @Test
    fun message_permanentlyDenied_isPermanentlyDeniedMessage() {
        val pending = PendingPermission(permission, PermissionStatus.PermanentlyDenied)

        assertEquals(permission.permanentlyDeniedMessage, pending.message)
    }

    @Test
    fun message_granted_isFirstTimeMessage() {
        val pending = PendingPermission(permission, PermissionStatus.Granted)

        assertEquals(permission.message, pending.message)
    }
}
