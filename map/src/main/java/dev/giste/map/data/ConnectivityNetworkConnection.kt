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

package dev.giste.map.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.giste.map.domain.NetworkConnection
import javax.inject.Inject

/**
 * [NetworkConnection] from Android's connectivity manager: the active network is unmetered if it
 * says so (`NET_CAPABILITY_NOT_METERED`), as WorkManager's `NetworkType.UNMETERED` also checks.
 *
 * @param context the application context, to get the connectivity manager.
 */
internal class ConnectivityNetworkConnection @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : NetworkConnection {

    override fun isUnmetered(): Boolean {
        val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
}
