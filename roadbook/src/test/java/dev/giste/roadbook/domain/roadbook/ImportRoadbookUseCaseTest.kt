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

package dev.giste.roadbook.domain.roadbook

import dev.giste.roadbook.FakeLogger
import dev.giste.roadbook.ImportRoadbookFailure
import dev.giste.roadbook.ImportRoadbookResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportRoadbookUseCaseTest {

    private val roadbook =
        Roadbook(name = "Stage 1", waypoints = listOf(testWaypoint(1), testWaypoint(2)))
    private val previous = Roadbook(name = "Stage 0", waypoints = listOf(testWaypoint(1)))

    private val parser = FakeRoadbookParser(ParseRoadbookResult.Success(roadbook))
    private val repository = FakeRoadbookRepository(SavedRoadbook.Available(previous))
    private val logger = FakeLogger()
    private val importRoadbook = ImportRoadbookUseCase(parser, repository, logger)

    private val readableSource = RoadbookSource { ReadRoadbookSourceResult.Success(FILE_CONTENT) }
    private val unreadableSource = RoadbookSource { ReadRoadbookSourceResult.Unreadable }

    @Test
    fun invoke_validFile_savesRoadbookAndSucceeds() = runTest {
        val result = importRoadbook(readableSource)

        assertEquals(ImportRoadbookResult.Success, result)
        assertEquals(SavedRoadbook.Available(roadbook), repository.roadbook.value)
    }

    @Test
    fun invoke_validFile_parsesContentOfSource() = runTest {
        importRoadbook(readableSource)

        assertEquals(listOf(FILE_CONTENT), parser.parsedContents)
    }

    @Test
    fun invoke_unreadableSource_failsWithoutParsingOrSaving() = runTest {
        val result = importRoadbook(unreadableSource)

        assertEquals(ImportRoadbookResult.Failure(ImportRoadbookFailure.UNREADABLE), result)
        assertTrue(parser.parsedContents.isEmpty())
        assertEquals(0, repository.saveCount)
    }

    @Test
    fun invoke_malformedFile_failsAndKeepsPreviousRoadbook() = runTest {
        parser.result = ParseRoadbookResult.Failure(ParseRoadbookFailure.MALFORMED)

        val result = importRoadbook(readableSource)

        assertEquals(ImportRoadbookResult.Failure(ImportRoadbookFailure.MALFORMED), result)
        assertEquals(SavedRoadbook.Available(previous), repository.roadbook.value)
        assertEquals(0, repository.saveCount)
    }

    @Test
    fun invoke_fileWithoutWaypoints_failsAndKeepsPreviousRoadbook() = runTest {
        parser.result = ParseRoadbookResult.Failure(ParseRoadbookFailure.NO_WAYPOINTS)

        val result = importRoadbook(readableSource)

        assertEquals(ImportRoadbookResult.Failure(ImportRoadbookFailure.NO_WAYPOINTS), result)
        assertEquals(SavedRoadbook.Available(previous), repository.roadbook.value)
        assertEquals(0, repository.saveCount)
    }

    @Test
    fun invoke_saveFails_failsAsNotSaved() = runTest {
        repository.failSaves = true

        val result = importRoadbook(readableSource)

        assertEquals(ImportRoadbookResult.Failure(ImportRoadbookFailure.NOT_SAVED), result)
        assertEquals(SavedRoadbook.Available(previous), repository.roadbook.value)
    }

    @Test
    fun invoke_failure_logsWarning() = runTest {
        importRoadbook(unreadableSource)

        assertEquals(1, logger.warnings.size)
    }

    private companion object {
        const val FILE_CONTENT = """{"route":{}}"""
    }
}
