package com.ace.hub.service

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.*
import android.view.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.material3.MaterialTheme
import androidx.core.app.NotificationCompat
import com.ace.hub.data.MonitorData
import com.ace.hub.ui.overlay.OverlayContent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: ComposeView
    private val monitorData = MutableStateFlow(MonitorData())

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var monitoringService: MonitoringService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MonitoringService.MonitorBinder
            monitoringService = binder.getService()
            bound = true
            serviceScope.launch {
                monitoringService?.monitorData?.collect { monitorData.value = it }
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            monitoringService = null
            bound = false
        }
    }

    private var startTime: Long = 0L

    override fun onCreate() {
        super.onCreate()
        startTime = System.currentTimeMillis()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundWithNotification()
        setupComposeOverlay()
        bindToMonitoringService()
        
        // Timer for notification
        serviceScope.launch {
            while (isActive) {
                delay(1000)
                updateNotification()
            }
        }
    }

    private fun updateNotification() {
        val elapsedMillis = System.currentTimeMillis() - startTime
        val seconds = (elapsedMillis / 1000) % 60
        val minutes = (elapsedMillis / 1000 / 60) % 60
        val hours = (elapsedMillis / 1000 / 3600)
        val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)

        val notification = NotificationCompat.Builder(this, "acehub_overlay")
            .setContentTitle("Playing Time")
            .setContentText(timeString)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()
        
        startForeground(2, notification)
    }

    private fun setupComposeOverlay() {
        overlayView = ComposeView(this)
        var offsetX by mutableStateOf(0f)
        var offsetY by mutableStateOf(0f)

        overlayView.setContent {
            MaterialTheme {
                var isExpanded by remember { mutableStateOf(false) }
                val data by monitorData.collectAsState()
                
                Box(
                    modifier = Modifier
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        }
                ) {
                    OverlayContent(
                        monitorData = data,
                        isExpanded = isExpanded,
                        onToggleMode = { isExpanded = !isExpanded }
                    )
                }
            }
        }

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager.addView(overlayView, params)
    }

    private fun startForegroundWithNotification() {
        val channelId = "acehub_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "AceHub Overlay", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("AceHub Overlay")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()
            
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(2, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(2, notification)
        }
    }

    private fun bindToMonitoringService() {
        bindService(Intent(this, MonitoringService::class.java), connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (bound) unbindService(connection)
        if (::overlayView.isInitialized) windowManager.removeView(overlayView)
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?) = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
}
