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
import dev.giste.odometer.OdometerLocation
import dev.giste.odometer.OdometerSettings

/**
 * Decides what a new location fix means for the odometer.
 *
 * The odometer keeps an *anchor*: the last accepted fix, from which the next distance is
 * measured. The caller keeps the anchor, and moves it to the new fix on [FixResult.Anchored] and
 * [FixResult.Travelled].
 *
 * The checks run in this order:
 *
 * 1. A fix with an unknown horizontal accuracy, or one worse than
 *    [OdometerSettings.maxHorizontalAccuracy], is ignored. Its position cannot be trusted.
 * 2. Without an anchor, the fix becomes the anchor. There is no distance to measure yet.
 * 3. A fix slower than [OdometerSettings.minSpeed] is ignored, and the anchor stays where it is.
 *    GPS jitter while stopped adds nothing, and the distance moved from the anchor is counted
 *    once the vehicle moves again. A fix with an unknown speed is never treated as stopped.
 * 4. A fix closer to the anchor than [OdometerSettings.minStepDistance] is ignored, and the
 *    anchor stays where it is. GPS noise between close fixes would otherwise add up, and the
 *    short steps are counted in one piece once the fix is far enough from the anchor.
 * 5. Otherwise the distance from the anchor to the fix is travelled. The altitude change is
 *    only included when [OdometerSettings.useAltitude] is on.
 *
 * @param anchor the last accepted fix, or `null` if there is none yet.
 * @param fix the new fix.
 * @param settings the current filter settings.
 * @return what to do with [fix].
 */
internal fun filterFix(
    anchor: OdometerLocation?,
    fix: OdometerLocation,
    settings: OdometerSettings,
): FixResult {
    val accuracy = fix.horizontalAccuracy
    if (accuracy == null || accuracy > settings.maxHorizontalAccuracy) {
        return FixResult.Ignored(IgnoreReason.INACCURATE)
    }
    if (anchor == null) return FixResult.Anchored

    val speed = fix.speed
    if (speed != null && speed < settings.minSpeed) return FixResult.Ignored(IgnoreReason.STOPPED)

    val maxVerticalAccuracy = settings.maxVerticalAccuracy.takeIf { settings.useAltitude }
    val distance = travelledDistance(anchor, fix, maxVerticalAccuracy)
    if (distance < settings.minStepDistance) return FixResult.Ignored(IgnoreReason.SHORT_STEP)

    return FixResult.Travelled(distance)
}

/** The outcome of [filterFix] for one fix. */
internal sealed interface FixResult {

    /**
     * The fix is not used, and the anchor does not move.
     *
     * @property reason why the fix was ignored.
     */
    data class Ignored(val reason: IgnoreReason) : FixResult

    /** There was no anchor: the fix becomes the anchor, and no distance is added. */
    data object Anchored : FixResult

    /**
     * The fix is accepted: [distance] is added, and the fix becomes the new anchor.
     *
     * @property distance the distance from the anchor to the fix. It is at least
     * [OdometerSettings.minStepDistance], so it is only [Distance.ZERO] when that setting is.
     */
    data class Travelled(val distance: Distance) : FixResult
}

/** Why [filterFix] ignored a fix. */
internal enum class IgnoreReason {

    /** Its horizontal accuracy is unknown or worse than the setting allows. */
    INACCURATE,

    /** It is slower than the minimum speed, so the vehicle is treated as stopped. */
    STOPPED,

    /** It is closer to the anchor than the minimum step distance. */
    SHORT_STEP,
}
