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

package dev.giste.roadbooknavigator.ui

import android.content.pm.ActivityInfo
import dev.giste.roadbooknavigator.settings.ScreenOrientation

/**
 * The activity orientation (`ActivityInfo.SCREEN_ORIENTATION_*`) that shows the app in this
 * screen orientation.
 *
 * The fixed orientations use the `USER_` variants: the app stays vertical or horizontal, but turns
 * upside down when the device does, so it can be mounted either way up, unless the rider has
 * locked the rotation.
 *
 * @return the value for `Activity.requestedOrientation`.
 */
internal fun ScreenOrientation.toActivityOrientation(): Int = when (this) {
    ScreenOrientation.FOLLOW_SYSTEM -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    ScreenOrientation.VERTICAL -> ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
    ScreenOrientation.HORIZONTAL -> ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE
}
