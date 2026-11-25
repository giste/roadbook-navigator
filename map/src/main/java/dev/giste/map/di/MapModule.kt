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

package dev.giste.map.di

import android.content.Context
import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.giste.map.MapDownloadServer
import dev.giste.map.data.download.ConnectivityNetworkConnection
import dev.giste.map.data.download.FreeSpace
import dev.giste.map.data.download.HttpMapFileDownloader
import dev.giste.map.data.remote.HttpMapUpdateRepository
import dev.giste.map.data.remote.HttpRemoteMapRepository
import dev.giste.map.data.LocalMapFileRepository
import dev.giste.map.data.MapFileChecker
import dev.giste.map.data.download.MapFileDownloader
import dev.giste.map.data.MapFolder
import dev.giste.map.data.remote.MapHttpClient
import dev.giste.map.data.download.StorageManagerFreeSpace
import dev.giste.map.data.VtmMapFileChecker
import dev.giste.map.data.download.WorkManagerMapDownloadRepository
import dev.giste.map.domain.MapDownloadRepository
import dev.giste.map.domain.MapFileRepository
import dev.giste.map.domain.MapUpdateRepository
import dev.giste.map.domain.NetworkConnection
import dev.giste.map.domain.RemoteMapRepository
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient

/** Binds the `:map` implementations for the whole app. */
@Module
@InstallIn(SingletonComponent::class)
internal interface MapModule {

    /**
     * Provides [LocalMapFileRepository] wherever a [MapFileRepository] is injected.
     *
     * It holds no state (each collection reads the folder, and the changes it follows are a
     * singleton of their own), so it is not scoped.
     */
    @Binds
    fun bindMapFileRepository(repository: LocalMapFileRepository): MapFileRepository

    /** Provides [VtmMapFileChecker] wherever a [MapFileChecker] is injected. */
    @Binds
    fun bindMapFileChecker(checker: VtmMapFileChecker): MapFileChecker

    /**
     * Provides [HttpRemoteMapRepository] wherever a [RemoteMapRepository] is injected.
     *
     * It holds no state (each call reads the site), so it is not scoped.
     */
    @Binds
    fun bindRemoteMapRepository(repository: HttpRemoteMapRepository): RemoteMapRepository

    /**
     * Provides [HttpMapUpdateRepository] wherever a [MapUpdateRepository] is injected.
     *
     * It is a singleton of its own, as it keeps the dates it found for every screen.
     */
    @Binds
    fun bindMapUpdateRepository(repository: HttpMapUpdateRepository): MapUpdateRepository

    /**
     * Provides [WorkManagerMapDownloadRepository] wherever a [MapDownloadRepository] is injected.
     * It is a singleton of its own.
     */
    @Binds
    fun bindMapDownloadRepository(repository: WorkManagerMapDownloadRepository): MapDownloadRepository

    /**
     * Provides [HttpMapFileDownloader] wherever a [MapFileDownloader] is injected.
     *
     * It is a singleton of its own, as it writes one map at a time under a lock.
     */
    @Binds
    fun bindMapFileDownloader(downloader: HttpMapFileDownloader): MapFileDownloader

    /** Provides [ConnectivityNetworkConnection] wherever a [NetworkConnection] is injected. */
    @Binds
    fun bindNetworkConnection(connection: ConnectivityNetworkConnection): NetworkConnection

    /** Provides [StorageManagerFreeSpace] wherever a [FreeSpace] is injected. */
    @Binds
    fun bindFreeSpace(freeSpace: StorageManagerFreeSpace): FreeSpace

    /**
     * Lets an app provide its own [MapDownloadServer]. Without one, `:map` uses the default
     * server.
     */
    @BindsOptionalOf
    fun optionalMapDownloadServer(): MapDownloadServer

    /** Locations used by the bindings above. */
    companion object {

        /**
         * Provides the folder that holds the map files.
         *
         * It is in the no-backup folder, because map files are hundreds of megabytes: Auto Backup
         * would try to upload them (and fail over its size limit), and they can be installed
         * again anyway.
         *
         * @param context the application context, to locate the folder.
         * @return the `map_files` folder inside the app's no-backup folder. It may not exist yet.
         */
        @Provides
        @MapFolder
        fun provideMapFolder(@ApplicationContext context: Context): File =
            File(context.noBackupFilesDir, LocalMapFileRepository.FOLDER_NAME)

        /**
         * Provides the HTTP client for the download site, for its folder listings and its maps.
         *
         * One client for the whole app, so its connections and threads are shared. The timeouts
         * are for a silent server, not for a slow download: they apply to each wait for data, so
         * a download of several minutes is not cut.
         *
         * @return the client.
         */
        @Provides
        @Singleton
        @MapHttpClient
        fun provideHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        /** How long to wait for the download site to accept a connection. */
        private const val CONNECT_TIMEOUT_SECONDS = 15L

        /** How long to wait for the next data from the download site. */
        private const val READ_TIMEOUT_SECONDS = 30L
    }
}
