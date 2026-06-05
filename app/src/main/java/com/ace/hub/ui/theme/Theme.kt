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
    customSeedColor: Color = Color(0xFF6750A4),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        useSystemTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            if (darkTheme) {
                androidx.compose.material3.darkColorScheme().copy(
                    primary = customSeedColor
                    // In a real M3 setup we'd use ColorScheme.fromSeed but Compose M3 
                    // doesn't have a direct static method that returns a full ColorScheme 
                    // from a seed in all versions easily without extra logic.
                    // However, we can use the internal BuildColorPalette logic or just 
                    // generate it. 
                    // For simplicity and correctness with M3, we should use the 
                    // dynamicColor APIs or a library. 
                    // But we can approximate it or use the M3 Palette generation.
                )
                // Better way to generate M3 palette from seed in Compose:
                // We'll use the Material 3 dynamic color utility if available or 
                // just manually define it for now to be safe with the "Strictly Purple" 
                // previous requirement if seed is default.
                
                // Let's use a simpler approach for now to ensure it works.
                if (customSeedColor == Color(0xFF6750A4)) {
                    // Default Purple Dark
                    darkColorScheme(
                        primary = Color(0xFFD0BCFF),
                        primaryContainer = Color(0xFF4F378B),
                        surface = Color(0xFF1C1B1F)
                    )
                } else {
                    // Custom Seed Dark
                    darkColorScheme(primary = customSeedColor)
                }
            } else {
                if (customSeedColor == Color(0xFF6750A4)) {
                    // Default Purple Light
                    lightColorScheme(
                        primary = Color(0xFF6750A4),
                        primaryContainer = Color(0xFFEADDFF),
                        surface = Color(0xFFFEF7FF)
                    )
                } else {
                    // Custom Seed Light
                    lightColorScheme(primary = customSeedColor)
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
