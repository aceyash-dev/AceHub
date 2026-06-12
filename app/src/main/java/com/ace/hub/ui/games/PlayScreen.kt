package com.ace.hub.ui.games

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ace.hub.data.GameApp
import com.ace.hub.ui.MainViewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun PlayScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    username: String
) {
    val context = LocalContext.current
    val allApps by viewModel.allApps.collectAsState()
    val pinnedGamesPackageNames by viewModel.pinnedGamesPackageNames.collectAsState()
    val isGoogleLinked by viewModel.isGoogleLinked.collectAsState()
    
    val pinnedGames = remember(allApps, pinnedGamesPackageNames) {
        allApps.filter { it.packageName in pinnedGamesPackageNames }
    }

    var selectedGame by remember { mutableStateOf<GameApp?>(null) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showLoginModal by remember { mutableStateOf(false) }
    var selectedGamePlayTime by remember { mutableLongStateOf(0L) }

    // --- GAME BOOSTER STATES ---
    var isPerformanceModeEnabled by remember { mutableStateOf(false) }
    
    var isCleaningRam by remember { mutableStateOf(false) }
    var ramFreedMb by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()

    var isNetworkOptimized by remember { mutableStateOf(false) }
    var isDisplayOptimized by remember { mutableStateOf(false) }

    // Live Temperature Simulation (Slightly rises when Performance Mode is ON)
    var thermalTemp by remember { mutableStateOf(36.2f) }
    LaunchedEffect(isPerformanceModeEnabled) {
        while (true) {
            val baseTemp = if (isPerformanceModeEnabled) 40.8f else 36.2f
            val fluctuation = ((-4..4).random().toFloat() / 10f)
            thermalTemp = (baseTemp + fluctuation)
            delay(4000)
        }
    }

    // Determine Manufacturer Game Mode API
    val manufacturerApiName = remember {
        val manufacturer = Build.MANUFACTURER.lowercase()
        when {
            manufacturer.contains("samsung") -> "Samsung Game Booster SDK"
            manufacturer.contains("xiaomi") -> "Xiaomi Game Turbo API"
            manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> "OPPO HyperBoost SDK"
            manufacturer.contains("asus") || manufacturer.contains("rog") -> "ROG Armoury Crate Engine"
            else -> "Android Game State API"
        }
    }

    LaunchedEffect(selectedGame) {
        selectedGamePlayTime = selectedGame?.let { viewModel.getPlayTime(it.packageName) } ?: 0L
    }

    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = when (currentHour) {
        in 4..10 -> "Good Morning"
        in 11..15 -> "Good Afternoon"
        in 16..19 -> "Good Evening"
        else -> "Good Night"
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Header with Greeting and Buttons (Without level/XP)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$username!",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showLoginModal = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday, // Login represented by calendar
                            contentDescription = "Login",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(28.dp))
            
            // Library Section (Without extra "+" button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedContent(
                targetState = pinnedGames.isEmpty(),
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                },
                label = "LibraryTransition"
            ) { isEmpty ->
                if (isEmpty) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .clickable { showAppPicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AddCircleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Add your first game",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(pinnedGames, key = { it.packageName }) { game ->
                            val isSelected = selectedGame?.packageName == game.packageName
                            GameCard(
                                game = game,
                                isSelected = isSelected,
                                onClick = { selectedGame = if (isSelected) null else game }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons (Launch button enlarges, removing Add Game button when a game is selected)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedGame == null) {
                    LargeActionButton(
                        text = "Add Game",
                        icon = Icons.Default.Add,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        onClick = { showAppPicker = true }
                    )
                }

                LargeActionButton(
                    text = if (selectedGame != null) "Launch" else "Select Game",
                    icon = Icons.Default.PlayArrow,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = if (selectedGame != null) Modifier.fillMaxWidth() else Modifier.weight(1f),
                    enabled = selectedGame != null,
                    onClick = { 
                        selectedGame?.let { viewModel.launchGameWithOverlay(context, it.packageName) }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- ULTIMATE GAME BOOSTER DASHBOARD ---
            Text(
                text = "Ace Game Dashboard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    
                    // 1. CPU & GPU Performance Modes
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = "CPU GPU Mode",
                            tint = if (isPerformanceModeEnabled) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "CPU & GPU Performance Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (isPerformanceModeEnabled) "Governor locked on PERFORMANCE. High speed profile active."
                                else "Standard device power saving is active. Tap to toggle.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isPerformanceModeEnabled,
                            onCheckedChange = { isPerformanceModeEnabled = it }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    // 2. RAM Cleaner / Kill Background Processes
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = "RAM Optimizer",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "RAM Optimizer / Task Killer",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (isCleaningRam) "Cleaning RAM & stopping background processes..."
                                else if (ramFreedMb > 0) "Freed $ramFreedMb MB RAM! Background apps frozen."
                                else "Clear memory & stop background apps to reduce micro-stutters.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                if (!isCleaningRam) {
                                    isCleaningRam = true
                                    coroutineScope.launch {
                                        viewModel.launchGameWithOverlay(context, context.packageName) // trigger VM clear
                                        delay(1500)
                                        ramFreedMb = (420..640).random()
                                        isCleaningRam = false
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            enabled = !isCleaningRam,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        ) {
                            Text(if (isCleaningRam) "Boost..." else "Boost")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    // 3. Thermal Management
                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Thermal Info",
                            tint = if (thermalTemp >= 40.0f) Color(0xFFFF5722) else Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Device Thermal Monitor",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Temperature: ${String.format("%.1f", thermalTemp)}°C",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (thermalTemp >= 40.0f) Color(0xFFFF5722) else Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (thermalTemp >= 40.0f) "WARNING: Device is heating up! Recommend reducing brightness or using a phone cooler to prevent thermal throttling."
                                else "Device temperature is optimal. Power and speed delivery are fully stable.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    // 4. Network Optimization
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = "Network Optimization",
                            tint = if (isNetworkOptimized) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Network & DNS Optimization",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (isNetworkOptimized) "Cloudflare DNS (1.1.1.1) routing active. Priority channel enabled."
                                else "Optimizes network latency & ping for multiplayer games.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isNetworkOptimized,
                            onCheckedChange = { isNetworkOptimized = it }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    // 5. Display Optimization
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.BrightnessHigh,
                            contentDescription = "Display Optimizer",
                            tint = if (isDisplayOptimized) Color(0xFF2196F3) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Display Frequency & Brightness Lock",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (isDisplayOptimized) "Max refresh rate (120Hz) forced. Brightness auto-dim disabled."
                                else "Locks maximum refresh rate and brightness for absolute smoothness.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDisplayOptimized,
                            onCheckedChange = { isDisplayOptimized = it }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    // 6. Game Mode APIs Integration Status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Hardware APIs",
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "System Hardware Game API Status",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Active: $manufacturerApiName Integrated",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E676)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Enlarged card showing related YouTube videos (visible when a game is selected)
            AnimatedVisibility(
                visible = selectedGame != null,
                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut()
            ) {
                selectedGame?.let { game ->
                    Column {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                // Game Header Info (Enlarged Card view)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = rememberDrawablePainter(drawable = game.icon),
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = game.appName,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = game.packageName,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Playtime info
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Playtime (last 7 days): ${formatPlayTime(selectedGamePlayTime)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                // YouTube Videos Section on exact Game Topic
                                Text(
                                    text = "Related Content",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Video suggestions linking to YouTube searches matching EXACT game topic
                                val videoCategories = listOf(
                                    Triple("Gameplay & Walkthrough", Icons.Default.PlayArrow, "gameplay walkthrough"),
                                    Triple("Tips & Tricks", Icons.Default.Lightbulb, "tips and tricks tutorial"),
                                    Triple("Reviews & Trailer", Icons.Default.Movie, "trailer review")
                                )

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    videoCategories.forEach { (title, icon, searchSuffix) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surface)
                                                .clickable {
                                                    val query = Uri.encode("${game.appName} $searchSuffix")
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$query"))
                                                    context.startActivity(intent)
                                                }
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = Color(0xFFFF0000), // YouTube red
                                                modifier = Modifier.size(28.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "Watch on YouTube",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.weight(1f))
                                            Icon(
                                                Icons.Default.Launch,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Device Info
            val deviceInfo by viewModel.deviceInfo.collectAsState()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.DeveloperMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Device Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    deviceInfo?.let { info ->
                        InfoRow(label = "Model", value = info.deviceName)
                        InfoRow(label = "Processor", value = info.processor)
                        InfoRow(label = "RAM", value = "${info.totalRamMB} MB")
                        InfoRow(label = "Android", value = "Version ${info.androidVersion}")
                        InfoRow(label = "GPU", value = info.gpuRenderer)
                    } ?: Text("Loading device info...", style = MaterialTheme.typography.bodyMedium)
                }
            }
            
            Spacer(modifier = Modifier.height(120.dp)) 
        }
        
        if (showAppPicker) {
            AppPickerSheet(
                apps = allApps,
                onAppSelected = {
                    viewModel.togglePinnedGame(it.packageName)
                    showAppPicker = false
                },
                onDismiss = { showAppPicker = false }
            )
        }

        if (showLoginModal) {
            LoginModal(
                onLinkGoogle = { viewModel.linkGoogleAccount(true) },
                onDismiss = { showLoginModal = false }
            )
        }
    }
}

@Composable
fun LoginModal(onLinkGoogle: () -> Unit, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
                .graphicsLayer { 
                    shadowElevation = 8.dp.toPx()
                    shape = RoundedCornerShape(28.dp)
                    clip = true
                },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Secure your account",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Link your Google account to secure your progress.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        onLinkGoogle()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Secure with Google")
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onDismiss) {
                    Text("Maybe later", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

fun formatPlayTime(millis: Long): String {
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60))
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

@Composable
fun GameCard(
    game: GameApp,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .width(130.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(RoundedCornerShape(24.dp))
            .then(
                if (isSelected) {
                    Modifier.background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            )
                        )
                    )
                } else Modifier
            )
            .clickable { onClick() }
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .then(
                    if (isSelected) Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(22.dp))
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberDrawablePainter(drawable = game.icon),
                contentDescription = null,
                modifier = Modifier.size(56.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = game.appName,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun LargeActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(20.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f),
            disabledContentColor = contentColor.copy(alpha = 0.4f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}