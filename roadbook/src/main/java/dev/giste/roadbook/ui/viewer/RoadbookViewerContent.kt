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

package dev.giste.roadbook.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.geo.Distance
import dev.giste.roadbook.ImportRoadbookFailure
import dev.giste.roadbook.R
import dev.giste.roadbook.ui.theme.RoadbookTheme
import dev.giste.roadbook.ui.waypoint.ROW_ASPECT_RATIO
import dev.giste.roadbook.ui.waypoint.WaypointItem
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

/** Test tags of the viewer parts that tests cannot find by their own text. */
internal object RoadbookViewerTestTags {

    /** The floating import button: it shows only an icon. */
    const val IMPORT: String = "roadbook_viewer_import"
}

/**
 * The roadbook viewer, without state of its own: it shows [state] and reports what the user does.
 *
 * It shows a progress indicator while loading or importing, a message when there is no roadbook
 * to show, or the roadbook as a list of waypoints kept at the saved position. A floating import
 * button sits at the bottom end in every one of them, the same with or without a roadbook, and is
 * hidden while importing. A failed import is reported in a snackbar.
 *
 * @param state what to show.
 * @param showImportButton whether to show the floating import button. The app hides it while
 * riding, with or without a roadbook.
 * @param onImportClick called when the user asks to import a roadbook.
 * @param onImportFailureShown called once a failed import has been reported.
 * @param onSetPartial called with the partial of a waypoint whose distances were long-pressed.
 * @param onScrollStopped called with the index of the first visible waypoint, from 0, each time
 * the list stops scrolling.
 * @param modifier the modifier applied to the viewer.
 */
@Composable
internal fun RoadbookViewerContent(
    state: RoadbookUiState,
    showImportButton: Boolean,
    onImportClick: () -> Unit,
    onImportFailureShown: () -> Unit,
    onSetPartial: (Distance) -> Unit,
    onScrollStopped: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    RoadbookTheme(style = state.style) {
        val dimensions = GisteTheme.dimensions
        val snackbarHostState = remember { SnackbarHostState() }
        val failure = (state.import as? ImportState.Failed)?.reason
        val failureMessage = failure?.let { importFailureMessage(it) }
        val importing = state.import == ImportState.Importing

        LaunchedEffect(failureMessage) {
            if (failureMessage != null) {
                snackbarHostState.showSnackbar(failureMessage)
                onImportFailureShown()
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(RoadbookTheme.colors.surface),
        ) {
            when (val roadbook = state.roadbook) {
                RoadbookContentState.Loading -> Unit
                RoadbookContentState.Empty -> NoRoadbook(
                    message = stringResource(R.string.roadbook_empty),
                    modifier = Modifier.align(Alignment.Center),
                )
                RoadbookContentState.Unreadable -> NoRoadbook(
                    message = stringResource(R.string.roadbook_unreadable),
                    modifier = Modifier.align(Alignment.Center),
                )
                is RoadbookContentState.Content -> {
                    // A new roadbook gets a new list, which starts at its own position.
                    key(roadbook.roadbook) {
                        RoadbookList(
                            content = roadbook,
                            onSetPartial = onSetPartial,
                            onScrollStopped = onScrollStopped,
                        )
                    }
                }
            }
            // Not while loading either: until the saved roadbook is read, an import could be
            // overwritten by it.
            if (showImportButton && !importing && state.roadbook != RoadbookContentState.Loading) {
                ImportButton(
                    onClick = onImportClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(dimensions.spacingMedium),
                )
            }
            if (state.roadbook == RoadbookContentState.Loading || importing) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/**
 * The waypoints of the roadbook, one under the other, starting at the saved position.
 *
 * It follows the position: when it changes from elsewhere (e.g. a remote control key), the list
 * scrolls to it. When the user scrolls, it reports the first visible waypoint each time the list
 * stops, so the position is saved; reporting the waypoint already at the top changes nothing, so
 * the list does not scroll back.
 *
 * When the user lets go, the list snaps so a waypoint starts exactly at the top: the nearest one
 * after a slow drag, or one in the direction of a fling. A waypoint is never left half hidden, and
 * what the user sees is what is saved, since the position has no offset.
 *
 * Below the last waypoint there is empty space, the height of the list minus one row, so every
 * waypoint can be scrolled to the top, the last ones included. Otherwise the list would stop
 * short of a position near the end, report an earlier waypoint, and lose the position.
 */
@Composable
private fun RoadbookList(
    content: RoadbookContentState.Content,
    onSetPartial: (Distance) -> Unit,
    onScrollStopped: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = content.position.value - 1,
    )
    val currentOnScrollStopped by rememberUpdatedState(onScrollStopped)

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .filter { scrolling -> !scrolling }
            .collect { currentOnScrollStopped(listState.firstVisibleItemIndex) }
    }
    LaunchedEffect(listState, content.position) {
        val target = content.position.value - 1
        if (listState.firstVisibleItemIndex != target) {
            listState.animateScrollToItem(target)
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val rowHeight = maxWidth / ROW_ASPECT_RATIO
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(listState, SnapPosition.Start),
            contentPadding = PaddingValues(bottom = (maxHeight - rowHeight).coerceAtLeast(0.dp)),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items = content.roadbook.waypoints, key = { it.number.value }) { waypoint ->
                WaypointItem(
                    waypoint = waypoint,
                    shortDistanceThreshold = content.shortDistanceThreshold,
                    onSetPartial = onSetPartial,
                )
            }
        }
    }
}

/** A message saying why there is no roadbook to show. The floating button imports one. */
@Composable
private fun NoRoadbook(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        style = MaterialTheme.typography.titleLarge,
        color = RoadbookTheme.colors.onSurface,
        textAlign = TextAlign.Center,
        modifier = modifier.padding(GisteTheme.dimensions.spacingLarge),
    )
}

/**
 * The floating button that opens the file picker: a standard Material 3 floating action button,
 * with its default shape and colors, showing only the import icon.
 *
 * It never takes the focus: it is meant to be touched, and otherwise, when an app drives the
 * screen with keys (e.g. a remote control), an arrow or Tab key it does not handle would move the
 * focus onto it, and an Enter or center key would open the file picker.
 */
@Composable
private fun ImportButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val minTouchTarget = GisteTheme.dimensions.minTouchTarget
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minWidth = minTouchTarget, minHeight = minTouchTarget)
            .focusProperties { canFocus = false }
            .testTag(RoadbookViewerTestTags.IMPORT),
    ) {
        Icon(painter = painterResource(R.drawable.roadbook_ic_import), contentDescription = null)
    }
}

/** What to tell the user when an import failed for [reason]. */
@Composable
private fun importFailureMessage(reason: ImportRoadbookFailure): String = stringResource(
    when (reason) {
        ImportRoadbookFailure.UNREADABLE -> R.string.roadbook_import_failed_unreadable
        ImportRoadbookFailure.MALFORMED -> R.string.roadbook_import_failed_malformed
        ImportRoadbookFailure.NO_WAYPOINTS -> R.string.roadbook_import_failed_no_waypoints
        ImportRoadbookFailure.NOT_SAVED -> R.string.roadbook_import_failed_not_saved
    },
)
