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

package dev.giste.roadbooknavigator.integration

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dev.giste.roadbooknavigator.MainActivity
import dev.giste.roadbooknavigator.settings.AppSettingsSource
import dev.giste.roadbooknavigator.settings.ScreenOrientation
import dev.giste.roadbooknavigator.settings.ThemeMode
import dev.giste.roadbooknavigator.ui.dashboard.DashboardTestTags
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Moving between the dashboard and the settings in the real app, with the real Hilt graph and
 * location granted.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "en")
class NavigationIntegrationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val grantLocationRule = GrantLocationRule()

    @get:Rule(order = 2)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var appSettings: AppSettingsSource

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    /** Opens the settings from the dashboard: the vehicle starts stopped, so the button shows. */
    private fun openSettings() {
        composeRule.awaitPartial("0.00")
        composeRule.onNodeWithTag(DashboardTestTags.SETTINGS).performClick()
        composeRule.awaitText("Settings")
    }

    @Test
    fun settingsButton_pressed_opensTheSettings() {
        openSettings()

        composeRule.onNodeWithTag(DashboardTestTags.PARTIAL).assertDoesNotExist()
    }

    @Test
    fun systemBack_inTheSettings_returnsToTheDashboard() {
        openSettings()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        composeRule.awaitText("Settings", exists = false)
        composeRule.awaitPartial("0.00")
    }

    @Test
    fun themeModePicked_isStoredForTheApp() {
        openSettings()

        composeRule.onNode(hasText("Dark") and hasClickAction()).performClick()
        composeRule.waitForIdle()

        assertEquals(ThemeMode.DARK, runBlocking { appSettings.settings.first() }.themeMode)
    }

    @Test
    fun orientationPicked_isStoredAndApplied() {
        openSettings()

        composeRule.onNode(hasText("Horizontal") and hasClickAction()).performClick()
        composeRule.waitForIdle()

        val stored = runBlocking { appSettings.settings.first() }.orientation
        assertEquals(ScreenOrientation.HORIZONTAL, stored)
        assertEquals(
            ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE,
            composeRule.activity.requestedOrientation,
        )
    }
}
