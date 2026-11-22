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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.giste.core.permission.PermissionGate
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.roadbooknavigator.permission.PreciseLocationPermission
import dev.giste.roadbooknavigator.settings.AppSettings
import dev.giste.roadbooknavigator.settings.AppSettingsSource
import dev.giste.roadbooknavigator.ui.AppNavHost
import dev.giste.roadbooknavigator.ui.RiderTypography
import dev.giste.roadbooknavigator.ui.isDarkTheme
import javax.inject.Inject

/**
 * The only activity. A Hilt entry point, so the composables it hosts can get Hilt view models.
 *
 * It shows the app in the theme mode of the [AppSettings], following every change, with the
 * larger [RiderTypography], and every screen behind the location permission gate.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** The app settings, for the theme mode. Injected by Hilt in [onCreate]. */
    @Inject
    internal lateinit var appSettingsSource: AppSettingsSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Until the stored settings are read, the defaults apply (the system's theme).
            val appSettings by appSettingsSource.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings())
            GisteTheme(
                darkTheme = appSettings.themeMode.isDarkTheme(),
                typography = RiderTypography,
            ) {
                // Every screen needs precise location, so the whole app waits behind the gate.
                PermissionGate(permissions = listOf(PreciseLocationPermission)) {
                    AppNavHost(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
