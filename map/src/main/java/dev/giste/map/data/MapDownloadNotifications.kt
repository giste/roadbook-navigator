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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.ServiceInfo
import android.text.format.Formatter
import androidx.work.ForegroundInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.giste.map.R
import dev.giste.map.domain.FileSize
import dev.giste.map.domain.RemoteMapPath
import javax.inject.Inject

/**
 * The notification shown while a map is downloaded, which keeps the download running as a
 * foreground service.
 *
 * It shows the map's name and how much of it is downloaded, and opens the app when tapped. It goes
 * in its own low-importance channel, so it makes no sound. On Android 13 and later it is only seen
 * once the user allows notifications; the download runs either way.
 *
 * @param context the application context.
 */
internal class MapDownloadNotifications @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    /**
     * The foreground information for the download of [map].
     *
     * @param map the map being downloaded.
     * @param downloaded how much of it is on the device, or `null` before it starts.
     * @param total its whole size, or `null` while unknown.
     * @return the notification, as a data-sync foreground service.
     */
    fun foregroundInfo(map: RemoteMapPath, downloaded: FileSize?, total: FileSize?): ForegroundInfo {
        createChannel()
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.map_ic_download)
            .setContentTitle(context.getString(R.string.map_download_notification_title, map.name))
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
        openAppIntent()?.let(builder::setContentIntent)
        if (downloaded != null && total != null && total.bytes > 0) {
            val percent = (downloaded.bytes * PERCENT / total.bytes).toInt().coerceIn(0, PERCENT)
            builder.setProgress(PERCENT, percent, false)
            builder.setContentText(
                context.getString(R.string.map_download_notification_progress, downloaded.format(), total.format()),
            )
        } else {
            builder.setProgress(0, 0, true)
        }
        return ForegroundInfo(NOTIFICATION_ID, builder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    /** Creates the downloads channel. Creating it again only updates its name. */
    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.map_download_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
    }

    /**
     * Opens the app's launcher screen, whichever app reuses `:map`, or `null` if it has none.
     * Immutable, as nothing needs to change it.
     */
    private fun openAppIntent(): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    /** This size as short text in the current locale, e.g. "120 MB". */
    private fun FileSize.format(): String = Formatter.formatShortFileSize(context, bytes)

    private companion object {
        /** The downloads channel. Kept once created, so it never changes. */
        const val CHANNEL_ID = "map_downloads"

        /** Downloads run one at a time, so they share one notification. */
        const val NOTIFICATION_ID = 0x6d6170

        const val PERCENT = 100
    }
}
