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

import dev.giste.core.logging.Logger
import timber.log.Timber
import javax.inject.Inject

/**
 * [Logger] implementation that forwards every call to Timber.
 *
 * It is stateless: what actually gets printed depends on the trees planted in Timber
 * (see [dev.giste.core.logging.DebugLogging]). With no tree planted, calls are no-ops.
 */
internal class TimberLogger @Inject constructor() : Logger {

    override fun v(message: String, vararg args: Any?): Unit = Timber.v(message, *args)

    override fun d(message: String, vararg args: Any?): Unit = Timber.d(message, *args)

    override fun i(message: String, vararg args: Any?): Unit = Timber.i(message, *args)

    override fun w(message: String, vararg args: Any?): Unit = Timber.w(message, *args)

    override fun e(message: String, vararg args: Any?): Unit = Timber.e(message, *args)

    override fun e(t: Throwable, message: String, vararg args: Any?): Unit =
        Timber.e(t, message, *args)

    override fun withTag(tag: String): Logger = TaggedTimberLogger(tag)

    /**
     * Logger that sets an explicit Timber tag before every call.
     *
     * Timber stores an explicit tag per thread and clears it after the next log call, so it must
     * be set right before each call rather than once.
     */
    private class TaggedTimberLogger(private val tag: String) : Logger {

        override fun v(message: String, vararg args: Any?) = Timber.tag(tag).v(message, *args)

        override fun d(message: String, vararg args: Any?) = Timber.tag(tag).d(message, *args)

        override fun i(message: String, vararg args: Any?) = Timber.tag(tag).i(message, *args)

        override fun w(message: String, vararg args: Any?) = Timber.tag(tag).w(message, *args)

        override fun e(message: String, vararg args: Any?) = Timber.tag(tag).e(message, *args)

        override fun e(t: Throwable, message: String, vararg args: Any?) =
            Timber.tag(tag).e(t, message, *args)

        override fun withTag(tag: String): Logger = TaggedTimberLogger(tag)
    }
}
