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

import dev.giste.map.domain.FileSize
import dev.giste.map.domain.RemoteMapEntry
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DirectoryListingParserTest {

    /** A test resource as text. */
    private fun resource(name: String): String =
        checkNotNull(javaClass.classLoader?.getResource(name)) { "Missing test resource $name" }.readText()

    /** A listing page whose table holds [rows]. */
    private fun listing(vararg rows: String): String = """
        <html><head><title>Index of /maps/v5/europe</title></head><body><table>
        <tr><th><a href="?C=N;O=D">Name</a></th></tr>
        ${rows.joinToString("\n")}
        </table></body></html>
    """.trimIndent()

    /** A listing row like Apache's, for an entry linked as [href]. */
    private fun row(href: String, date: String = "2026-09-07 15:30  ", size: String = "2.4M") =
        """<tr><td valign="top"><img src="/icons/unknown.gif" alt="[   ]"></td><td><a href="$href">$href</a>   </td>""" +
            """<td align="right">$date</td><td align="right">$size</td><td>&nbsp;</td></tr>"""

    private fun mapFile(name: String, bytes: Long?, day: LocalDate?) =
        RemoteMapEntry.MapFile(name, bytes?.let(::FileSize), day)

    @Test
    fun parse_mapsforgeRoot_listsItsFolders() {
        val entries = DirectoryListingParser.parse(resource("mapsforge_listing_root.html"))

        val expected = listOf(
            "africa", "asia", "australia-oceania", "central-america", "europe",
            "north-america", "russia", "south-america", "world",
        ).map { RemoteMapEntry.Folder(it) }
        assertEquals(expected, entries)
    }

    @Test
    fun parse_mapsforgeEurope_listsFoldersFirstThenMaps() {
        val entries = checkNotNull(DirectoryListingParser.parse(resource("mapsforge_listing_europe.html")))

        val folders = listOf("france", "germany", "italy", "netherlands", "spain", "united-kingdom")
            .map { RemoteMapEntry.Folder(it) }
        assertEquals(folders, entries.take(6))
        assertEquals(49, entries.drop(6).count { it is RemoteMapEntry.MapFile })
        assertEquals(55, entries.size)
        assertEquals("albania.map", entries[6].name)
        assertEquals("ukraine.map", entries.last().name)
    }

    @Test
    fun parse_mapsforgeEurope_readsSizesAndDays() {
        val entries = checkNotNull(DirectoryListingParser.parse(resource("mapsforge_listing_europe.html")))

        assertEquals(
            mapFile("andorra.map", 2_516_582, LocalDate.of(2026, 9, 7)),
            entries.single { it.name == "andorra.map" },
        )
        assertEquals(
            mapFile("albania.map", 45_088_768, LocalDate.of(2026, 8, 21)),
            entries.single { it.name == "albania.map" },
        )
    }

    @Test
    fun parse_notAListing_isNull() {
        assertNull(DirectoryListingParser.parse("<html><head><title>Maintenance</title></head></html>"))
    }

    @Test
    fun parse_listingWithoutEntries_isEmpty() {
        assertEquals(emptyList<RemoteMapEntry>(), DirectoryListingParser.parse(listing()))
    }

    @Test
    fun parse_parentAndSortLinks_areLeftOut() {
        val html = listing(row("/maps/v5/"), row("?C=M;O=A"), row("#top"))

        assertEquals(emptyList<RemoteMapEntry>(), DirectoryListingParser.parse(html))
    }

    @Test
    fun parse_linksElsewhere_areLeftOut() {
        val html = listing(row("https://example.org/spain.map"), row("deeper/spain.map"), row("spain.map?x=1"))

        assertEquals(emptyList<RemoteMapEntry>(), DirectoryListingParser.parse(html))
    }

    @Test
    fun parse_otherFiles_areLeftOut() {
        val html = listing(row("readme.txt"), row("spain.map.md5"), row("spain.map"))

        assertEquals(listOf("spain.map"), DirectoryListingParser.parse(html)?.map { it.name })
    }

    @Test
    fun parse_mapExtensionInCapitals_isAMap() {
        assertEquals(listOf("SPAIN.MAP"), DirectoryListingParser.parse(listing(row("SPAIN.MAP")))?.map { it.name })
    }

    @Test
    fun parse_unsafeNames_areLeftOut() {
        val html = listing(row("../"), row("./"), row("%2E%2E/"), row("a%2Fb.map"), row("a%5Cb.map"), row("a%0Ab.map"))

        assertEquals(emptyList<RemoteMapEntry>(), DirectoryListingParser.parse(html))
    }

    @Test
    fun parse_encodedName_isDecoded() {
        val entries = DirectoryListingParser.parse(listing(row("la%20rioja.map"), row("a+b.map")))

        assertEquals(listOf("la rioja.map", "a+b.map"), entries?.map { it.name })
    }

    @Test
    fun parse_badlyEncodedLink_isLeftOut() {
        assertEquals(emptyList<RemoteMapEntry>(), DirectoryListingParser.parse(listing(row("bad%zz.map"))))
    }

    @Test
    fun parse_sizes_useTheirUnitsInPowersOf1024() {
        val html = listing(
            row("bytes.map", size = "812"),
            row("kilo.map", size = "1.5K"),
            row("mega.map", size = " 43M"),
            row("giga.map", size = "3.3G"),
            row("tera.map", size = "1T"),
        )

        val sizes = DirectoryListingParser.parse(html)?.map { (it as RemoteMapEntry.MapFile).approximateSize?.bytes }

        assertEquals(listOf(812L, 1_536L, 45_088_768L, 3_543_348_019L, 1_099_511_627_776L), sizes)
    }

    @Test
    fun parse_missingOrUnreadableSizeAndDay_areEmpty() {
        val html = listing(
            row("dash.map", date = "&nbsp;", size = "  - "),
            row("odd.map", date = "yesterday", size = "big"),
            row("impossible.map", date = "2026-13-40 10:00", size = "2.4X"),
        )

        val expected = listOf(
            mapFile("dash.map", null, null),
            mapFile("odd.map", null, null),
            mapFile("impossible.map", null, null),
        )
        assertEquals(expected, DirectoryListingParser.parse(html))
    }

    @Test
    fun parse_rowWithoutCells_isLeftOut() {
        val html = listing("""<tr><th colspan="5"><hr></th></tr>""", """<tr><td>no link</td></tr>""")

        assertEquals(emptyList<RemoteMapEntry>(), DirectoryListingParser.parse(html))
    }

    @Test
    fun parse_rowWithOnlyALink_isAMapWithoutSizeOrDay() {
        val html = listing("""<tr><td></td><td><a href="spain.map">spain.map</a></td></tr>""")

        assertEquals(listOf(mapFile("spain.map", null, null)), DirectoryListingParser.parse(html))
    }

    @Test
    fun parse_foldersAfterMaps_comeFirstInTheirOrder() {
        val html = listing(row("b.map"), row("zeta/"), row("a.map"), row("alpha/"))

        val expected = listOf(
            RemoteMapEntry.Folder("zeta"),
            RemoteMapEntry.Folder("alpha"),
            mapFile("b.map", 2_516_582, LocalDate.of(2026, 9, 7)),
            mapFile("a.map", 2_516_582, LocalDate.of(2026, 9, 7)),
        )
        assertEquals(expected, DirectoryListingParser.parse(html))
    }
}
