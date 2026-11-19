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

/**
 * How the roadbook is colored. It only applies to the roadbook: the rest of the app keeps its own
 * theme.
 */
public enum class RoadbookStyle {

    /** The colors of the app theme, so the roadbook follows its light and dark modes. */
    APP_THEME,

    /**
     * The colors of FIA paper roadbooks: black on white, with a blue track and red danger marks,
     * in the light and dark modes alike, so the rider sees what they are used to on paper.
     */
    FIA,
}
