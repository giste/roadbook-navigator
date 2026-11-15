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

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.giste.roadbook.data.rn2.Rn2RoadbookParser
import dev.giste.roadbook.domain.roadbook.RoadbookParser

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
}
