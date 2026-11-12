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
import dev.giste.roadbooknavigator.core.R
import dev.giste.roadbooknavigator.core.permission.AppPermission
import dev.giste.roadbooknavigator.core.permission.domain.PermissionStatus
import dev.giste.roadbooknavigator.core.ui.theme.RoadbookNavigatorTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionGateContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val requested = mutableListOf<AppPermission>()

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
        setGate(PendingPermission(AppPermission.PRECISE_LOCATION, PermissionStatus.Denied))

        composeRule.onNodeWithText(CONTENT).assertDoesNotExist()
    }

    @Test
    fun permissionGateContent_allowClicked_requestsPendingPermission() {
        setGate(PendingPermission(AppPermission.PRECISE_LOCATION, PermissionStatus.Denied))
        val allow = InstrumentationRegistry.getInstrumentation().targetContext
            .getString(R.string.core_permission_location_action_allow)

        composeRule.onNodeWithText(allow).performClick()

        composeRule.runOnIdle { assertEquals(listOf(AppPermission.PRECISE_LOCATION), requested) }
    }

    private companion object {
        const val CONTENT = "Gated content"
    }
}
