package com.ace.hub.data

import android.content.Context
import android.content.SharedPreferences

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    var username: String
        get() = prefs.getString("username", "User") ?: "User"
        set(value) = prefs.edit().putString("username", value).apply()

    var useSystemTheme: Boolean
        get() = prefs.getBoolean("use_system_theme", true)
        set(value) = prefs.edit().putBoolean("use_system_theme", value).apply()

    var customSeedColor: Int
        get() = prefs.getInt("custom_seed_color", 0xFF6750A4.toInt())
        set(value) = prefs.edit().putInt("custom_seed_color", value).apply()
}
