package com.ace.hub.service

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.*
import android.util.Log
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
import com.ace.hub.data.UserPreferences
import com.ace.hub.ui.overlay.OverlayContent
import com.ace.hub.ui.theme.AceHubTheme
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class OverlayService : LifecycleService() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: ComposeView
    private lateinit var userPrefs: UserPreferences
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
    private var gamePackageName: String? = null
    private var overlayAdded = false
    private var isOverlayVisible = true

    override fun onCreate() {
        super.onCreate()
        userPrefs = UserPreferences(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (action == ACTION_HIDE) {
            isOverlayVisible = !isOverlayVisible
            if (::overlayView.isInitialized) {
                overlayView.visibility = if (isOverlayVisible) View.VISIBLE else View.GONE
            }
            updateNotification()
            return START_STICKY
        }

        super.onStartCommand(intent, flags, startId)
        Log.d("AceHub", "OverlayService started")
        
        val newPackageName = intent?.getStringExtra("package_name")
        if (newPackageName != null && newPackageName != gamePackageName) {
            gamePackageName = newPackageName
            startTime = System.currentTimeMillis()
        }
        
        if (!overlayAdded) {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            startForegroundWithNotification()
            setupComposeOverlay()
            bindToMonitoringService()
            
            android.widget.Toast.makeText(this, "AceHub is monitoring your game performance...", android.widget.Toast.LENGTH_SHORT).show()
            
            // Timer for notification
            serviceScope.launch {
                while (isActive) {
                    delay(1000)
                    updateNotification()
                }
            }
            overlayAdded = true
        }
        return START_STICKY
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(2, createNotification())
    }

    private fun createNotification(): Notification {
        val elapsedMillis = System.currentTimeMillis() - startTime
        val seconds = (elapsedMillis / 1000) % 60
        val minutes = (elapsedMillis / 1000 / 60) % 60
        val hours = (elapsedMillis / 1000 / 3600)
        val timeString = String.format("%02d:%02d:%02d", hours, minutes, seconds)
        
        val appName = getAppName(gamePackageName)
        val data = monitorData.value
        
        val stopIntent = Intent(this, OverlayService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        val hideIntent = Intent(this, OverlayService::class.java).apply { action = ACTION_HIDE }
        val hidePendingIntent = PendingIntent.getService(this, 1, hideIntent, PendingIntent.FLAG_IMMUTABLE)

        val openIntent = Intent(this, com.ace.hub.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(this, 2, openIntent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, "acehub_overlay")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("🎮 AceHub Active")
            .setContentText("$appName • $timeString")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        """
                        FPS: ${data.fps.toInt()} • CPU: ${data.cpuUsage.toInt()}% • RAM: ${data.ramUsedMB} MB
                        GPU: ${data.gpuUsage.toInt()}%
                        Session: $timeString
                        """.trimIndent()
                    )
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .addAction(
                if (isOverlayVisible) android.R.drawable.ic_menu_view else android.R.drawable.ic_menu_add, 
                if (isOverlayVisible) "Hide Overlay" else "Show Overlay", 
                hidePendingIntent
            )
            .addAction(android.R.drawable.ic_menu_send, "Open AceHub", openPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
    }

    private fun getAppName(packageName: String?): String {
        if (packageName == null) return "Gaming Session"
        return try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(packageName, 0)
            ).toString()
        } catch (_: Exception) {
            "Gaming Session"
        }
    }

    private fun setupComposeOverlay() {
        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
        }

        overlayView.setContent {
            var offsetX by remember { mutableFloatStateOf(0f) }
            var offsetY by remember { mutableFloatStateOf(0f) }
            
            AceHubTheme {
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
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        Log.d("AceHub", "Creating overlay")
        try {
            windowManager.addView(overlayView, params)
            Log.d("AceHub", "Overlay added")
        } catch (e: Exception) {
            Log.e("AceHub", "Failed to add overlay view", e)
        }
    }

    private fun startForegroundWithNotification() {
        val channelId = "acehub_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "AceHub Overlay", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        
        val notification = createNotification()
            
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

    companion object {
        private const val ACTION_STOP = "com.ace.hub.ACTION_STOP"
        private const val ACTION_HIDE = "com.ace.hub.ACTION_HIDE"
    }
}
