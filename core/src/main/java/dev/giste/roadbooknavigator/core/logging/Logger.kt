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

package dev.giste.roadbooknavigator.core.logging

/**
 * The only logging entry point of the app.
 *
 * Every module logs through this interface instead of calling `android.util.Log`, `println` or
 * a logging library directly, so domain code stays free of Android dependencies and tests can
 * replace it with a fake. An implementation is bound in the Hilt `SingletonComponent`, so it can
 * be injected anywhere.
 *
 * Log calls never throw and are safe to call from any thread. Nothing is printed until
 * [DebugLogging.install] has been called, which `:app` only does in debuggable builds, so
 * release builds log nothing. Never log precise locations or other personal data anyway.
 *
 * Unless the logger was created with [withTag], each log line is tagged with the simple name of
 * the calling class (for example `OdometerViewModel`), so messages do not need a class prefix.
 *
 * Messages are [String.format] patterns: when [args] are given, they are substituted into
 * `message` (e.g. `logger.d("Loaded %d waypoints", count)`). Without [args], `message` is
 * logged as is.
 */
public interface Logger {

    /**
     * Logs a verbose message: fine-grained detail that is only useful while debugging.
     *
     * @param message the message, or a [String.format] pattern when [args] are given.
     * @param args the values substituted into [message].
     */
    public fun v(message: String, vararg args: Any?)

    /**
     * Logs a debug message: information useful to follow the app's flow.
     *
     * @param message the message, or a [String.format] pattern when [args] are given.
     * @param args the values substituted into [message].
     */
    public fun d(message: String, vararg args: Any?)

    /**
     * Logs an info message: a noteworthy, expected event.
     *
     * @param message the message, or a [String.format] pattern when [args] are given.
     * @param args the values substituted into [message].
     */
    public fun i(message: String, vararg args: Any?)

    /**
     * Logs a warning: something unexpected happened, but the app can carry on.
     *
     * @param message the message, or a [String.format] pattern when [args] are given.
     * @param args the values substituted into [message].
     */
    public fun w(message: String, vararg args: Any?)

    /**
     * Logs an error without an exception.
     *
     * @param message the message, or a [String.format] pattern when [args] are given.
     * @param args the values substituted into [message].
     */
    public fun e(message: String, vararg args: Any?)

    /**
     * Logs an error caused by [t]. The stack trace of [t] is appended to the message.
     *
     * @param t the exception that caused the error.
     * @param message the message, or a [String.format] pattern when [args] are given.
     * @param args the values substituted into [message].
     */
    public fun e(t: Throwable, message: String, vararg args: Any?)

    /**
     * Returns a logger that tags every line with [tag] instead of the calling class name.
     *
     * The returned logger is independent: this logger keeps its own tagging.
     *
     * @param tag the tag to use for every line logged through the returned logger.
     * @return a logger that always uses [tag].
     */
    public fun withTag(tag: String): Logger
}
