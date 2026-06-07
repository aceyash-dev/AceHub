package com.ace.hub.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.ace.hub.R
import com.ace.hub.ui.games.formatPlayTime
import com.patrykandpatrick.vico.compose.cartesian.*
import com.patrykandpatrick.vico.compose.cartesian.layer.*
import com.patrykandpatrick.vico.compose.cartesian.axis.*
import com.patrykandpatrick.vico.compose.common.shader.color
import com.patrykandpatrick.vico.core.cartesian.data.*
import com.patrykandpatrick.vico.core.common.shader.DynamicShader
import java.io.File

@Composable
fun MeScreen(
    viewModel: com.ace.hub.ui.MainViewModel
) {
    val username by viewModel.username.collectAsState()
    val allApps by viewModel.allApps.collectAsState()
    val recentGamesPackageNames by viewModel.recentGamesPackageNames.collectAsState()
    val pinnedGamesPackageNames by viewModel.pinnedGamesPackageNames.collectAsState()

    val recentGames = remember(allApps, recentGamesPackageNames) {
        recentGamesPackageNames.mapNotNull { pkg -> allApps.find { it.packageName == pkg } }
    }
    
    val pinnedGames = remember(allApps, pinnedGamesPackageNames) {
        allApps.filter { it.packageName in pinnedGamesPackageNames }
    }

    val context = LocalContext.current
    val pfpFile = File(context.filesDir, "profile_pic.jpg")
    
    var pfpUri by remember { mutableStateOf<Uri?>(if (pfpFile.exists()) Uri.fromFile(pfpFile) else null) }
    var isEditMode by remember { mutableStateOf(false) }
    var showUsernameDialog by remember { mutableStateOf(false) }
    var tempUsername by remember { mutableStateOf(username) }
    
    val weeklyPlaytime = remember { viewModel.getWeeklyPlaytime() }
    val modelState = remember(weeklyPlaytime) {
        if (weeklyPlaytime.isNotEmpty()) {
            CartesianChartModel(
                LineCartesianLayerModel.build {
                    series(weeklyPlaytime)
                }
            )
        } else null
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
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        
        // Profile Header
        Surface(
            modifier = Modifier
                .size(120.dp)
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
                        modifier = Modifier.padding(32.dp),
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
                        Icon(androidx.compose.material.icons.Icons.Default.Edit, null, tint = Color.White)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (username.isBlank()) "Ace User" else username,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(enabled = isEditMode) { showUsernameDialog = true }
        )
        Text(
            text = "${allApps.size} Games Installed • ${pinnedGames.size} Pinned • ${formatPlayTime(viewModel.getTotalPlayTime())} Played",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { isEditMode = !isEditMode },
            colors = if (isEditMode) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                     else ButtonDefaults.buttonColors(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Icon(if (isEditMode) androidx.compose.material.icons.Icons.Default.Check else androidx.compose.material.icons.Icons.Default.Edit, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isEditMode) "Done" else "Edit Profile")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Section: Gaming Stats
        ReefSectionHeader("Gaming Statistics")
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
                label = "Games Played",
                value = "${recentGames.size}",
                icon = Icons.Rounded.SportsEsports,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        // Weekly Activity Graph
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
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
                modelState?.let { model ->
                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            rememberLineCartesianLayer(
                                lines = listOf(
                                    rememberLineSpec(
                                        shader = DynamicShader.color(MaterialTheme.colorScheme.primary),
                                        thickness = 3.dp
                                    )
                                )
                            ),
                            startAxis = rememberStartAxis(
                                label = rememberAxisLabelComponent(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textSize = 10.sp
                                ),
                                axis = null,
                                tick = null
                            ),
                            bottomAxis = rememberBottomAxis(
                                label = null,
                                axis = null,
                                tick = null
                            )
                        ),
                        model = model,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section: Pinned Games
        if (pinnedGames.isNotEmpty()) {
            ReefSectionHeader("Pinned Games")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    pinnedGames.forEach { game ->
                        ListItem(
                            headlineContent = { Text(game.appName) },
                            supportingContent = { Text("Pinned to library") },
                            leadingContent = { 
                                Image(
                                    painter = com.google.accompanist.drawablepainter.rememberDrawablePainter(game.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Section: Recent Games
        if (recentGames.isNotEmpty()) {
            ReefSectionHeader("Recent Games")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    recentGames.take(5).forEach { game ->
                        ListItem(
                            headlineContent = { Text(game.appName) },
                            supportingContent = { Text("Last played recently") },
                            leadingContent = { 
                                Image(
                                    painter = com.google.accompanist.drawablepainter.rememberDrawablePainter(game.icon),
                                    contentDescription = null,
                                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp))
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Section: Achievements Placeholder
        ReefSectionHeader("Achievements")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Unlock achievements by playing games!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Section: Links
        ReefSectionHeader("Links")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            Column {
                ListItem(
                    headlineContent = { Text("GitHub") },
                    supportingContent = { Text("@aceyash-dev") },
                    leadingContent = { 
                        Icon(
                            painter = painterResource(id = R.drawable.ic_github), 
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        ) 
                    },
                    trailingContent = { Icon(Icons.Rounded.ChevronRight, null) },
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/aceyash-dev"))
                        context.startActivity(intent)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                ListItem(
                    headlineContent = { Text("Play Store") },
                    supportingContent = { Text("Check for updates") },
                    leadingContent = { 
                        Icon(
                            painter = painterResource(id = R.drawable.ic_playstore), 
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        ) 
                    },
                    trailingContent = { Icon(Icons.Rounded.ChevronRight, null) },
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
                        context.startActivity(intent)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // About
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
        ) {
            ListItem(
                headlineContent = { Text("About AceHub") },
                supportingContent = { Text("Version 1.0.0 • ©Ace Horizon 2026") },
                leadingContent = { Icon(Icons.Rounded.Info, null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }

        Spacer(modifier = Modifier.height(120.dp))
    }

    if (showUsernameDialog) {
        AlertDialog(
            onDismissRequest = { showUsernameDialog = false },
            title = { Text("Edit Username") },
            text = {
                OutlinedTextField(
                    value = tempUsername,
                    onValueChange = { tempUsername = it },
                    label = { Text("Username") },
                    singleLine = true
                )
            },
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
            }
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
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ReefSectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, bottom = 8.dp)
    )
}
