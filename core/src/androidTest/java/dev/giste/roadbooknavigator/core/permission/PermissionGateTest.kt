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

package dev.giste.roadbooknavigator.core.permission

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dev.giste.roadbooknavigator.core.test.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the real gate in an activity, with location granted.
 *
 * Revoking a runtime permission kills the app process, which would stop the instrumentation,
 * so the denied paths are covered by `PermissionGateContentTest` and the JVM tests instead.
 */
@RunWith(AndroidJUnit4::class)
class PermissionGateTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun grantLocation() {
        // Explicit, so the test does not depend on the install flags granting it.
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .forEach { instrumentation.uiAutomation.grantRuntimePermission(packageName, it) }
    }

    @Test
    fun permissionGate_locationGranted_showsContent() {
        composeRule.setContent {
            PermissionGate(permissions = listOf(TestPermissions.location)) {
                Text(CONTENT)
            }
        }

        composeRule.onNodeWithText(CONTENT).assertIsDisplayed()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.core_test_permission_title),
        ).assertDoesNotExist()
    }

    @Test
    fun permissionGate_noPermissions_showsContent() {
        composeRule.setContent {
            PermissionGate(permissions = emptyList()) {
                Text(CONTENT)
            }
        }

        composeRule.onNodeWithText(CONTENT).assertIsDisplayed()
    }

    private companion object {
        const val CONTENT = "Gated content"
    }
}
