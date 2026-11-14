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

package dev.giste.odometer

import dev.giste.core.logging.Logger

/**
 * [Logger] for tests: prints nothing and records warnings and errors, so tests can check that a
 * problem was logged.
 *
 * `:core` does not share its test doubles, so `:odometer` keeps its own.
 */
internal class FakeLogger : Logger {

    /** The messages passed to [w], in call order. */
    val warnings: MutableList<String> = mutableListOf()

    /** The exceptions passed to [e], in call order. */
    val loggedErrors: MutableList<Throwable> = mutableListOf()

    override fun v(message: String, vararg args: Any?) = Unit

    override fun d(message: String, vararg args: Any?) = Unit

    override fun i(message: String, vararg args: Any?) = Unit

    override fun w(message: String, vararg args: Any?) {
        warnings += message
    }

    override fun e(message: String, vararg args: Any?) = Unit

    override fun e(t: Throwable, message: String, vararg args: Any?) {
        loggedErrors += t
    }

    override fun withTag(tag: String): Logger = this
}
