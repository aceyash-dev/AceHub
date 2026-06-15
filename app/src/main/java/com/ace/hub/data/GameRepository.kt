package com.ace.hub.data

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.Calendar

class GameRepository(private val context: Context) {

    private val gameKeywords = listOf(
        "game", "games", "play", "puzzle", "arcade", "racing", "chess",
        "sudoku", "solitaire", "casino", "poker", "craft", "clash",
        "quest", "hero", "battle", "war", "shoot", "strike", "run",
        "jump", "ball", "sports", "soccer", "football", "basketball",
        "cricket", "ludo", "candy", "fruit", "bird", "zombie", "dragon",
        "knight", "kingdom", "empire", "tower", "defense", "pubg",
        "fortnite", "minecraft", "roblox", "genshin", "among"
    )

    private suspend fun getBaseAppList(): List<ApplicationInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        // Querying without META_DATA is significantly faster
        pm.getInstalledApplications(0)
    }

    suspend fun getInstalledGames(): List<GameApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installedApps = getBaseAppList()

        val games = installedApps.filter { appInfo ->
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            
            // Filter by name/keyword first (fast)
            val pkgLower = appInfo.packageName.lowercase()
            val matchesKeyword = gameKeywords.any { keyword -> pkgLower.contains(keyword) }
            
            if (!matchesKeyword && isSystem) return@filter false
            
            // Only check launch intent for potential candidates (slow)
            val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
            if (launchIntent == null) return@filter false

            if (!isSystem) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (appInfo.category == ApplicationInfo.CATEGORY_GAME) return@filter true
                }
                @Suppress("DEPRECATION")
                if (appInfo.flags and ApplicationInfo.FLAG_IS_GAME != 0) return@filter true
                return@filter matchesKeyword
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (appInfo.category == ApplicationInfo.CATEGORY_GAME) return@filter true
                }
            }
            false
        }

        coroutineScope {
            games.map { appInfo ->
                async {
                    val pkgInfo = try {
                        pm.getPackageInfo(appInfo.packageName, 0)
                    } catch (_: Exception) {
                        null
                    }
                    GameApp(
                        packageName = appInfo.packageName,
                        appName = pm.getApplicationLabel(appInfo).toString(),
                        icon = try {
                            pm.getApplicationIcon(appInfo)
                        } catch (_: Exception) {
                            null
                        },
                        versionName = pkgInfo?.versionName ?: "Unknown"
                    )
                }
            }.awaitAll().sortedBy { it.appName.lowercase() }
        }
    }

    suspend fun getAllApps(): List<GameApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installedApps = getBaseAppList()

        coroutineScope {
            installedApps.mapNotNull { appInfo ->
                val packageName = appInfo.packageName
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                               (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                
                val isWhitelistedSystemApp = packageName.contains("com.android.chrome") || 
                                             packageName.contains("org.mozilla.firefox") ||
                                             packageName.contains("com.microsoft.emmx")
                
                if (!isSystem || isWhitelistedSystemApp) {
                    async {
                        val launchIntent = pm.getLaunchIntentForPackage(packageName)
                        if (launchIntent != null) {
                            val pkgInfo = try {
                                pm.getPackageInfo(packageName, 0)
                            } catch (_: Exception) {
                                null
                            }
                            GameApp(
                                packageName = packageName,
                                appName = pm.getApplicationLabel(appInfo).toString(),
                                icon = try {
                                    pm.getApplicationIcon(appInfo)
                                } catch (_: Exception) {
                                    null
                                },
                                versionName = pkgInfo?.versionName ?: "Unknown"
                            )
                        } else null
                    }
                } else null
            }.awaitAll().filterNotNull().distinctBy { it.packageName }.sortedBy { it.appName.lowercase() }
        }
    }

    fun launchGame(packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    suspend fun getAppPlayTime(packageName: String): Long = withContext(Dispatchers.IO) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7) // Last 7 days

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            calendar.timeInMillis,
            System.currentTimeMillis()
        )

        stats?.find { it.packageName == packageName }?.totalTimeInForeground ?: 0L
    }

    suspend fun getTotalPlayTime(): Long = withContext(Dispatchers.IO) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            calendar.timeInMillis,
            System.currentTimeMillis()
        )
        
        stats?.sumOf { it.totalTimeInForeground } ?: 0L
    }

    suspend fun getWeeklyPlaytime(): List<Float> = withContext(Dispatchers.IO) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val weeklyStats = mutableListOf<Float>()

        for (i in 6 downTo 0) {
            val startCalendar = Calendar.getInstance()
            startCalendar.add(Calendar.DAY_OF_YEAR, -i)
            startCalendar.set(Calendar.HOUR_OF_DAY, 0)
            startCalendar.set(Calendar.MINUTE, 0)
            startCalendar.set(Calendar.SECOND, 0)
            startCalendar.set(Calendar.MILLISECOND, 0)

            val endCalendar = Calendar.getInstance()
            endCalendar.add(Calendar.DAY_OF_YEAR, -i)
            endCalendar.set(Calendar.HOUR_OF_DAY, 23)
            endCalendar.set(Calendar.MINUTE, 59)
            endCalendar.set(Calendar.SECOND, 59)
            endCalendar.set(Calendar.MILLISECOND, 999)

            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startCalendar.timeInMillis,
                endCalendar.timeInMillis
            )

            val dailyTotal = stats?.sumOf { it.totalTimeInForeground } ?: 0L
            weeklyStats.add(dailyTotal / (1000f * 60f)) // Convert to minutes
        }
        weeklyStats
    }

    suspend fun getWeeklyPlaytimeForGame(packageName: String): List<Float> = withContext(Dispatchers.IO) {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val weeklyStats = mutableListOf<Float>()

        for (i in 6 downTo 0) {
            val startCalendar = Calendar.getInstance()
            startCalendar.add(Calendar.DAY_OF_YEAR, -i)
            startCalendar.set(Calendar.HOUR_OF_DAY, 0)
            startCalendar.set(Calendar.MINUTE, 0)
            startCalendar.set(Calendar.SECOND, 0)
            startCalendar.set(Calendar.MILLISECOND, 0)

            val endCalendar = Calendar.getInstance()
            endCalendar.add(Calendar.DAY_OF_YEAR, -i)
            endCalendar.set(Calendar.HOUR_OF_DAY, 23)
            endCalendar.set(Calendar.MINUTE, 59)
            endCalendar.set(Calendar.SECOND, 59)
            endCalendar.set(Calendar.MILLISECOND, 999)

            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startCalendar.timeInMillis,
                endCalendar.timeInMillis
            )

            val dailyTotal = stats?.find { it.packageName == packageName }?.totalTimeInForeground ?: 0L
            weeklyStats.add(dailyTotal / (1000f * 60f)) // Convert to minutes
        }
        weeklyStats
    }
}
