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
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dev.giste.map.MapDownloadServer
import dev.giste.roadbooknavigator.MainActivity
import dev.giste.roadbooknavigator.settings.AppSettings
import dev.giste.roadbooknavigator.settings.AppSettingsSource
import dev.giste.roadbooknavigator.settings.ScreenOrientation
import dev.giste.roadbooknavigator.settings.ThemeMode
import dev.giste.roadbooknavigator.ui.dashboard.DashboardTestTags
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Moving between the dashboard, the settings, the map manager and the map downloader in the real
 * app, with the real Hilt graph and location granted.
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

    /**
     * The map downloader reads its folders from this server: a closed local port, so the tests
     * never use the network (the downloader then says it cannot reach the site).
     */
    @BindValue
    @JvmField
    val mapDownloadServer: MapDownloadServer = MapDownloadServer("http://127.0.0.1:1/")

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

    /**
     * Opens the map manager from the map on the dashboard. The vehicle starts stopped, and the
     * tests install no map, so the map shows its manage button either way.
     */
    private fun openMapManager() {
        composeRule.awaitPartial("0.00")
        composeRule.onNodeWithTag(MAP_MANAGE_TAG).performClick()
        composeRule.awaitText("Maps")
    }

    @Test
    fun manageMapsButton_pressed_opensTheMapManager() {
        openMapManager()

        composeRule.onNodeWithTag(DashboardTestTags.PARTIAL).assertDoesNotExist()
    }

    @Test
    fun systemBack_inTheMapManager_returnsToTheDashboard() {
        openMapManager()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        composeRule.awaitText("Maps", exists = false)
        composeRule.awaitPartial("0.00")
    }

    @Test
    fun downloadButton_pressedInTheMapManager_opensTheMapDownloader() {
        openMapManager()

        composeRule.onNodeWithTag(MAP_MANAGER_DOWNLOAD_TAG).performClick()

        composeRule.awaitText("Download maps")
    }

    @Test
    fun systemBack_inTheMapDownloader_returnsToTheMapManager() {
        openMapManager()
        composeRule.onNodeWithTag(MAP_MANAGER_DOWNLOAD_TAG).performClick()
        composeRule.awaitText("Download maps")

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        composeRule.awaitText("Download maps", exists = false)
        composeRule.awaitText("Maps")
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

        awaitStored { it.themeMode == ThemeMode.DARK }
    }

    @Test
    fun orientationPicked_isStoredAndApplied() {
        openSettings()

        composeRule.onNode(hasText("Horizontal") and hasClickAction()).performClick()
        composeRule.waitForIdle()

        awaitStored { it.orientation == ScreenOrientation.HORIZONTAL }
        // Applied once the stored value reaches the activity, on the main thread.
        composeRule.waitUntil(TIMEOUT_MILLIS) {
            composeRule.activity.requestedOrientation ==
                ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
        }
    }

    /**
     * Waits until the stored app settings match [predicate], or fails after [TIMEOUT_MILLIS].
     * Call it once the change has been made (e.g. after `waitForIdle`), since it blocks the main
     * thread, where Compose handles clicks.
     *
     * DataStore stores them on its own thread, in real time: the first write of a test JVM can take
     * seconds while it loads its classes. `waitUntil` would not do, because under Robolectric it
     * measures its timeout on the test clock, which runs much faster than real time.
     */
    private fun awaitStored(predicate: (AppSettings) -> Boolean) {
        runBlocking { withTimeout(TIMEOUT_MILLIS) { appSettings.settings.first(predicate) } }
    }

    private companion object {
        /**
         * The tag of the map's manage button: `:map`'s `MapViewerTestTags.MANAGE`, which is
         * internal to `:map`, so it is repeated here.
         */
        const val MAP_MANAGE_TAG = "map_viewer_manage"

        /** The tag of the map manager's download button: `:map`'s `MapManagerTestTags.DOWNLOAD`. */
        const val MAP_MANAGER_DOWNLOAD_TAG = "map_manager_download"
    }
}
