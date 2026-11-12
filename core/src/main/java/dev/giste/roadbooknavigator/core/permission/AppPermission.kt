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

package dev.giste.roadbooknavigator.core.permission

/**
 * A runtime permission the app can require through [PermissionGate].
 *
 * Each entry maps to one or more Android manifest permissions, which `:core` declares in its
 * manifest so the gate can always request them.
 */
public enum class AppPermission {

    /**
     * Precise location, while the app is in use (foreground only).
     *
     * Requests `ACCESS_FINE_LOCATION` together with `ACCESS_COARSE_LOCATION`, as Android 12+
     * requires. It counts as granted only when the user allows *precise* location: approximate
     * location is not accurate enough for the odometer.
     */
    PRECISE_LOCATION,
}
