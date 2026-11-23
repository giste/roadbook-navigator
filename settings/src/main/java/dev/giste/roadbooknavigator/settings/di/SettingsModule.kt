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

package dev.giste.roadbooknavigator.settings.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.giste.core.coroutines.IoDispatcher
import dev.giste.location.LocationSettingsSource
import dev.giste.map.MapSettingsSource
import dev.giste.odometer.OdometerSettingsSource
import dev.giste.roadbook.RoadbookSettings
import dev.giste.roadbook.RoadbookSettingsSource
import dev.giste.roadbooknavigator.settings.AppSettings
import dev.giste.roadbooknavigator.settings.AppSettingsSource
import dev.giste.roadbooknavigator.settings.RemoteSettings
import dev.giste.roadbooknavigator.settings.RemoteSettingsSource
import dev.giste.roadbooknavigator.settings.data.AppSettingsDataStore
import dev.giste.roadbooknavigator.settings.data.DataStoreAppSettingsRepository
import dev.giste.roadbooknavigator.settings.data.DataStoreRemoteSettingsRepository
import dev.giste.roadbooknavigator.settings.data.DataStoreRoadbookSettingsRepository
import dev.giste.roadbooknavigator.settings.data.FixedLocationSettingsSource
import dev.giste.roadbooknavigator.settings.data.FixedMapSettingsSource
import dev.giste.roadbooknavigator.settings.data.FixedOdometerSettingsSource
import dev.giste.roadbooknavigator.settings.data.RemoteSettingsDataStore
import dev.giste.roadbooknavigator.settings.data.RoadbookSettingsDataStore
import dev.giste.roadbooknavigator.settings.domain.SettingsRepository
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Binds the settings source of every feature module and of the app, for the whole app (AGENTS.md
 * §4.4), and the repositories the settings screen changes. The settings the screen changes come
 * from their DataStore repositories; the `:location` and `:odometer` ones are fixed.
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface SettingsModule {

    /** Provides the stored app settings wherever an [AppSettingsSource] is injected. */
    @Binds
    fun bindAppSettingsSource(repository: DataStoreAppSettingsRepository): AppSettingsSource

    /** Provides the app settings repository to the settings screen, which changes them. */
    @Binds
    fun bindAppSettingsRepository(
        repository: DataStoreAppSettingsRepository,
    ): SettingsRepository<AppSettings>

    /** Provides the stored remote settings wherever a [RemoteSettingsSource] is injected. */
    @Binds
    fun bindRemoteSettingsSource(
        repository: DataStoreRemoteSettingsRepository,
    ): RemoteSettingsSource

    /** Provides the remote settings repository to the settings screen, which changes them. */
    @Binds
    fun bindRemoteSettingsRepository(
        repository: DataStoreRemoteSettingsRepository,
    ): SettingsRepository<RemoteSettings>

    /** Provides the `:roadbook` settings repository to the settings screen, which changes them. */
    @Binds
    fun bindRoadbookSettingsRepository(
        repository: DataStoreRoadbookSettingsRepository,
    ): SettingsRepository<RoadbookSettings>

    /** Provides the fixed `:location` settings wherever a [LocationSettingsSource] is injected. */
    @Binds
    fun bindLocationSettingsSource(source: FixedLocationSettingsSource): LocationSettingsSource

    /** Provides the fixed `:map` settings wherever a [MapSettingsSource] is injected. */
    @Binds
    fun bindMapSettingsSource(source: FixedMapSettingsSource): MapSettingsSource

    /** Provides the fixed `:odometer` settings wherever an [OdometerSettingsSource] is injected. */
    @Binds
    fun bindOdometerSettingsSource(source: FixedOdometerSettingsSource): OdometerSettingsSource

    /** Provides the stored `:roadbook` settings wherever a [RoadbookSettingsSource] is injected. */
    @Binds
    fun bindRoadbookSettingsSource(
        repository: DataStoreRoadbookSettingsRepository,
    ): RoadbookSettingsSource

    /**
     * Provides one DataStore per settings file (AGENTS.md §7).
     *
     * Each is a [Singleton], because DataStore allows only one instance per file. A corrupted file
     * is replaced with an empty one, so the module falls back to its defaults instead of failing.
     */
    companion object {

        /**
         * Provides the DataStore of the app's own settings.
         *
         * @param context the application context, to locate the file.
         * @param ioDispatcher the dispatcher DataStore reads and writes the file on.
         * @return the DataStore stored in the `settings_app` file.
         */
        @Provides
        @Singleton
        @AppSettingsDataStore
        fun provideAppSettingsDataStore(
            @ApplicationContext context: Context,
            @IoDispatcher ioDispatcher: CoroutineDispatcher,
        ): DataStore<Preferences> =
            settingsDataStore(context, ioDispatcher, DataStoreAppSettingsRepository.FILE_NAME)

        /**
         * Provides the DataStore of the remote control settings.
         *
         * @param context the application context, to locate the file.
         * @param ioDispatcher the dispatcher DataStore reads and writes the file on.
         * @return the DataStore stored in the `settings_remote` file.
         */
        @Provides
        @Singleton
        @RemoteSettingsDataStore
        fun provideRemoteSettingsDataStore(
            @ApplicationContext context: Context,
            @IoDispatcher ioDispatcher: CoroutineDispatcher,
        ): DataStore<Preferences> =
            settingsDataStore(context, ioDispatcher, DataStoreRemoteSettingsRepository.FILE_NAME)

        /**
         * Provides the DataStore of the `:roadbook` settings.
         *
         * @param context the application context, to locate the file.
         * @param ioDispatcher the dispatcher DataStore reads and writes the file on.
         * @return the DataStore stored in the `settings_roadbook` file.
         */
        @Provides
        @Singleton
        @RoadbookSettingsDataStore
        fun provideRoadbookSettingsDataStore(
            @ApplicationContext context: Context,
            @IoDispatcher ioDispatcher: CoroutineDispatcher,
        ): DataStore<Preferences> =
            settingsDataStore(context, ioDispatcher, DataStoreRoadbookSettingsRepository.FILE_NAME)

        /**
         * A Preferences DataStore in the file [fileName], reading and writing on [ioDispatcher],
         * that replaces a corrupted file with an empty one.
         */
        private fun settingsDataStore(
            context: Context,
            ioDispatcher: CoroutineDispatcher,
            fileName: String,
        ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = CoroutineScope(ioDispatcher + SupervisorJob()),
            produceFile = { context.preferencesDataStoreFile(fileName) },
        )
    }
}
