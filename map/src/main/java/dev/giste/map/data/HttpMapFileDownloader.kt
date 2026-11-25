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

package dev.giste.map.data

import dev.giste.core.coroutines.IoDispatcher
import dev.giste.core.logging.Logger
import dev.giste.map.domain.FileSize
import dev.giste.map.domain.MapDownloadFailure
import dev.giste.map.domain.RemoteMapPath
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/** What happened to one try at downloading a map. */
internal sealed interface MapFileDownloadResult {

    /** The map is installed. */
    data object Done : MapFileDownloadResult

    /**
     * The try stopped for a reason that may go away by itself, such as a lost connection or a
     * busy server. What was downloaded is kept when it can be resumed.
     */
    data object TryAgain : MapFileDownloadResult

    /**
     * The download stopped for good, and what was downloaded was thrown away.
     *
     * @property reason why.
     */
    data class Failed(val reason: MapDownloadFailure) : MapFileDownloadResult
}

/** Downloads one map from the download site into the map folder. */
internal interface MapFileDownloader {

    /**
     * Tries once to download [map], resuming what an earlier try left. Main-safe. It never
     * throws, except to cancel; a cancelled download can be resumed by the next try.
     *
     * @param map the map to download.
     * @param onProgress called with how much of the map is on the device and its whole size (or
     * `null` while unknown): once when the download starts, then each time it grows by about one
     * percent (or by [HttpMapFileDownloader.PROGRESS_STEP_BYTES] when the size is unknown).
     * @return what happened.
     */
    suspend fun download(
        map: RemoteMapPath,
        onProgress: suspend (downloaded: FileSize, total: FileSize?) -> Unit,
    ): MapFileDownloadResult
}

/**
 * Downloads maps over HTTP into the map folder, keeping the site's folders, e.g.
 * `europe/italy/centro.map` from the site goes to `<map folder>/europe/italy/centro.map`.
 *
 * The map is written to a `.part` file next to where it goes, which the map files listing ignores.
 * Only when it is complete and [checker] says the renderer can read it is it renamed to its `.map`
 * name, so a broken download never reaches the map. The installed file's modification time is the
 * site's `Last-Modified`, so it can later be compared with the site to find updates.
 *
 * Maps are big (up to a few gigabytes), so a try that stops is resumed by the next one with an
 * HTTP range request. The `.part` file's modification time keeps the site's `Last-Modified` while
 * it waits, and is sent in `If-Range`: if the map changed on the site in the meantime, the site
 * sends the whole new map instead of the rest, so two versions are never mixed. A site that sends
 * no `Last-Modified` cannot be resumed safely, so its `.part` file is deleted when a try stops.
 *
 * @param client the HTTP client.
 * @param site the addresses of the maps.
 * @param folder the map folder.
 * @param checker tells whether the renderer can read a downloaded map.
 * @param freeSpace tells whether the map fits.
 * @param folderChanges is told when a map is installed, so the listings refresh.
 * @param ioDispatcher the dispatcher the download runs on: OkHttp is called synchronously, so the
 * download can be written as it is read.
 * @param logger logs what happened to each try.
 */
internal class HttpMapFileDownloader @Inject constructor(
    @param:MapHttpClient private val client: OkHttpClient,
    private val site: MapSite,
    @param:MapFolder private val folder: File,
    private val checker: MapFileChecker,
    private val freeSpace: FreeSpace,
    private val folderChanges: MapFolderChanges,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val logger: Logger,
) : MapFileDownloader {

    override suspend fun download(
        map: RemoteMapPath,
        onProgress: suspend (downloaded: FileSize, total: FileSize?) -> Unit,
    ): MapFileDownloadResult = withContext(ioDispatcher) {
        val target = File(folder, map.toString())
        val part = File(target.path + PART_SUFFIX)
        try {
            storage { prepareFolder(target) }
            fetch(map, target, part, onProgress)
        } catch (failure: StorageFailure) {
            logger.e(failure.cause ?: failure, "Cannot write the map %s", map)
            part.delete()
            MapFileDownloadResult.Failed(MapDownloadFailure.STORAGE_ERROR)
        } catch (error: IOException) {
            // A cancelled download fails here too, as its call is cancelled: stop as cancelled.
            currentCoroutineContext().ensureActive()
            logger.w("Download of the map %s stopped: %s", map, error.toString())
            MapFileDownloadResult.TryAgain
        }
    }

    /**
     * Creates the folder of [target]. Names from the site are safe, but [target] is checked again
     * to be inside the map folder, as nothing outside it may be written.
     */
    private fun prepareFolder(target: File) {
        val inFolder = target.canonicalFile.toPath().startsWith(folder.canonicalFile.toPath())
        if (!inFolder) throw IOException("${target.name} is not inside the map folder")
        val parent = checkNotNull(target.parentFile)
        parent.mkdirs()
        if (!parent.isDirectory) throw IOException("Cannot create the folder of ${target.name}")
    }

    /** Asks the site for [map], or for its rest if [part] holds its beginning, and saves it. */
    private suspend fun fetch(
        map: RemoteMapPath,
        target: File,
        part: File,
        onProgress: suspend (FileSize, FileSize?) -> Unit,
    ): MapFileDownloadResult {
        val resumeFrom = if (part.isFile) part.length() else 0L
        val request = Request.Builder().url(site.url(map)).apply {
            if (resumeFrom > 0) {
                header("Range", "bytes=$resumeFrom-")
                header("If-Range", httpDate(part.lastModified()))
            }
        }.build()
        val call = client.newCall(request)
        // A cancelled coroutine cancels the call, so a blocked read stops at once.
        val cancellation = currentCoroutineContext().job.invokeOnCompletion { call.cancel() }
        try {
            return call.execute().use { response -> save(map, response, resumeFrom, target, part, onProgress) }
        } finally {
            cancellation.dispose()
        }
    }

    /** Saves the map in [response] to [part], then installs it as [target]. */
    private suspend fun save(
        map: RemoteMapPath,
        response: Response,
        resumeFrom: Long,
        target: File,
        part: File,
        onProgress: suspend (FileSize, FileSize?) -> Unit,
    ): MapFileDownloadResult {
        val start = when (response.code) {
            // The whole map: the site cannot resume, or the map changed since the part was saved.
            HTTP_OK -> 0L
            HTTP_PARTIAL_CONTENT -> response.rangeStart()
            else -> return response.failure(map, part)
        }
        if (start != 0L && start != resumeFrom) {
            // Not the rest that was asked for, so it cannot be placed. Start again from scratch.
            logger.w("The map site sent %s from byte %d, not %d", map, start, resumeFrom)
            part.delete()
            return MapFileDownloadResult.TryAgain
        }
        val total = if (response.code == HTTP_PARTIAL_CONTENT) response.rangeTotal() else response.knownLength()
        val remaining = total?.minus(start) ?: response.knownLength()
        if (remaining != null && remaining > freeSpace.bytesFreeIn(folder)) {
            logger.w("Not enough space for the map %s (%d bytes)", map, remaining)
            part.delete()
            return MapFileDownloadResult.Failed(MapDownloadFailure.NOT_ENOUGH_SPACE)
        }
        val lastModified = response.headers.getDate("Last-Modified")?.time
        logger.i("Downloading the map %s from byte %d of %s", map, start, total ?: "?")

        writeBody(response, part, append = start > 0, total, lastModified, onProgress)

        if (total != null && part.length() != total) {
            // The site closed the answer early without an error. Resume on the next try, if the
            // part can be resumed safely.
            logger.w("The map %s ended at %d of %d bytes", map, part.length(), total)
            if (lastModified == null) part.delete()
            return MapFileDownloadResult.TryAgain
        }
        return install(map, part, target, lastModified)
    }

    /**
     * Writes the body of [response] to [part], reporting the progress. Its modification time is
     * then set back to [lastModified], whatever happens, so it can be resumed. If the writing
     * stops and there is no [lastModified] to resume it safely with, [part] is deleted.
     */
    private suspend fun writeBody(
        response: Response,
        part: File,
        append: Boolean,
        total: Long?,
        lastModified: Long?,
        onProgress: suspend (FileSize, FileSize?) -> Unit,
    ) {
        val totalSize = total?.let(::FileSize)
        var complete = false
        try {
            val output = storage { FileOutputStream(part, append) }
            output.use {
                var written = if (append) part.length() else 0L
                var reported = written
                onProgress(FileSize(written), totalSize)
                val buffer = ByteArray(BUFFER_BYTES)
                val source = response.body.byteStream()
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = source.read(buffer)
                    if (read == -1) break
                    storage { output.write(buffer, 0, read) }
                    written += read
                    if (written - reported >= progressStep(total)) {
                        reported = written
                        onProgress(FileSize(written), totalSize)
                    }
                }
                if (reported != written) onProgress(FileSize(written), totalSize)
            }
            complete = true
        } finally {
            when {
                lastModified != null -> part.setLastModified(lastModified)
                !complete -> part.delete()
            }
        }
    }

    /**
     * Installs the complete [part] as [target] if the renderer can read it, replacing any older
     * [target], and tells the listings.
     */
    private fun install(map: RemoteMapPath, part: File, target: File, lastModified: Long?): MapFileDownloadResult {
        val problem = checker.problemWith(part)
        if (problem != null) {
            logger.w("The downloaded map %s cannot be read: %s", map, problem)
            part.delete()
            return MapFileDownloadResult.Failed(MapDownloadFailure.UNREADABLE_MAP)
        }
        storage {
            // In the same folder, so the map appears at once, whole, or not at all.
            Files.move(
                part.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        }
        if (lastModified != null) target.setLastModified(lastModified)
        logger.i("Installed the map %s", map)
        folderChanges.notifyChanged()
        return MapFileDownloadResult.Done
    }

    /**
     * What an answer other than the map means. A busy or broken server, or a range the site no
     * longer has, may go away; anything else will not.
     */
    private fun Response.failure(map: RemoteMapPath, part: File): MapFileDownloadResult {
        logger.w("The map site answered %d for %s", code, map)
        return when {
            code == HTTP_RANGE_NOT_SATISFIABLE -> {
                // The beginning kept does not fit the map on the site any more: start again.
                part.delete()
                MapFileDownloadResult.TryAgain
            }
            code in RETRY_CODES || code >= HTTP_SERVER_ERROR -> MapFileDownloadResult.TryAgain
            else -> {
                part.delete()
                MapFileDownloadResult.Failed(MapDownloadFailure.SERVER_ERROR)
            }
        }
    }

    /** The length of the body, or `null` if the site did not say it. */
    private fun Response.knownLength(): Long? = body.contentLength().takeIf { it >= 0 }

    /** The first byte of a partial answer, from `Content-Range: bytes <first>-<last>/<total>`. */
    private fun Response.rangeStart(): Long =
        CONTENT_RANGE.find(header("Content-Range").orEmpty())?.groupValues?.get(1)?.toLongOrNull()
            // Without a readable range, the bytes cannot be placed: treat them as misplaced.
            ?: -1L

    /** The whole size in a partial answer, or `null` if the site wrote `*` or nothing. */
    private fun Response.rangeTotal(): Long? =
        CONTENT_RANGE.find(header("Content-Range").orEmpty())?.groupValues?.get(2)?.toLongOrNull()

    /**
     * How much the download grows between two reports: about one percent of [total], or
     * [PROGRESS_STEP_BYTES] when it is unknown.
     */
    private fun progressStep(total: Long?): Long =
        total?.let { (it / PERCENT).coerceAtLeast(1) } ?: PROGRESS_STEP_BYTES

    /** Runs a file-system [operation], marking its I/O failures as [StorageFailure]. */
    private inline fun <T> storage(operation: () -> T): T = try {
        operation()
    } catch (error: IOException) {
        throw StorageFailure(error)
    } catch (error: SecurityException) {
        throw StorageFailure(error)
    }

    /**
     * A failure to write to the device, kept apart from network failures, which are also
     * `IOException`s but may go away by themselves.
     */
    private class StorageFailure(cause: Throwable) : Exception(cause)

    /** Values of the HTTP exchange and of the progress reports. */
    companion object {

        /** Added to the map's file name while it is downloaded. */
        const val PART_SUFFIX: String = ".part"

        /** How much a download of unknown size grows between two progress reports. */
        const val PROGRESS_STEP_BYTES: Long = 4L * 1024 * 1024

        private const val PERCENT = 100
        private const val BUFFER_BYTES = 64 * 1024
        private const val HTTP_OK = 200
        private const val HTTP_PARTIAL_CONTENT = 206
        private const val HTTP_RANGE_NOT_SATISFIABLE = 416
        private const val HTTP_SERVER_ERROR = 500

        /** Request Timeout and Too Many Requests: the site may answer later. */
        private val RETRY_CODES = setOf(408, 429)

        /** `bytes <first>-<last>/<total or *>`. */
        private val CONTENT_RANGE = Regex("""^bytes (\d+)-\d+/(\d+|\*)$""")

        /** HTTP dates, e.g. `Sun, 06 Nov 1994 08:49:37 GMT` (RFC 9110). */
        private val HTTP_DATE: DateTimeFormatter =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US).withZone(ZoneOffset.UTC)

        /** [millis] as an HTTP date. */
        private fun httpDate(millis: Long): String = HTTP_DATE.format(Instant.ofEpochMilli(millis))
    }
}
