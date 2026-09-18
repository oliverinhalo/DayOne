package com.dayone.app.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("dayone_settings", Context.MODE_PRIVATE)

    var zoomScalar: Float
        get() = prefs.getFloat(KEY_ZOOM_SCALAR, DEFAULT_ZOOM_SCALAR)
        set(value) = prefs.edit().putFloat(KEY_ZOOM_SCALAR, value.coerceIn(0.5f, 4.0f)).apply()

    companion object {
        private const val KEY_ZOOM_SCALAR = "zoom_scalar"
        const val DEFAULT_ZOOM_SCALAR = 1.5f
    }
}
