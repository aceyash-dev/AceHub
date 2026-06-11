package com.ace.hub.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.ace.hub.R
import com.ace.hub.data.GameApp
import com.ace.hub.ui.games.formatPlayTime
import java.io.File

@Composable
fun MeScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit = {}
) {
    val username by viewModel.username.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val recentGamesPackageNames by viewModel.recentGamesPackageNames.collectAsState()
    val pinnedGamesPackageNames by viewModel.pinnedGamesPackageNames.collectAsState()

    // XP/Level System
    val totalXp by viewModel.totalXp.collectAsState()
    val showGlowingRing by viewModel.showGlowingRing.collectAsState()
    val showNameplate by viewModel.showNameplate.collectAsState()
    val isGoogleLinked by viewModel.isGoogleLinked.collectAsState()

    val level = remember(totalXp) {
        when {
            totalXp >= 125 -> 5
            totalXp >= 80 -> 4
            totalXp >= 50 -> 3
            totalXp >= 20 -> 2
            else -> 1
        }
    }

    val xpToNext = remember(level, totalXp) {
        val next = when(level) {
            1 -> 20
            2 -> 50
            3 -> 80
            4 -> 125
            else -> 125
        }
        val current = when(level) {
            1 -> 0
            2 -> 20
            3 -> 50
            4 -> 80
            else -> 125
        }
        if (level == 5) 1f else (totalXp - current).toFloat() / (next - current).toFloat()
    }

    val recentGames = remember(allApps, recentGamesPackageNames) {
        recentGamesPackageNames.mapNotNull { pkg -> allApps.find { it.packageName == pkg } }
    }
    
    val pinnedGames = remember(allApps, pinnedGamesPackageNames) {
        allApps.filter { it.packageName in pinnedGamesPackageNames }
    }

    val context = LocalContext.current
    val updateUrl by viewModel.newUpdateAvailable.collectAsState()
    
    val pfpFile = File(context.filesDir, "profile_pic.jpg")
    var pfpUri by remember { mutableStateOf(if (pfpFile.exists()) Uri.fromFile(pfpFile) else null) }
    var isEditMode by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var tempUsername by remember { mutableStateOf(username) }
    
    var selectedGameForStats by remember { mutableStateOf<GameApp?>(null) }
    
    val selectableGamesForGraph = remember(recentGames, pinnedGames) {
        (pinnedGames + recentGames).distinctBy { it.packageName }.take(5)
    }
    
    val gameWeeklyPlaytime = remember(selectedGameForStats, viewModel) {
        selectedGameForStats?.let { viewModel.getWeeklyPlaytimeForGame(it.packageName) } ?: viewModel.getWeeklyPlaytime()
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                pfpFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            pfpUri = Uri.fromFile(pfpFile)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        
        // 1. Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Box(modifier = Modifier.padding(24.dp)) {
                // Settings Icon
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Picture with Level Effects
                    val infiniteTransition = rememberInfiniteTransition(label = "ring")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
                        label = "rotation"
                    )

                    Box(contentAlignment = Alignment.Center) {
                        if (level >= 5 && showGlowingRing) {
                            Canvas(modifier = Modifier.size(116.dp)) {
                                rotate(rotation) {
                                    drawCircle(
                                        brush = Brush.sweepGradient(
                                            listOf(Color(0xFFFFD700), Color(0xFFFFD700).copy(0.2f), Color(0xFFFFD700))
                                        ),
                                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .clickable(enabled = isEditMode) { launcher.launch("image/*") },
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = androidx.compose.foundation.BorderStroke(2.dp, if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (pfpUri != null) {
                                    Image(
                                        painter = rememberAsyncImagePainter(pfpUri),
                                        contentDescription = "PFP",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Person, 
                                        contentDescription = "PFP", 
                                        modifier = Modifier.padding(28.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                if (isEditMode) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Edit, null, tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Username with Level 3 Effect
                    Box(contentAlignment = Alignment.Center) {
                        if (level >= 3 && showNameplate) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF8A2BE2).copy(alpha = 0.2f))
                                    .drawBehind {
                                        drawRoundRect(
                                            color = Color(0xFFD0BCFF),
                                            style = Stroke(width = 1.dp.toPx()),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx())
                                        )
                                    }
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = username.ifBlank { "Ace User" },
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD0BCFF),
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.clickable(enabled = isEditMode) { 
                                        tempUsername = username
                                        showUsernameDialog = true 
                                    }
                                )
                            }
                        } else {
                            Text(
                                text = username.ifBlank { "Ace User" },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable(enabled = isEditMode) { 
                                    tempUsername = username
                                    showUsernameDialog = true 
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Level Indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                "LVL $level",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Black
                            )
                        }
                        Text(
                            text = "$totalXp XP",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isGoogleLinked) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = Color(0xFF4285F4).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4285F4).copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color(0xFF4285F4),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Secured with Google",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF4285F4),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // XP Bar
                    LinearProgressIndicator(
                        progress = { xpToNext },
                        modifier = Modifier
                            .width(180.dp)
                            .height(6.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isEditMode) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = showGlowingRing, onCheckedChange = { viewModel.updateGlowingRing(it) })
                                Text("Show Level 5 Ring", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = showNameplate, onCheckedChange = { viewModel.updateNameplate(it) })
                                Text("Show Level 3 Nameplate", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { isEditMode = false },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Done")
                            }
                        }
                    } else {
                        Button(
                            onClick = { isEditMode = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit Profile")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Total Play Time",
                value = formatPlayTime(viewModel.getTotalPlayTime()),
                icon = Icons.Rounded.History,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Pinned Games",
                value = "${pinnedGames.size}",
                icon = Icons.Rounded.PushPin,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Pinned Games (Horizontal)
        if (pinnedGames.isNotEmpty()) {
            ReefSectionHeader("Pinned Library")
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(pinnedGames) { game ->
                    PinnedGameCard(game)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 4. Recent Games (Vertical with Usage)
        if (recentGames.isNotEmpty()) {
            ReefSectionHeader("Recent Activity")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    recentGames.take(5).forEach { game ->
                        ListItem(
                            headlineContent = { Text(game.appName, fontWeight = FontWeight.SemiBold) },
                            supportingContent = { 
                                Text(
                                    text = "${formatPlayTime(viewModel.getPlayTime(game.packageName))} played",
                                    color = MaterialTheme.colorScheme.primary
                                ) 
                            },
                            leadingContent = { 
                                Image(
                                    painter = com.google.accompanist.drawablepainter.rememberDrawablePainter(game.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                                )
                            },
                            trailingContent = { Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.outline) },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clickable { /* Launch or details */ }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 5. Gaming Analytics (Graph)
        ReefSectionHeader("Gaming Analytics")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Weekly Activity (min)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Game selection chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedGameForStats == null,
                            onClick = { selectedGameForStats = null },
                            label = { Text("All Games") },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    items(selectableGamesForGraph) { game ->
                        FilterChip(
                            selected = selectedGameForStats?.packageName == game.packageName,
                            onClick = { selectedGameForStats = game },
                            label = { Text(game.appName) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (gameWeeklyPlaytime.isNotEmpty()) {
                    SimpleLineChart(
                        data = gameWeeklyPlaytime,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        Text("No activity data for this period", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 6. Links & Updates
        ReefSectionHeader("Links & Community")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column {
                ListItem(
                    headlineContent = { Text("GitHub Repository") },
                    supportingContent = { Text("Source code and issues") },
                    leadingContent = { 
                        Icon(
                            painter = painterResource(id = R.drawable.ic_github), 
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        ) 
                    },
                    trailingContent = { Icon(Icons.Rounded.OpenInNew, null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/aceyash-dev/AceHub"))
                        context.startActivity(intent)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text("App Updates") },
                    supportingContent = { 
                        if (updateUrl != null) {
                            Text("New version available!", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        } else {
                            Text("Check for latest releases")
                        }
                    },
                    leadingContent = { Icon(Icons.Rounded.SystemUpdate, null, modifier = Modifier.size(24.dp)) },
                    trailingContent = { 
                        if (updateUrl != null) {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Update")
                            }
                        } else {
                            Icon(Icons.Rounded.ChevronRight, null)
                        }
                    },
                    modifier = Modifier.clickable {
                        if (updateUrl == null) {
                            viewModel.checkForUpdates()
                        } else {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
                            context.startActivity(intent)
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 7. About
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            ListItem(
                headlineContent = { Text("About AceHub") },
                supportingContent = { Text("Version $versionName • ©Ace Horizon 2026") },
                leadingContent = { Icon(Icons.Rounded.Info, null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }

        Spacer(modifier = Modifier.height(100.dp))
    }

    if (showUsernameDialog) {
        AlertDialog(
            onDismissRequest = { showUsernameDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateUsername(tempUsername)
                    showUsernameDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUsernameDialog = false }) {
                    Text("Cancel")
                }
            },
            title = { Text("Edit Username") },
            text = {
                Column {
                    Text(
                        "Change how you appear in AceHub.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    OutlinedTextField(
                        value = tempUsername,
                        onValueChange = { tempUsername = it },
                        label = { Text("Username") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

@Composable
fun PinnedGameCard(game: GameApp) {
    Column(
        modifier = Modifier.width(90.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .size(90.dp)
                .clickable { /* Launch */ },
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = com.google.accompanist.drawablepainter.rememberDrawablePainter(game.icon),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(12.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = game.appName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Spacer(modifier = Modifier.height(12.dp))
            AnimatedContent(
                targetState = value,
                transitionSpec = {
                    slideInVertically { h -> h } + fadeIn() togetherWith
                    slideOutVertically { h -> -h } + fadeOut()
                },
                label = "stat_value"
            ) { targetValue ->
                Text(targetValue, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            }
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ReefSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp
        ),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, bottom = 12.dp)
    )
}

@Composable
fun SimpleLineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    if (data.isEmpty()) return

    val maxValue = remember(data) { (data.maxOrNull() ?: 0f).coerceAtLeast(1f) }
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val spacing = if (data.size > 1) width / (data.size - 1) else width
        
        val points = data.mapIndexed { index, value ->
            Offset(
                x = index * spacing,
                y = height - (value / maxValue * height)
            )
        }
        
        val path = Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    val p0 = points[i - 1]
                    val p1 = points[i]
                    val cpX = (p0.x + p1.x) / 2
                    cubicTo(cpX, p0.y, cpX, p1.y, p1.x, p1.y)
                }
            }
        }
        
        // Bold Thicker Line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Rich Gradient Fill
        val fillPath = Path().apply {
            addPath(path)
            lineTo(points.last().x, height)
            lineTo(points.first().x, height)
            close()
        }
        
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.4f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )
        
        // Draw points for today and max
        points.forEachIndexed { index, point ->
            if (index == points.size - 1) { // Highlight today
                drawCircle(
                    color = lineColor,
                    radius = 6.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = point
                )
            }
        }
    }
}
