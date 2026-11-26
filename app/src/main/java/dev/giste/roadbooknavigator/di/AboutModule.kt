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

package dev.giste.roadbooknavigator.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.giste.core.coroutines.IoDispatcher
import dev.giste.core.logging.Logger
import dev.giste.roadbooknavigator.data.AboutLibrariesOpenSourceLibraryRepository
import dev.giste.roadbooknavigator.data.readLibraryDefinitions
import dev.giste.roadbooknavigator.domain.OpenSourceLibraryRepository
import kotlinx.coroutines.CoroutineDispatcher

/** Provides what the About screen needs. */
@Module
@InstallIn(SingletonComponent::class)
internal object AboutModule {

    /**
     * Provides the open-source libraries the app ships, read from the list the AboutLibraries
     * plugin generates at build time.
     *
     * @param context the application context, to read the list from its raw resources.
     * @param ioDispatcher where the list is read.
     * @param logger reports why the list could not be read.
     * @return the source of the About screen's library list.
     */
    @Provides
    fun provideOpenSourceLibraryRepository(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
        logger: Logger,
    ): OpenSourceLibraryRepository = AboutLibrariesOpenSourceLibraryRepository(
        readDefinitions = context::readLibraryDefinitions,
        ioDispatcher = ioDispatcher,
        logger = logger,
    )
}
