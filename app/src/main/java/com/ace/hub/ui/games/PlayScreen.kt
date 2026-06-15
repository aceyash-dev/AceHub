package com.ace.hub.ui.games

import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.rememberAsyncImagePainter
import com.ace.hub.data.GameApp
import com.ace.hub.ui.MainViewModel
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import java.util.Calendar
import com.ace.hub.R
import com.ace.hub.service.OverlayService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private const val YOUTUBE_API_KEY = "AIzaSyBFx8rV891WSD3BKZp9lD_FJF6LYWDtAGI"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val allApps by viewModel.allApps.collectAsState()
    val pinnedGamesPackageNames by viewModel.pinnedGamesPackageNames.collectAsState()

    val pinnedGames = remember(allApps, pinnedGamesPackageNames) {
        allApps.filter { it.packageName in pinnedGamesPackageNames }
    }

    var selectedGame by remember { mutableStateOf<GameApp?>(null) }
    var showAppPicker by remember { mutableStateOf(false) }
    var selectedGamePlayTime by remember { mutableLongStateOf(0L) }

    var timerMinutesSelection by remember { mutableFloatStateOf(0f) }

    // Dynamic Live Feed Network States
    var fetchedVideoIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var isVideoLoading by remember { mutableStateOf(false) }
    var videoErrorMsg by remember { mutableStateOf<String?>(null) }

    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val username by viewModel.username.collectAsState()

    val greeting = when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        hour < 21 -> "Good Evening"
        else -> "Good Night"
    }

    val subtitle = when {
        hour < 12 -> "Rise and grind"
        hour < 17 -> "Keep the momentum going"
        hour < 21 -> "Time for another session"
        else -> "One more match before sleep?"
    }

    LaunchedEffect(selectedGame) {
        selectedGamePlayTime = selectedGame?.let { viewModel.getPlayTime(it.packageName) } ?: 0L
    }

    // Live Streaming API Controller Scope
    LaunchedEffect(selectedGame) {
        val gameName = selectedGame?.appName
        if (gameName.isNullOrBlank()) {
            fetchedVideoIds = emptyList()
            videoErrorMsg = null
            return@LaunchedEffect
        }

        isVideoLoading = true
        videoErrorMsg = null

        fetchedVideoIds = withContext(Dispatchers.IO) {
            val fetchedList = mutableListOf<String>()
            try {
                val encodedQuery = URLEncoder.encode("$gameName high tier gameplay metrics walkthrough", "UTF-8")
                val targetUrl = "https://www.googleapis.com/youtube/v3/search" +
                        "?part=snippet" +
                        "&maxResults=5" +
                        "&order=relevance" +
                        "&q=$encodedQuery" +
                        "&type=video" +
                        "&videoCategoryId=20" +
                        "&key=$YOUTUBE_API_KEY"

                val url = URL(targetUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    reader.close()

                    val jsonResponse = JSONObject(response.toString())
                    val itemsArray = jsonResponse.optJSONArray("items")
                    if (itemsArray != null) {
                        for (i in 0 until itemsArray.length()) {
                            val itemObj = itemsArray.getJSONObject(i)
                            val idObj = itemObj.optJSONObject("id")
                            val videoId = idObj?.optString("videoId")
                            if (!videoId.isNullOrBlank()) {
                                fetchedList.add(videoId)
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        videoErrorMsg = "API Connection Fault (${connection.responseCode})"
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    videoErrorMsg = "Unable to connect to streaming layout components."
                }
            }
            fetchedList
        }
        isVideoLoading = false
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Hero Banner Header Layout
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = when {
                                hour < 12 -> listOf(Color(0xFFFFB74D), Color(0xFFFFE082))
                                hour < 17 -> listOf(Color(0xFF4FC3F7), Color(0xFF81D4FA))
                                hour < 21 -> listOf(Color(0xFFFF7043), Color(0xFFAB47BC))
                                else -> listOf(Color(0xFF0D1117), Color(0xFF1A237E))
                            }
                        )
                    )
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "$greeting, ${username.ifBlank { "Ace" }} 👋",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (hour < 21 && hour >= 6) Color.Black.copy(alpha = 0.8f) else Color.White
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (hour < 21 && hour >= 6) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.7f)
                    )

                    Spacer(Modifier.weight(1f))

                    Text(
                        text = when {
                            hour < 12 -> "🌅 ☁️ 🐦"
                            hour < 17 -> "☀️ 🌤️ 🌳"
                            hour < 21 -> "🌇 ✨ 🌆"
                            else -> "🌙 ⭐ ☁️"
                        },
                        fontSize = 32.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Library Header
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

            // Infinite App Grid Picker Transition Matrix
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
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showAppPicker = true
                            },
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
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedGame = if (isSelected) null else game
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Premium Auto-Stop Timer Module Card
            AnimatedVisibility(
                visible = selectedGame != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.HourglassTop,
                                    contentDescription = "Session Limits",
                                    tint = if (timerMinutesSelection > 0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Auto-Stop Game Session",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (timerMinutesSelection.toInt() == 0) "Disabled" else "${timerMinutesSelection.toInt()} Min",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (timerMinutesSelection > 0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Slider(
                            value = timerMinutesSelection,
                            onValueChange = { timerMinutesSelection = it },
                            valueRange = 0f..120f,
                            steps = 7,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Text(
                            text = "Forces game closure and overlay window shutdown automatically when time expires.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Primary Screen Actions Execution Layer
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
                    text = if (selectedGame != null) "Launch Game" else "Select Game",
                    icon = Icons.Default.PlayArrow,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = if (selectedGame != null) Modifier.fillMaxWidth() else Modifier.weight(1f),
                    enabled = selectedGame != null,
                    onClick = {
                        selectedGame?.let { game ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val intent = Intent(context, OverlayService::class.java).apply {
                                putExtra("package_name", game.packageName)
                                putExtra("timer_minutes", timerMinutesSelection.toInt())
                            }
                            context.startService(intent)
                            viewModel.launchGameWithOverlay(context, game.packageName)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Premium Content Visual Grid Module Layout
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
                                        text = "Weekly Playtime: ${formatPlayTime(selectedGamePlayTime)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = "Popular Content Live Feed",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                when {
                                    isVideoLoading -> {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(3) { PremiumVideoShimmerPlaceholder() }
                                        }
                                    }
                                    !videoErrorMsg.isNullOrBlank() -> {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = videoErrorMsg ?: "Failed to complete data stream processing.",
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                    fetchedVideoIds.isEmpty() -> {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Provide a valid YouTube Data API Key to stream custom metrics live.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    else -> {
                                        var playingVideoId by remember { mutableStateOf<String?>(null) }
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(fetchedVideoIds, key = { it }) { id ->
                                                ExpandableVideoCard(
                                                    videoId = id,
                                                    gameName = game.appName,
                                                    isPlaying = playingVideoId == id,
                                                    onPlay = { playingVideoId = id },
                                                    onClose = { playingVideoId = null }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // My Device System Layout Monitor Block
            val deviceInfo by viewModel.deviceInfo.collectAsState()
            Text(
                text = "My Device",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    deviceInfo?.let { info ->
                        InfoRow(label = "Model", value = info.deviceName)
                        InfoRow(label = "Chipset", value = info.processor)
                        InfoRow(label = "GPU", value = info.gpuRenderer)
                        InfoRow(label = "RAM", value = "${(info.totalRamMB / 1024.0).toInt()} GB")
                        InfoRow(label = "Android", value = info.androidVersion)
                    } ?: Text("Initializing hardware monitor...", style = MaterialTheme.typography.bodyMedium)
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
    }
}

@Composable
fun PremiumVideoShimmerPlaceholder() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .width(280.dp)
            .height(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = alpha))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
    )
}

@Composable
fun GameCard(
    game: GameApp,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .border(
                width = 2.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(24.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                             else MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberDrawablePainter(drawable = game.icon),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = game.appName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun LargeActionButton(
    text: String,
    icon: ImageVector,
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
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ExpandableVideoCard(
    videoId: String,
    gameName: String,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onClose: () -> Unit
) {
    val cardWidth by animateDpAsState(targetValue = if (isPlaying) 320.dp else 280.dp, label = "width")
    val cardHeight by animateDpAsState(targetValue = if (isPlaying) 260.dp else 220.dp, label = "height")

    Card(
        modifier = Modifier
            .width(cardWidth)
            .height(cardHeight)
            .clip(RoundedCornerShape(24.dp))
            .clickable(enabled = !isPlaying) { onPlay() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        if (isPlaying) {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            webViewClient = WebViewClient()
                            webChromeClient = WebChromeClient()
                            loadUrl("https://www.youtube.com/embed/$videoId?autoplay=1")
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = rememberAsyncImagePainter("https://img.youtube.com/vi/$videoId/0.jpg"),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayCircleOutline,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

fun formatPlayTime(millis: Long): String {
    val totalMinutes = millis / (1000 * 60)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
