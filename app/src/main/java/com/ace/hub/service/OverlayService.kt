package com.ace.hub.service

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.*
import android.util.Log
import android.view.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.core.app.NotificationCompat
import com.ace.hub.MainActivity
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
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

class OverlayService : LifecycleService(), SavedStateRegistryOwner {

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val overlayViewModelStore = ViewModelStore()

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
    }

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
                monitoringService?.monitorData?.collect {
                    monitorData.value = it
                    checkForegroundPackage(it.foregroundPackage)
                }
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            monitoringService = null
            bound = false
        }
    }

    private var gamePackageName: String? = null
    private var appIcon: Drawable? = null
    private var overlayAdded = false
    private var params = WindowManager.LayoutParams()

    private var timerRemainingSeconds = mutableIntStateOf(0)
    private var timerJob: Job? = null

    private var sessionStartTime = 0L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        super.onStartCommand(intent, flags, startId)

        val newPackageName = intent?.getStringExtra("package_name")
        val timerMinutes = intent?.getIntExtra("timer_minutes", 0) ?: 0

        if (!newPackageName.isNullOrBlank() && newPackageName != gamePackageName) {
            gamePackageName = newPackageName
            appIcon = try {
                packageManager.getApplicationIcon(newPackageName)
            } catch (e: Exception) {
                null
            }

            sessionStartTime = System.currentTimeMillis()

            // Trigger automatic background termination countdown loop instantly
            if (timerMinutes > 0) {
                startAutoCloseTimer(timerMinutes)
            } else {
                timerJob?.cancel()
                timerRemainingSeconds.intValue = 0
            }
        }

        if (!overlayAdded) {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            startForegroundWithNotification()
            setupComposeOverlay()
            bindToMonitoringService()
            overlayAdded = true
        }
        return START_STICKY
    }

    private fun startAutoCloseTimer(minutes: Int) {
        timerJob?.cancel()
        timerRemainingSeconds.intValue = minutes * 60
        timerJob = serviceScope.launch {
            while (timerRemainingSeconds.intValue > 0) {
                delay(1000)
                timerRemainingSeconds.intValue--
            }
            closeGame()
        }
    }

    private fun closeGame() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
        stopSelf()
    }

    private fun checkForegroundPackage(currentPackage: String?) {
        if (currentPackage == null) return
        if (gamePackageName != null && currentPackage != gamePackageName && currentPackage != packageName) {
            Log.d("AceHub", "Closing overlay: foreground is $currentPackage, expected $gamePackageName")
            stopSelf()
        }
    }

    private fun setupComposeOverlay() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 300
        }

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore = overlayViewModelStore
            })
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
        }

        overlayView.setContent {
            AceHubTheme {
                val data by monitorData.collectAsState()

                Box(
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    val metrics = windowManager.currentWindowMetrics
                                    val screenWidth = metrics.bounds.width()
                                    val centerX = params.x + overlayView.width / 2
                                    val targetX = if (centerX < screenWidth / 2) 0 else screenWidth - overlayView.width
                                    animateSnap(targetX)
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                params.x += dragAmount.x.toInt()
                                params.y += dragAmount.y.toInt()
                                try {
                                    windowManager.updateViewLayout(overlayView, params)
                                } catch (_: Exception) {}
                            }
                        }
                ) {
                    OverlayContent(
                        monitorData = data,
                        appIcon = appIcon,
                        remainingSeconds = timerRemainingSeconds.intValue,
                        onCloseOverlay = { stopSelf() },
                        onGoToApp = {
                            val intent = Intent(this@OverlayService, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(intent)
                        }
                    )
                }
            }
        }

        try {
            windowManager.addView(overlayView, params)
        } catch (e: Exception) {
            Log.e("AceHub", "Failed to add overlay", e)
        }
    }

    private fun animateSnap(targetX: Int) {
        serviceScope.launch {
            val startX = params.x
            val duration = 200L
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < duration) {
                val progress = (System.currentTimeMillis() - startTime).toFloat() / duration
                params.x = (startX + (targetX - startX) * progress).toInt()
                try {
                    windowManager.updateViewLayout(overlayView, params)
                } catch (e: Exception) { break }
                delay(10)
            }
            params.x = targetX
            try {
                windowManager.updateViewLayout(overlayView, params)
            } catch (_: Exception) {}
        }
    }

    private fun startForegroundWithNotification() {
        val channelId = "acehub_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "AceHub", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("AceHub Performance Overlay")
            .setContentText("Optimizing performance for the current session.")
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
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
        val sessionDurationMillis = System.currentTimeMillis() - sessionStartTime
        val sessionMinutes = sessionDurationMillis / (1000 * 60)
        if (sessionMinutes >= 1) {
            val prefs = UserPreferences(applicationContext)
            prefs.totalPlaytimeMinutes += sessionMinutes
        }

        super.onDestroy()
        if (bound) unbindService(connection)
        if (overlayAdded && ::overlayView.isInitialized) {
            try {
                windowManager.removeView(overlayView)
            } catch (_: Exception) {}
        }
        overlayViewModelStore.clear()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_STOP = "com.ace.hub.ACTION_STOP"
    }
}