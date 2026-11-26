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

import dev.giste.geo.Distance
import dev.giste.roadbook.ShortDistanceThreshold
import org.junit.Assert.assertEquals
import org.junit.Test

class ShortDistanceStepTest {

    private fun threshold(meters: Double) = ShortDistanceThreshold.of(Distance.meters(meters))

    @Test
    fun stepped_oneStepUp_isFiftyMetersLonger() {
        assertEquals(threshold(300.0), threshold(250.0).stepped(1))
    }

    @Test
    fun stepped_oneStepDown_isFiftyMetersShorter() {
        assertEquals(threshold(200.0), threshold(250.0).stepped(-1))
    }

    @Test
    fun offeredRange_is100To500Meters() {
        assertEquals(threshold(100.0), SHORT_DISTANCE_MIN)
        assertEquals(threshold(500.0), SHORT_DISTANCE_MAX)
    }

    @Test
    fun stepped_belowTheOfferedMinimum_staysAtIt() {
        assertEquals(SHORT_DISTANCE_MIN, SHORT_DISTANCE_MIN.stepped(-1))
    }

    @Test
    fun stepped_aboveTheOfferedMaximum_staysAtIt() {
        assertEquals(SHORT_DISTANCE_MAX, SHORT_DISTANCE_MAX.stepped(1))
    }

    @Test
    fun stepped_manyStepsDown_staysAtTheOfferedMinimum() {
        assertEquals(SHORT_DISTANCE_MIN, threshold(250.0).stepped(-100))
    }

    @Test
    fun stepped_manyStepsUp_staysAtTheOfferedMaximum() {
        assertEquals(SHORT_DISTANCE_MAX, threshold(250.0).stepped(100))
    }

    @Test
    fun stepped_fromOutsideTheOfferedRange_landsInIt() {
        // A value an older version stored, which the codec would already have brought in.
        assertEquals(SHORT_DISTANCE_MAX, ShortDistanceThreshold.MAX.stepped(-1))
    }

    @Test
    fun withinOfferedRange_shorter_isTheOfferedMinimum() {
        assertEquals(SHORT_DISTANCE_MIN, ShortDistanceThreshold.MIN.withinOfferedRange())
    }

    @Test
    fun withinOfferedRange_longer_isTheOfferedMaximum() {
        assertEquals(SHORT_DISTANCE_MAX, ShortDistanceThreshold.MAX.withinOfferedRange())
    }

    @Test
    fun withinOfferedRange_inside_isKept() {
        assertEquals(threshold(350.0), threshold(350.0).withinOfferedRange())
    }
}
