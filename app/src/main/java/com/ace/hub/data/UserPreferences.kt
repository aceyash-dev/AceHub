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

    var isUsageAnalyticsEnabled: Boolean
        get() = prefs.getBoolean("usage_analytics_enabled", true)
        set(value) = prefs.edit().putBoolean("usage_analytics_enabled", value).apply()

    var isOverlayEnabled: Boolean
        get() = prefs.getBoolean("overlay_enabled", true)
        set(value) = prefs.edit().putBoolean("overlay_enabled", value).apply()

    var isAutoBoostEnabled: Boolean
        get() = prefs.getBoolean("auto_boost_enabled", false)
        set(value) = prefs.edit().putBoolean("auto_boost_enabled", value).apply()

    var pinnedGames: Set<String>
        get() = prefs.getStringSet("pinned_games", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("pinned_games", value).apply()

    var recentGames: List<String>
        get() = prefs.getString("recent_games", "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        set(value) = prefs.edit().putString("recent_games", value.joinToString(",")).apply()

    var profileImageUri: String?
        get() = prefs.getString("profile_image_uri", null)
        set(value) = prefs.edit().putString("profile_image_uri", value).apply()
}
