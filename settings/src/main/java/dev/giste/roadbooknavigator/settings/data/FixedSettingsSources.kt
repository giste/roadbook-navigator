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

package dev.giste.roadbooknavigator.settings.data

import dev.giste.location.LocationSettings
import dev.giste.location.LocationSettingsSource
import dev.giste.odometer.OdometerSettings
import dev.giste.odometer.OdometerSettingsSource
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/*
 * The app does not let the rider change the :location and :odometer settings: their defaults are
 * meant for motor vehicles, which is what the app is for. So nothing is stored, and these sources
 * always give the defaults. If the settings screen ever offers them, they become stored settings,
 * like the others.
 */

/** The `:location` settings: always the module's defaults, suited to motor vehicles. */
internal class FixedLocationSettingsSource @Inject constructor() : LocationSettingsSource {

    /**
     * [LocationSettings]' defaults, emitted at once and never changed. Like any settings source,
     * it never completes and never throws.
     */
    override val settings: StateFlow<LocationSettings> =
        MutableStateFlow(LocationSettings()).asStateFlow()
}

/** The `:odometer` settings: always the module's defaults, suited to motor vehicles. */
internal class FixedOdometerSettingsSource @Inject constructor() : OdometerSettingsSource {

    /**
     * [OdometerSettings]' defaults, emitted at once and never changed. Like any settings source,
     * it never completes and never throws.
     */
    override val settings: StateFlow<OdometerSettings> =
        MutableStateFlow(OdometerSettings()).asStateFlow()
}
