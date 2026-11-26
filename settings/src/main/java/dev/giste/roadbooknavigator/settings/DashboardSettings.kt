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
 * How the dashboard shares the screen between its parts. The app decides how to apply it.
 *
 * @property landscapeSideColumn on a screen wider than tall, the share of the width taken by the
 * side column with the distances and the map. The roadbook takes the rest. Defaults to
 * [DEFAULT_LANDSCAPE_SIDE_COLUMN].
 * @property portraitDistances on a screen taller than wide, the share of the height taken by the
 * row with the total and the partial. The map and the roadbook share the rest. Defaults to
 * [DEFAULT_PORTRAIT_DISTANCES].
 */
public data class DashboardSettings(
    public val landscapeSideColumn: ScreenShare = DEFAULT_LANDSCAPE_SIDE_COLUMN,
    public val portraitDistances: ScreenShare = DEFAULT_PORTRAIT_DISTANCES,
) {

    /** Default values of [DashboardSettings]. */
    public companion object {

        /** Default [landscapeSideColumn]: 30 % of the width. */
        public val DEFAULT_LANDSCAPE_SIDE_COLUMN: ScreenShare = ScreenShare.of(30)

        /** Default [portraitDistances]: 8 % of the height. */
        public val DEFAULT_PORTRAIT_DISTANCES: ScreenShare = ScreenShare.of(8)
    }
}
