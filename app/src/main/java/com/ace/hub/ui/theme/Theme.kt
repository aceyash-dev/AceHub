package com.ace.hub.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import kotlin.random.Random

@Composable
fun AceHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useSystemTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val purpleSeed = Color(0xFF6750A4)
    
    val colorScheme = when {
        useSystemTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFD0BCFF),
                    primaryContainer = Color(0xFF4F378B),
                    surface = Color(0xFF1C1B1F),
                    onPrimary = Color(0xFF381E72),
                    onPrimaryContainer = Color(0xFFEADDFF),
                    secondary = Color(0xFFCCC2DC),
                    onSecondary = Color(0xFF332D41),
                    secondaryContainer = Color(0xFF4A4458),
                    onSecondaryContainer = Color(0xFFE8DEF8)
                )
            } else {
                lightColorScheme(
                    primary = purpleSeed,
                    primaryContainer = Color(0xFFEADDFF),
                    surface = Color(0xFFFEF7FF),
                    onPrimary = Color.White,
                    onPrimaryContainer = Color(0xFF21005D),
                    secondary = Color(0xFF625B71),
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFFE8DEF8),
                    onSecondaryContainer = Color(0xFF1D192B)
                )
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
