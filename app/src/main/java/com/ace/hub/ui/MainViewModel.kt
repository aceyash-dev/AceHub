package com.ace.hub.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ace.hub.data.DeviceInfo
import com.ace.hub.data.GameApp
import com.ace.hub.data.GameRepository
import com.ace.hub.data.MonitorData
import com.ace.hub.data.SystemMonitor
import com.ace.hub.service.MonitoringService
import com.ace.hub.service.OverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val systemMonitor = SystemMonitor(context)
    private val gameRepository = GameRepository(context)

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

            // Collect monitoring data from service
            viewModelScope.launch {
                monitoringService?.monitorData?.collect { data ->
                    _monitorData.value = data
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            monitoringService = null
            isBound = false
        }
    }

    init {
        // Start and bind to monitoring service
        val serviceIntent = Intent(context, MonitoringService::class.java)
        context.startForegroundService(serviceIntent)
        context.bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)

        // Load device info
        viewModelScope.launch(Dispatchers.IO) {
            _deviceInfo.value = systemMonitor.getDeviceInfo()
        }

        // Load games list
        loadGames()
    }

    fun loadGames() {
        viewModelScope.launch(Dispatchers.IO) {
            _games.value = gameRepository.getInstalledGames()
            _allApps.value = gameRepository.getAllApps()
        }
    }

    fun launchGameWithOverlay(packageName: String) {
        // Start overlay service
        if (Settings.canDrawOverlays(context)) {
            val overlayIntent = Intent(context, OverlayService::class.java)
            context.startForegroundService(overlayIntent)
        }

        // Launch the game
        gameRepository.launchGame(packageName)
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
    }
}
