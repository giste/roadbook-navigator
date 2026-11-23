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

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File

/*
 * Writes the smallest files VTM opens as mapsforge maps: a valid header (format version 5, see
 * https://github.com/mapsforge/mapsforge/blob/master/docs/Specification-Binary-Map-File.md) and
 * one empty sub-file. They draw nothing, but they are enough to test that files are checked,
 * without shipping real maps of hundreds of megabytes.
 */

/** The magic bytes every mapsforge file starts with. */
private const val MAGIC = "mapsforge binary OSM"

/** Size of the empty sub-file that follows the header. */
private const val SUB_FILE_SIZE = 16

/** 2024-01-01, a valid map date (the format rejects dates before 2010). */
private const val MAP_DATE_MILLIS = 1_704_067_200_000L

/**
 * Writes a mapsforge map file to [file].
 *
 * @param file where to write it. Its folder must exist.
 * @param fileVersion the format version: VTM reads 3 to 5.
 * @param magic the magic bytes: anything but [MAGIC] makes the file invalid.
 * @return [file].
 */
internal fun writeMapFile(
    file: File,
    fileVersion: Int = 5,
    magic: String = MAGIC,
): File {
    val body = remainingHeader(fileVersion)
    val headerSize = magic.length + Int.SIZE_BYTES + body.size
    val fileSize = headerSize + SUB_FILE_SIZE
    // The remaining header holds the file size and the sub-file address, which depend on its own
    // size: it has a fixed size, so write it once to measure it and again with the real values.
    val finalBody = remainingHeader(fileVersion, fileSize = fileSize.toLong(), subFileStart = headerSize.toLong())

    val bytes = ByteArrayOutputStream()
    DataOutputStream(bytes).use { out ->
        out.write(magic.toByteArray(Charsets.US_ASCII))
        out.writeInt(finalBody.size)
        out.write(finalBody)
        out.write(ByteArray(SUB_FILE_SIZE))
    }
    file.writeBytes(bytes.toByteArray())
    return file
}

/**
 * The header after the magic bytes and its own size, around Madrid, without tags and with one
 * sub-file for zoom levels 0 to 21.
 */
private fun remainingHeader(fileVersion: Int, fileSize: Long = 0, subFileStart: Long = 0): ByteArray {
    val bytes = ByteArrayOutputStream()
    DataOutputStream(bytes).use { out ->
        out.writeInt(fileVersion)
        out.writeLong(fileSize)
        out.writeLong(MAP_DATE_MILLIS)
        // Bounding box in microdegrees: min latitude, min longitude, max latitude, max longitude.
        out.writeInt(40_000_000)
        out.writeInt(-4_000_000)
        out.writeInt(41_000_000)
        out.writeInt(-3_000_000)
        out.writeShort(256) // Tile size in pixels.
        out.writeVariableLengthString("Mercator")
        out.writeByte(0) // No optional fields.
        out.writeShort(0) // No POI tags.
        out.writeShort(0) // No way tags.
        out.writeByte(1) // One sub-file: base zoom 14, zoom levels 0 to 21.
        out.writeByte(14)
        out.writeByte(0)
        out.writeByte(21)
        out.writeLong(subFileStart)
        out.writeLong(SUB_FILE_SIZE.toLong())
    }
    return bytes.toByteArray()
}

/** Writes a string as mapsforge does: its length in one byte (short strings only), then UTF-8. */
private fun DataOutputStream.writeVariableLengthString(value: String) {
    val utf8 = value.toByteArray(Charsets.UTF_8)
    require(utf8.size < 128) { "Only lengths that fit in one variable-length byte are supported" }
    writeByte(utf8.size)
    write(utf8)
}
