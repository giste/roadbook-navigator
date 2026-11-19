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

package dev.giste.roadbooknavigator.dashboard

import dev.giste.location.LocationError
import dev.giste.location.LocationEvent
import dev.giste.roadbooknavigator.fixAt
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StoppedDetectionTest {

    @Test
    fun isStoppedAfter_stoppedAboveMovingSpeed_isMoving() {
        assertFalse(isStoppedAfter(stopped = true, fixAt(10.1)))
    }

    @Test
    fun isStoppedAfter_stoppedAtMovingSpeed_staysStopped() {
        assertTrue(isStoppedAfter(stopped = true, fixAt(10.0)))
    }

    @Test
    fun isStoppedAfter_stoppedBetweenThresholds_staysStopped() {
        assertTrue(isStoppedAfter(stopped = true, fixAt(7.0)))
    }

    @Test
    fun isStoppedAfter_movingBetweenThresholds_staysMoving() {
        assertFalse(isStoppedAfter(stopped = false, fixAt(7.0)))
    }

    @Test
    fun isStoppedAfter_movingAtStoppedSpeed_staysMoving() {
        assertFalse(isStoppedAfter(stopped = false, fixAt(5.0)))
    }

    @Test
    fun isStoppedAfter_movingBelowStoppedSpeed_isStopped() {
        assertTrue(isStoppedAfter(stopped = false, fixAt(4.9)))
    }

    @Test
    fun isStoppedAfter_movingFixWithoutSpeed_isStopped() {
        assertTrue(isStoppedAfter(stopped = false, fixAt(null)))
    }

    @Test
    fun isStoppedAfter_movingProviderDisabled_isStopped() {
        assertTrue(isStoppedAfter(stopped = false, LocationEvent.ProviderDisabled))
    }

    @Test
    fun isStoppedAfter_movingLocationError_isStopped() {
        assertTrue(isStoppedAfter(stopped = false, LocationEvent.Error(LocationError.UNKNOWN)))
    }

    @Test
    fun isStoppedAfter_providerEnabled_keepsThePreviousAnswer() {
        assertFalse(isStoppedAfter(stopped = false, LocationEvent.ProviderEnabled))
        assertTrue(isStoppedAfter(stopped = true, LocationEvent.ProviderEnabled))
    }

    @Test
    fun stoppedStates_noEvents_startsStopped() = runTest {
        assertEquals(listOf(true), emptyFlow<LocationEvent>().stoppedStates().toList())
    }

    @Test
    fun stoppedStates_events_emitsOnlyChanges() = runTest {
        val events = flowOf(
            fixAt(0.0), // stopped, unchanged
            fixAt(20.0), // moving
            fixAt(7.0), // still moving: between the thresholds
            fixAt(3.0), // stopped
            fixAt(7.0), // still stopped
        )

        assertEquals(listOf(true, false, true), events.stoppedStates().toList())
    }
}
