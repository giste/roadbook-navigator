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

package dev.giste.roadbooknavigator.settings.domain

import dev.giste.roadbooknavigator.settings.DashboardSettings
import dev.giste.roadbooknavigator.settings.ScreenShare
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardLayoutStepTest {

    private fun settings(sideColumn: Int = 30, distances: Int = 8) = DashboardSettings(
        landscapeSideColumn = ScreenShare.of(sideColumn),
        portraitDistances = ScreenShare.of(distances),
    )

    private fun DashboardSettings.sideColumnStep(steps: Int) = withLandscapeSideColumnStepped(steps)

    private fun DashboardSettings.distancesStep(steps: Int) = withPortraitDistancesStepped(steps)

    private fun side(percent: Int) = settings(sideColumn = percent)

    private fun rows(percent: Int) = settings(distances = percent)

    @Test
    fun landscapeSideColumnStepped_upOrDown_movesFivePercentPerStep() {
        assertEquals(side(35), settings().sideColumnStep(1))
        assertEquals(side(25), side(35).sideColumnStep(-2))
    }

    @Test
    fun landscapeSideColumnStepped_pastTheOfferedRange_stopsAtItsEnds() {
        assertEquals(side(40), side(40).sideColumnStep(1))
        assertEquals(side(25), side(25).sideColumnStep(-1))
    }

    @Test
    fun landscapeSideColumnStepped_keepsTheDistancesRow() {
        assertEquals(
            settings(sideColumn = 35, distances = 11),
            rows(11).sideColumnStep(1),
        )
    }

    @Test
    fun portraitDistancesStepped_upOrDown_movesOnePercentPerStep() {
        assertEquals(rows(9), settings().distancesStep(1))
        assertEquals(rows(9), rows(11).distancesStep(-2))
    }

    @Test
    fun portraitDistancesStepped_pastTheOfferedRange_stopsAtItsEnds() {
        assertEquals(rows(12), rows(12).distancesStep(1))
        assertEquals(rows(8), rows(8).distancesStep(-1))
    }

    @Test
    fun portraitDistancesStepped_keepsTheSideColumn() {
        assertEquals(
            settings(sideColumn = 40, distances = 9),
            side(40).distancesStep(1),
        )
    }

    @Test
    fun withinOfferedRanges_offeredValues_areKept() {
        assertEquals(settings(35, 10), settings(35, 10).withinOfferedRanges())
    }

    @Test
    fun withinOfferedRanges_defaults_areOffered() {
        assertEquals(DashboardSettings(), DashboardSettings().withinOfferedRanges())
    }

    @Test
    fun withinOfferedRanges_outsideTheRanges_bringsThemToTheNearestEnd() {
        assertEquals(settings(25, 8), settings(10, 3).withinOfferedRanges())
        assertEquals(settings(40, 12), settings(60, 20).withinOfferedRanges())
    }

    @Test
    fun withinOfferedRanges_sideColumnBetweenSteps_roundsToTheNearestStep() {
        assertEquals(side(25), side(27).withinOfferedRanges())
        assertEquals(side(30), side(28).withinOfferedRanges())
        assertEquals(side(40), side(39).withinOfferedRanges())
    }

    @Test
    fun landscapeSideColumnStepped_fromBetweenSteps_landsOnAStep() {
        // Only a damaged file can hold such a value; the codec brings it onto a step first, but the
        // step still lands on one.
        assertEquals(side(30), side(27).sideColumnStep(1))
    }
}
