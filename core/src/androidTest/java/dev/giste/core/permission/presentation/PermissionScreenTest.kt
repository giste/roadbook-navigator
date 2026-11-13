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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.giste.core.R
import dev.giste.core.permission.domain.PermissionStatus
import dev.giste.core.ui.theme.GisteTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var requests = 0
    private var settingsOpened = 0

    private fun string(@StringRes id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen(status: PermissionStatus) {
        composeRule.setContent {
            GisteTheme {
                PermissionScreen(
                    status = status,
                    title = TITLE,
                    message = MESSAGE,
                    allowAction = ALLOW,
                    onRequest = { requests++ },
                    onOpenSettings = { settingsOpened++ },
                )
            }
        }
    }

    @Test
    fun permissionScreen_anyStatus_showsTitleAsHeading() {
        setScreen(PermissionStatus.Denied)

        composeRule.onNodeWithText(TITLE)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
    }

    @Test
    fun permissionScreen_anyStatus_showsMessage() {
        setScreen(PermissionStatus.ShowRationale)

        composeRule.onNodeWithText(MESSAGE).assertIsDisplayed()
    }

    @Test
    fun permissionScreen_deniedAllowClicked_requestsPermission() {
        setScreen(PermissionStatus.Denied)

        composeRule.onNodeWithText(ALLOW).performClick()

        composeRule.runOnIdle { assertEquals(1, requests) }
    }

    @Test
    fun permissionScreen_denied_hasNoSettingsButton() {
        setScreen(PermissionStatus.Denied)

        composeRule.onNodeWithText(string(R.string.core_permission_action_open_settings))
            .assertDoesNotExist()
    }

    @Test
    fun permissionScreen_showRationaleAllowClicked_requestsPermission() {
        setScreen(PermissionStatus.ShowRationale)

        composeRule.onNodeWithText(ALLOW).performClick()

        composeRule.runOnIdle { assertEquals(1, requests) }
    }

    @Test
    fun permissionScreen_permanentlyDeniedOpenSettingsClicked_opensSettings() {
        setScreen(PermissionStatus.PermanentlyDenied)

        composeRule.onNodeWithText(string(R.string.core_permission_action_open_settings))
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, settingsOpened)
            assertEquals(0, requests)
        }
    }

    @Test
    fun permissionScreen_permanentlyDeniedTryAgainClicked_requestsPermission() {
        setScreen(PermissionStatus.PermanentlyDenied)

        composeRule.onNodeWithText(string(R.string.core_permission_action_try_again))
            .performClick()

        composeRule.runOnIdle {
            assertEquals(1, requests)
            assertEquals(0, settingsOpened)
        }
    }

    @Test
    fun permissionScreen_permanentlyDenied_hasNoAllowButton() {
        setScreen(PermissionStatus.PermanentlyDenied)

        composeRule.onNodeWithText(ALLOW).assertDoesNotExist()
    }

    @Test
    fun permissionScreen_buttons_meetGloveFriendlyTouchTarget() {
        setScreen(PermissionStatus.PermanentlyDenied)

        composeRule.onNodeWithText(string(R.string.core_permission_action_open_settings))
            .assertHeightIsAtLeast(56.dp)
        composeRule.onNodeWithText(string(R.string.core_permission_action_try_again))
            .assertHeightIsAtLeast(56.dp)
    }

    @Test
    fun permissionScreen_allowButton_meetsGloveFriendlyTouchTarget() {
        setScreen(PermissionStatus.Denied)

        composeRule.onNodeWithText(ALLOW).assertHeightIsAtLeast(56.dp)
    }

    private companion object {
        const val TITLE = "Permission title"
        const val MESSAGE = "Permission message"
        const val ALLOW = "Allow permission"
    }
}
