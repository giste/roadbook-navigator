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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import dev.giste.roadbook.domain.drawing.IconType
import dev.giste.roadbook.ui.icon.cross.danger1Icon
import dev.giste.roadbook.ui.icon.cross.danger2Icon
import dev.giste.roadbook.ui.icon.cross.danger3Icon
import dev.giste.roadbook.ui.icon.cross.fuelZoneIcon
import dev.giste.roadbook.ui.icon.cross.resetPartialIcon
import dev.giste.roadbook.ui.icon.landmark.aboveBridgeIcon
import dev.giste.roadbook.ui.icon.landmark.churchIcon
import dev.giste.roadbook.ui.icon.landmark.fortCastleIcon
import dev.giste.roadbook.ui.icon.landmark.houseIcon
import dev.giste.roadbook.ui.icon.landmark.trafficLightIcon
import dev.giste.roadbook.ui.icon.landmark.treeIcon
import dev.giste.roadbook.ui.icon.landmark.tunnelIcon
import dev.giste.roadbook.ui.icon.landmark.underBridgeIcon
import dev.giste.roadbook.ui.icon.sign.alertIcon
import dev.giste.roadbook.ui.icon.sign.doNotEnterIcon
import dev.giste.roadbook.ui.icon.sign.roundaboutIcon
import dev.giste.roadbook.ui.icon.sign.stopIcon
import dev.giste.roadbook.ui.icon.speed.speedLimit100Icon
import dev.giste.roadbook.ui.icon.speed.speedLimit10Icon
import dev.giste.roadbook.ui.icon.speed.speedLimit110Icon
import dev.giste.roadbook.ui.icon.speed.speedLimit120Icon
import dev.giste.roadbook.ui.icon.speed.speedLimit130Icon
import dev.giste.roadbook.ui.icon.speed.speedLimit140Icon
import dev.giste.roadbook.ui.icon.speed.speedLimit150Icon
import dev.giste.roadbook.ui.icon.speed.speedLimit20Icon
import dev.giste.roadbook.ui.icon.speed.speedLimit30Icon
import dev.giste.roadbook.ui.icon.speed.speedLimit40Icon
import dev.giste.roadbook.ui.icon.speed.speedLimit50Icon
import dev.giste.roadbook.ui.icon.speed.speedLimit60Icon
import dev.giste.roadbook.ui.icon.speed.speedLimit70Icon
import dev.giste.roadbook.ui.icon.speed.speedLimit80Icon
import dev.giste.roadbook.ui.icon.speed.speedLimit90Icon
import dev.giste.roadbook.ui.icon.terrain.riverWaterIcon

/**
 * Builds the vector of the symbol this type stands for, drawn with [colors].
 *
 * Building a vector is not free, so composables use [rememberIconVector] instead.
 *
 * @param colors the colors to draw with.
 * @return the vector, or `null` for [IconType.UNKNOWN], which has no symbol: it is shown by the
 * name or id it has in the roadbook file.
 */
internal fun IconType.toImageVector(colors: IconColors): ImageVector? = when (this) {
    IconType.DANGER_1 -> danger1Icon(colors)
    IconType.DANGER_2 -> danger2Icon(colors)
    IconType.DANGER_3 -> danger3Icon(colors)
    IconType.FUEL_ZONE -> fuelZoneIcon(colors)
    IconType.RESET_PARTIAL -> resetPartialIcon(colors)
    IconType.ABOVE_BRIDGE -> aboveBridgeIcon(colors)
    IconType.CHURCH -> churchIcon(colors)
    IconType.FORT_CASTLE -> fortCastleIcon(colors)
    IconType.HOUSE -> houseIcon(colors)
    IconType.TRAFFIC_LIGHT -> trafficLightIcon(colors)
    IconType.TREE -> treeIcon(colors)
    IconType.TUNNEL -> tunnelIcon(colors)
    IconType.UNDER_BRIDGE -> underBridgeIcon(colors)
    IconType.ALERT -> alertIcon(colors)
    IconType.DO_NOT_ENTER -> doNotEnterIcon(colors)
    IconType.ROUNDABOUT -> roundaboutIcon(colors)
    IconType.STOP -> stopIcon(colors)
    IconType.SPEED_LIMIT_10 -> speedLimit10Icon(colors)
    IconType.SPEED_LIMIT_20 -> speedLimit20Icon(colors)
    IconType.SPEED_LIMIT_30 -> speedLimit30Icon(colors)
    IconType.SPEED_LIMIT_40 -> speedLimit40Icon(colors)
    IconType.SPEED_LIMIT_50 -> speedLimit50Icon(colors)
    IconType.SPEED_LIMIT_60 -> speedLimit60Icon(colors)
    IconType.SPEED_LIMIT_70 -> speedLimit70Icon(colors)
    IconType.SPEED_LIMIT_80 -> speedLimit80Icon(colors)
    IconType.SPEED_LIMIT_90 -> speedLimit90Icon(colors)
    IconType.SPEED_LIMIT_100 -> speedLimit100Icon(colors)
    IconType.SPEED_LIMIT_110 -> speedLimit110Icon(colors)
    IconType.SPEED_LIMIT_120 -> speedLimit120Icon(colors)
    IconType.SPEED_LIMIT_130 -> speedLimit130Icon(colors)
    IconType.SPEED_LIMIT_140 -> speedLimit140Icon(colors)
    IconType.SPEED_LIMIT_150 -> speedLimit150Icon(colors)
    IconType.RIVER_WATER -> riverWaterIcon(colors)
    IconType.UNKNOWN -> null
}

/**
 * The vector of [type] drawn with [colors], built once and kept for as long as both stay the
 * same, so a roadbook with many symbols does not rebuild them on every recomposition.
 *
 * @param type the symbol to draw.
 * @param colors the colors to draw with, usually from `RoadbookColors.iconColors`.
 * @return the vector, or `null` for [IconType.UNKNOWN].
 */
@Composable
internal fun rememberIconVector(type: IconType, colors: IconColors): ImageVector? =
    remember(type, colors) { type.toImageVector(colors) }
