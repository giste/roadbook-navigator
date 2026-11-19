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

package dev.giste.roadbook.data.rn2

import dev.giste.roadbook.FakeLogger
import dev.giste.roadbook.domain.drawing.CanvasPoint
import dev.giste.roadbook.domain.drawing.Icon
import dev.giste.roadbook.domain.drawing.IconType
import dev.giste.roadbook.domain.drawing.Road
import dev.giste.roadbook.domain.drawing.RoadType
import dev.giste.roadbook.domain.drawing.Text
import dev.giste.roadbook.domain.drawing.Track
import dev.giste.roadbook.domain.drawing.TulipElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Rn2ElementMapperTest {

    private val logger = FakeLogger()
    private val mapper = Rn2ElementMapper(logger)
    private val exit = CanvasPoint(75.0, 0.0)

    /** A symbol of a known type, so mapping it logs no warning. */
    private val churchIcon = Rn2Icon(name = "Church")

    // Roads

    @Test
    fun mapTulip_road_keepsItsPoints() {
        val road = Rn2Road(
            start = Rn2Point(10.0, 20.0),
            end = Rn2Point(-30.0, -40.0),
            handles = listOf(Rn2Point(1.0, 2.0), Rn2Point(3.0, 4.0)),
        )

        val mapped = tulip(road).single() as Road

        assertEquals(CanvasPoint(10.0, 20.0), mapped.start)
        assertEquals(CanvasPoint(-30.0, -40.0), mapped.end)
        assertEquals(listOf(CanvasPoint(1.0, 2.0), CanvasPoint(3.0, 4.0)), mapped.handles)
    }

    @Test
    fun mapTulip_roadWithoutPoints_hasNone() {
        val mapped = tulip(Rn2Road()).single() as Road

        assertNull(mapped.start)
        assertNull(mapped.end)
    }

    @Test
    fun mapTulip_roadTypeIds_areRallyNavigatorKinds() {
        val expected = mapOf(
            15 to RoadType.LOW_VISIBILITY_TRACK,
            16 to RoadType.OFF_TRACK,
            4 to RoadType.SMALL_TRACK,
            17 to RoadType.TRACK,
            18 to RoadType.TARMAC_ROAD,
            12 to RoadType.DUAL_CARRIAGEWAY,
        )

        expected.forEach { (typeId, type) ->
            val road = tulip(Rn2Road(typeId = typeId)).single() as Road

            assertEquals("typeId $typeId", type, road.type)
        }
    }

    @Test
    fun mapTulip_roadWithoutOrWithUnknownType_isATrack() {
        // A plain road does not inherit: only the roads of a track do.
        val roads = tulip(Rn2Road(), Rn2Road(typeId = 99), inherited = RoadType.TARMAC_ROAD)

        assertEquals(listOf(RoadType.TRACK, RoadType.TRACK), roads.map { (it as Road).type })
    }

    // Tracks

    @Test
    fun mapTulip_trackWithoutPoints_usesTheDefaults() {
        val track = tulip(Rn2Track()).single() as Track

        assertNull(track.roadIn.start)
        assertEquals(DEFAULT_ROAD_IN_END, track.roadIn.end)
        assertEquals(CanvasPoint.CENTER, track.roadOut.start)
        assertEquals(exit, track.roadOut.end)
    }

    @Test
    fun mapTulip_trackWithAllPoints_keepsThem() {
        val track = Rn2Track(
            roadIn = Rn2Road(start = Rn2Point(0.0, 40.0), end = Rn2Point(0.0, 5.0)),
            roadOut = Rn2Road(start = Rn2Point(0.0, -5.0), end = Rn2Point(-20.0, -50.0)),
        )

        val mapped = tulip(track).single() as Track

        assertEquals(CanvasPoint(0.0, 40.0), mapped.roadIn.start)
        assertEquals(CanvasPoint(0.0, 5.0), mapped.roadIn.end)
        assertEquals(CanvasPoint(0.0, -5.0), mapped.roadOut.start)
        assertEquals(CanvasPoint(-20.0, -50.0), mapped.roadOut.end)
    }

    @Test
    fun mapTulip_trackWithRoadOutEnd_doesNotComputeTheExit() {
        var exitComputed = false
        val track = Rn2Track(roadOut = Rn2Road(end = Rn2Point(1.0, -1.0)))

        mapper.mapTulip(listOf(track), RoadType.TRACK) { exitComputed = true; exit }

        assertFalse(exitComputed)
    }

    @Test
    fun mapTulip_trackWithoutTypes_inheritsTheRoadIn() {
        val track = tulip(Rn2Track(), inherited = RoadType.TARMAC_ROAD).single() as Track

        assertEquals(RoadType.TARMAC_ROAD, track.roadIn.type)
        assertEquals(RoadType.TARMAC_ROAD, track.roadOut.type)
    }

    @Test
    fun mapTulip_trackWithRoadInType_givesItToTheRoadOut() {
        val track = Rn2Track(roadIn = Rn2Road(typeId = 4))

        val mapped = tulip(track, inherited = RoadType.TARMAC_ROAD).single() as Track

        assertEquals(RoadType.SMALL_TRACK, mapped.roadIn.type)
        assertEquals(RoadType.SMALL_TRACK, mapped.roadOut.type)
    }

    @Test
    fun mapTulip_trackWithBothTypes_keepsThem() {
        val track = Rn2Track(roadIn = Rn2Road(typeId = 4), roadOut = Rn2Road(typeId = 12))

        val mapped = tulip(track).single() as Track

        assertEquals(RoadType.SMALL_TRACK, mapped.roadIn.type)
        assertEquals(RoadType.DUAL_CARRIAGEWAY, mapped.roadOut.type)
    }

    @Test
    fun mapTulip_secondTrack_inheritsTheRoadOutOfTheFirst() {
        val first = Rn2Track(roadOut = Rn2Road(typeId = 18))

        val second = tulip(first, Rn2Track())[1] as Track

        assertEquals(RoadType.TARMAC_ROAD, second.roadIn.type)
    }

    @Test
    fun mapTulip_withTrack_leavesOnItsRoadOut() {
        val track = Rn2Track(roadOut = Rn2Road(typeId = 16))

        val result = mapper.mapTulip(listOf(track), RoadType.TRACK) { exit }

        assertEquals(RoadType.OFF_TRACK, result.roadTypeOut)
    }

    @Test
    fun mapTulip_withoutTrack_leavesOnTheInheritedType() {
        val result = mapper.mapTulip(listOf(Rn2Road(typeId = 18)), RoadType.SMALL_TRACK) { exit }

        assertEquals(RoadType.SMALL_TRACK, result.roadTypeOut)
    }

    // Texts

    @Test
    fun mapTulip_text_keepsItsValues() {
        val text = Rn2Text(
            text = "CAP 120",
            fontSize = 24,
            lineHeight = 0.8,
            width = 120.0,
            height = 30.0,
            maxWidth = 150.0,
            maxHeight = 60.0,
            x = 5.0,
            y = -10.0,
        )

        val mapped = tulip(text).single() as Text

        assertEquals(
            Text(
                text = "CAP 120",
                fontSize = 24,
                lineHeight = 0.8,
                width = 120.0,
                height = 30.0,
                maxWidth = 150.0,
                maxHeight = 60.0,
                center = CanvasPoint(5.0, -10.0),
            ),
            mapped,
        )
    }

    @Test
    fun mapTulip_textWithoutOptionalValues_usesTheDefaults() {
        val mapped = tulip(Rn2Text(text = "Village")).single() as Text

        assertEquals(18, mapped.fontSize)
        assertEquals(1.0, mapped.lineHeight, 0.0)
        assertEquals(180.0, mapped.maxWidth, 0.0)
        assertEquals(100.0, mapped.maxHeight, 0.0)
    }

    @Test
    fun mapTulip_textThatCannotBeDrawn_isSkippedWithAWarning() {
        val invalid = listOf(
            Rn2Text(fontSize = 0),
            Rn2Text(lineHeight = 0.0),
            Rn2Text(width = -1.0),
            Rn2Text(height = -1.0),
            Rn2Text(maxWidth = -1.0),
            Rn2Text(maxHeight = -1.0),
            Rn2Text(x = Double.POSITIVE_INFINITY),
        )

        invalid.forEach { text -> assertTrue("$text", tulip(text).isEmpty()) }
        assertEquals(invalid.size, logger.warnings.size)
    }

    // Icons

    @Test
    fun mapTulip_scaledIcon_keepsSizeAndScaleApart() {
        val icon = Rn2Icon(
            name = "Church",
            width = 350.0,
            height = 200.0,
            scaleX = 0.1,
            scaleY = 0.2,
            angle = 47.0,
            x = 10.0,
            y = -20.0,
        )

        val mapped = tulip(icon).single() as Icon

        assertEquals(IconType.CHURCH, mapped.type)
        assertEquals(350.0, mapped.width, 0.0)
        assertEquals(200.0, mapped.height, 0.0)
        assertEquals(0.1, mapped.scaleX, 0.0)
        assertEquals(0.2, mapped.scaleY, 0.0)
        assertEquals(47, mapped.angle)
        assertEquals(CanvasPoint(10.0, -20.0), mapped.center)
    }

    @Test
    fun mapTulip_scaledIconWithoutSize_isTheDefaultSize() {
        val mapped = tulip(Rn2Icon(scaleX = 0.5, scaleY = 0.5)).single() as Icon

        assertEquals(DEFAULT_ICON_SIZE, mapped.width, 0.0)
        assertEquals(DEFAULT_ICON_SIZE, mapped.height, 0.0)
    }

    @Test
    fun mapTulip_iconWithoutScale_isASquareOfSideW() {
        // Without both scales, the file gives the size as w, and a scale on one axis is ignored.
        val mapped = tulip(Rn2Icon(w = 70.0, width = 350.0, scaleX = 0.5)).single() as Icon

        assertEquals(70.0, mapped.width, 0.0)
        assertEquals(70.0, mapped.height, 0.0)
        assertEquals(1.0, mapped.scaleX, 0.0)
        assertEquals(1.0, mapped.scaleY, 0.0)
    }

    @Test
    fun mapTulip_iconAngle_isRoundedToWholeDegrees() {
        val mapped = tulip(Rn2Icon(angle = 12.6)).single() as Icon

        assertEquals(13, mapped.angle)
    }

    @Test
    fun mapTulip_iconWithoutAngleOrCenter_isUnrotatedAtTheCenter() {
        val mapped = tulip(Rn2Icon()).single() as Icon

        assertEquals(0, mapped.angle)
        assertEquals(CanvasPoint.CENTER, mapped.center)
    }

    @Test
    fun mapTulip_icon_keepsItsOriginalIdAndName() {
        val mapped = tulip(Rn2Icon(id = "abc", name = "Camel")).single() as Icon

        assertEquals("abc", mapped.originalId)
        assertEquals("Camel", mapped.originalName)
    }

    @Test
    fun mapTulip_unknownIcon_isKeptWithAWarning() {
        val mapped = tulip(Rn2Icon(id = "abc", name = "Camel")).single() as Icon

        assertEquals(IconType.UNKNOWN, mapped.type)
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun mapTulip_iconThatCannotBeDrawn_isSkippedWithAWarning() {
        val invalid = listOf(
            Rn2Icon(w = 0.0),
            Rn2Icon(width = 0.0, scaleX = 1.0, scaleY = 1.0),
            Rn2Icon(height = -5.0, scaleX = 1.0, scaleY = 1.0),
            Rn2Icon(scaleX = 0.0, scaleY = 1.0),
            Rn2Icon(scaleX = 1.0, scaleY = 0.0),
            Rn2Icon(angle = Double.NaN),
            Rn2Icon(y = Double.NEGATIVE_INFINITY),
        )

        invalid.forEach { icon -> assertTrue("$icon", tulip(icon).isEmpty()) }
        assertEquals(invalid.size, logger.warnings.size)
    }

    // Other elements

    @Test
    fun mapTulip_roadWithAPointThatIsNotFinite_isSkipped() {
        val road = Rn2Road(handles = listOf(Rn2Point(Double.NaN, 0.0)))

        assertTrue(tulip(road).isEmpty())
    }

    @Test
    fun mapTulip_trackWithARoadThatCannotBeDrawn_isSkipped() {
        val track = Rn2Track(roadOut = Rn2Road(end = Rn2Point(0.0, Double.NaN)))

        assertTrue(tulip(track).isEmpty())
    }

    @Test
    fun mapTulip_unknownElement_isSkippedWithAWarning() {
        val mapped = tulip(Rn2UnknownElement, Rn2Road())

        assertEquals(1, mapped.size)
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun mapTulip_elements_keepTheirOrder() {
        val mapped = tulip(Rn2Text(text = "a"), Rn2Road(), Rn2Icon(), Rn2Text(text = "b"))

        assertEquals(
            listOf(Text::class, Road::class, Icon::class, Text::class),
            mapped.map { it::class },
        )
    }

    // Notes

    @Test
    fun mapNotes_textsAndIcons_areMappedInOrder() {
        val mapped = mapper.mapNotes(listOf(Rn2Text(text = "a"), churchIcon, Rn2Text(text = "b")))

        assertEquals(listOf(Text::class, Icon::class, Text::class), mapped.map { it::class })
        assertEquals("a", (mapped[0] as Text).text)
        assertTrue(logger.warnings.isEmpty())
    }

    @Test
    fun mapNotes_road_isSkippedWithAWarning() {
        val mapped = mapper.mapNotes(listOf(Rn2Road(), churchIcon))

        assertEquals(listOf(Icon::class), mapped.map { it::class })
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun mapNotes_track_isSkippedWithAWarning() {
        val mapped = mapper.mapNotes(listOf(Rn2Track(), churchIcon))

        assertEquals(listOf(Icon::class), mapped.map { it::class })
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun mapNotes_unknownElement_isSkippedWithAWarning() {
        val mapped = mapper.mapNotes(listOf(Rn2UnknownElement, Rn2Text()))

        assertEquals(listOf(Text::class), mapped.map { it::class })
        assertEquals(1, logger.warnings.size)
    }

    @Test
    fun mapNotes_elementsThatCannotBeDrawn_areSkippedWithAWarning() {
        val invalid = listOf(Rn2Text(fontSize = 0), Rn2Icon(name = "Church", w = 0.0))

        val mapped = mapper.mapNotes(invalid)

        assertTrue(mapped.isEmpty())
        assertEquals(invalid.size, logger.warnings.size)
    }

    private fun tulip(
        vararg elements: Rn2Element,
        inherited: RoadType = RoadType.TRACK,
    ): List<TulipElement> =
        mapper.mapTulip(elements.toList(), inherited) { exit }.elements
}
