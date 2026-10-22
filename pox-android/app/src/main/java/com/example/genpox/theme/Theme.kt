package com.example.genpox.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun GENPOXTheme(
    cyberColors: CyberColors = CyberColors(),
    content: @Composable () -> Unit,
) {
    val darkColorScheme = darkColorScheme(
        primary = cyberColors.green,
        onPrimary = Color.Black,
        secondary = cyberColors.greenDim,
        onSecondary = Color.Black,
        background = cyberColors.background,
        onBackground = cyberColors.green,
        surface = cyberColors.greenPanel,
        onSurface = cyberColors.green,
    )

    CompositionLocalProvider(LocalCyberColors provides cyberColors) {
        MaterialTheme(
            colorScheme = darkColorScheme,
            typography = Typography,
            content = content,
        )
    }
}
