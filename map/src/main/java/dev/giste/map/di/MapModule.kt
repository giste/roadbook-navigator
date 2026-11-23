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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.giste.map.data.LocalMapFileRepository
import dev.giste.map.data.MapFileChecker
import dev.giste.map.data.MapFolder
import dev.giste.map.data.VtmMapFileChecker
import dev.giste.map.domain.MapFileRepository
import java.io.File

/** Binds the `:map` implementations for the whole app. */
@Module
@InstallIn(SingletonComponent::class)
internal interface MapModule {

    /**
     * Provides [LocalMapFileRepository] wherever a [MapFileRepository] is injected.
     *
     * It holds no state (each collection reads the folder), so it is not scoped.
     */
    @Binds
    fun bindMapFileRepository(repository: LocalMapFileRepository): MapFileRepository

    /** Provides [VtmMapFileChecker] wherever a [MapFileChecker] is injected. */
    @Binds
    fun bindMapFileChecker(checker: VtmMapFileChecker): MapFileChecker

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
    }
}
