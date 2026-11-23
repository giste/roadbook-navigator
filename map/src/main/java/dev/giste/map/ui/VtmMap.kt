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

package dev.giste.map.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.giste.map.MapSettings
import dev.giste.map.domain.MapCenter
import dev.giste.map.domain.MapFile
import org.oscim.android.MapView
import org.oscim.backend.CanvasAdapter
import org.oscim.core.BoundingBox
import org.oscim.layers.tile.buildings.BuildingLayer
import org.oscim.layers.tile.vector.labeling.LabelLayer
import org.oscim.map.Map
import org.oscim.renderer.GLViewport
import org.oscim.scalebar.DefaultMapScaleBar
import org.oscim.scalebar.MapScaleBarLayer
import org.oscim.theme.internal.VtmThemes
import org.oscim.tiling.source.mapfile.MapFileTileSource
import org.oscim.tiling.source.mapfile.MultiMapFileTileSource

/**
 * Where the location is drawn, as VTM's vertical pivot in `[-1, 1]` from the view's center:
 * below the middle, so more of the road ahead is shown.
 */
private const val MAP_VIEW_CENTER_Y: Float = 0.6f

/** Gap between the scale bar and the view's left edge, in density-independent pixels. */
private const val SCALE_BAR_OFFSET: Float = 5f

/**
 * The map of [state], drawn by VTM's OpenGL `MapView`.
 *
 * It starts with the settings' zoom level and tilt, centered on the location, or on the middle of
 * the maps before the first fix. Every fix then moves the map to the location and turns it so the
 * bearing points up, in a short animation. A different set of files creates a new view.
 *
 * A read-only map ignores touches: they pass through to what is under it. VTM's `MapView` does
 * nothing with a touch when it is not clickable, so no gesture can pan, zoom, rotate or tilt it,
 * and it never takes the focus either. Its zoom level and tilt are then always the settings'. An
 * interactive map handles VTM's gestures, and its zoom level and tilt are set again only when the
 * settings change, so the user's pinch and tilt are kept meanwhile.
 *
 * It needs OpenGL and VTM's native library, so it is not used in tests or previews:
 * [MapViewerContent] takes it as a slot.
 *
 * @param state the map to draw.
 * @param interactive whether the user may move, zoom, rotate and tilt the map with gestures.
 * @param modifier the modifier applied to the view.
 */
@Composable
internal fun VtmMap(state: MapUiState.Ready, interactive: Boolean, modifier: Modifier = Modifier) {
    // The tile source is set once per view, so new files need a new view.
    key(state.files) {
        val mapView = rememberMapView()
        // The settings applied last, so the update below sets the zoom and tilt only on a change.
        val applied = remember { AppliedSettings(state.settings) }
        AndroidView(
            factory = { mapView.apply { map().setUp(state.files, state.settings, state.center) } },
            modifier = modifier,
            update = { view ->
                view.isClickable = interactive
                view.isFocusable = interactive
                view.isFocusableInTouchMode = interactive
                val map = view.map()
                // A read-only map goes back to the settings at once, e.g. after it stops being
                // interactive with the user's zoom still on it.
                val applySettings = !interactive || state.settings != applied.settings
                if (applySettings || state.center != null) {
                    // getMapPosition() returns a copy, so changing it changes nothing on screen
                    // until it is animated to.
                    val target = map.mapPosition
                    if (applySettings) {
                        target.applySettings(state.settings)
                        applied.settings = state.settings
                    }
                    state.center?.let(target::follow)
                    map.animator().animateTo(target)
                }
            },
        )
    }
}

/** The settings a [VtmMap] last applied. Not state: changing it must not recompose. */
private class AppliedSettings(var settings: MapSettings)

/**
 * Creates a VTM `MapView` for this composition, and ties it to the lifecycle: it pauses and
 * resumes with the screen, and is destroyed when it leaves the composition.
 *
 * Leaving the composition destroys it even while the lifecycle goes on, e.g. when another screen
 * replaces the one that shows it: otherwise each return to the screen would leak a view and its
 * rendering threads.
 */
@Composable
private fun rememberMapView(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        // Adding the observer replays the events up to the current state, so a view created
        // while the screen is resumed is resumed at once.
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onDestroy()
        }
    }
    return mapView
}

/**
 * Sets up a new map: the files as one vector source with buildings and labels, a scale bar, the
 * default theme, and the starting position.
 *
 * The scale bar sits at the bottom left, because the app may float controls over the top corners
 * and the bottom right holds the attribution.
 *
 * @param files the map files to draw.
 * @param settings the starting zoom level and tilt.
 * @param center the starting center, or `null` to start at the middle of the maps.
 */
private fun Map.setUp(files: List<MapFile>, settings: MapSettings, center: MapCenter?) {
    val fileSources = files.map { file -> MapFileTileSource().apply { setMapFile(file.path) } }
    val source = MultiMapFileTileSource().apply { fileSources.forEach(::add) }
    // Opens the files. If one was removed or damaged since it was checked, VTM logs it and draws
    // nothing; the next time the map is shown, the check leaves that file out.
    val tileLayer = setBaseMap(source)
    layers().add(BuildingLayer(this, tileLayer))
    layers().add(LabelLayer(this, tileLayer))
    val scaleBar = MapScaleBarLayer(this, DefaultMapScaleBar(this))
    scaleBar.renderer.setPosition(GLViewport.Position.BOTTOM_LEFT)
    scaleBar.renderer.setOffset(SCALE_BAR_OFFSET * CanvasAdapter.getScale(), 0f)
    layers().add(scaleBar)
    setTheme(VtmThemes.DEFAULT)
    viewport().setMapViewCenterY(MAP_VIEW_CENTER_Y)

    val start = mapPosition.applySettings(settings)
    if (center != null) {
        start.follow(center)
    } else {
        // Only the files that opened have map info, so this never fails.
        fileSources.mapNotNull { it.mapInfo?.boundingBox }
            .reduceOrNull(BoundingBox::extendBoundingBox)
            ?.let { start.setPosition(it.centerPoint) }
    }
    mapPosition = start
}
