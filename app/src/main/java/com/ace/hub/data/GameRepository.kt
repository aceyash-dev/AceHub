package com.ace.hub.data

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
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

    fun getInstalledGames(): List<GameApp> {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        val games = installedApps.filter { appInfo ->
            // Skip system apps that aren't updated
            if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0 &&
                appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0
            ) {
                return@filter false
            }

            // Must have a launch intent
            if (pm.getLaunchIntentForPackage(appInfo.packageName) == null) {
                return@filter false
            }

            // API 26+: use category
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (appInfo.category == ApplicationInfo.CATEGORY_GAME) {
                    return@filter true
                }
            }

            // Fallback: check FLAG_IS_GAME (deprecated but still useful)
            @Suppress("DEPRECATION")
            if (appInfo.flags and ApplicationInfo.FLAG_IS_GAME != 0) {
                return@filter true
            }

            // Fallback: keyword matching on package name
            val pkgLower = appInfo.packageName.lowercase()
            gameKeywords.any { keyword -> pkgLower.contains(keyword) }
        }

        return games.map { appInfo ->
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
        }.sortedBy { it.appName.lowercase() }
    }

    fun launchGame(packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun getAllApps(): List<GameApp> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                mainIntent,
                PackageManager.ResolveInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        return resolveInfos.mapNotNull { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
            val pkgInfo = try {
                pm.getPackageInfo(activityInfo.packageName, 0)
            } catch (_: Exception) {
                null
            }
            GameApp(
                packageName = activityInfo.packageName,
                appName = resolveInfo.loadLabel(pm).toString(),
                icon = try {
                    resolveInfo.loadIcon(pm)
                } catch (_: Exception) {
                    null
                },
                versionName = pkgInfo?.versionName ?: "Unknown"
            )
        }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
    }

    fun getAppPlayTime(packageName: String): Long {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7) // Last 7 days

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            calendar.timeInMillis,
            System.currentTimeMillis()
        )

        return stats?.find { it.packageName == packageName }?.totalTimeInForeground ?: 0L
    }

    fun getTotalPlayTime(): Long {
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
        
        val gamePackages = getInstalledGames().map { it.packageName }.toSet()
        return stats?.filter { it.packageName in gamePackages }?.sumOf { it.totalTimeInForeground } ?: 0L
    }

    fun getWeeklyPlaytime(): List<Float> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val gamePackages = getInstalledGames().map { it.packageName }.toSet()
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

            val dailyTotal = stats?.filter { it.packageName in gamePackages }?.sumOf { it.totalTimeInForeground } ?: 0L
            weeklyStats.add(dailyTotal / (1000f * 60f)) // Convert to minutes
        }
        return weeklyStats
    }
}
