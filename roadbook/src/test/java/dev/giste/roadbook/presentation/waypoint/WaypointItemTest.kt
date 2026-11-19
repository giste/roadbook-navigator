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

package dev.giste.roadbook.presentation.waypoint

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.geo.Distance
import dev.giste.roadbook.RoadbookStyle
import dev.giste.roadbook.ShortDistanceThreshold
import dev.giste.roadbook.domain.roadbook.Waypoint
import dev.giste.roadbook.domain.roadbook.testWaypoint
import dev.giste.roadbook.presentation.theme.RoadbookTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class WaypointItemTest {

    @get:Rule
    val rule = createComposeRule()

    /** The partials passed to `onSetPartial`, in call order. */
    private val setPartials = mutableListOf<Distance>()

    private val plain = testWaypoint(
        number = 12,
        partial = Distance.meters(25_430.0),
        distanceFromPrevious = Distance.meters(1_270.0),
    )

    private fun show(waypoint: Waypoint) {
        rule.setContent {
            GisteTheme {
                RoadbookTheme(style = RoadbookStyle.APP_THEME) {
                    WaypointItem(
                        waypoint = waypoint,
                        shortDistanceThreshold = ShortDistanceThreshold.DEFAULT,
                        onSetPartial = { setPartials += it },
                    )
                }
            }
        }
    }

    /** The distances, the only element of the row that screen readers and gestures reach. */
    private fun distances(): SemanticsNodeInteraction =
        rule.onNode(SemanticsMatcher.keyIsDefined(SemanticsActions.OnLongClick))

    @Test
    fun waypointItem_showsPartialDistanceFromPreviousAndNumber() {
        show(plain)

        rule.onNodeWithText("25.43").assertExists()
        rule.onNodeWithText("1.27").assertExists()
        rule.onNodeWithText("12").assertExists()
    }

    @Test
    fun waypointItem_waypointDoesNotReset_showsNoNewPartial() {
        show(plain)

        rule.onAllNodesWithText("0.00").assertCountEquals(0)
    }

    @Test
    fun waypointItem_waypointResetsPartial_showsNewPartialOfZero() {
        show(testWaypoint(number = 13, partial = Distance.meters(26_100.0), resetsPartial = true))

        rule.onNodeWithText("26.10").assertExists()
        rule.onNodeWithText("0.00").assertExists()
    }

    @Test
    fun longPress_plainWaypoint_setsItsPartial() {
        show(plain)

        distances().performTouchInput { longClick() }

        assertEquals(listOf(Distance.meters(25_430.0)), setPartials)
    }

    @Test
    fun longPress_waypointResetsPartial_setsZero() {
        show(testWaypoint(number = 13, partial = Distance.meters(26_100.0), resetsPartial = true))

        distances().performTouchInput { longClick() }

        assertEquals(listOf(Distance.ZERO), setPartials)
    }

    @Test
    fun tap_doesNotSetPartial() {
        show(plain)

        distances().performTouchInput { click() }

        assertTrue(setPartials.isEmpty())
    }

    @Test
    fun longPressAction_hasLabelForScreenReaders() {
        show(plain)

        val action = distances().fetchSemanticsNode().config.getOrNull(SemanticsActions.OnLongClick)

        assertEquals("Set the partial to this waypoint", action?.label)
    }

    @Test
    fun distances_areOneElementWithTheVisibleFigures() {
        show(plain)

        val texts = distances().fetchSemanticsNode().config[SemanticsProperties.Text]

        assertEquals(listOf("25.43", "1.27", "12"), texts.map { it.text })
    }

    @Test
    @Config(qualifiers = "es")
    fun waypointItem_spanish_formatsAndLabelsInSpanish() {
        show(plain)

        val action = distances().fetchSemanticsNode().config.getOrNull(SemanticsActions.OnLongClick)

        rule.onNodeWithText("25,43").assertExists()
        assertEquals("Poner el parcial de esta viñeta", action?.label)
    }
}
