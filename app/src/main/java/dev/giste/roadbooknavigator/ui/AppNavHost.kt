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

package dev.giste.roadbooknavigator.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.giste.map.MapDownloader
import dev.giste.map.MapManager
import dev.giste.roadbooknavigator.settings.SettingsScreen
import dev.giste.roadbooknavigator.ui.dashboard.Dashboard
import kotlinx.serialization.Serializable

/** Route of the dashboard, the start screen. */
@Serializable
internal data object DashboardRoute

/** Route of the settings screen, opened from the dashboard. */
@Serializable
internal data object SettingsRoute

/** Route of the map manager, opened from the map on the dashboard. */
@Serializable
internal data object MapManagerRoute

/** Route of the map downloader, opened from the map manager. */
@Serializable
internal data object MapDownloaderRoute

/**
 * The app's screens and how to move between them: the dashboard, the settings and the map
 * manager on top of it, and the map downloader on top of the map manager. Back, with a screen's
 * back button or the system gesture, returns to the screen below.
 *
 * @param modifier applied to the navigation host.
 * @param navController moves between the screens. A parameter so tests can pass their own.
 */
@Composable
internal fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = DashboardRoute, modifier = modifier) {
        composable<DashboardRoute> {
            // The dashboard has no bars of its own, so the scaffold only keeps it clear of the
            // system bars.
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                Dashboard(
                    onOpenSettings = { navController.navigate(SettingsRoute) },
                    onManageMaps = { navController.navigate(MapManagerRoute) },
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                )
            }
        }
        composable<SettingsRoute> {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable<MapManagerRoute> {
            MapManager(
                onBack = { navController.popBackStack() },
                onDownloadMaps = { navController.navigate(MapDownloaderRoute) },
            )
        }
        composable<MapDownloaderRoute> {
            MapDownloader(onBack = { navController.popBackStack() })
        }
    }
}
