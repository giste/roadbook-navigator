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

/**
 * The settings of the app itself, which belong to no feature module.
 *
 * @property themeMode whether the app uses its light or dark colors. Defaults to
 * [DEFAULT_THEME_MODE].
 * @property orientation which way the app is shown on the screen. Defaults to
 * [DEFAULT_ORIENTATION].
 */
public data class AppSettings(
    public val themeMode: ThemeMode = DEFAULT_THEME_MODE,
    public val orientation: ScreenOrientation = DEFAULT_ORIENTATION,
) {

    /** Default values of [AppSettings]. */
    public companion object {

        /** Default [themeMode]: follow the system. */
        public val DEFAULT_THEME_MODE: ThemeMode = ThemeMode.SYSTEM

        /** Default [orientation]: follow the device's rotation. */
        public val DEFAULT_ORIENTATION: ScreenOrientation = ScreenOrientation.FOLLOW_SYSTEM
    }
}
