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

package dev.giste.roadbooknavigator.settings

/** What a remote control key does on the dashboard. */
public enum class RemoteAction {

    /** Moves the roadbook forward to the next waypoint. */
    NEXT_WAYPOINT,

    /** Moves the roadbook back to the previous waypoint. */
    PREVIOUS_WAYPOINT,

    /** Makes the partial one step longer. */
    INCREASE_PARTIAL,

    /** Makes the partial one step shorter. */
    DECREASE_PARTIAL,

    /** Sets the partial to zero. */
    RESET_PARTIAL,
}
