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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutomaticTagTreeTest {

    private val tree = AutomaticTagTree()

    @Test
    fun callerTag_loggingFramesFirst_returnsFirstCallerClass() {
        val stackTrace = arrayOf(
            frame("dev.giste.core.logging.data.AutomaticTagTree"),
            frame("timber.log.Timber\$DebugTree"),
            frame("timber.log.Timber\$Tree"),
            frame("timber.log.Timber\$Forest"),
            frame("dev.giste.core.logging.data.TimberLogger"),
            frame("dev.giste.roadbooknavigator.odometer.presentation.OdometerViewModel"),
            frame("dev.giste.roadbooknavigator.app.MainActivity"),
        )

        assertEquals("OdometerViewModel", tree.callerTag(stackTrace))
    }

    @Test
    fun callerTag_taggedLoggerFrame_isSkipped() {
        val stackTrace = arrayOf(
            frame("dev.giste.core.logging.data.TimberLogger\$TaggedTimberLogger"),
            frame("dev.giste.roadbooknavigator.roadbook.data.Rn2Parser"),
        )

        assertEquals("Rn2Parser", tree.callerTag(stackTrace))
    }

    @Test
    fun callerTag_nestedOrLambdaCaller_returnsOutermostClass() {
        val stackTrace = arrayOf(
            frame("dev.giste.core.permission.PermissionViewModel\$uiState\$1"),
        )

        assertEquals("PermissionViewModel", tree.callerTag(stackTrace))
    }

    @Test
    fun callerTag_classInDefaultPackage_returnsClassName() {
        assertEquals("Caller", tree.callerTag(arrayOf(frame("Caller"))))
    }

    @Test
    fun callerTag_onlyLoggingFrames_returnsNull() {
        val stackTrace = arrayOf(
            frame("timber.log.Timber\$Forest"),
            frame("dev.giste.core.logging.data.TimberLogger"),
        )

        assertNull(tree.callerTag(stackTrace))
    }

    @Test
    fun callerTag_emptyStackTrace_returnsNull() {
        assertNull(tree.callerTag(emptyArray()))
    }

    @Test
    fun callerTag_customIgnoredClasses_skipsThem() {
        val customTree = AutomaticTagTree(ignoredClassNames = listOf("com.example.Wrapper"))
        val stackTrace = arrayOf(
            frame("com.example.Wrapper\$Inner"),
            frame("com.example.Caller"),
        )

        assertEquals("Caller", customTree.callerTag(stackTrace))
    }

    @Test
    fun callerTag_classNameStartingWithIgnoredName_isNotSkipped() {
        val stackTrace = arrayOf(
            frame("dev.giste.core.logging.data.TimberLogger"),
            frame("dev.giste.core.logging.data.TimberLoggerFactory"),
        )

        assertEquals("TimberLoggerFactory", tree.callerTag(stackTrace))
    }

    @Test
    fun createStackElementTag_calledFromTest_returnsTestClassName() {
        val tag = tree.createStackElementTag(frame("com.example.Ignored"))

        assertEquals("AutomaticTagTreeTest", tag)
    }

    @Test
    fun createStackElementTag_callerOnStack_returnsCallerTag() {
        val stackTree = AutomaticTagTree(
            stackTrace = { arrayOf(frame("timber.log.Timber\$Forest"), frame("com.example.Caller")) },
        )

        val tag = stackTree.createStackElementTag(frame("com.example.Ignored"))

        assertEquals("Caller", tag)
    }

    @Test
    fun createStackElementTag_everyFrameIgnored_fallsBackToElementTag() {
        val ignoreAllTree = AutomaticTagTree(
            stackTrace = { arrayOf(frame("timber.log.Timber\$Forest")) },
        )

        val tag = ignoreAllTree.createStackElementTag(frame("com.example.Fallback\$1"))

        assertEquals("Fallback", tag)
    }

    private fun frame(className: String) = StackTraceElement(className, "method", "File.kt", 1)
}
