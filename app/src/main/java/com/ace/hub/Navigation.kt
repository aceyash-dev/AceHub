package com.ace.hub

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.ace.hub.ui.MainViewModel
import com.ace.hub.ui.MeScreen
import com.ace.hub.ui.SettingsScreen
import com.ace.hub.ui.games.PlayScreen
import com.ace.hub.ui.theme.AceHubTheme
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Navigation(viewModel: MainViewModel) {
    val context = LocalContext.current
    val useSystemTheme by viewModel.useSystemTheme.collectAsState()
    val username by viewModel.username.collectAsState()
    val customSeedColor by viewModel.customSeedColor.collectAsState()
    val isUsageAnalyticsEnabled by viewModel.isUsageAnalyticsEnabled.collectAsState()
    val isOverlayEnabled by viewModel.isOverlayEnabled.collectAsState()
    val isAutoBoostEnabled by viewModel.isAutoBoostEnabled.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showMeScreen by remember { mutableStateOf(false) }

    val pfpFile = File(context.filesDir, "profile_pic.jpg")
    val pfpPainter = rememberAsyncImagePainter(if (pfpFile.exists()) pfpFile else null)

    AceHubTheme(
        darkTheme = if (useSystemTheme) isSystemInDarkTheme() else true,
        useSystemTheme = useSystemTheme,
        customSeedColor = Color(customSeedColor)
    ) {
        Scaffold(
            topBar = {
                AnimatedVisibility(
                    visible = !showMeScreen,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                "AceHub",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        },
                        actions = {
                            Surface(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable { showMeScreen = true },
                                color = MaterialTheme.colorScheme.primaryContainer,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            ) {
                                if (pfpFile.exists()) {
                                    Image(
                                        painter = pfpPainter,
                                        contentDescription = "Me",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = "Me",
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                        )
                    )
                }
            },
            bottomBar = {
                AnimatedVisibility(
                    visible = !showMeScreen,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 8.dp,
                        modifier = Modifier.clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    ) {
                        NavigationBar(
                            modifier = Modifier.height(84.dp),
                            containerColor = Color.Transparent
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                label = { Text("Play", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium) },
                                icon = { 
                                    Icon(
                                        if (selectedTab == 0) Icons.Default.SportsEsports else Icons.Default.SportsEsports, 
                                        null 
                                    ) 
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                label = { Text("Settings", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium) },
                                icon = { 
                                    Icon(
                                        if (selectedTab == 1) Icons.Default.Settings else Icons.Default.Settings, 
                                        null 
                                    ) 
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                AnimatedContent(
                    targetState = showMeScreen,
                    transitionSpec = {
                        if (targetState) {
                            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(300)) + fadeIn() togetherWith
                                    fadeOut(animationSpec = tween(200))
                        } else {
                            fadeIn(animationSpec = tween(200)) togetherWith
                                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(300)) + fadeOut()
                        }
                    },
                    label = "me_screen_transition"
                ) { isMeScreen ->
                    if (isMeScreen) {
                        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                            MeScreen(viewModel = viewModel)
                            FilledIconButton(
                                onClick = { showMeScreen = false },
                                modifier = Modifier
                                    .padding(16.dp)
                                    .align(Alignment.TopStart),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.8f)
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                            }
                        }
                    } else {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                val direction = if (targetState > initialState) AnimatedContentTransitionScope.SlideDirection.Left else AnimatedContentTransitionScope.SlideDirection.Right
                                slideIntoContainer(direction, animationSpec = tween(300)) + fadeIn() togetherWith
                                        slideOutOfContainer(direction, animationSpec = tween(300)) + fadeOut()
                            },
                            label = "tab_transition"
                        ) { targetTab ->
                            if (targetTab == 0) {
                                PlayScreen(
                                    viewModel = viewModel, 
                                    username = username
                                )
                            } else {
                                    SettingsScreen(
                                        username = username,
                                        onUsernameChanged = { viewModel.updateUsername(it) },
                                        useSystemTheme = useSystemTheme,
                                        onUseSystemThemeChanged = { viewModel.updateSystemTheme(it) },
                                        customSeedColor = customSeedColor,
                                        onCustomSeedColorChanged = { viewModel.updateCustomSeedColor(it) },
                                        isUsageAnalyticsEnabled = isUsageAnalyticsEnabled,
                                        onUsageAnalyticsEnabledChanged = { viewModel.updateUsageAnalytics(it) },
                                        hasUsagePermission = viewModel.hasUsagePermission(),
                                        isOverlayEnabled = isOverlayEnabled,
                                        onOverlayEnabledChanged = { viewModel.updateOverlayEnabled(it) },
                                        isAutoBoostEnabled = isAutoBoostEnabled,
                                        onAutoBoostEnabledChanged = { viewModel.updateAutoBoost(it) }
                                    )
                            }
                        }
                    }
                }
            }
        }
    }
}
