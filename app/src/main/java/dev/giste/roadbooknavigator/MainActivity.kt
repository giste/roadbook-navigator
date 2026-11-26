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

package dev.giste.roadbooknavigator

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.giste.core.permission.PermissionGate
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.roadbooknavigator.ui.permission.PreciseLocationPermission
import dev.giste.roadbooknavigator.settings.AppSettings
import dev.giste.roadbooknavigator.settings.AppSettingsSource
import dev.giste.roadbooknavigator.ui.AppNavHost
import dev.giste.roadbooknavigator.ui.KeyEventInterceptors
import dev.giste.roadbooknavigator.ui.LocalKeyEventInterceptors
import dev.giste.roadbooknavigator.ui.RiderTypography
import dev.giste.roadbooknavigator.ui.applyFullScreen
import dev.giste.roadbooknavigator.ui.isDarkTheme
import dev.giste.roadbooknavigator.ui.toActivityOrientation
import javax.inject.Inject

/**
 * The only activity. A Hilt entry point, so the composables it hosts can get Hilt view models.
 *
 * It shows the app in the theme mode, the screen orientation and the full screen mode of the
 * [AppSettings], following every change, with the larger [RiderTypography], and every screen
 * behind the location permission gate.
 *
 * Its key events go first to the [KeyEventInterceptors] the screens add (the dashboard's remote
 * control), and only then to the views and the system.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * The app settings, for the theme mode, the orientation and full screen. Injected by Hilt in
     * [onCreate].
     */
    @Inject
    internal lateinit var appSettingsSource: AppSettingsSource

    /** Where the screens intercept key events, provided to them as [LocalKeyEventInterceptors]. */
    private val keyEventInterceptors = KeyEventInterceptors()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Until the stored settings are read, the defaults apply (the system's theme and
            // rotation, full screen).
            val appSettings by appSettingsSource.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            // A new orientation recreates the activity; the screens keep their state, as on any
            // rotation.
            LaunchedEffect(appSettings.orientation) {
                requestedOrientation = appSettings.orientation.toActivityOrientation()
            }
            LaunchedEffect(appSettings.fullScreen) {
                window.applyFullScreen(appSettings.fullScreen)
            }
            GisteTheme(
                darkTheme = appSettings.themeMode.isDarkTheme(),
                typography = RiderTypography,
            ) {
                CompositionLocalProvider(LocalKeyEventInterceptors provides keyEventInterceptors) {
                    // Every screen needs precise location, so the whole app waits behind the gate.
                    PermissionGate(permissions = listOf(PreciseLocationPermission)) {
                        AppNavHost(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }

    // Before the views and the window: otherwise the window would change the volume or skip music
    // with keys that a remote control uses for the dashboard.
    // dispatchKeyEvent is public API of android.app.Activity. Lint flags it only because
    // androidx.core's ComponentActivity marks its own override as restricted.
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        keyEventInterceptors.intercept(event) || super.dispatchKeyEvent(event)
}
