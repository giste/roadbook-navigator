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

package dev.giste.location.domain

import dev.giste.geo.Speed
import dev.giste.location.LocationError
import dev.giste.location.LocationEvent
import dev.giste.location.LocationSettings
import dev.giste.location.MotionState.MOVING
import dev.giste.location.MotionState.STOPPED
import dev.giste.location.fixAt
import org.junit.Assert.assertEquals
import org.junit.Test

class MotionDetectionTest {

    /** The default speeds: moving above 10 km/h, stopped below 5 km/h. */
    private val defaults = LocationSettings()

    @Test
    fun after_stoppedAboveMovingSpeed_isMoving() {
        assertEquals(MOVING, STOPPED.after(fixAt(10.1), defaults))
    }

    @Test
    fun after_stoppedAtMovingSpeed_staysStopped() {
        assertEquals(STOPPED, STOPPED.after(fixAt(10.0), defaults))
    }

    @Test
    fun after_stoppedBetweenSpeeds_staysStopped() {
        assertEquals(STOPPED, STOPPED.after(fixAt(7.0), defaults))
    }

    @Test
    fun after_movingBetweenSpeeds_staysMoving() {
        assertEquals(MOVING, MOVING.after(fixAt(7.0), defaults))
    }

    @Test
    fun after_movingAtStoppedSpeed_staysMoving() {
        assertEquals(MOVING, MOVING.after(fixAt(5.0), defaults))
    }

    @Test
    fun after_movingBelowStoppedSpeed_isStopped() {
        assertEquals(STOPPED, MOVING.after(fixAt(4.9), defaults))
    }

    @Test
    fun after_movingFixWithoutSpeed_isStopped() {
        assertEquals(STOPPED, MOVING.after(fixAt(null), defaults))
    }

    @Test
    fun after_movingProviderDisabled_isStopped() {
        assertEquals(STOPPED, MOVING.after(LocationEvent.ProviderDisabled, defaults))
    }

    @Test
    fun after_movingLocationError_isStopped() {
        assertEquals(STOPPED, MOVING.after(LocationEvent.Error(LocationError.UNKNOWN), defaults))
    }

    @Test
    fun after_providerEnabled_keepsTheState() {
        assertEquals(MOVING, MOVING.after(LocationEvent.ProviderEnabled, defaults))
        assertEquals(STOPPED, STOPPED.after(LocationEvent.ProviderEnabled, defaults))
    }

    @Test
    fun after_customSpeeds_usesThem() {
        val walking = LocationSettings(
            movingSpeed = Speed.kilometersPerHour(2.0),
            stoppedSpeed = Speed.kilometersPerHour(1.0),
        )

        assertEquals(MOVING, STOPPED.after(fixAt(3.0), walking))
        assertEquals(STOPPED, MOVING.after(fixAt(0.5), walking))
    }
}
