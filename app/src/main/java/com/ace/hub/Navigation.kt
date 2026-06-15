package com.ace.hub

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import coil.compose.AsyncImage
import com.ace.hub.ui.BootScreen
import com.ace.hub.ui.MainViewModel
import com.ace.hub.ui.MeScreen
import com.ace.hub.ui.SettingsScreen
import com.ace.hub.ui.games.PlayScreen
import com.ace.hub.ui.theme.AceHubTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Navigation(
    viewModel: MainViewModel,
    onCheckPermissions: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val useSystemTheme by viewModel.useSystemTheme.collectAsState()
    val bootFinished by viewModel.bootFinished.collectAsState()

    // 0: Play, 1: Me, 2: Settings
    val pagerState = rememberPagerState(pageCount = { 3 })
    val profilePhotoUrl by viewModel.profileImageUri.collectAsState()

    var backPressedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            delay(2000)
            backPressedOnce = false
        }
    }

    BackHandler {
        if (pagerState.currentPage != 0) {
            scope.launch { 
                val target = if (pagerState.currentPage == 2) 1 else 0
                pagerState.animateScrollToPage(target) 
            }
        } else {
            if (backPressedOnce) {
                (context as? android.app.Activity)?.finish()
            } else {
                backPressedOnce = true
                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(bootFinished) {
        if (bootFinished) {
            onCheckPermissions()
        }
    }

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
                        TopAppBar(
                            navigationIcon = {
                                if (pagerState.currentPage != 0) {
                                    IconButton(
                                        onClick = {
                                            scope.launch { 
                                                val target = if (pagerState.currentPage == 2) 1 else 0
                                                pagerState.animateScrollToPage(target) 
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                                    }
                                }
                            },
                            title = {
                                Text(
                                    text = when(pagerState.currentPage) {
                                        0 -> "AceHub"
                                        1 -> "Me"
                                        else -> "Settings"
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            actions = {
                                if (pagerState.currentPage == 0) {
                                    IconButton(
                                        onClick = {
                                            scope.launch { 
                                                pagerState.animateScrollToPage(1)
                                            }
                                        }
                                    ) {
                                        if (profilePhotoUrl != null) {
                                            Surface(
                                                modifier = Modifier.size(38.dp),
                                                shape = CircleShape,
                                                tonalElevation = 4.dp
                                            ) {
                                                AsyncImage(
                                                    model = profilePhotoUrl,
                                                    contentDescription = "Profile",
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        } else {
                                            Surface(
                                                modifier = Modifier.size(38.dp),
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.Person,
                                                        contentDescription = "Me"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
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
                                    selected = pagerState.currentPage >= 1, // Me or Settings
                                    onClick = { 
                                        scope.launch { 
                                            pagerState.animateScrollToPage(2)
                                        }
                                    },
                                    label = { Text("Settings", style = MaterialTheme.typography.labelSmall) },
                                    icon = { 
                                        val iconScale by animateFloatAsState(
                                            targetValue = if (pagerState.currentPage >= 1) 1.2f else 1f,
                                            animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
                                            label = "icon_scale_settings"
                                        )
                                        Icon(
                                            if (pagerState.currentPage >= 1) Icons.Filled.Settings else Icons.Outlined.Settings, 
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
                ) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1,
                            userScrollEnabled = false // Control navigation strictly via UI
                        ) { page ->
                            when(page) {
                                0 -> {
                                    PlayScreen(viewModel = viewModel)
                                }
                                1 -> {
                                    MeScreen(
                                        viewModel = viewModel,
                                        onNavigateToSettings = {
                                            scope.launch { pagerState.animateScrollToPage(2) }
                                        }
                                    )
                                }
                                2 -> {
                                    val isOverlayEnabled by viewModel.isOverlayEnabled.collectAsState()
                                    val isAutoBoostEnabled by viewModel.isAutoBoostEnabled.collectAsState()
                                    val autoDnd by viewModel.autoDnd.collectAsState()
                                    val brightnessLock by viewModel.brightnessLock.collectAsState()

                                    SettingsScreen(
                                        useSystemTheme = useSystemTheme,
                                        onUseSystemThemeChanged = { viewModel.updateSystemTheme(it) },
                                        isOverlayEnabled = isOverlayEnabled,
                                        onOverlayEnabledChanged = { viewModel.updateOverlayEnabled(it) },
                                        isAutoBoostEnabled = isAutoBoostEnabled,
                                        onAutoBoostEnabledChanged = { viewModel.updateAutoBoost(it) },
                                        autoDnd = autoDnd,
                                        onAutoDndChanged = { viewModel.updateAutoDnd(it) },
                                        brightnessLock = brightnessLock,
                                        onBrightnessLockChanged = { viewModel.updateBrightnessLock(it) }
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
