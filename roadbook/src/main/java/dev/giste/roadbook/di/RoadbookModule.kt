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
package dev.giste.roadbook.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.giste.core.coroutines.IoDispatcher
import dev.giste.roadbook.data.persistence.DataStoreRoadbookRepository
import dev.giste.roadbook.data.persistence.RoadbookFileSerializer
import dev.giste.roadbook.data.rn2.Rn2RoadbookParser
import dev.giste.roadbook.domain.roadbook.RoadbookParser
import dev.giste.roadbook.domain.roadbook.RoadbookRepository
import dev.giste.roadbook.domain.roadbook.SavedRoadbook
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/** Binds the `:roadbook` implementations for the whole app. */
@Module
@InstallIn(SingletonComponent::class)
internal interface RoadbookModule {

    /**
     * Provides [Rn2RoadbookParser] wherever a [RoadbookParser] is injected: `.rn2` is the only
     * roadbook format so far.
     */
    @Binds
    fun bindRoadbookParser(parser: Rn2RoadbookParser): RoadbookParser

    /**
     * Provides [DataStoreRoadbookRepository] wherever a [RoadbookRepository] is injected.
     *
     * It holds no state of its own (the DataStore does), so it is not scoped.
     */
    @Binds
    fun bindRoadbookRepository(repository: DataStoreRoadbookRepository): RoadbookRepository

    /** Storage used by the bindings above. */
    companion object {

        /**
         * Provides the DataStore of the saved roadbook.
         *
         * A [Singleton], because DataStore allows only one instance per file. It needs no
         * corruption handler: [RoadbookFileSerializer] reads a damaged file as an unreadable
         * roadbook instead of failing.
         *
         * The type is internal to `:roadbook`, so it needs no qualifier to be told apart from the
         * DataStores of other modules.
         *
         * @param context the application context, to locate the file.
         * @param ioDispatcher the dispatcher DataStore reads and writes the file on.
         * @param serializer reads and writes the file.
         * @return the DataStore stored in the `roadbook_route.json` file.
         */
        @Provides
        @Singleton
        fun provideRoadbookDataStore(
            @ApplicationContext context: Context,
            @IoDispatcher ioDispatcher: CoroutineDispatcher,
            serializer: RoadbookFileSerializer,
        ): DataStore<SavedRoadbook> = DataStoreFactory.create(
            serializer = serializer,
            scope = CoroutineScope(ioDispatcher + SupervisorJob()),
            produceFile = { context.dataStoreFile(RoadbookFileSerializer.FILE_NAME) },
        )
    }
}
