package com.speedsound

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) { prefs.edit().putBoolean(KEY_ENABLED, value).apply() }

    var sensitivity: Float
        get() = prefs.getFloat(KEY_SENSITIVITY, DEFAULT_SENSITIVITY)
        set(value) { prefs.edit().putFloat(KEY_SENSITIVITY, value).apply() }

    var maxSpeedKmh: Float
        get() = prefs.getFloat(KEY_MAX_SPEED, DEFAULT_MAX_SPEED)
        set(value) { prefs.edit().putFloat(KEY_MAX_SPEED, value).apply() }

    var minVolumePercent: Int
        get() = prefs.getInt(KEY_MIN_VOLUME, DEFAULT_MIN_VOLUME)
        set(value) { prefs.edit().putInt(KEY_MIN_VOLUME, value).apply() }

    var maxVolumePercent: Int
        get() = prefs.getInt(KEY_MAX_VOLUME, DEFAULT_MAX_VOLUME)
        set(value) { prefs.edit().putInt(KEY_MAX_VOLUME, value).apply() }

    var hasBeenInitialized: Boolean
        get() = prefs.getBoolean(KEY_INITIALIZED, false)
        set(value) { prefs.edit().putBoolean(KEY_INITIALIZED, value).apply() }

    companion object {
        private const val PREFS_NAME = "speedsound_prefs"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_SENSITIVITY = "sensitivity"
        private const val KEY_MAX_SPEED = "max_speed"
        private const val KEY_MIN_VOLUME = "min_volume"
        private const val KEY_MAX_VOLUME = "max_volume"
        private const val KEY_INITIALIZED = "initialized"

        const val DEFAULT_SENSITIVITY = 1.0f
        const val DEFAULT_MAX_SPEED = 50f   // km/h
        const val DEFAULT_MIN_VOLUME = 20   // %
        const val DEFAULT_MAX_VOLUME = 100  // %
    }
}
