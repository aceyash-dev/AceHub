package com.ace.hub.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Choreographer
import com.ace.hub.data.MonitorData
import com.ace.hub.data.SystemMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MonitoringService : Service() {

    private val binder = MonitorBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var monitoringJob: Job? = null

    private val _monitorData = MutableStateFlow(MonitorData())
    val monitorData: StateFlow<MonitorData> = _monitorData.asStateFlow()

    private var frameCount = 0
    private var lastFpsTimestamp = 0L
    private var currentFps = 60f
    private val fpsHistory = mutableListOf<Float>().apply { repeat(40) { add(60f) } }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            frameCount++
            val currentTime = System.currentTimeMillis()
            if (lastFpsTimestamp == 0L) {
                lastFpsTimestamp = currentTime
            } else if (currentTime - lastFpsTimestamp >= 1000) {
                currentFps = (frameCount * 1000f) / (currentTime - lastFpsTimestamp)
                frameCount = 0
                lastFpsTimestamp = currentTime
                
                fpsHistory.add(currentFps)
                if (fpsHistory.size > 40) fpsHistory.removeAt(0)
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    inner class MonitorBinder : Binder() {
        fun getService(): MonitoringService = this@MonitoringService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        if (monitoringJob?.isActive == true) return
        
        Handler(Looper.getMainLooper()).post {
            Choreographer.getInstance().postFrameCallback(frameCallback)
        }

        monitoringJob = serviceScope.launch {
            val systemMonitor = SystemMonitor(applicationContext)
            while (isActive) {
                val data = systemMonitor.collectMonitorData()
                _monitorData.value = data.copy(
                    fps = currentFps,
                    fpsHistoryList = fpsHistory.toList()
                )
                delay(1000L)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        monitoringJob?.cancel()
        serviceScope.cancel()
        Handler(Looper.getMainLooper()).post {
            Choreographer.getInstance().removeFrameCallback(frameCallback)
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
}
