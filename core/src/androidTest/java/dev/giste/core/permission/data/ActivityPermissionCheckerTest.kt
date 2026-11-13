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

package dev.giste.core.permission.data

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Checks the real Android calls with location granted, and with a permission the test app never
 * gets. Revoking a runtime permission kills the app process, which would stop the
 * instrumentation, so the revoked and rationale answers are not tested here.
 */
@RunWith(AndroidJUnit4::class)
class ActivityPermissionCheckerTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(ComponentActivity::class.java)

    @Before
    fun grantLocation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .forEach { instrumentation.uiAutomation.grantRuntimePermission(packageName, it) }
    }

    @Test
    fun isGranted_locationGranted_isTrue() {
        activityRule.scenario.onActivity { activity ->
            val checker = ActivityPermissionChecker(activity)

            assertTrue(checker.isGranted(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }

    @Test
    fun isGranted_neverGranted_isFalse() {
        activityRule.scenario.onActivity { activity ->
            val checker = ActivityPermissionChecker(activity)

            assertFalse(checker.isGranted(Manifest.permission.CAMERA))
        }
    }

    @Test
    fun shouldShowRationale_locationGranted_isFalse() {
        activityRule.scenario.onActivity { activity ->
            val checker = ActivityPermissionChecker(activity)

            assertFalse(checker.shouldShowRationale(Manifest.permission.ACCESS_FINE_LOCATION))
        }
    }
}
