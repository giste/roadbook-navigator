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

package dev.giste.odometer

import dev.giste.geo.LocationFix
import kotlinx.coroutines.flow.Flow

/**
 * [OdometerLocationSource] for tests that exposes whatever flow the test gives it, e.g. a
 * `MutableSharedFlow` to emit fixes on demand, or a flow that throws.
 *
 * @property locations the flow returned to the controller.
 */
internal class FakeOdometerLocationSource(
    override val locations: Flow<LocationFix>,
) : OdometerLocationSource

/**
 * [OdometerSettingsSource] for tests that exposes whatever flow the test gives it.
 *
 * @property settings the flow returned to the controller.
 */
internal class FakeOdometerSettingsSource(
    override val settings: Flow<OdometerSettings>,
) : OdometerSettingsSource
