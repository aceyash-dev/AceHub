package com.ace.hub.service

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.*
import android.view.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
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

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForegroundWithNotification()
        setupComposeOverlay()
        bindToMonitoringService()
    }

    private fun setupComposeOverlay() {
        overlayView = ComposeView(this).apply {
            setContent {
                MaterialTheme {
                    var isExpanded by remember { mutableStateOf(false) }
                    val data by monitorData.collectAsState()
                    
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
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
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
