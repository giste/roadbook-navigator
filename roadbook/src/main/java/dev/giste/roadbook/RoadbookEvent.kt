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

package dev.giste.roadbook

import dev.giste.geo.Distance

/**
 * Something that happened in the roadbook and that the app may need to react to, e.g. by changing
 * the odometer. Emitted by [RoadbookController.events].
 *
 * `:roadbook` only reports what happened. What the app does about it (resetting or setting the
 * odometer, …) is the app's decision.
 */
public sealed interface RoadbookEvent {

    /**
     * The user imported a new roadbook, which replaced the previous one and is now shown from its
     * first waypoint.
     *
     * It is emitted only after an import succeeds. It is not emitted when the persisted roadbook
     * is restored after a restart, so reacting to it (e.g. resetting the odometer) happens once
     * per import.
     */
    public data object Loaded : RoadbookEvent

    /**
     * The user asked to set the partial to the one the roadbook gives at a waypoint, e.g. to
     * correct the odometer when passing that waypoint. The user does it by long-pressing the
     * waypoint's distance.
     *
     * @property partial the partial at that waypoint: the distance shown for it, or
     * [Distance.ZERO] when the waypoint resets the partial.
     */
    public data class SetPartialRequested(public val partial: Distance) : RoadbookEvent
}
