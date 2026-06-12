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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
    val isGoogleLinked by viewModel.isGoogleLinked.collectAsState()
    val isGitHubLinked by viewModel.isGitHubLinked.collectAsState()

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
                    // Profile Picture - Rounded & Bold Outlined
                    Box(contentAlignment = Alignment.Center) {
                        Surface(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .clickable(enabled = isEditMode) { launcher.launch("image/*") },
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = androidx.compose.foundation.BorderStroke(4.dp, MaterialTheme.colorScheme.primary)
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
                                            .background(Color.Black.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Edit, null, tint = Color.White)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Username Text
                    Text(
                        text = username.ifBlank { "Ace User" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(enabled = isEditMode) {
                            tempUsername = username
                            showUsernameDialog = true
                        }
                    )

                    // Account Status Tags (Syntax Corrected)
                    if (isGoogleLinked || isGitHubLinked) {
                        Row(
                            modifier = Modifier.padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isGoogleLinked) {
                                StatusTag("Secured with Google", Color(0xFF4285F4))
                            }
                            if (isGitHubLinked) {
                                StatusTag("GitGuy", Color(0xFF333333))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (isEditMode) {
                        Button(
                            onClick = { isEditMode = false },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Done")
                        }
                    } else {
                        Button(
                            onClick = { isEditMode = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
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
                    PinnedGameCard(game, onClick = { viewModel.launchGameWithOverlay(context, game.packageName) })
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
                            modifier = Modifier.clickable { viewModel.launchGameWithOverlay(context, game.packageName) }
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
                    headlineContent = { Text("Explorer") },
                    supportingContent = { Text("Local Application file storage nodes") },
                    leadingContent = { Icon(Icons.Rounded.Folder, null, modifier = Modifier.size(24.dp)) },
                    trailingContent = { Icon(Icons.Rounded.ChevronRight, null, modifier = Modifier.size(18.dp)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 7. About Layout
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = packageInfo.versionName
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            ListItem(
                headlineContent = { Text("About AceHub") },
                supportingContent = { Text("Version $versionName • © Ace Horizon 2026") },
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

// --- High-Performance Layout Helper Widgets ---

@Composable
fun StatusTag(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PinnedGameCard(game: GameApp, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable { onClick() }
    ) {
        Image(
            painter = com.google.accompanist.drawablepainter.rememberDrawablePainter(game.icon),
            contentDescription = game.appName,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = game.appName,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ReefSectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun SimpleLineChart(data: List<Float>, modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas
        val maxValue = data.maxOrNull() ?: 1f
        val pointsSpace = size.width / (data.size - 1).coerceAtLeast(1)

        val path = Path().apply {
            data.forEachIndexed { index, value ->
                val x = index * pointsSpace
                val y = size.height - (value / maxValue * size.height)
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}