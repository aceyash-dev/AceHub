package com.ace.hub.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ace.hub.data.*
import com.ace.hub.service.MonitoringService
import com.ace.hub.service.OverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
data class GitHubRelease(
    val tag_name: String,
    val html_url: String
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val systemMonitor = SystemMonitor(context)
    private val gameRepository = GameRepository(context)
    private val userPrefs = UserPreferences(context)

    private val _username = MutableStateFlow(userPrefs.username)
    val username: StateFlow<String> = _username.asStateFlow()

    private val _useSystemTheme = MutableStateFlow(userPrefs.useSystemTheme)
    val useSystemTheme: StateFlow<Boolean> = _useSystemTheme.asStateFlow()

    private val _isUsageAnalyticsEnabled = MutableStateFlow(userPrefs.isUsageAnalyticsEnabled)
    val isUsageAnalyticsEnabled: StateFlow<Boolean> = _isUsageAnalyticsEnabled.asStateFlow()

    private val _isOverlayEnabled = MutableStateFlow(userPrefs.isOverlayEnabled)
    val isOverlayEnabled: StateFlow<Boolean> = _isOverlayEnabled.asStateFlow()

    private val _isAutoBoostEnabled = MutableStateFlow(userPrefs.isAutoBoostEnabled)
    val isAutoBoostEnabled: StateFlow<Boolean> = _isAutoBoostEnabled.asStateFlow()

    private val _newUpdateAvailable = MutableStateFlow<String?>(null)
    val newUpdateAvailable: StateFlow<String?> = _newUpdateAvailable.asStateFlow()

    private val _pinnedGamesPackageNames = MutableStateFlow(userPrefs.pinnedGames)
    val pinnedGamesPackageNames: StateFlow<Set<String>> = _pinnedGamesPackageNames.asStateFlow()

    private val _recentGamesPackageNames = MutableStateFlow(userPrefs.recentGames)
    val recentGamesPackageNames: StateFlow<List<String>> = _recentGamesPackageNames.asStateFlow()

    private val _profileImageUri = MutableStateFlow(userPrefs.profileImageUri)
    val profileImageUri: StateFlow<String?> = _profileImageUri.asStateFlow()

    fun updateUsername(name: String) {
        _username.value = name
        userPrefs.username = name
    }

    fun updateSystemTheme(use: Boolean) {
        _useSystemTheme.value = use
        userPrefs.useSystemTheme = use
    }

    fun updateUsageAnalytics(enabled: Boolean) {
        _isUsageAnalyticsEnabled.value = enabled
        userPrefs.isUsageAnalyticsEnabled = enabled
    }

    fun updateOverlayEnabled(enabled: Boolean) {
        _isOverlayEnabled.value = enabled
        userPrefs.isOverlayEnabled = enabled
    }

    fun updateAutoBoost(enabled: Boolean) {
        _isAutoBoostEnabled.value = enabled
        userPrefs.isAutoBoostEnabled = enabled
    }

    fun togglePinnedGame(packageName: String) {
        val current = _pinnedGamesPackageNames.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _pinnedGamesPackageNames.value = current
        userPrefs.pinnedGames = current
    }

    fun addToRecentGames(packageName: String) {
        val current = _recentGamesPackageNames.value.toMutableList()
        current.remove(packageName)
        current.add(0, packageName)
        val limited = current.take(20)
        _recentGamesPackageNames.value = limited
        userPrefs.recentGames = limited
    }

    fun updateProfileImage(uri: String?) {
        _profileImageUri.value = uri
        userPrefs.profileImageUri = uri
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            try {
                val latestRelease = withContext(Dispatchers.IO) {
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url("https://api.github.com/repos/aceyash-dev/AceHub/releases/latest")
                        .build()
                    
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@withContext null
                        val body = response.body?.string() ?: return@withContext null
                        Json { ignoreUnknownKeys = true }.decodeFromString<GitHubRelease>(body)
                    }
                }

                if (latestRelease != null) {
                    val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName
                    // Simple version comparison: if tag name is different from current version name
                    // In a more robust app, you might parse semantic versioning (1.0.0 > 0.9.0)
                    if (latestRelease.tag_name != currentVersion && !latestRelease.tag_name.contains(currentVersion)) {
                        _newUpdateAvailable.value = latestRelease.html_url
                    }
                }
            } catch (e: Exception) {
                Log.e("AceHub", "Failed to check for updates", e)
            }
        }
    }

    fun hasUsagePermission(): Boolean {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, time - 1000 * 60, time)
        return stats?.isNotEmpty() == true
    }

    private val _monitorData = MutableStateFlow(MonitorData())
    val monitorData: StateFlow<MonitorData> = _monitorData.asStateFlow()

    private val _deviceInfo = MutableStateFlow<DeviceInfo?>(null)
    val deviceInfo: StateFlow<DeviceInfo?> = _deviceInfo.asStateFlow()

    private val _games = MutableStateFlow<List<GameApp>>(emptyList())
    val games: StateFlow<List<GameApp>> = _games.asStateFlow()

    private val _allApps = MutableStateFlow<List<GameApp>>(emptyList())
    val allApps: StateFlow<List<GameApp>> = _allApps.asStateFlow()

    private var monitoringService: MonitoringService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MonitoringService.MonitorBinder
            monitoringService = binder.getService()
            isBound = true

            // Collect monitoring data from service
            viewModelScope.launch {
                monitoringService?.monitorData?.collect { data ->
                    _monitorData.value = data
                    
                    // Detect games launched outside AceHub
                    data.foregroundPackage?.let { pkg ->
                        if (pkg != context.packageName && _games.value.any { it.packageName == pkg }) {
                            addToRecentGames(pkg)
                        }
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            monitoringService = null
            isBound = false
        }
    }

    init {
        // Start and bind to monitoring service
        val serviceIntent = Intent(context, MonitoringService::class.java)
        context.startForegroundService(serviceIntent)
        context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        // Load device info
        viewModelScope.launch(Dispatchers.IO) {
            _deviceInfo.value = systemMonitor.getDeviceInfo()
        }

        // Load games list
        loadGames()

        // Check for updates on startup
        checkForUpdates()
    }

    fun loadGames() {
        viewModelScope.launch(Dispatchers.IO) {
            val gamesDeferred = async { gameRepository.getInstalledGames() }
            val appsDeferred = async { gameRepository.getAllApps() }
            
            _games.value = gamesDeferred.await()
            _allApps.value = appsDeferred.await()
        }
    }

    fun launchGameWithOverlay(context: Context, packageName: String) {
        addToRecentGames(packageName)
        
        if (_isAutoBoostEnabled.value) {
            autoBoost()
        }

        if (_isOverlayEnabled.value && Settings.canDrawOverlays(context)) {
            val overlayIntent = Intent(context, OverlayService::class.java).apply {
                putExtra("package_name", packageName)
            }
            try {
                context.startForegroundService(overlayIntent)
            } catch (e: Exception) {
                context.startService(overlayIntent)
            }
        }
        
        gameRepository.launchGame(packageName)
    }

    private fun autoBoost() {
        // Logic to clear background processes or optimize performance
        viewModelScope.launch(Dispatchers.Default) {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            activityManager.runningAppProcesses?.forEach { process ->
                process.pkgList?.forEach { pkg ->
                    if (pkg != context.packageName) {
                        try {
                            activityManager.killBackgroundProcesses(pkg)
                        } catch (_: Exception) {}
                    }
                }
            }
        }
    }

    fun getPlayTime(packageName: String): Long {
        return gameRepository.getAppPlayTime(packageName)
    }

    fun getTotalPlayTime(): Long {
        return gameRepository.getTotalPlayTime()
    }

    fun getWeeklyPlaytime(): List<Float> {
        return gameRepository.getWeeklyPlaytime()
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
    }
}
