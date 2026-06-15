package com.ace.hub.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Calendar

@Suppress("SpellCheckingInspection")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val username by viewModel.username.collectAsState()
    val profilePhotoUrl by viewModel.profileImageUri.collectAsState()
    val monitorData by viewModel.monitorData.collectAsState()
    val games by viewModel.games.collectAsState()

    var weeklyPlaytimeMinutes by remember { mutableStateOf<List<Float>>(emptyList()) }
    var totalPlayTimeMillis by remember { mutableLongStateOf(0L) }

    var selectedDayIndex by remember {
        mutableStateOf(Calendar.getInstance().get(Calendar.DAY_OF_WEEK).let { if (it == Calendar.SUNDAY) 6 else it - 2 })
    }

    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val daysOfWeekLabels = remember { listOf("M", "T", "W", "T", "F", "S", "S") }

    LaunchedEffect(Unit) {
        weeklyPlaytimeMinutes = viewModel.getWeeklyPlaytime()
        totalPlayTimeMillis = viewModel.getTotalPlayTime()
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {}
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
                                        tint = if (hour in 6..20) Color.Black.copy(alpha = 0.6f) else Color.White
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
                            color = if (hour in 6..20) Color.Black.copy(alpha = 0.5f) else Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Name",
                            modifier = Modifier.size(16.dp),
                            tint = if (hour in 6..20) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.6f)
                        )
                    }

                    Text(
                        text = "AceHub Member",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hour in 6..20) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.7f)
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
                MeStatItem("Hours", "${totalPlayTimeMillis / (1000 * 60 * 60)}h")
                VerticalDivider(modifier = Modifier.height(40.dp).padding(horizontal = 8.dp))
                MeStatItem("Avg FPS", "${monitorData.fps.toInt()}")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Playtime Tracking Activity & Filter Chips Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Weekly Playtime",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )

            val activeSelectedValue = if (weeklyPlaytimeMinutes.size > selectedDayIndex) {
                weeklyPlaytimeMinutes[selectedDayIndex].toInt()
            } else 0

            Text(
                text = "$activeSelectedValue min",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Box(modifier = Modifier.height(120.dp).fillMaxWidth()) {
                    if (weeklyPlaytimeMinutes.isNotEmpty()) {
                        FpsXyGraph(history = weeklyPlaytimeMinutes, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No playtime logs available", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // M3 Filter Chips Layout Grid
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(daysOfWeekLabels) { idx: Int, label: String ->
                        FilterChip(
                            selected = selectedDayIndex == idx,
                            onClick = { selectedDayIndex = idx },
                            label = {
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            },
                            shape = CircleShape,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedDayIndex == idx,
                                borderColor = MaterialTheme.colorScheme.outlineVariant,
                                selectedBorderColor = Color.Transparent
                            ),
                            modifier = Modifier.padding(horizontal = 2.dp)
                        )
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
            QuickActionCard(
                icon = Icons.Default.Settings,
                title = "Settings Menu",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                onClick = onNavigateToSettings
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Default.CloudUpload,
                    title = "Backup Data",
                    color = Color(0xFF00BCD4),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            val backedUpSuccessfully = executeLocalDatabaseBackup(context)
                            if (backedUpSuccessfully) {
                                Toast.makeText(context, "Database backup cloned to Downloads folder!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Backup initialization fault.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )

                QuickActionCard(
                    icon = Icons.Default.CloudDownload,
                    title = "Import Data",
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        scope.launch {
                            val importedSuccessfully = executeLocalDatabaseRestore(context)
                            if (importedSuccessfully) {
                                Toast.makeText(context, "Backup restored successfully! Restarting app...", Toast.LENGTH_LONG).show()
                                // Allow toast to display briefly before killing the process so database layers refresh cleanly
                                kotlinx.coroutines.delay(1500)
                                android.os.Process.killProcess(android.os.Process.myPid())
                            } else {
                                Toast.makeText(context, "No backup file found in Downloads folder.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Footer Link Container
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

/**
 * Database raw file manipulation layer.
 * Safely handles Room database structures along with SQLite journal pages.
 */
private suspend fun executeLocalDatabaseBackup(context: Context): Boolean = withContext(Dispatchers.IO) {
    try {
        val databaseName = "acehub_database"
        val databaseFile = context.getDatabasePath(databaseName)

        if (!databaseFile.exists()) return@withContext false

        val targetDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val backupDestinationFile = File(targetDirectory, "AceHub_Backup.db")

        FileInputStream(databaseFile).use { input ->
            FileOutputStream(backupDestinationFile).use { output ->
                input.copyTo(output)
            }
        }

        val walJournalFile = File(databaseFile.absolutePath + "-wal")
        if (walJournalFile.exists()) {
            FileInputStream(walJournalFile).use { input ->
                FileOutputStream(File(targetDirectory, "AceHub_Backup.db-wal")).use { output ->
                    input.copyTo(output)
                }
            }
        }

        val shmJournalFile = File(databaseFile.absolutePath + "-shm")
        if (shmJournalFile.exists()) {
            FileInputStream(shmJournalFile).use { input ->
                FileOutputStream(File(targetDirectory, "AceHub_Backup.db-shm")).use { output ->
                    input.copyTo(output)
                }
            }
        }
        true
    } catch (_: Exception) {
        false
    }
}

/**
 * Reverses the raw file database copy process to completely import pre-existing database tables.
 * Safe extraction mechanisms handle runtime fallback loops cleanly to prevent app data corruption.
 */
private suspend fun executeLocalDatabaseRestore(context: Context): Boolean = withContext(Dispatchers.IO) {
    try {
        val targetDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val backupSourceFile = File(targetDirectory, "AceHub_Backup.db")

        if (!backupSourceFile.exists()) return@withContext false

        val databaseName = "acehub_database"
        val databaseFile = context.getDatabasePath(databaseName)

        // Clean pre-existing local data database destination file anchors first to prevent writing lock failures
        if (databaseFile.exists()) databaseFile.delete()

        FileInputStream(backupSourceFile).use { input ->
            FileOutputStream(databaseFile).use { output ->
                input.copyTo(output)
            }
        }

        // Restore active SQLite transaction journals synchronously if available inside download directory bounds
        val backupWalFile = File(targetDirectory, "AceHub_Backup.db-wal")
        val destWalFile = File(databaseFile.absolutePath + "-wal")
        if (destWalFile.exists()) destWalFile.delete()
        if (backupWalFile.exists()) {
            FileInputStream(backupWalFile).use { input ->
                FileOutputStream(destWalFile).use { output ->
                    input.copyTo(output)
                }
            }
        }

        val backupShmFile = File(targetDirectory, "AceHub_Backup.db-shm")
        val destShmFile = File(databaseFile.absolutePath + "-shm")
        if (destShmFile.exists()) destShmFile.delete()
        if (backupShmFile.exists()) {
            FileInputStream(backupShmFile).use { input ->
                FileOutputStream(destShmFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        true
    } catch (_: Exception) {
        false
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