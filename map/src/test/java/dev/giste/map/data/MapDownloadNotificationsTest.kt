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
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import androidx.test.core.app.ApplicationProvider
import dev.giste.map.domain.FileSize
import dev.giste.map.domain.RemoteFolderPath
import dev.giste.map.domain.RemoteMapPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en")
class MapDownloadNotificationsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notifications = MapDownloadNotifications(context)
    private val map = RemoteMapPath(RemoteFolderPath.ROOT.child("europe"), "andorra.map")

    @Test
    fun foregroundInfo_isADataSyncService() {
        val info = notifications.foregroundInfo(map, downloaded = null, total = null)

        assertEquals(ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC, info.foregroundServiceType)
    }

    @Test
    fun foregroundInfo_namesTheMap() {
        val notification = notifications.foregroundInfo(map, downloaded = null, total = null).notification

        assertEquals("Downloading andorra.map", notification.extras.getString(Notification.EXTRA_TITLE))
        assertTrue(notification.flags and Notification.FLAG_ONGOING_EVENT != 0)
    }

    @Test
    fun foregroundInfo_beforeTheStart_isIndeterminate() {
        val notification = notifications.foregroundInfo(map, downloaded = null, total = null).notification

        assertTrue(notification.extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE))
    }

    @Test
    fun foregroundInfo_unknownSize_isIndeterminate() {
        val notification = notifications.foregroundInfo(map, FileSize(10), total = null).notification

        assertTrue(notification.extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE))
    }

    @Test
    fun foregroundInfo_knownSize_showsThePercentAndTheSizes() {
        val notification = notifications.foregroundInfo(map, FileSize(25_000_000), FileSize(100_000_000)).notification

        assertFalse(notification.extras.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE))
        assertEquals(25, notification.extras.getInt(Notification.EXTRA_PROGRESS))
        assertEquals(100, notification.extras.getInt(Notification.EXTRA_PROGRESS_MAX))
        assertEquals("25 MB of 100 MB", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
    }

    @Test
    fun foregroundInfo_opensTheApp() {
        val notification = notifications.foregroundInfo(map, downloaded = null, total = null).notification

        // Robolectric's test app has no launcher activity: the notification then opens nothing.
        val launcher = context.packageManager.getLaunchIntentForPackage(context.packageName)
        assertEquals(launcher != null, notification.contentIntent != null)
    }

    @Test
    fun foregroundInfo_hasACancelActionForThatMap() {
        val notification = notifications.foregroundInfo(map, downloaded = null, total = null).notification

        val action = notification.actions.single()
        assertEquals("Cancel", action.title.toString())
        val intent = shadowOf(action.actionIntent).savedIntent
        assertEquals(MapDownloadCancelReceiver::class.java.name, intent.component?.className)
        assertEquals("europe/andorra.map", intent.getStringExtra(MapDownloadCancelReceiver.EXTRA_MAP))
    }

    @Test
    fun foregroundInfo_twoMaps_haveTheirOwnCancelActions() {
        val other = RemoteMapPath(RemoteFolderPath.ROOT, "world.map")

        val first = notifications.foregroundInfo(map, null, null).notification.actions.single().actionIntent
        val second = notifications.foregroundInfo(other, null, null).notification.actions.single().actionIntent

        val key = MapDownloadCancelReceiver.EXTRA_MAP
        assertEquals("europe/andorra.map", shadowOf(first).savedIntent.getStringExtra(key))
        assertEquals("world.map", shadowOf(second).savedIntent.getStringExtra(key))
    }

    @Test
    fun foregroundInfo_createsAQuietChannel() {
        notifications.foregroundInfo(map, downloaded = null, total = null)

        val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel("map_downloads")
        assertNotNull(channel)
        assertEquals("Map downloads", channel.name)
        assertEquals(NotificationManager.IMPORTANCE_LOW, channel.importance)
    }

    @Test
    @Config(qualifiers = "es")
    fun foregroundInfo_spanish_isTranslated() {
        val notification = notifications.foregroundInfo(map, FileSize(25_000_000), FileSize(100_000_000)).notification

        assertEquals("Descargando andorra.map", notification.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("25 MB de 100 MB", notification.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals("Cancelar", notification.actions.single().title.toString())
    }
}
