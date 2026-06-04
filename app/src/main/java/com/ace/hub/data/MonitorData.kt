package com.ace.hub.data

import android.graphics.drawable.Drawable

data class MonitorData(
    val cpuUsage: Float = 0f,
    val gpuUsage: Float = 0f,
    val fps: Float = 60f,
    val cpuHistoryList: List<Float> = emptyList(),
    val fpsHistoryList: List<Float> = List(20) { 60f },
    val coreFrequencies: List<Long?> = emptyList(),
    val coreCount: Int = Runtime.getRuntime().availableProcessors(),
    val ramUsedMB: Long = 0,
    val ramTotalMB: Long = 0,
    val gpuRenderer: String = "Unknown",
    val gpuVendor: String = "Unknown",
    val gpuVersion: String = "Unknown",
    val batteryTempCelsius: Float = 0f,
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    val batteryHealth: String = "Unknown",
    val thermalStatus: Int = 0,
    val thermalStatusText: String = "Normal"
)

data class DeviceInfo(
    val deviceName: String,
    val manufacturer: String,
    val model: String,
    val androidVersion: String,
    val apiLevel: Int,
    val processor: String,
    val coreCount: Int,
    val totalRamMB: Long,
    val gpuRenderer: String,
    val gpuVendor: String,
    val gpuVersion: String,
    val screenResolution: String,
    val screenDensity: Int
)

data class GameApp(
    val packageName: String,
    val appName: String,
    val icon: Drawable?
)
