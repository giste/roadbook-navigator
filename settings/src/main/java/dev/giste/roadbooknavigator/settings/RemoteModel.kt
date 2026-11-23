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

/** The remote control the rider uses, which decides the keys of each [RemoteAction]. */
public enum class RemoteModel {

    /** The DND2 remote: the D-pad moves the roadbook and changes the partial. */
    DND2,

    /** The Terra Pirata remote: media keys move the roadbook, volume keys change the partial. */
    TERRA_PIRATA,

    /** Keys the rider assigns to each action, for any other remote. */
    CUSTOM,
}
