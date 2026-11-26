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
 * A share of the screen, as a whole percentage: how much of its width or height one part of a
 * screen takes. Always between [MIN_PERCENT] and [MAX_PERCENT], so the part and the rest of the
 * screen both get some space.
 *
 * @property percent the share, in percent.
 */
@JvmInline
public value class ScreenShare private constructor(public val percent: Int) {

    /** The share as a fraction of the screen, e.g. `0.3` for 30 %. */
    public val fraction: Float
        get() = percent / 100f

    /** Creating and checking shares. */
    public companion object {

        /** The smallest share, in percent. */
        public const val MIN_PERCENT: Int = 1

        /** The largest share, in percent. */
        public const val MAX_PERCENT: Int = 99

        /**
         * The share of [percent].
         *
         * @param percent the share, in percent.
         * @return the share.
         * @throws IllegalArgumentException if [percent] is not between [MIN_PERCENT] and
         * [MAX_PERCENT]; check values read from outside with [isValid] first.
         */
        public fun of(percent: Int): ScreenShare {
            require(isValid(percent)) { "Share out of range: $percent %" }
            return ScreenShare(percent)
        }

        /**
         * Whether [percent] is a valid share.
         *
         * @param percent the share, in percent.
         * @return whether it is between [MIN_PERCENT] and [MAX_PERCENT].
         */
        public fun isValid(percent: Int): Boolean = percent in MIN_PERCENT..MAX_PERCENT
    }
}
