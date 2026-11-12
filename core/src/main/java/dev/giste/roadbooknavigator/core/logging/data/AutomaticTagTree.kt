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

package dev.giste.roadbooknavigator.core.logging.data

import timber.log.Timber

/**
 * Timber debug tree that tags each line with the class that called the
 * [dev.giste.roadbooknavigator.core.logging.Logger].
 *
 * Timber's own [Timber.DebugTree] tags lines with the first class outside Timber, which would
 * always be [TimberLogger]. This tree walks the stack trace further and skips every frame that
 * belongs to one of [ignoredClassNames].
 *
 * It builds a stack trace for every untagged log line, which is slow. That is acceptable
 * because it is only planted in debuggable builds.
 *
 * @param ignoredClassNames fully qualified names of the logging infrastructure classes. A name
 * matches that exact class and its nested, anonymous and lambda classes (`Outer$Inner`), but
 * not other classes that merely start with the same text (`OuterTest`).
 * @param stackTrace returns the current stack trace. Only replaced in tests.
 */
internal class AutomaticTagTree(
    private val ignoredClassNames: List<String> = DEFAULT_IGNORED_CLASS_NAMES,
    private val stackTrace: () -> Array<StackTraceElement> = { Throwable().stackTrace },
) : Timber.DebugTree() {

    /**
     * Returns the tag for the line being logged: the simple name of the first non-logging class
     * on the current stack, or Timber's default tag for [element] when every frame belongs to
     * the logging infrastructure.
     *
     * Public (instead of protected) so tests can call it directly.
     */
    public override fun createStackElementTag(element: StackTraceElement): String? =
        callerTag(stackTrace()) ?: super.createStackElementTag(element)

    /**
     * Returns the simple class name of the first frame in [stackTrace] that is not ignored, or
     * `null` when every frame is ignored.
     *
     * Nested, inner, anonymous and lambda classes are reduced to their outermost class
     * (`PermissionViewModel$uiState$1` becomes `PermissionViewModel`), so a class always logs
     * under the same tag.
     */
    fun callerTag(stackTrace: Array<StackTraceElement>): String? = stackTrace
        .firstOrNull { frame -> !isIgnored(frame.className) }
        ?.className
        ?.substringAfterLast('.')
        ?.substringBefore('$')

    /** Whether [className] is one of [ignoredClassNames] or nested inside one of them. */
    private fun isIgnored(className: String): Boolean =
        ignoredClassNames.any { className == it || className.startsWith("$it$") }

    private companion object {
        /**
         * Classes between the caller and this tree: Timber (including `Timber$Forest` and
         * `Timber$DebugTree`), our [TimberLogger] (including its tagged logger) and this tree.
         */
        val DEFAULT_IGNORED_CLASS_NAMES = listOf(
            Timber::class.java.name,
            TimberLogger::class.java.name,
            AutomaticTagTree::class.java.name,
        )
    }
}
