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

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.giste.roadbooknavigator.core.permission.RequiredPermission
import dev.giste.roadbooknavigator.core.permission.TestPermissions
import dev.giste.roadbooknavigator.core.permission.domain.PermissionStatus
import dev.giste.roadbooknavigator.core.test.R
import dev.giste.roadbooknavigator.core.ui.theme.RoadbookNavigatorTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionGateContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val permission = TestPermissions.location
    private val requested = mutableListOf<RequiredPermission>()

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setGate(pending: PendingPermission?) {
        composeRule.setContent {
            RoadbookNavigatorTheme {
                PermissionGateContent(
                    pending = pending,
                    onRequest = { requested += it },
                    onOpenSettings = {},
                ) {
                    Text(CONTENT)
                }
            }
        }
    }

    @Test
    fun permissionGateContent_allGranted_showsContent() {
        setGate(pending = null)

        composeRule.onNodeWithText(CONTENT).assertIsDisplayed()
    }

    @Test
    fun permissionGateContent_permissionPending_hidesContent() {
        setGate(PendingPermission(permission, PermissionStatus.Denied))

        composeRule.onNodeWithText(CONTENT).assertDoesNotExist()
    }

    @Test
    fun permissionGateContent_denied_showsPermissionTexts() {
        setGate(PendingPermission(permission, PermissionStatus.Denied))

        composeRule.onNodeWithText(string(R.string.core_test_permission_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.core_test_permission_message))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.core_test_permission_allow)).assertIsDisplayed()
    }

    @Test
    fun permissionGateContent_showRationale_showsRationale() {
        setGate(PendingPermission(permission, PermissionStatus.ShowRationale))

        composeRule.onNodeWithText(string(R.string.core_test_permission_rationale))
            .assertIsDisplayed()
    }

    @Test
    fun permissionGateContent_permanentlyDenied_showsPermanentlyDeniedMessage() {
        setGate(PendingPermission(permission, PermissionStatus.PermanentlyDenied))

        composeRule.onNodeWithText(string(R.string.core_test_permission_permanently_denied))
            .assertIsDisplayed()
    }

    @Test
    fun permissionGateContent_allowClicked_requestsPendingPermission() {
        setGate(PendingPermission(permission, PermissionStatus.Denied))

        composeRule.onNodeWithText(string(R.string.core_test_permission_allow)).performClick()

        composeRule.runOnIdle { assertEquals(listOf(permission), requested) }
    }

    private companion object {
        const val CONTENT = "Gated content"
    }
}
