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

package dev.giste.roadbook.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import dev.giste.roadbook.RoadbookStyle
import dev.giste.roadbook.ui.icon.IconColors

/**
 * The colors the roadbook is drawn with. They come from the app theme or from the FIA palette,
 * depending on the [RoadbookStyle] (see [roadbookColors]).
 *
 * @property surface the paper: the background of the roadbook.
 * @property onSurface the ink: roads, texts and the lines of symbols.
 * @property track the route the rider follows through a tulip, so it stands out from the other
 * roads.
 * @property trackSecondary the center line of a dual carriageway on the track.
 * @property roadSecondary the center line of a dual carriageway on another road.
 * @property danger danger marks, the border of very dangerous waypoints, and symbols the app
 * cannot draw.
 * @property muted secondary shapes of symbols, such as the walls of buildings.
 * @property shortDistanceSurface the background of the distances of a waypoint that comes soon
 * after the previous one.
 * @property onShortDistanceSurface the distances drawn over [shortDistanceSurface].
 * @property waypointNumberSurface the background of the waypoint number.
 * @property onWaypointNumberSurface the waypoint number, over [waypointNumberSurface].
 * @property action the container of the floating buttons over the roadbook, such as the import
 * button: the primary color of the style, so the buttons stand out from the paper in every style
 * and mode.
 * @property onAction the icons drawn over [action].
 */
@Immutable
internal data class RoadbookColors(
    val surface: Color,
    val onSurface: Color,
    val track: Color,
    val trackSecondary: Color,
    val roadSecondary: Color,
    val danger: Color,
    val muted: Color,
    val shortDistanceSurface: Color,
    val onShortDistanceSurface: Color,
    val waypointNumberSurface: Color,
    val onWaypointNumberSurface: Color,
    val action: Color,
    val onAction: Color,
) {

    /**
     * The colors to draw symbols with on [background].
     *
     * @param background what is behind the symbols. Defaults to [surface]; pass another color
     * when the symbols are drawn on a highlighted background.
     * @return the symbol colors.
     */
    fun iconColors(background: Color = surface): IconColors = IconColors(
        onSurface = onSurface,
        surface = background,
        danger = danger,
        muted = muted,
    )
}

/**
 * The colors of FIA paper roadbooks: black ink on white paper, a blue track, red danger marks, a
 * pale green behind waypoints that come soon, and white numbers on black. Floating buttons are
 * dark gray with white icons: not a FIA color, but it stands out from the paper without taking the
 * blue of the track, and was preferred on a device to the blue and to black. They do not change
 * with the light and dark modes.
 */
internal val FIA_ROADBOOK_COLORS: RoadbookColors = RoadbookColors(
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    track = Color(0xFF0000FF),
    trackSecondary = Color(0xFF535F70),
    roadSecondary = Color(0x80000000),
    danger = Color(0xFFFF0000),
    muted = Color(0xFF74777F),
    shortDistanceSurface = Color(0xFFD2F8D2),
    onShortDistanceSurface = Color(0xFF000000),
    waypointNumberSurface = Color(0xFF000000),
    onWaypointNumberSurface = Color(0xFFFFFFFF),
    action = Color(0xFF424242),
    onAction = Color(0xFFFFFFFF),
)

/** Alpha of the center line of a dual carriageway that is not the track, over the ink color. */
private const val ROAD_SECONDARY_ALPHA: Float = 0.5f

/**
 * The roadbook colors of [style].
 *
 * @param style the roadbook style the user chose.
 * @param colorScheme the app theme, used by [RoadbookStyle.APP_THEME].
 * @return the colors to draw the roadbook with.
 */
internal fun roadbookColors(style: RoadbookStyle, colorScheme: ColorScheme): RoadbookColors =
    when (style) {
        RoadbookStyle.APP_THEME -> RoadbookColors(
            surface = colorScheme.surface,
            onSurface = colorScheme.onSurface,
            track = colorScheme.primary,
            // Drawn over the track, so it takes the track's content color: the secondary color
            // has the same lightness as primary and would only stand out by its hue.
            trackSecondary = colorScheme.onPrimary,
            roadSecondary = colorScheme.onSurface.copy(alpha = ROAD_SECONDARY_ALPHA),
            danger = colorScheme.error,
            muted = colorScheme.outline,
            shortDistanceSurface = colorScheme.tertiaryContainer,
            onShortDistanceSurface = colorScheme.onTertiaryContainer,
            waypointNumberSurface = colorScheme.inverseSurface,
            onWaypointNumberSurface = colorScheme.inverseOnSurface,
            // As the app's other floating buttons over content (e.g. the map's): the default
            // primaryContainer has little contrast on a light surface.
            action = colorScheme.primary,
            onAction = colorScheme.onPrimary,
        )
        RoadbookStyle.FIA -> FIA_ROADBOOK_COLORS
    }

/** The roadbook colors provided by [RoadbookTheme], or `null` outside it. */
private val LocalRoadbookColors = staticCompositionLocalOf<RoadbookColors?> { null }

/**
 * Provides the roadbook colors of [style] to [content]. Wrap the roadbook, and every preview of
 * its parts, in it.
 *
 * @param style the roadbook style the user chose.
 * @param content the roadbook content.
 */
@Composable
internal fun RoadbookTheme(style: RoadbookStyle, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalRoadbookColors provides roadbookColors(style, MaterialTheme.colorScheme),
        content = content,
    )
}

/** Accessors for the values that [RoadbookTheme] provides. */
internal object RoadbookTheme {

    /**
     * The roadbook colors of the enclosing [RoadbookTheme], or those of
     * [RoadbookStyle.APP_THEME] outside one.
     */
    val colors: RoadbookColors
        @Composable
        @ReadOnlyComposable
        get() = LocalRoadbookColors.current
            ?: roadbookColors(RoadbookStyle.APP_THEME, MaterialTheme.colorScheme)
}
