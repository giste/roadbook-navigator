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

package dev.giste.odometer.domain

import dev.giste.geo.Distance
import dev.giste.geo.Speed
import dev.giste.odometer.OdometerSettings
import dev.giste.odometer.testFix
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FixFilterTest {

    private val settings = OdometerSettings()
    private val anchor = testFix(0.0)

    @Test
    fun filterFix_unknownAccuracy_isIgnoredAsInaccurate() {
        val result = filterFix(anchor, testFix(50.0, horizontalAccuracy = null), settings)

        assertEquals(FixResult.Ignored(IgnoreReason.INACCURATE), result)
    }

    @Test
    fun filterFix_accuracyWorseThanLimit_isIgnoredAsInaccurate() {
        val fix = testFix(50.0, horizontalAccuracy = Distance.meters(20.1))

        val result = filterFix(anchor, fix, settings)

        assertEquals(FixResult.Ignored(IgnoreReason.INACCURATE), result)
    }

    @Test
    fun filterFix_inaccurateWithoutAnchor_isIgnoredAsInaccurate() {
        val result = filterFix(null, testFix(horizontalAccuracy = Distance.meters(50.0)), settings)

        assertEquals(FixResult.Ignored(IgnoreReason.INACCURATE), result)
    }

    @Test
    fun filterFix_accuracyAtLimit_isAccepted() {
        val fix = testFix(50.0, horizontalAccuracy = settings.maxHorizontalAccuracy)

        val result = filterFix(anchor, fix, settings)

        assertTrue(result is FixResult.Travelled)
    }

    @Test
    fun filterFix_noAnchor_isAnchored() {
        val result = filterFix(null, testFix(0.0), settings)

        assertEquals(FixResult.Anchored, result)
    }

    @Test
    fun filterFix_stoppedWithoutAnchor_isAnchored() {
        val result = filterFix(null, testFix(0.0, speed = Speed.ZERO), settings)

        assertEquals(FixResult.Anchored, result)
    }

    @Test
    fun filterFix_slowerThanMinSpeed_isIgnoredAsStopped() {
        val fix = testFix(3.0, speed = Speed.metersPerSecond(0.4))

        val result = filterFix(anchor, fix, settings)

        assertEquals(FixResult.Ignored(IgnoreReason.STOPPED), result)
    }

    @Test
    fun filterFix_atMinSpeed_isTravelled() {
        val result = filterFix(anchor, testFix(30.0, speed = settings.minSpeed), settings)

        assertTrue(result is FixResult.Travelled)
    }

    @Test
    fun filterFix_unknownSpeed_isTravelled() {
        val result = filterFix(anchor, testFix(30.0, speed = null), settings)

        assertTrue(result is FixResult.Travelled)
    }

    @Test
    fun filterFix_movingAccurateFix_travelsDistanceFromAnchor() {
        val result = filterFix(anchor, testFix(30.0), settings)

        assertEquals(30.0, (result as FixResult.Travelled).distance.inMeters, 1e-6)
    }

    @Test
    fun filterFix_shorterThanMinStep_isIgnoredAsShortStep() {
        val result = filterFix(anchor, testFix(4.9), settings)

        assertEquals(FixResult.Ignored(IgnoreReason.SHORT_STEP), result)
    }

    @Test
    fun filterFix_atMinStep_isTravelled() {
        val result = filterFix(anchor, testFix(5.0 + 1e-6), settings)

        assertTrue(result is FixResult.Travelled)
    }

    @Test
    fun filterFix_samePositionAsAnchor_isIgnoredAsShortStep() {
        val result = filterFix(anchor, testFix(0.0), settings)

        assertEquals(FixResult.Ignored(IgnoreReason.SHORT_STEP), result)
    }

    @Test
    fun filterFix_minStepZero_travelsZeroAtSamePosition() {
        val everyStep = OdometerSettings(minStepDistance = Distance.ZERO)

        val result = filterFix(anchor, testFix(0.0), everyStep)

        assertEquals(FixResult.Travelled(Distance.ZERO), result)
    }

    @Test
    fun filterFix_minSpeedZero_countsStoppedFix() {
        val countEverything = OdometerSettings(minSpeed = Speed.ZERO)

        val result = filterFix(anchor, testFix(30.0, speed = Speed.ZERO), countEverything)

        assertEquals(30.0, (result as FixResult.Travelled).distance.inMeters, 1e-6)
    }

    @Test
    fun filterFix_largeMaxHorizontalAccuracy_acceptsInaccurateFix() {
        val lenient = OdometerSettings(maxHorizontalAccuracy = Distance.meters(500.0))
        val fix = testFix(30.0, horizontalAccuracy = Distance.meters(300.0))

        val result = filterFix(anchor, fix, lenient)

        assertTrue(result is FixResult.Travelled)
    }

    @Test
    fun filterFix_maxHorizontalAccuracyZero_ignoresRealFix() {
        val impossible = OdometerSettings(maxHorizontalAccuracy = Distance.ZERO)

        val result = filterFix(anchor, testFix(30.0), impossible)

        assertEquals(FixResult.Ignored(IgnoreReason.INACCURATE), result)
    }

    /** Two fixes 30 m apart horizontally and 40 m apart vertically: 50 m in 3D. */
    private val lowFix = testFix(0.0, altitudeInMeters = 0.0, verticalAccuracy = Distance.meters(2.0))
    private val highFix =
        testFix(30.0, altitudeInMeters = 40.0, verticalAccuracy = Distance.meters(2.0))

    @Test
    fun filterFix_defaultSettings_ignoresAltitude() {
        val result = filterFix(lowFix, highFix, settings) as FixResult.Travelled

        assertEquals(30.0, result.distance.inMeters, 1e-6)
    }

    @Test
    fun filterFix_useAltitudeWithAccurateAltitudes_includesClimb() {
        val withAltitude = OdometerSettings(useAltitude = true)

        val result = filterFix(lowFix, highFix, withAltitude) as FixResult.Travelled

        assertEquals(50.0, result.distance.inMeters, 1e-6)
    }

    @Test
    fun filterFix_useAltitudeWithPoorAltitudes_ignoresClimb() {
        val strict = OdometerSettings(useAltitude = true, maxVerticalAccuracy = Distance.meters(1.0))

        val result = filterFix(lowFix, highFix, strict) as FixResult.Travelled

        assertEquals(30.0, result.distance.inMeters, 1e-6)
    }

    @Test
    fun filterFix_minStep_comparesDistanceIncludingClimb() {
        val withAltitude = OdometerSettings(useAltitude = true, minStepDistance = Distance.meters(40.0))

        val withClimb = filterFix(lowFix, highFix, withAltitude)
        val flat = filterFix(lowFix, highFix, withAltitude.copy(useAltitude = false))

        assertTrue(withClimb is FixResult.Travelled)
        assertEquals(FixResult.Ignored(IgnoreReason.SHORT_STEP), flat)
    }
}
