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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import dev.giste.core.permission.PermissionGate
import dev.giste.core.ui.theme.GisteTheme
import dev.giste.roadbook.RoadbookViewer
import dev.giste.roadbooknavigator.permission.PreciseLocationPermission

/**
 * The only activity. A Hilt entry point, so the composables it hosts can get Hilt view models.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GisteTheme {
                // Every screen needs precise location, so the whole app waits behind the gate.
                PermissionGate(permissions = listOf(PreciseLocationPermission)) {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        RoadbookViewer(
                            // TODO: show the import button only when stopped. That rule needs the
                            //  speed from :odometer, which is wired into :app in the next step.
                            showImportButton = true,
                            modifier = Modifier.fillMaxSize().padding(innerPadding),
                        )
                    }
                }
            }
        }
    }
}
