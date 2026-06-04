package com.ace.hub

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ace.hub.ui.MainViewModel
import com.ace.hub.ui.MeScreen
import com.ace.hub.ui.games.PlayScreen
import com.ace.hub.ui.theme.AceHubTheme

@Composable
fun Navigation(viewModel: MainViewModel) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var accentColor by remember { mutableStateOf<Color?>(null) }
    var useSystemTheme by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("User") }

    AceHubTheme(darkTheme = if (useSystemTheme) isSystemInDarkTheme() else false, customAccent = accentColor) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.height(64.dp),
                    tonalElevation = 0.dp,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("Play") },
                        icon = { Icon(Icons.Default.SportsEsports, null) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("Me") },
                        icon = { Icon(Icons.Default.AccountCircle, null) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer)
                    )
                }
            }
        ) { innerPadding ->
            // Improved navigation transition: slide expand style
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    val slideDirection = if (targetState > initialState) AnimatedContentTransitionScope.SlideDirection.Left else AnimatedContentTransitionScope.SlideDirection.Right
                    slideIntoContainer(towards = slideDirection, animationSpec = tween(500)) + expandHorizontally(expandFrom = androidx.compose.ui.Alignment.CenterHorizontally) togetherWith
                            slideOutOfContainer(towards = slideDirection, animationSpec = tween(500)) + shrinkHorizontally(shrinkTowards = androidx.compose.ui.Alignment.CenterHorizontally)
                },
                label = "tab_transition"
            ) { targetTab ->
                if (targetTab == 0) {
                    PlayScreen(
                        viewModel = viewModel, 
                        modifier = Modifier.padding(innerPadding),
                        username = username
                    )
                } else {
                    MeScreen(
                        onAccentColorChanged = { accentColor = it },
                        useSystemTheme = useSystemTheme,
                        onUseSystemThemeChanged = { useSystemTheme = it },
                        username = username,
                        onUsernameChanged = { username = it }
                    )
                }
            }
        }
    }
}
