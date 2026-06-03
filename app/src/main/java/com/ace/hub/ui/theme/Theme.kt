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
    dynamicColor: Boolean = true,
    customAccent: Color? = null,
    content: @Composable () -> Unit
) {
    // Generate a random palette if dynamic colors are unavailable or disabled
    val randomPrimary = remember {
        val colors = listOf(Color(0xFF6200EE), Color(0xFF03DAC6), Color(0xFFE91E63), Color(0xFF4CAF50))
        colors[Random.nextInt(colors.size)]
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkColorScheme(primary = customAccent ?: randomPrimary)
        else -> lightColorScheme(primary = customAccent ?: randomPrimary)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
