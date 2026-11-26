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
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dev.giste.map.MapDownloadServer
import dev.giste.map.MapSettingsSource
import dev.giste.map.MapZoomLevel
import dev.giste.roadbooknavigator.BuildConfig
import dev.giste.roadbooknavigator.MainActivity
import dev.giste.roadbooknavigator.settings.AppSettings
import dev.giste.roadbooknavigator.settings.AppSettingsSource
import dev.giste.roadbooknavigator.settings.ScreenOrientation
import dev.giste.roadbooknavigator.settings.ThemeMode
import dev.giste.roadbooknavigator.ui.dashboard.DashboardTestTags
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Moving between the dashboard, the settings, the About screen, the map manager and the map
 * downloader in the real app, with the real Hilt graph and location granted.
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

    /** The settings `:map` receives, as `:settings` provides them. */
    @Inject
    lateinit var mapSettings: MapSettingsSource

    /**
     * The map downloader reads its folders from this server: a closed local port, so the tests
     * never use the network (the downloader then says it cannot reach the site).
     */
    @BindValue
    @JvmField
    val mapDownloadServer: MapDownloadServer = MapDownloadServer("http://127.0.0.1:1/")

    /**
     * Injects the test and sets up a test WorkManager. The test application does not start
     * WorkManager (the real app configures it with Hilt's worker factory), and the map manager
     * reads `:map`'s download queue as soon as it opens, so without one it fails.
     */
    @Before
    fun setUp() {
        hiltRule.inject()
        WorkManagerTestInitHelper.initializeTestWorkManager(
            ApplicationProvider.getApplicationContext(),
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
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

        composeRule.onNodeWithTag(MAP_MANAGER_ADD_TAG).performClick()

        composeRule.awaitText("Download maps")
    }

    @Test
    fun systemBack_inTheMapDownloader_returnsToTheMapManager() {
        openMapManager()
        composeRule.onNodeWithTag(MAP_MANAGER_ADD_TAG).performClick()
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

    @Test
    fun mapZoomDecreased_reachesTheMap() {
        openSettings()

        composeRule.onNode(
            hasText("−") and hasClickAction() and hasAnyAncestor(hasTestTag(MAP_ZOOM_STEPPER_TAG)),
        ).performScrollTo().performClick()
        composeRule.waitForIdle()

        // From the default zoom level, 18, one level farther.
        composeRule.awaitValue(mapSettings.settings) { it.zoomLevel == MapZoomLevel.of(17) }
    }

    /** Opens the About screen from the settings, whose About button is at the end of the list. */
    private fun openAbout() {
        openSettings()
        composeRule.onNode(hasText("About and licences") and hasClickAction()).performScrollTo().performClick()
        composeRule.awaitText("Version ${BuildConfig.VERSION_NAME}")
    }

    @Test
    fun aboutButton_pressedInTheSettings_opensTheAboutScreenWithTheLibraries() {
        openAbout()

        // A library from the list the AboutLibraries plugin generates for this build.
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Timber"))
        composeRule.onNodeWithText("Timber").assertExists()
    }

    @Test
    fun systemBack_inTheAbout_returnsToTheSettings() {
        openAbout()
        // Leave only once the library list is read. The read runs on the real Dispatchers.IO,
        // whose threads outlive the test: one still reading the app's resources when Robolectric
        // tears the test down breaks the DataStore writes of later tests in this JVM.
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Timber"))

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        composeRule.awaitText("Version ${BuildConfig.VERSION_NAME}", exists = false)
        composeRule.awaitText("Settings")
    }

    /** Waits until the stored app settings match [predicate] (see [awaitValue]). */
    private fun awaitStored(predicate: (AppSettings) -> Boolean) {
        composeRule.awaitValue(appSettings.settings, predicate)
    }

    private companion object {
        /**
         * The tag of the map's manage button: `:map`'s `MapViewerTestTags.MANAGE`, which is
         * internal to `:map`, so it is repeated here.
         */
        const val MAP_MANAGE_TAG = "map_viewer_manage"

        /** The tag of the map manager's add button: `:map`'s `MapManagerTestTags.ADD`. */
        const val MAP_MANAGER_ADD_TAG = "map_manager_add"

        /**
         * The tag of the settings' map zoom stepper: `:settings`' `SettingsTestTags.MAP_ZOOM_STEPPER`,
         * internal to `:settings`. Its − and + repeat in the other steppers.
         */
        const val MAP_ZOOM_STEPPER_TAG = "settings_map_zoom_stepper"
    }
}
