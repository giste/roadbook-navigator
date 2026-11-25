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

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.giste.core.logging.Logger
import dev.giste.map.domain.FileSize
import dev.giste.map.domain.MapDownload
import dev.giste.map.domain.MapDownloadFailure
import dev.giste.map.domain.MapDownloadRepository
import dev.giste.map.domain.RemoteMapPath
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Downloads maps with WorkManager, which runs them in the background and keeps them across
 * process deaths and restarts.
 *
 * All downloads are one chain of unique work, [QUEUE], so they run one at a time, in the order
 * they were started: several big downloads at once would only share the bandwidth. Each work is
 * a [MapDownloadWorker] tagged with its map, which is how [downloads] tells them apart. A download
 * that fails for good still succeeds as work (see [MapDownloadWorker]), so the rest of the chain
 * runs.
 *
 * WorkManager keeps finished work for a while (about a day), so a map's last failure is listed
 * until it is started again or forgotten. A failure followed by a successful download of the same
 * map may still be listed: the map is installed then, which the screens show instead.
 *
 * @param context the application context, to get the app's [WorkManager]. It is got when first
 * used, after the app has configured it.
 * @param logger logs failures of WorkManager's own storage.
 */
@Singleton
internal class WorkManagerMapDownloadRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val logger: Logger,
) : MapDownloadRepository {

    private val workManager: WorkManager by lazy { WorkManager.getInstance(context) }

    /** Makes [start] check and enqueue in one step, so two quick starts enqueue a map once. */
    private val starting = Mutex()

    override val downloads: Flow<List<MapDownload>>
        get() = workManager.getWorkInfosForUniqueWorkFlow(QUEUE)
            .map { it.toDownloads() }
            .catch { error ->
                // WorkManager keeps its work in a database, which may fail to be read.
                logger.e(error, "Cannot read the map downloads")
                emit(emptyList())
            }

    override suspend fun start(map: RemoteMapPath) {
        starting.withLock {
            val pending = workManager.getWorkInfosForUniqueWorkFlow(QUEUE).first()
            if (pending.any { !it.state.isFinished && tagOf(map) in it.tags }) return
            val request = OneTimeWorkRequestBuilder<MapDownloadWorker>()
                .setInputData(workDataOf(MapDownloadWorker.KEY_MAP to map.toString()))
                .setConstraints(Constraints(requiredNetworkType = NetworkType.UNMETERED))
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                .addTag(tagOf(map))
                .build()
            // APPEND_OR_REPLACE: after the downloads already queued. A chain that was cancelled
            // or failed (only by a bug) is replaced, rather than failing the new download at once.
            workManager.enqueueUniqueWork(QUEUE, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
            logger.i("Queued the download of the map %s", map)
        }
    }

    /** The name of the work chain and the tags of its works. */
    companion object {

        /** The unique work that holds every map download. */
        const val QUEUE: String = "map_downloads"

        /** The start of the tag that says which map a work downloads. */
        const val TAG_PREFIX: String = "map_download:"

        /** The first delay before a download that stopped is tried again. It doubles each time. */
        private const val BACKOFF_SECONDS = 30L

        /**
         * The tag of the work that downloads [map].
         *
         * @param map the map.
         * @return [TAG_PREFIX] and the map's path, e.g. `map_download:europe/andorra.map`.
         */
        fun tagOf(map: RemoteMapPath): String = TAG_PREFIX + map
    }
}

/**
 * The downloads these works stand for, at most one per map: the waiting or running work of a map
 * if it has one, or else its last failure. Successful works are left out, as their maps are
 * installed, and so are works that failed or were cancelled, which only a bug causes.
 *
 * @return the downloads.
 */
internal fun List<WorkInfo>.toDownloads(): List<MapDownload> =
    mapNotNull { info -> info.map()?.let { it to info } }
        .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        .mapNotNull { (map, infos) ->
            val active = infos.firstOrNull { !it.state.isFinished }
            if (active != null) active.toActiveDownload(map) else infos.firstNotNullOfOrNull { it.toFailure(map) }
        }

/** The map this work downloads, from its tag, or `null` if it has none that can be read. */
private fun WorkInfo.map(): RemoteMapPath? {
    val path = tags.firstOrNull { it.startsWith(WorkManagerMapDownloadRepository.TAG_PREFIX) }
        ?.removePrefix(WorkManagerMapDownloadRepository.TAG_PREFIX)
        ?: return null
    return try {
        RemoteMapPath.parse(path)
    } catch (_: IllegalArgumentException) {
        null
    }
}

/** The download of [map] that this waiting or running work stands for. */
private fun WorkInfo.toActiveDownload(map: RemoteMapPath): MapDownload =
    if (state == WorkInfo.State.RUNNING) {
        val total = progress.getLong(MapDownloadWorker.KEY_TOTAL, MapDownloadWorker.UNKNOWN)
        MapDownload.Running(
            map = map,
            downloaded = FileSize(progress.getLong(MapDownloadWorker.KEY_DOWNLOADED, 0L).coerceAtLeast(0L)),
            total = total.takeIf { it >= 0 }?.let(::FileSize),
        )
    } else {
        // ENQUEUED or BLOCKED: waiting for Wi-Fi, for a retry or for the downloads before it.
        MapDownload.Queued(map)
    }

/** The failure of [map] this finished work reports, or `null` if it reports none. */
private fun WorkInfo.toFailure(map: RemoteMapPath): MapDownload.Failed? {
    if (state != WorkInfo.State.SUCCEEDED) return null
    val name = outputData.getString(MapDownloadWorker.KEY_FAILURE) ?: return null
    val reason = MapDownloadFailure.entries.firstOrNull { it.name == name } ?: return null
    return MapDownload.Failed(map, reason)
}
