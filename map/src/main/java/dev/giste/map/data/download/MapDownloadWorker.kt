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

package dev.giste.map.data.download

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.giste.core.logging.Logger
import dev.giste.map.domain.FileSize
import dev.giste.map.domain.RemoteMapPath

/**
 * Downloads one map with [downloader], as a foreground service with a notification, so a download
 * of several minutes is not stopped. WorkManager runs it again, with a growing delay, after a
 * failure that may go away by itself, and the download resumes where it stopped.
 *
 * It reports the progress as [KEY_DOWNLOADED] and [KEY_TOTAL]. Downloads are queued as one chain
 * of work (see [WorkManagerMapDownloadRepository]), where a failed work would cancel the rest of
 * the chain: so a download that fails for good still succeeds as work, with the reason in its
 * output as [KEY_FAILURE].
 *
 * @param context the application context.
 * @param params the work's parameters: [KEY_MAP] in its input says which map to download.
 * @param downloader downloads the map.
 * @param notifications the notification of the download.
 * @param logger logs problems.
 */
@HiltWorker
internal class MapDownloadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val downloader: MapFileDownloader,
    private val notifications: MapDownloadNotifications,
    private val logger: Logger,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val map = inputData.getString(KEY_MAP)?.let(::parseMap)
        if (map == null) {
            // Only a bug enqueues such work: there is nothing to download.
            logger.e("Map download work without a valid map: %s", inputData.getString(KEY_MAP))
            return Result.failure()
        }
        goForeground(map, downloaded = null, total = null)
        val result = downloader.download(map) { downloaded, total ->
            setProgress(workDataOf(KEY_DOWNLOADED to downloaded.bytes, KEY_TOTAL to (total?.bytes ?: UNKNOWN)))
            goForeground(map, downloaded, total)
        }
        return when (result) {
            MapFileDownloadResult.Done -> Result.success()
            MapFileDownloadResult.TryAgain -> Result.retry()
            is MapFileDownloadResult.Failed -> Result.success(workDataOf(KEY_FAILURE to result.reason.name))
        }
    }

    /**
     * Shows or updates the notification and runs as a foreground service. Android may refuse to
     * start one while the app is in the background, e.g. when a retry starts later: the download
     * then runs without it, and if it is stopped, the next try resumes it.
     */
    private suspend fun goForeground(map: RemoteMapPath, downloaded: FileSize?, total: FileSize?) {
        try {
            setForeground(notifications.foregroundInfo(map, downloaded, total))
        } catch (error: IllegalStateException) {
            logger.w("Cannot download the map %s in the foreground: %s", map, error.toString())
        }
    }

    /** [path] as a map, or `null` if it is not a safe path. */
    private fun parseMap(path: String): RemoteMapPath? = try {
        RemoteMapPath.parse(path)
    } catch (_: IllegalArgumentException) {
        null
    }

    /** The keys of the work's input, progress and output. */
    companion object {

        /** Input: the map to download, as [RemoteMapPath.toString] writes it. */
        const val KEY_MAP: String = "map"

        /** Progress: how many bytes of the map are on the device. */
        const val KEY_DOWNLOADED: String = "downloaded"

        /** Progress: the map's whole size in bytes, or [UNKNOWN]. */
        const val KEY_TOTAL: String = "total"

        /** Output: the name of the [dev.giste.map.domain.MapDownloadFailure], if it failed for good. */
        const val KEY_FAILURE: String = "failure"

        /** [KEY_TOTAL] while the size is unknown. */
        const val UNKNOWN: Long = -1
    }
}
