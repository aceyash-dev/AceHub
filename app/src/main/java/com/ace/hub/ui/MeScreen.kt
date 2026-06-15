package com.ace.hub.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.ace.hub.R
import com.ace.hub.ui.components.FpsXyGraph
import java.util.Calendar

@Composable
fun MeScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val username by viewModel.username.collectAsState()
    val profilePhotoUrl by viewModel.profileImageUri.collectAsState()
    val monitorData by viewModel.monitorData.collectAsState()
    val games by viewModel.games.collectAsState()

    var weeklyStats by remember { mutableStateOf<List<Float>>(emptyList()) }
    var totalPlayTime by remember { mutableLongStateOf(0L) }

    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }

    LaunchedEffect(Unit) {
        weeklyStats = viewModel.getWeeklyPlaytime()
        totalPlayTime = viewModel.getTotalPlayTime()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                // Non-persistable context flag safely caught
            }
            viewModel.updateProfileImage(it.toString())
        }
    }

    var showEditNameDialog by remember { mutableStateOf(false) }

    if (showEditNameDialog) {
        EditNameDialog(
            currentName = username,
            onDismiss = { showEditNameDialog = false },
            onSave = { newName ->
                viewModel.updateUsername(newName)
                showEditNameDialog = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Profile Hero Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Surface(
                            modifier = Modifier.size(100.dp),
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            tonalElevation = 8.dp
                        ) {
                            if (!profilePhotoUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = profilePhotoUrl,
                                    contentDescription = "Profile",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(50.dp),
                                        tint = if (hour < 21 && hour >= 6) Color.Black.copy(alpha = 0.6f) else Color.White
                                    )
                                }
                            }
                        }

                        SmallFloatingActionButton(
                            onClick = {
                                launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            shape = CircleShape,
                            modifier = Modifier.size(30.dp).offset(x = 2.dp, y = 2.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Change Photo", modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showEditNameDialog = true }
                    ) {
                        Text(
                            text = username.ifBlank { "Ace Yash" },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (hour < 21 && hour >= 6) Color.Black.copy(alpha = 0.8f) else Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Name",
                            modifier = Modifier.size(16.dp),
                            tint = if (hour < 21 && hour >= 6) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Text(
                        text = "AceHub Member • Level 12 Gamer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hour < 21 && hour >= 6) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats Row
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MeStatItem("Games", "${games.size}")
                VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 8.dp))
                MeStatItem("Hours", "${totalPlayTime / (1000 * 60 * 60)}h")
                VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 8.dp))
                MeStatItem("Avg FPS", "${monitorData.fps.toInt()}")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Weekly Activity
        Text(
            "Recent Activity",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 12.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Box(modifier = Modifier.height(100.dp).fillMaxWidth()) {
                    if (weeklyStats.isNotEmpty()) {
                        FpsXyGraph(history = weeklyStats, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No activity recorded", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                        Text(day, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Actions Grid
        Text(
            "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start).padding(start = 4.dp, bottom = 12.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    icon = Icons.Default.Settings,
                    title = "Settings",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToSettings
                )
                QuickActionCard(
                    icon = Icons.Default.BarChart,
                    title = "Statistics",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                    onClick = { /* Implement Analytics Sheet navigation upstream */ }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                QuickActionCard(
                    icon = Icons.Default.Games,
                    title = "My Games",
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f),
                    onClick = { /* Handle Filter shortcut implementation */ }
                )
                QuickActionCard(
                    icon = Icons.Default.EmojiEvents,
                    title = "Achievements",
                    color = Color(0xFFFFC107),
                    modifier = Modifier.weight(1f),
                    onClick = { /* Handle Achievement breakdown tracking */ }
                )
            }
            QuickActionCard(
                icon = Icons.Default.CloudSync,
                title = "Backup & Restore",
                color = Color(0xFF00BCD4),
                modifier = Modifier.fillMaxWidth(),
                onClick = { /* Fire Room DB Cloud-Save backup hook */ }
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Footer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 120.dp)
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/aceyash-dev/AceHub"))
                    context.startActivity(intent)
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Built with ❤️ by Ace Yash",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_github),
                    contentDescription = "GitHub",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "GitHub →",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun EditNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile Name") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { onSave(name) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun MeStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun QuickActionCard(
    icon: ImageVector,
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(14.dp),
                color = color.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = color
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}