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

package dev.giste.roadbook.ui.icon

import androidx.compose.ui.graphics.Color
import dev.giste.roadbook.domain.drawing.IconType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IconVectorsTest {

    private val lightColors = IconColors(
        onSurface = Color.Black,
        surface = Color.White,
        danger = Color.Red,
        muted = Color.Gray,
    )
    private val darkColors = IconColors(
        onSurface = Color.White,
        surface = Color.Black,
        danger = Color.Magenta,
        muted = Color.DarkGray,
    )

    /** The symbol each type must draw, by the name of its vector. */
    private val expectedVectorNames: Map<IconType, String> = mapOf(
        IconType.DANGER_1 to "Cross.Danger1",
        IconType.DANGER_2 to "Cross.Danger2",
        IconType.DANGER_3 to "Cross.Danger3",
        IconType.FUEL_ZONE to "Cross.FuelZone",
        IconType.RESET_PARTIAL to "Cross.ResetPartial",
        IconType.ABOVE_BRIDGE to "Landmark.AboveBridge",
        IconType.CHURCH to "Landmark.Church",
        IconType.FORT_CASTLE to "Landmark.FortCastle",
        IconType.HOUSE to "Landmark.House",
        IconType.TRAFFIC_LIGHT to "Landmark.TrafficLight",
        IconType.TREE to "Landmark.Tree",
        IconType.TUNNEL to "Landmark.Tunnel",
        IconType.UNDER_BRIDGE to "Landmark.UnderBridge",
        IconType.ALERT to "Sign.Alert",
        IconType.DO_NOT_ENTER to "Sign.DoNotEnter",
        IconType.ROUNDABOUT to "Sign.Roundabout",
        IconType.STOP to "Sign.Stop",
        IconType.SPEED_LIMIT_10 to "Speed.Limit10",
        IconType.SPEED_LIMIT_20 to "Speed.Limit20",
        IconType.SPEED_LIMIT_30 to "Speed.Limit30",
        IconType.SPEED_LIMIT_40 to "Speed.Limit40",
        IconType.SPEED_LIMIT_50 to "Speed.Limit50",
        IconType.SPEED_LIMIT_60 to "Speed.Limit60",
        IconType.SPEED_LIMIT_70 to "Speed.Limit70",
        IconType.SPEED_LIMIT_80 to "Speed.Limit80",
        IconType.SPEED_LIMIT_90 to "Speed.Limit90",
        IconType.SPEED_LIMIT_100 to "Speed.Limit100",
        IconType.SPEED_LIMIT_110 to "Speed.Limit110",
        IconType.SPEED_LIMIT_120 to "Speed.Limit120",
        IconType.SPEED_LIMIT_130 to "Speed.Limit130",
        IconType.SPEED_LIMIT_140 to "Speed.Limit140",
        IconType.SPEED_LIMIT_150 to "Speed.Limit150",
        IconType.RIVER_WATER to "Terrain.RiverWater",
    )

    @Test
    fun toImageVector_everyKnownType_drawsItsOwnSymbol() {
        val actual = IconType.entries
            .filter { it != IconType.UNKNOWN }
            .associateWith { it.toImageVector(lightColors)?.name }

        assertEquals(expectedVectorNames, actual)
    }

    @Test
    fun toImageVector_unknownType_hasNoSymbol() {
        assertNull(IconType.UNKNOWN.toImageVector(lightColors))
    }

    @Test
    fun toImageVector_everyKnownType_followsTheThemeColors() {
        IconType.entries.filter { it != IconType.UNKNOWN }.forEach { type ->
            assertNotEquals(
                "$type ignores the theme colors",
                type.toImageVector(lightColors),
                type.toImageVector(darkColors),
            )
        }
    }

    @Test
    fun toImageVector_sameColors_buildsEqualVectors() {
        IconType.entries.filter { it != IconType.UNKNOWN }.forEach { type ->
            assertEquals(type.toImageVector(lightColors), type.toImageVector(lightColors))
        }
    }
}
