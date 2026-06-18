package com.example.dacs3.data

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("mindful_flow_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_NOTIFICATION_SOUND = "notification_sound_enabled"
        private const val KEY_SELECTED_SOUND_URI = "selected_sound_uri"
        private const val KEY_DARK_MODE = "dark_mode_enabled"
    }

    var isNotificationSoundEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_NOTIFICATION_SOUND, true)
        set(value) = sharedPreferences.edit().putBoolean(KEY_NOTIFICATION_SOUND, value).apply()

    var selectedSoundUri: String?
        get() = sharedPreferences.getString(KEY_SELECTED_SOUND_URI, null)
        set(value) = sharedPreferences.edit().putString(KEY_SELECTED_SOUND_URI, value).apply()

    var isDarkMode: Boolean
        get() = sharedPreferences.getBoolean(KEY_DARK_MODE, false)
        set(value) = sharedPreferences.edit().putBoolean(KEY_DARK_MODE, value).apply()
}
