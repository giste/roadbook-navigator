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

import kotlinx.coroutines.flow.Flow

/**
 * Input port: the location fixes the odometer measures distance from.
 *
 * `:odometer` does not read the location itself. The app implements this interface (usually by
 * adapting its location provider) and binds it with Hilt in `SingletonComponent`.
 */
public interface OdometerLocationSource {

    /**
     * The device location fixes, in the order they were taken.
     *
     * [OdometerController] collects it only while its [OdometerController.state] is collected,
     * so it can be a cold flow that starts the provider on collection. It should not throw. If it
     * does, the odometer logs the error and stops measuring until its state is collected again.
     */
    public val locations: Flow<OdometerLocation>
}
