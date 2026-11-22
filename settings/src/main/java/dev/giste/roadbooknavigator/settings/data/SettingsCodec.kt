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

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import dev.giste.core.logging.Logger
import dev.giste.geo.Distance
import dev.giste.geo.Speed
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Turns a module's settings of type [T] into DataStore preferences and back.
 *
 * Reading never fails: a missing value is the default, and an invalid one (e.g. a negative
 * distance) is the default too, with a warning. Writing stores only the values that differ from
 * the defaults, so a setting the user never changed follows the module's default, even if a later
 * version changes it.
 *
 * @param T the module's settings model.
 */
internal interface SettingsCodec<T> {

    /** The settings when nothing is stored: the module's defaults. */
    val default: T

    /**
     * Reads the settings from [preferences].
     *
     * @param preferences what is stored.
     * @param logger warns about invalid values.
     * @return the stored settings, with the default for every missing or invalid value.
     */
    fun read(preferences: Preferences, logger: Logger): T

    /**
     * Writes [settings] into [preferences], removing the values equal to the defaults.
     *
     * @param preferences the preferences to change.
     * @param settings the settings to store.
     */
    fun write(preferences: MutablePreferences, settings: T)
}

/**
 * The distance stored in meters under [key], or [default] if it is missing, negative or not
 * finite.
 */
internal fun Preferences.readDistance(
    key: Preferences.Key<Double>,
    default: Distance,
    logger: Logger,
): Distance {
    val meters = this[key] ?: return default
    if (!meters.isFinite() || meters < 0.0) {
        logger.w("Invalid stored %s: %s, using the default", key.name, meters)
        return default
    }
    return Distance.meters(meters)
}

/**
 * The speed stored in meters per second under [key], or [default] if it is missing, negative or
 * not finite.
 */
internal fun Preferences.readSpeed(
    key: Preferences.Key<Double>,
    default: Speed,
    logger: Logger,
): Speed {
    val metersPerSecond = this[key] ?: return default
    if (!metersPerSecond.isFinite() || metersPerSecond < 0.0) {
        logger.w("Invalid stored %s: %s, using the default", key.name, metersPerSecond)
        return default
    }
    return Speed.metersPerSecond(metersPerSecond)
}

/** The duration stored in milliseconds under [key], or [default] if it is missing or not positive. */
internal fun Preferences.readPositiveDuration(
    key: Preferences.Key<Long>,
    default: Duration,
    logger: Logger,
): Duration {
    val millis = this[key] ?: return default
    if (millis <= 0) {
        logger.w("Invalid stored %s: %s, using the default", key.name, millis)
        return default
    }
    return millis.milliseconds
}

/** The enum entry whose name is stored under [key], or [default] if it is missing or unknown. */
internal inline fun <reified E : Enum<E>> Preferences.readEnum(
    key: Preferences.Key<String>,
    default: E,
    logger: Logger,
): E {
    val name = this[key] ?: return default
    return enumValues<E>().firstOrNull { it.name == name } ?: run {
        logger.w("Invalid stored %s: %s, using the default", key.name, name)
        default
    }
}

/** Stores [value] under [key], or removes the key when [value] is [default]. */
internal fun <V : Any> MutablePreferences.storeUnlessDefault(
    key: Preferences.Key<V>,
    value: V,
    default: V,
) {
    if (value == default) remove(key) else this[key] = value
}
