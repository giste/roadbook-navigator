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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.giste.core.coroutines.ApplicationScope
import dev.giste.core.logging.Logger
import dev.giste.map.domain.MapDownloadRepository
import dev.giste.map.domain.RemoteMapPath
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Cancels a map download from its notification's cancel action ([MapDownloadNotifications]).
 *
 * Cancelling rebuilds the download queue, which takes a moment, so it runs in the application
 * scope while [goAsync] keeps the receiver alive. The notification asks no confirmation, as Android
 * notification actions are a deliberate tap on a labelled button.
 */
@AndroidEntryPoint
internal class MapDownloadCancelReceiver : BroadcastReceiver() {

    /** Cancels the download. Injected by Hilt in [onReceive]. */
    @Inject
    lateinit var repository: MapDownloadRepository

    /** Where the cancel runs, outliving this call. Injected by Hilt in [onReceive]. */
    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    /** Logs a broken intent. Injected by Hilt in [onReceive]. */
    @Inject
    lateinit var logger: Logger

    override fun onReceive(context: Context, intent: Intent) {
        val map = intent.getStringExtra(EXTRA_MAP)?.let(::parseMap)
        if (map == null) {
            // Only a bug sends such an intent: the receiver is not exported.
            logger.w("Map download cancel without a valid map: %s", intent.getStringExtra(EXTRA_MAP))
            return
        }
        val pending = goAsync()
        scope.launch {
            try {
                repository.cancel(map)
            } finally {
                pending.finish()
            }
        }
    }

    /** [path] as a map, or `null` if it is not a safe path. */
    private fun parseMap(path: String): RemoteMapPath? = try {
        RemoteMapPath.parse(path)
    } catch (_: IllegalArgumentException) {
        null
    }

    /** The key of the intent's extra. */
    companion object {

        /** The map whose download to cancel, as [RemoteMapPath.toString] writes it. */
        const val EXTRA_MAP: String = "dev.giste.map.extra.MAP"
    }
}
