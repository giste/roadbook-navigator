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
import dev.giste.odometer.OdometerSettingsSource
import dev.giste.roadbook.RoadbookSettingsSource
import dev.giste.roadbooknavigator.settings.data.DataStoreLocationSettingsRepository
import dev.giste.roadbooknavigator.settings.data.DataStoreOdometerSettingsRepository
import dev.giste.roadbooknavigator.settings.data.DataStoreRoadbookSettingsRepository
import dev.giste.roadbooknavigator.settings.data.LocationSettingsDataStore
import dev.giste.roadbooknavigator.settings.data.OdometerSettingsDataStore
import dev.giste.roadbooknavigator.settings.data.RoadbookSettingsDataStore
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/**
 * Binds the settings source of every feature module to its DataStore repository, for the whole
 * app (AGENTS.md §4.4).
 */
@Module
@InstallIn(SingletonComponent::class)
internal interface SettingsModule {

    /** Provides the stored `:location` settings wherever a [LocationSettingsSource] is injected. */
    @Binds
    fun bindLocationSettingsSource(
        repository: DataStoreLocationSettingsRepository,
    ): LocationSettingsSource

    /** Provides the stored `:odometer` settings wherever an [OdometerSettingsSource] is injected. */
    @Binds
    fun bindOdometerSettingsSource(
        repository: DataStoreOdometerSettingsRepository,
    ): OdometerSettingsSource

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
         * Provides the DataStore of the `:location` settings.
         *
         * @param context the application context, to locate the file.
         * @param ioDispatcher the dispatcher DataStore reads and writes the file on.
         * @return the DataStore stored in the `settings_location` file.
         */
        @Provides
        @Singleton
        @LocationSettingsDataStore
        fun provideLocationSettingsDataStore(
            @ApplicationContext context: Context,
            @IoDispatcher ioDispatcher: CoroutineDispatcher,
        ): DataStore<Preferences> =
            settingsDataStore(context, ioDispatcher, DataStoreLocationSettingsRepository.FILE_NAME)

        /**
         * Provides the DataStore of the `:odometer` settings.
         *
         * @param context the application context, to locate the file.
         * @param ioDispatcher the dispatcher DataStore reads and writes the file on.
         * @return the DataStore stored in the `settings_odometer` file.
         */
        @Provides
        @Singleton
        @OdometerSettingsDataStore
        fun provideOdometerSettingsDataStore(
            @ApplicationContext context: Context,
            @IoDispatcher ioDispatcher: CoroutineDispatcher,
        ): DataStore<Preferences> =
            settingsDataStore(context, ioDispatcher, DataStoreOdometerSettingsRepository.FILE_NAME)

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
