package com.example.genpox.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CyberGreen,
    onPrimary = Color.Black,
    secondary = CyberGreenDim,
    onSecondary = Color.Black,
    background = CyberBackground,
    onBackground = CyberGreen,
    surface = CyberPanel,
    onSurface = CyberGreen
)

@Composable
fun GENPOXTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

