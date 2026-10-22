package com.example.genpox.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

@Immutable
data class CyberColors(
    val green: Color = Color(0xFF00FF66),
    val greenDim: Color = Color(0xFF00993C),
    val greenBorder: Color = Color(0xFF0D2513),
    val greenPanel: Color = Color(0xFF09120B),

    val purple: Color = Color(0xFFA855F7),
    val purpleDim: Color = Color(0xFF701A75),
    val purpleBorder: Color = Color(0xFF4A125E),
    val purplePanel: Color = Color(0xFF150B24),

    val cyan: Color = Color(0xFF00E5FF),
    val cyanDim: Color = Color(0xFF00A3B8),
    val cyanBorder: Color = Color(0xFF00363A),
    val cyanPanel: Color = Color(0xFF00181A),

    val yellow: Color = Color(0xFFFBBF24),
    val orange: Color = Color(0xFFF97316),

    val red: Color = Color(0xFFEF4444),
    val redDim: Color = Color(0xFFB91C1C),
    val redBorder: Color = Color(0xFF450A0A),
    val redPanel: Color = Color(0xFF180202),

    val background: Color = Color(0xFF060B07),
    val backgroundDark: Color = Color(0xFF010201),

    val crtScanlineOpacity: Float = 0.22f,
    val crtGrilleOpacity: Float = 0.12f,
    val crtBloomIntensity: Float = 0.25f,
    val crtCurvatureBezel: Float = 0.04f,
    val crtGlitchIntensity: Float = 0.05f,
)

val LocalCyberColors = staticCompositionLocalOf { CyberColors() }

object CyberTheme {
    var colorsState by mutableStateOf(CyberColors())

    val colors: CyberColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCyberColors.current

    val green: Color get() = colorsState.green
    val greenDim: Color get() = colorsState.greenDim
    val greenBorder: Color get() = colorsState.greenBorder
    val greenPanel: Color get() = colorsState.greenPanel

    val purple: Color get() = colorsState.purple
    val purpleDim: Color get() = colorsState.purpleDim
    val purpleBorder: Color get() = colorsState.purpleBorder
    val purplePanel: Color get() = colorsState.purplePanel

    val cyan: Color get() = colorsState.cyan
    val cyanDim: Color get() = colorsState.cyanDim
    val cyanBorder: Color get() = colorsState.cyanBorder
    val cyanPanel: Color get() = colorsState.cyanPanel

    val yellow: Color get() = colorsState.yellow
    val orange: Color get() = colorsState.orange

    val red: Color get() = colorsState.red
    val redDim: Color get() = colorsState.redDim
    val redBorder: Color get() = colorsState.redBorder
    val redPanel: Color get() = colorsState.redPanel

    val background: Color get() = colorsState.background
    val backgroundDark: Color get() = colorsState.backgroundDark

    val crtScanlineOpacity: Float get() = colorsState.crtScanlineOpacity
    val crtGrilleOpacity: Float get() = colorsState.crtGrilleOpacity
    val crtBloomIntensity: Float get() = colorsState.crtBloomIntensity
    val crtCurvatureBezel: Float get() = colorsState.crtCurvatureBezel
    val crtGlitchIntensity: Float get() = colorsState.crtGlitchIntensity

    fun update(transform: CyberColors.() -> CyberColors) {
        colorsState = colorsState.transform()
    }

    fun resetToDefaults() {
        colorsState = CyberColors()
    }

    fun savePreset(context: Context, presetName: String) {
        val prefs = context.getSharedPreferences("cyber_theme_presets", Context.MODE_PRIVATE)
        val c = colorsState
        prefs.edit().apply {
            putInt("${presetName}_green", c.green.toArgb())
            putInt("${presetName}_greenDim", c.greenDim.toArgb())
            putInt("${presetName}_greenBorder", c.greenBorder.toArgb())
            putInt("${presetName}_greenPanel", c.greenPanel.toArgb())

            putInt("${presetName}_purple", c.purple.toArgb())
            putInt("${presetName}_purpleDim", c.purpleDim.toArgb())
            putInt("${presetName}_purpleBorder", c.purpleBorder.toArgb())
            putInt("${presetName}_purplePanel", c.purplePanel.toArgb())

            putInt("${presetName}_cyan", c.cyan.toArgb())
            putInt("${presetName}_cyanDim", c.cyanDim.toArgb())
            putInt("${presetName}_cyanBorder", c.cyanBorder.toArgb())
            putInt("${presetName}_cyanPanel", c.cyanPanel.toArgb())

            putInt("${presetName}_red", c.red.toArgb())
            putInt("${presetName}_redDim", c.redDim.toArgb())
            putInt("${presetName}_redBorder", c.redBorder.toArgb())
            putInt("${presetName}_redPanel", c.redPanel.toArgb())

            putInt("${presetName}_background", c.background.toArgb())
            putInt("${presetName}_backgroundDark", c.backgroundDark.toArgb())

            putFloat("${presetName}_crtScanlineOpacity", c.crtScanlineOpacity)
            putFloat("${presetName}_crtGrilleOpacity", c.crtGrilleOpacity)
            putFloat("${presetName}_crtBloomIntensity", c.crtBloomIntensity)
            putFloat("${presetName}_crtCurvatureBezel", c.crtCurvatureBezel)
            putFloat("${presetName}_crtGlitchIntensity", c.crtGlitchIntensity)
            apply()
        }
    }

    fun loadPreset(context: Context, presetName: String): Boolean {
        val prefs = context.getSharedPreferences("cyber_theme_presets", Context.MODE_PRIVATE)
        if (!prefs.contains("${presetName}_green")) return false
        val defaultColors = CyberColors()
        colorsState = CyberColors(
            green = Color(prefs.getInt("${presetName}_green", defaultColors.green.toArgb())),
            greenDim = Color(prefs.getInt("${presetName}_greenDim", defaultColors.greenDim.toArgb())),
            greenBorder = Color(prefs.getInt("${presetName}_greenBorder", defaultColors.greenBorder.toArgb())),
            greenPanel = Color(prefs.getInt("${presetName}_greenPanel", defaultColors.greenPanel.toArgb())),
            purple = Color(prefs.getInt("${presetName}_purple", defaultColors.purple.toArgb())),
            purpleDim = Color(prefs.getInt("${presetName}_purpleDim", defaultColors.purpleDim.toArgb())),
            purpleBorder = Color(prefs.getInt("${presetName}_purpleBorder", defaultColors.purpleBorder.toArgb())),
            purplePanel = Color(prefs.getInt("${presetName}_purplePanel", defaultColors.purplePanel.toArgb())),
            cyan = Color(prefs.getInt("${presetName}_cyan", defaultColors.cyan.toArgb())),
            cyanDim = Color(prefs.getInt("${presetName}_cyanDim", defaultColors.cyanDim.toArgb())),
            cyanBorder = Color(prefs.getInt("${presetName}_cyanBorder", defaultColors.cyanBorder.toArgb())),
            cyanPanel = Color(prefs.getInt("${presetName}_cyanPanel", defaultColors.cyanPanel.toArgb())),
            red = Color(prefs.getInt("${presetName}_red", defaultColors.red.toArgb())),
            redDim = Color(prefs.getInt("${presetName}_redDim", defaultColors.redDim.toArgb())),
            redBorder = Color(prefs.getInt("${presetName}_redBorder", defaultColors.redBorder.toArgb())),
            redPanel = Color(prefs.getInt("${presetName}_redPanel", defaultColors.redPanel.toArgb())),
            background = Color(prefs.getInt("${presetName}_background", defaultColors.background.toArgb())),
            backgroundDark = Color(prefs.getInt("${presetName}_backgroundDark", defaultColors.backgroundDark.toArgb())),
            crtScanlineOpacity = prefs.getFloat("${presetName}_crtScanlineOpacity", defaultColors.crtScanlineOpacity),
            crtGrilleOpacity = prefs.getFloat("${presetName}_crtGrilleOpacity", defaultColors.crtGrilleOpacity),
            crtBloomIntensity = prefs.getFloat("${presetName}_crtBloomIntensity", defaultColors.crtBloomIntensity),
            crtCurvatureBezel = prefs.getFloat("${presetName}_crtCurvatureBezel", defaultColors.crtCurvatureBezel),
            crtGlitchIntensity = prefs.getFloat("${presetName}_crtGlitchIntensity", defaultColors.crtGlitchIntensity),
        )
        return true
    }
}

val CyberGreen: Color get() = CyberTheme.colorsState.green
val CyberGreenDim: Color get() = CyberTheme.colorsState.greenDim
val CyberCyan: Color get() = CyberTheme.colorsState.cyan
val CyberCyanDim: Color get() = CyberTheme.colorsState.cyanDim
val CyberRed: Color get() = CyberTheme.colorsState.red
val CyberRedDim: Color get() = CyberTheme.colorsState.redDim
val CyberRedBorder: Color get() = CyberTheme.colorsState.redBorder
val CyberRedPanel: Color get() = CyberTheme.colorsState.redPanel
val CyberBackground: Color get() = CyberTheme.colorsState.background
val CyberBackgroundDark: Color get() = CyberTheme.colorsState.backgroundDark
val CyberPanel: Color get() = CyberTheme.colorsState.greenPanel
val CyberBorder: Color get() = CyberTheme.colorsState.greenBorder
val CyberOrange: Color get() = CyberTheme.colorsState.orange

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
