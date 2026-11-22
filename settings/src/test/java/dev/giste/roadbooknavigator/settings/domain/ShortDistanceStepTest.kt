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
    fun stepped_belowTheMinimum_staysAtTheMinimum() {
        assertEquals(ShortDistanceThreshold.MIN, ShortDistanceThreshold.MIN.stepped(-1))
    }

    @Test
    fun stepped_aboveTheMaximum_staysAtTheMaximum() {
        assertEquals(ShortDistanceThreshold.MAX, ShortDistanceThreshold.MAX.stepped(1))
    }

    @Test
    fun stepped_manyStepsDown_staysAtTheMinimum() {
        assertEquals(ShortDistanceThreshold.MIN, threshold(250.0).stepped(-100))
    }
}
