package com.ace.hub.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.util.DisplayMetrics
import android.view.WindowManager
import java.io.File
import java.io.RandomAccessFile

class SystemMonitor(private val context: Context) {

    private var prevCpuTotal: Long = 0L
    private var prevCpuIdle: Long = 0L

    private var cachedGpuInfo: Triple<String, String, String>? = null

    // ── CPU Usage ────────────────────────────────────────────────────────────

    fun getCpuUsage(): Float {
        return try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val line = reader.readLine() // first line: "cpu  user nice system idle iowait irq softirq ..."
            reader.close()

            val tokens = line.split("\\s+".toRegex())
            // tokens[0] = "cpu", tokens[1..] = numeric fields
            val user = tokens[1].toLong()
            val nice = tokens[2].toLong()
            val system = tokens[3].toLong()
            val idle = tokens[4].toLong()
            val iowait = if (tokens.size > 5) tokens[5].toLong() else 0L
            val irq = if (tokens.size > 6) tokens[6].toLong() else 0L
            val softirq = if (tokens.size > 7) tokens[7].toLong() else 0L

            val totalCpu = user + nice + system + idle + iowait + irq + softirq
            val totalIdle = idle + iowait

            val deltaTotal = totalCpu - prevCpuTotal
            val deltaIdle = totalIdle - prevCpuIdle

            prevCpuTotal = totalCpu
            prevCpuIdle = totalIdle

            if (deltaTotal == 0L) {
                0f
            } else {
                ((deltaTotal - deltaIdle).toFloat() / deltaTotal.toFloat()) * 100f
            }
        } catch (_: Exception) {
            0f
        }
    }

    // ── Core Frequencies ─────────────────────────────────────────────────────

    fun getCoreFrequencies(): List<Long?> {
        val coreCount = Runtime.getRuntime().availableProcessors()
        return (0 until coreCount).map { i ->
            try {
                val path = "/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq"
                val content = File(path).readText().trim()
                content.toLongOrNull()
            } catch (_: Exception) {
                null
            }
        }
    }

    // ── Memory Info ──────────────────────────────────────────────────────────

    fun getMemoryInfo(): Pair<Long, Long> {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val totalMB = memInfo.totalMem / (1024L * 1024L)
        val availMB = memInfo.availMem / (1024L * 1024L)
        val usedMB = totalMB - availMB

        return Pair(usedMB, totalMB)
    }

    // ── Battery Info ─────────────────────────────────────────────────────────

    data class BatteryInfo(
        val temperatureCelsius: Float,
        val levelPercent: Int,
        val isCharging: Boolean,
        val healthText: String
    )

    fun getBatteryInfo(): BatteryInfo {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = context.registerReceiver(null, intentFilter)

        if (batteryStatus == null) {
            return BatteryInfo(0f, 0, false, "Unknown")
        }

        val temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f

        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val levelPercent = if (scale > 0) (level * 100) / scale else 0

        val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)
        val healthText = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }

        return BatteryInfo(temperature, levelPercent, isCharging, healthText)
    }

    // ── GPU Info ─────────────────────────────────────────────────────────────

    fun getGpuInfo(): Triple<String, String, String> {
        cachedGpuInfo?.let { return it }

        var renderer = "Unknown"
        var vendor = "Unknown"
        var version = "Unknown"

        var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
        var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
        var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

        try {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
                return Triple(renderer, vendor, version).also { cachedGpuInfo = it }
            }

            val majorMinor = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, majorMinor, 0, majorMinor, 1)) {
                return Triple(renderer, vendor, version).also { cachedGpuInfo = it }
            }

            val configAttribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0)

            if (numConfigs[0] == 0 || configs[0] == null) {
                return Triple(renderer, vendor, version).also { cachedGpuInfo = it }
            }

            val contextAttribs = intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
                EGL14.EGL_NONE
            )
            eglContext = EGL14.eglCreateContext(
                eglDisplay, configs[0]!!, EGL14.EGL_NO_CONTEXT, contextAttribs, 0
            )

            val surfaceAttribs = intArrayOf(
                EGL14.EGL_WIDTH, 1,
                EGL14.EGL_HEIGHT, 1,
                EGL14.EGL_NONE
            )
            eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, configs[0]!!, surfaceAttribs, 0)

            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)

            renderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: "Unknown"
            vendor = GLES20.glGetString(GLES20.GL_VENDOR) ?: "Unknown"
            version = GLES20.glGetString(GLES20.GL_VERSION) ?: "Unknown"
        } catch (_: Exception) {
            // GPU info unavailable
        } finally {
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(
                    eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT
                )
                if (eglSurface != EGL14.EGL_NO_SURFACE) {
                    EGL14.eglDestroySurface(eglDisplay, eglSurface)
                }
                if (eglContext != EGL14.EGL_NO_CONTEXT) {
                    EGL14.eglDestroyContext(eglDisplay, eglContext)
                }
                EGL14.eglTerminate(eglDisplay)
            }
        }

        val result = Triple(renderer, vendor, version)
        cachedGpuInfo = result
        return result
    }

    // ── Thermal Status ───────────────────────────────────────────────────────

    fun getThermalStatus(): Pair<Int, String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val status = powerManager.currentThermalStatus
            val text = when (status) {
                PowerManager.THERMAL_STATUS_NONE -> "Normal"
                PowerManager.THERMAL_STATUS_LIGHT -> "Light"
                PowerManager.THERMAL_STATUS_MODERATE -> "Moderate"
                PowerManager.THERMAL_STATUS_SEVERE -> "Severe"
                PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
                PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
                PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown"
                else -> "Unknown"
            }
            return Pair(status, text)
        }
        return Pair(0, "Normal")
    }

    // ── Collect All Monitor Data ─────────────────────────────────────────────

    fun collectMonitorData(): MonitorData {
        val cpuUsage = getCpuUsage()
        val coreFreqs = getCoreFrequencies()
        val (ramUsed, ramTotal) = getMemoryInfo()
        val batteryInfo = getBatteryInfo()
        val (gpuRenderer, gpuVendor, gpuVersion) = getGpuInfo()
        val (thermalStatusCode, thermalStatusText) = getThermalStatus()
        val foregroundPackage = getRecentForegroundPackage()

        return MonitorData(
            cpuUsage = cpuUsage,
            coreFrequencies = coreFreqs,
            coreCount = Runtime.getRuntime().availableProcessors(),
            ramUsedMB = ramUsed,
            ramTotalMB = ramTotal,
            gpuRenderer = gpuRenderer,
            gpuVendor = gpuVendor,
            gpuVersion = gpuVersion,
            batteryTempCelsius = batteryInfo.temperatureCelsius,
            batteryLevel = batteryInfo.levelPercent,
            isCharging = batteryInfo.isCharging,
            batteryHealth = batteryInfo.healthText,
            thermalStatus = thermalStatusCode,
            thermalStatusText = thermalStatusText,
            foregroundPackage = foregroundPackage
        )
    }

    // ── Device Info ──────────────────────────────────────────────────────────

    @Suppress("DEPRECATION")
    fun getDeviceInfo(): DeviceInfo {
        val (gpuRenderer, gpuVendor, gpuVersion) = getGpuInfo()
        val (_, ramTotal) = getMemoryInfo()

        val processorName = try {
            File("/proc/cpuinfo").readLines()
                .firstOrNull { it.startsWith("Hardware") || it.startsWith("model name") }
                ?.substringAfter(":")
                ?.trim()
                ?: Build.HARDWARE
        } catch (_: Exception) {
            Build.HARDWARE
        }

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay
        val metrics = DisplayMetrics()
        display.getRealMetrics(metrics)
        val screenResolution = "${metrics.widthPixels} x ${metrics.heightPixels}"
        val screenDensity = metrics.densityDpi

        return DeviceInfo(
            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            processor = processorName,
            coreCount = Runtime.getRuntime().availableProcessors(),
            totalRamMB = ramTotal,
            gpuRenderer = gpuRenderer,
            gpuVendor = gpuVendor,
            gpuVersion = gpuVersion,
            screenResolution = screenResolution,
            screenDensity = screenDensity
        )
    }

    fun getRecentForegroundPackage(): String? {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
        val time = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(time - 2000, time)
        val event = android.app.usage.UsageEvents.Event()
        var lastPackage: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastPackage = event.packageName
            }
        }
        return lastPackage
    }
}
