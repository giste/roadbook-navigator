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


package dev.giste.core.logging.data

import timber.log.Timber

/**
 * Timber tree that records every line instead of printing it, so tests can assert on what a
 * [dev.giste.core.logging.Logger] sent to Timber.
 */
internal class RecordingTree : Timber.Tree() {

    /** A single line as Timber delivered it to the tree. */
    data class Line(val priority: Int, val tag: String?, val message: String, val t: Throwable?)

    /** Every line received, in order. */
    val lines: MutableList<Line> = mutableListOf()

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        lines += Line(priority, tag, message, t)
    }
}
