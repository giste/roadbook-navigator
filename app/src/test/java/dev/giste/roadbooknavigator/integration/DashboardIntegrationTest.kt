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

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dev.giste.geo.Distance
import dev.giste.odometer.OdometerController
import dev.giste.roadbook.RoadbookController
import dev.giste.roadbooknavigator.MainActivity
import dev.giste.roadbooknavigator.coordination.RoadbookOdometerCoordinator
import dev.giste.roadbooknavigator.ui.dashboard.DashboardTestTags
import dev.giste.roadbooknavigator.ui.dashboard.pressNumpadKeys
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The real app on the JVM: the real Hilt graph and modules, with location granted, driven
 * through [MainActivity]. Only the GPS (Robolectric's location manager) and the file picker
 * (imports go through [RoadbookController]) are replaced.
 */
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, qualifiers = "en")
class DashboardIntegrationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val grantLocationRule = GrantLocationRule()

    @get:Rule(order = 2)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var roadbook: RoadbookController

    @Inject
    lateinit var odometer: OdometerController

    @Inject
    internal lateinit var coordinator: RoadbookOdometerCoordinator

    @Before
    fun setUp() {
        hiltRule.inject()
        // HiltTestApplication replaces the app's Application, which starts it in the app.
        coordinator.start()
    }

    @Test
    fun importRoadbook_partialWasSet_resetsTheOdometer() {
        runBlocking { odometer.setTrip(Distance.meters(500.0)) }
        composeRule.awaitPartial("0.50")

        roadbook.importExample()

        composeRule.awaitPartial("0.00")
    }

    @Test
    fun longPressWaypointDistance_setsThePartialToIt() {
        roadbook.importExample()
        composeRule.awaitText("2")
        composeRule.awaitPartial("0.00")

        // The second waypoint, 0.11 km from the start: its distance section shows that partial.
        composeRule.onAllNodesWithText("0.11")[0].performTouchInput { longClick() }

        composeRule.awaitPartial("0.11")
    }

    @Test
    fun importButton_moving_isHiddenUntilStoppedAgain() {
        roadbook.importExample()
        composeRule.awaitText("Import roadbook")

        simulateFixAt(kilometersPerHour = 30f, metersNorth = 0.0)
        composeRule.awaitText("Import roadbook", exists = false)

        simulateFixAt(kilometersPerHour = 2f, metersNorth = 10.0)
        composeRule.awaitText("Import roadbook")
    }

    @Test
    fun setPartialDialog_partialTyped_setsTheOdometer() {
        composeRule.awaitPartial("0.00")

        composeRule.onNodeWithTag(DashboardTestTags.PARTIAL).performTouchInput { longClick() }
        composeRule.pressNumpadKeys("12.35")
        composeRule.onNodeWithText("Set").performClick()

        composeRule.awaitPartial("12.35")
    }

    @Test
    fun resetAllDialog_confirmed_resetsTheOdometer() {
        runBlocking { odometer.setTrip(Distance.meters(500.0)) }
        composeRule.awaitPartial("0.50")

        composeRule.onNodeWithTag(DashboardTestTags.TOTAL).performTouchInput { longClick() }
        composeRule.onNodeWithText("Reset").performClick()

        composeRule.awaitPartial("0.00")
    }
}
