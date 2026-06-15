package com.ace.hub.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
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
import kotlinx.coroutines.awaitAll
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

data class BootTask(
    val name: String,
    val completed: Boolean = false,
    val failed: Boolean = false,
    val message: String = ""
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

    private val _showBatteryStats = MutableStateFlow(userPrefs.showBatteryStats)
    val showBatteryStats: StateFlow<Boolean> = _showBatteryStats.asStateFlow()

    private val _vibrationOnLaunch = MutableStateFlow(userPrefs.vibrationOnLaunch)
    val vibrationOnLaunch: StateFlow<Boolean> = _vibrationOnLaunch.asStateFlow()

    private val _autoDnd = MutableStateFlow(userPrefs.autoDnd)
    val autoDnd: StateFlow<Boolean> = _autoDnd.asStateFlow()

    private val _brightnessLock = MutableStateFlow(userPrefs.brightnessLock)
    val brightnessLock: StateFlow<Boolean> = _brightnessLock.asStateFlow()

    private val _bootTasks = MutableStateFlow<List<BootTask>>(emptyList())
    val bootTasks: StateFlow<List<BootTask>> = _bootTasks.asStateFlow()

    private val _bootFinished = MutableStateFlow(false)
    val bootFinished: StateFlow<Boolean> = _bootFinished.asStateFlow()

    private val _isDashboardEnabled = MutableStateFlow<Boolean>(false)
    val isDashboardEnabled: StateFlow<Boolean> = _isDashboardEnabled.asStateFlow()

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

    fun updateShowBatteryStats(enabled: Boolean) {
        _showBatteryStats.value = enabled
        userPrefs.showBatteryStats = enabled
    }

    fun updateVibrationOnLaunch(enabled: Boolean) {
        _vibrationOnLaunch.value = enabled
        userPrefs.vibrationOnLaunch = enabled
    }

    fun updateAutoDnd(enabled: Boolean) {
        _autoDnd.value = enabled
        userPrefs.autoDnd = enabled
    }

    fun updateBrightnessLock(enabled: Boolean) {
        _brightnessLock.value = enabled
        userPrefs.brightnessLock = enabled
    }

    fun updateDashboardEnabled(enabled: Boolean) {
        _isDashboardEnabled.value = enabled
    }

    private fun initBootTasks() {
        _bootTasks.value = listOf(
            BootTask("Loading user preferences"),
            BootTask("Starting monitoring service"),
            BootTask("Loading device information"),
            BootTask("Scanning installed games"),
            BootTask("Scanning installed apps"),
            BootTask("Checking permissions"),
            BootTask("Loading usage statistics"),
            BootTask("Checking updates")
        )
    }

    private fun completeTask(
        name: String,
        success: Boolean,
        message: String = ""
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            _bootTasks.value = _bootTasks.value.map {
                if (it.name == name) {
                    it.copy(
                        completed = success,
                        failed = !success,
                        message = message
                    )
                } else {
                    it
                }
            }
        }
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

    data class SemVer(
        val parts: List<Int>,
        val preRelease: String? = null
    ) : Comparable<SemVer> {
        override fun compareTo(other: SemVer): Int {
            val maxParts = maxOf(this.parts.size, other.parts.size)
            for (i in 0 until maxParts) {
                val thisPart = this.parts.getOrElse(i) { 0 }
                val otherPart = other.parts.getOrElse(i) { 0 }
                if (thisPart != otherPart) {
                    return thisPart.compareTo(otherPart)
                }
            }

            if (this.preRelease == null && other.preRelease != null) return 1
            if (this.preRelease != null && other.preRelease == null) return -1
            if (this.preRelease == null && other.preRelease == null) return 0

            return comparePreRelease(this.preRelease!!, other.preRelease!!)
        }

        private fun comparePreRelease(pre1: String, pre2: String): Int {
            val parts1 = pre1.split(".")
            val parts2 = pre2.split(".")
            val size = maxOf(parts1.size, parts2.size)
            for (i in 0 until size) {
                val part1 = parts1.getOrNull(i)
                val part2 = parts2.getOrNull(i)

                if (part1 == null && part2 != null) return -1
                if (part1 != null && part2 == null) return 1
                if (part1 != null && part2 != null) {
                    val num1 = part1.toIntOrNull()
                    val num2 = part2.toIntOrNull()

                    if (num1 != null && num2 != null) {
                        val comp = num1.compareTo(num2)
                        if (comp != 0) return comp
                    } else if (num1 != null && num2 == null) {
                        return -1
                    } else if (num1 == null && num2 != null) {
                        return 1
                    } else {
                        val comp = part1.compareTo(part2)
                        if (comp != 0) return comp
                    }
                }
            }
            return 0
        }

        companion object {
            fun parse(version: String): SemVer {
                val clean = version.trim().removePrefix("v").removePrefix("V")
                val withoutBuild = clean.split("+").first()
                val parts = withoutBuild.split("-")
                val core = parts.first()
                val preRelease = if (parts.size > 1) parts.subList(1, parts.size).joinToString("-") else null

                val coreParts = core.split(".").mapNotNull { it.toIntOrNull() }
                return SemVer(coreParts, preRelease)
            }
        }
    }

    private fun isNewerVersion(current: String, latest: String): Boolean {
        return try {
            val currentSemVer = SemVer.parse(current)
            val latestSemVer = SemVer.parse(latest)
            latestSemVer > currentSemVer
        } catch (e: Exception) {
            false
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://api.github.com/repos/aceyash-dev/AceHub/releases/latest")
                    .build()

                val latestRelease = withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@withContext null
                        val body = response.body?.string() ?: return@withContext null
                        Json { ignoreUnknownKeys = true }.decodeFromString<GitHubRelease>(body)
                    }
                }

                if (latestRelease != null) {
                    val currentVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
                    if (isNewerVersion(currentVersion, latestRelease.tag_name)) {
                        _newUpdateAvailable.value = latestRelease.html_url
                    }
                }
            } catch (e: Exception) {
                Log.e("AceHub", "Failed to check for updates", e)
            }
        }
    }

    suspend fun hasUsagePermission(): Boolean = withContext(Dispatchers.IO) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val time = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, time - 1000 * 60, time)
        stats?.isNotEmpty() == true
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

            viewModelScope.launch {
                monitoringService?.monitorData?.collect { data ->
                    _monitorData.value = data

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
        viewModelScope.launch(Dispatchers.Default) {
            initBootTasks()

            completeTask("Loading user preferences", true)

            val deviceInfoJob = async {
                try {
                    val info = systemMonitor.getDeviceInfo()
                    _deviceInfo.value = info
                    completeTask("Loading device information", true)
                } catch (e: Exception) {
                    completeTask("Loading device information", false, e.message ?: "")
                }
            }

            val gamesJob = async {
                try {
                    loadGames()
                    completeTask("Scanning installed games", true)
                    completeTask("Scanning installed apps", true)
                } catch (e: Exception) {
                    completeTask("Scanning installed games", false, e.message ?: "")
                }
            }

            val monitoringJob = async {
                try {
                    withContext(Dispatchers.Main) { startMonitoring() }
                    completeTask("Starting monitoring service", true)
                } catch (e: Exception) {
                    completeTask("Starting monitoring service", false, e.message ?: "Service failed")
                }
            }

            val permissionsJob = async {
                val permissionGranted = hasUsagePermission()
                completeTask(
                    "Checking permissions",
                    permissionGranted,
                    if (!permissionGranted) "Usage access not granted" else ""
                )
            }

            awaitAll(deviceInfoJob, gamesJob, monitoringJob, permissionsJob)

            try {
                fetchInitialUsageStats()
                completeTask("Loading usage statistics", true)
            } catch (e: Exception) {
                completeTask("Loading usage statistics", false, e.message ?: "")
            }

            try {
                checkForUpdates()
                completeTask("Checking updates", true)
            } catch (e: Exception) {
                completeTask("Checking updates", false, e.message ?: "")
            }

            withContext(Dispatchers.Main) {
                _bootFinished.value = true
            }
        }
    }

    private fun startMonitoring() {
        val serviceIntent = Intent(context, MonitoringService::class.java)
        context.startForegroundService(serviceIntent)
        context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    suspend fun fetchInitialUsageStats() {
        if (!hasUsagePermission()) return

        withContext(Dispatchers.IO) {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            val calendar = java.util.Calendar.getInstance()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -7)

            val stats = usageStatsManager.queryUsageStats(
                android.app.usage.UsageStatsManager.INTERVAL_BEST,
                calendar.timeInMillis,
                System.currentTimeMillis()
            )

            if (stats != null) {
                val gamePackages = _games.value.map { it.packageName }.toSet()
                val recentGamesFromStats = stats
                    .filter { it.packageName in gamePackages && it.totalTimeInForeground > 0 }
                    .sortedByDescending { it.lastTimeUsed }
                    .map { it.packageName }
                    .take(15)

                if (recentGamesFromStats.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        val current = _recentGamesPackageNames.value.toMutableList()
                        recentGamesFromStats.forEach { pkg ->
                            if (!current.contains(pkg)) {
                                current.add(pkg)
                            }
                        }
                        _recentGamesPackageNames.value = current.take(20)
                        userPrefs.recentGames = _recentGamesPackageNames.value
                    }
                }
            }
        }
    }

    suspend fun loadGames() {
        withContext(Dispatchers.IO) {
            val gamesDeferred = async { gameRepository.getInstalledGames() }
            val appsDeferred = async { gameRepository.getAllApps() }

            val games = gamesDeferred.await()
            val apps = appsDeferred.await()

            withContext(Dispatchers.Main) {
                _games.value = games
                _allApps.value = apps
            }
        }
    }

    fun launchGameWithOverlay(context: Context, packageName: String) {
        addToRecentGames(packageName)

        if (_isAutoBoostEnabled.value) {
            autoBoost()
        }

        if (_autoDnd.value) {
            applyDnd(context, true)
        }
        if (_brightnessLock.value) {
            applyBrightnessLock(context, true)
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

    private fun applyDnd(context: Context, enabled: Boolean) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager.isNotificationPolicyAccessGranted) {
                notificationManager.setInterruptionFilter(
                    if (enabled) android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY
                    else android.app.NotificationManager.INTERRUPTION_FILTER_ALL
                )
            }
        }
    }

    private fun applyBrightnessLock(context: Context, enabled: Boolean) {
        if (Settings.System.canWrite(context)) {
            if (enabled) {
                Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
            }
        }
    }

    private fun autoBoost() {
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

    suspend fun getPlayTime(packageName: String): Long {
        return gameRepository.getAppPlayTime(packageName)
    }

    suspend fun getTotalPlayTime(): Long {
        return gameRepository.getTotalPlayTime()
    }

    suspend fun getWeeklyPlaytime(): List<Float> {
        return gameRepository.getWeeklyPlaytime()
    }

    suspend fun getWeeklyPlaytimeForGame(packageName: String): List<Float> {
        return gameRepository.getWeeklyPlaytimeForGame(packageName)
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
    }
}
