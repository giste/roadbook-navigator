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

import java.io.File
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class VtmMapFileCheckerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val checker = VtmMapFileChecker()

    @Test
    fun problemWith_validMapFile_isNull() {
        val file = writeMapFile(folder.newFile("valid.map"))

        assertNull(checker.problemWith(file))
    }

    @Test
    fun problemWith_missingFile_isReported() {
        assertNotNull(checker.problemWith(File(folder.root, "missing.map")))
    }

    @Test
    fun problemWith_folder_isReported() {
        assertNotNull(checker.problemWith(folder.newFolder("folder.map")))
    }

    @Test
    fun problemWith_emptyFile_isReported() {
        assertNotNull(checker.problemWith(folder.newFile("empty.map")))
    }

    @Test
    fun problemWith_notAMapFile_isReported() {
        val file = folder.newFile("text.map").apply { writeText("This is not a map, just some text.") }

        assertNotNull(checker.problemWith(file))
    }

    @Test
    fun problemWith_wrongMagicBytes_isReported() {
        val file = writeMapFile(folder.newFile("magic.map"), magic = "mapsforge binary XYZ")

        assertNotNull(checker.problemWith(file))
    }

    @Test
    fun problemWith_unsupportedVersion_isReported() {
        val file = writeMapFile(folder.newFile("version.map"), fileVersion = 6)

        assertNotNull(checker.problemWith(file))
    }

    @Test
    fun problemWith_truncatedFile_isReported() {
        // The header records the full size, so a partly copied file does not match it.
        val file = writeMapFile(folder.newFile("truncated.map"))
        file.writeBytes(file.readBytes().copyOf(file.length().toInt() - 1))

        assertNotNull(checker.problemWith(file))
    }
}
