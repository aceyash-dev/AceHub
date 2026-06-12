package com.ace.hub

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.ace.hub.ui.BootScreen
import com.ace.hub.ui.MainViewModel
import com.ace.hub.ui.MeScreen
import com.ace.hub.ui.SettingsScreen
import com.ace.hub.ui.games.PlayScreen
import com.ace.hub.ui.theme.AceHubTheme
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Navigation(
    viewModel: MainViewModel,
    onCheckPermissions: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val useSystemTheme by viewModel.useSystemTheme.collectAsState()
    val username by viewModel.username.collectAsState()
    val isUsageAnalyticsEnabled by viewModel.isUsageAnalyticsEnabled.collectAsState()
    val isOverlayEnabled by viewModel.isOverlayEnabled.collectAsState()
    val isAutoBoostEnabled by viewModel.isAutoBoostEnabled.collectAsState()
    val bootFinished by viewModel.bootFinished.collectAsState()

    LaunchedEffect(bootFinished) {
        if (bootFinished) {
            onCheckPermissions()
        }
    }

    val pagerState = rememberPagerState(pageCount = { 2 })
    var showMeScreen by remember { mutableStateOf(false) }

    val pfpFile = File(context.filesDir, "profile_pic.jpg")
    val pfpPainter = rememberAsyncImagePainter(if (pfpFile.exists()) pfpFile else null)

    AceHubTheme(
        darkTheme = if (useSystemTheme) isSystemInDarkTheme() else true,
        useSystemTheme = useSystemTheme
    ) {
        AnimatedContent(
            targetState = bootFinished,
            transitionSpec = {
                fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600))
            },
            label = "BootToMainTransition"
        ) { finished ->
            if (!finished) {
                BootScreen(viewModel = viewModel)
            } else {
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
                                        fontWeight = FontWeight.Bold
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
                                shape = RoundedCornerShape(32.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                tonalElevation = 3.dp,
                                modifier = Modifier
                                    .padding(horizontal = 48.dp, vertical = 12.dp)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(32.dp))
                            ) {
                                NavigationBar(
                                    modifier = Modifier.fillMaxSize(),
                                    containerColor = Color.Transparent,
                                    windowInsets = WindowInsets(0, 0, 0, 0)
                                ) {
                                    NavigationBarItem(
                                        selected = pagerState.currentPage == 0,
                                        onClick = { 
                                            scope.launch { pagerState.animateScrollToPage(0) }
                                        },
                                        label = { Text("Play", style = MaterialTheme.typography.labelSmall) },
                                        icon = { 
                                            val iconScale by animateFloatAsState(
                                                targetValue = if (pagerState.currentPage == 0) 1.2f else 1f,
                                                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
                                                label = "icon_scale_play"
                                            )
                                            Icon(
                                                if (pagerState.currentPage == 0) Icons.Filled.SportsEsports else Icons.Outlined.SportsEsports, 
                                                null,
                                                modifier = Modifier.size(20.dp).graphicsLayer(scaleX = iconScale, scaleY = iconScale)
                                            ) 
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                    NavigationBarItem(
                                        selected = pagerState.currentPage == 1,
                                        onClick = { 
                                            scope.launch { pagerState.animateScrollToPage(1) }
                                        },
                                        label = { Text("Settings", style = MaterialTheme.typography.labelSmall) },
                                        icon = { 
                                            val iconScale by animateFloatAsState(
                                                targetValue = if (pagerState.currentPage == 1) 1.2f else 1f,
                                                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
                                                label = "icon_scale_settings"
                                            )
                                            Icon(
                                                if (pagerState.currentPage == 1) Icons.Filled.Settings else Icons.Outlined.Settings, 
                                                null,
                                                modifier = Modifier.size(20.dp).graphicsLayer(scaleX = iconScale, scaleY = iconScale)
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    MeScreen(
                                        viewModel = viewModel,
                                        onNavigateToSettings = {
                                            scope.launch {
                                                showMeScreen = false
                                                pagerState.animateScrollToPage(1)
                                            }
                                        }
                                    )
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
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize(),
                                    beyondViewportPageCount = 1
                                ) { page ->
                                    if (page == 0) {
                                        PlayScreen(
                                            viewModel = viewModel, 
                                            username = username
                                        )
                                    } else {
                                        val showBatteryStats by viewModel.showBatteryStats.collectAsState()
                                        val vibrationOnLaunch by viewModel.vibrationOnLaunch.collectAsState()
                                        val autoDnd by viewModel.autoDnd.collectAsState()
                                        val brightnessLock by viewModel.brightnessLock.collectAsState()
                                        val isDashboardEnabled by viewModel.isDashboardEnabled.collectAsState()

                                        SettingsScreen(
                                            useSystemTheme = useSystemTheme,
                                            onUseSystemThemeChanged = { viewModel.updateSystemTheme(it) },
                                            isUsageAnalyticsEnabled = isUsageAnalyticsEnabled,
                                            onUsageAnalyticsEnabledChanged = { viewModel.updateUsageAnalytics(it) },
                                            hasUsagePermission = viewModel.hasUsagePermission(),
                                            isOverlayEnabled = isOverlayEnabled,
                                            onOverlayEnabledChanged = { viewModel.updateOverlayEnabled(it) },
                                            isAutoBoostEnabled = isAutoBoostEnabled,
                                            onAutoBoostEnabledChanged = { viewModel.updateAutoBoost(it) },
                                            showBatteryStats = showBatteryStats,
                                            onShowBatteryStatsChanged = { viewModel.updateShowBatteryStats(it) },
                                            vibrationOnLaunch = vibrationOnLaunch,
                                            onVibrationOnLaunchChanged = { viewModel.updateVibrationOnLaunch(it) },
                                            autoDnd = autoDnd,
                                            onAutoDndChanged = { viewModel.updateAutoDnd(it) },
                                            brightnessLock = brightnessLock,
                                            onBrightnessLockChanged = { viewModel.updateBrightnessLock(it) },
                                            isDashboardEnabled = isDashboardEnabled,
                                            onDashboardEnabledChanged = { viewModel.updateDashboardEnabled(it) }
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
}
