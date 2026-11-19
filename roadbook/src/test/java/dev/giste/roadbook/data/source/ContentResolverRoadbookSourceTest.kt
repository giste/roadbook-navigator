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

package dev.giste.roadbook.data.source

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.net.Uri
import dev.giste.roadbook.FakeLogger
import dev.giste.roadbook.domain.roadbook.ReadRoadbookSourceResult
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Tests [ContentResolverRoadbookSource], created through its factory, against Robolectric's
 * `ContentResolver`, which reads real files and lets tests register what a content URI returns.
 */
@RunWith(RobolectricTestRunner::class)
class ContentResolverRoadbookSourceTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val application: Application = RuntimeEnvironment.getApplication()
    private val logger = FakeLogger()
    private val contentUri: Uri = Uri.parse("content://dev.giste.roadbook.test/documents/1")

    private fun TestScope.sourceOf(uri: Uri) = ContentResolverRoadbookSourceFactory(
        context = application,
        ioDispatcher = StandardTestDispatcher(testScheduler),
        logger = logger,
    ).create(uri)

    /** Makes [contentUri] return what [supplier] gives, or throw what it throws. */
    private fun registerContent(supplier: () -> InputStream?) {
        shadowOf(application.contentResolver).registerInputStreamSupplier(contentUri, supplier)
    }

    @Test
    fun read_existingFile_returnsItsContentAsUtf8() = runTest {
        val file = File(folder.root, "stage.rn2").apply { writeText(CONTENT, Charsets.UTF_8) }

        val result = sourceOf(Uri.fromFile(file)).read()

        assertEquals(ReadRoadbookSourceResult.Success(CONTENT), result)
    }

    @Test
    fun read_contentUri_returnsItsContent() = runTest {
        registerContent { CONTENT.byteInputStream() }

        val result = sourceOf(contentUri).read()

        assertEquals(ReadRoadbookSourceResult.Success(CONTENT), result)
    }

    @Test
    fun read_missingFile_isUnreadableAndLogsError() = runTest {
        val missing = File(folder.root, "missing.rn2")

        val result = sourceOf(Uri.fromFile(missing)).read()

        assertEquals(ReadRoadbookSourceResult.Unreadable, result)
        assertTrue(logger.loggedErrors.single() is FileNotFoundException)
    }

    @Test
    fun read_permissionDenied_isUnreadableAndLogsError() = runTest {
        registerContent { throw SecurityException("Simulated missing permission") }

        val result = sourceOf(contentUri).read()

        assertEquals(ReadRoadbookSourceResult.Unreadable, result)
        assertTrue(logger.loggedErrors.single() is SecurityException)
    }

    @Test
    fun read_readingFails_isUnreadableAndLogsError() = runTest {
        registerContent { FailingInputStream() }

        val result = sourceOf(contentUri).read()

        assertEquals(ReadRoadbookSourceResult.Unreadable, result)
        assertTrue(logger.loggedErrors.single() is IOException)
    }

    @Test
    fun read_noStream_isUnreadableAndWarns() = runTest {
        Robolectric.setupContentProvider(NoFileProvider::class.java, NO_FILE_AUTHORITY)

        val result = sourceOf(Uri.parse("content://$NO_FILE_AUTHORITY/documents/1")).read()

        assertEquals(ReadRoadbookSourceResult.Unreadable, result)
        assertEquals(1, logger.warnings.size)
    }

    /** A stream that opens, but fails on the first read, like a file on a removed SD card. */
    private class FailingInputStream : InputStream() {
        override fun read(): Int = throw IOException("Simulated read failure")
    }

    /**
     * A provider that opens no file, as a provider that has crashed may do. Robolectric only goes
     * through the real `ContentResolver.openInputStream`, which returns `null` then, when a
     * provider is registered.
     */
    class NoFileProvider : ContentProvider() {
        override fun onCreate(): Boolean = true

        override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? = null

        override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?,
        ): Cursor? = null

        override fun getType(uri: Uri): String? = null

        override fun insert(uri: Uri, values: ContentValues?): Uri? = null

        override fun delete(
            uri: Uri,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0

        override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?,
        ): Int = 0
    }

    private companion object {
        /** The authority of [NoFileProvider]. */
        const val NO_FILE_AUTHORITY = "dev.giste.roadbook.test.nofile"

        /** Not only ASCII, to check that the file is decoded as UTF-8. */
        const val CONTENT = """{"route":{"name":"Etapa 1 – Almería"}}"""
    }
}
