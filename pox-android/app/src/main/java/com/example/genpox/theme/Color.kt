package com.example.genpox.theme

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object CyberTheme {
    var green by mutableStateOf(Color(0xFF00FF66))
    var greenDim by mutableStateOf(Color(0xFF00993C))
    var greenBorder by mutableStateOf(Color(0xFF0D2513))
    var greenPanel by mutableStateOf(Color(0xFF09120B))

    var purple by mutableStateOf(Color(0xFFA855F7))
    var purpleDim by mutableStateOf(Color(0xFF701A75))
    var purpleBorder by mutableStateOf(Color(0xFF4A125E))
    var purplePanel by mutableStateOf(Color(0xFF150B24))

    var cyan by mutableStateOf(Color(0xFF00E5FF))
    var cyanDim by mutableStateOf(Color(0xFF00A3B8))
    var cyanBorder by mutableStateOf(Color(0xFF00363A))
    var cyanPanel by mutableStateOf(Color(0xFF00181A))

    var red by mutableStateOf(Color(0xFFEF4444))
    var redDim by mutableStateOf(Color(0xFFB91C1C))
    var redBorder by mutableStateOf(Color(0xFF450A0A))
    var redPanel by mutableStateOf(Color(0xFF180202))

    var background by mutableStateOf(Color(0xFF060B07))
    var backgroundDark by mutableStateOf(Color(0xFF010201))

    fun resetToDefaults() {
        green = Color(0xFF00FF66)
        greenDim = Color(0xFF00993C)
        greenBorder = Color(0xFF0D2513)
        greenPanel = Color(0xFF09120B)

        purple = Color(0xFFA855F7)
        purpleDim = Color(0xFF701A75)
        purpleBorder = Color(0xFF4A125E)
        purplePanel = Color(0xFF150B24)

        cyan = Color(0xFF00E5FF)
        cyanDim = Color(0xFF00A3B8)
        cyanBorder = Color(0xFF00363A)
        cyanPanel = Color(0xFF00181A)

        red = Color(0xFFEF4444)
        redDim = Color(0xFFB91C1C)
        redBorder = Color(0xFF450A0A)
        redPanel = Color(0xFF180202)

        background = Color(0xFF060B07)
        backgroundDark = Color(0xFF010201)
    }

    fun savePreset(context: Context, presetName: String) {
        val prefs = context.getSharedPreferences("cyber_theme_presets", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("${presetName}_green", green.toArgb())
            putInt("${presetName}_greenDim", greenDim.toArgb())
            putInt("${presetName}_greenBorder", greenBorder.toArgb())
            putInt("${presetName}_greenPanel", greenPanel.toArgb())

            putInt("${presetName}_purple", purple.toArgb())
            putInt("${presetName}_purpleDim", purpleDim.toArgb())
            putInt("${presetName}_purpleBorder", purpleBorder.toArgb())
            putInt("${presetName}_purplePanel", purplePanel.toArgb())

            putInt("${presetName}_cyan", cyan.toArgb())
            putInt("${presetName}_cyanDim", cyanDim.toArgb())
            putInt("${presetName}_cyanBorder", cyanBorder.toArgb())
            putInt("${presetName}_cyanPanel", cyanPanel.toArgb())

            putInt("${presetName}_red", red.toArgb())
            putInt("${presetName}_redDim", redDim.toArgb())
            putInt("${presetName}_redBorder", redBorder.toArgb())
            putInt("${presetName}_redPanel", redPanel.toArgb())

            putInt("${presetName}_background", background.toArgb())
            putInt("${presetName}_backgroundDark", backgroundDark.toArgb())
            apply()
        }
    }

    fun loadPreset(context: Context, presetName: String): Boolean {
        val prefs = context.getSharedPreferences("cyber_theme_presets", Context.MODE_PRIVATE)
        if (!prefs.contains("${presetName}_green")) return false
        
        green = Color(prefs.getInt("${presetName}_green", green.toArgb()))
        greenDim = Color(prefs.getInt("${presetName}_greenDim", greenDim.toArgb()))
        greenBorder = Color(prefs.getInt("${presetName}_greenBorder", greenBorder.toArgb()))
        greenPanel = Color(prefs.getInt("${presetName}_greenPanel", greenPanel.toArgb()))

        purple = Color(prefs.getInt("${presetName}_purple", purple.toArgb()))
        purpleDim = Color(prefs.getInt("${presetName}_purpleDim", purpleDim.toArgb()))
        purpleBorder = Color(prefs.getInt("${presetName}_purpleBorder", purpleBorder.toArgb()))
        purplePanel = Color(prefs.getInt("${presetName}_purplePanel", purplePanel.toArgb()))

        cyan = Color(prefs.getInt("${presetName}_cyan", cyan.toArgb()))
        cyanDim = Color(prefs.getInt("${presetName}_cyanDim", cyanDim.toArgb()))
        cyanBorder = Color(prefs.getInt("${presetName}_cyanBorder", cyanBorder.toArgb()))
        cyanPanel = Color(prefs.getInt("${presetName}_cyanPanel", cyanPanel.toArgb()))

        red = Color(prefs.getInt("${presetName}_red", red.toArgb()))
        redDim = Color(prefs.getInt("${presetName}_redDim", redDim.toArgb()))
        redBorder = Color(prefs.getInt("${presetName}_redBorder", redBorder.toArgb()))
        redPanel = Color(prefs.getInt("${presetName}_redPanel", redPanel.toArgb()))

        background = Color(prefs.getInt("${presetName}_background", background.toArgb()))
        backgroundDark = Color(prefs.getInt("${presetName}_backgroundDark", backgroundDark.toArgb()))
        return true
    }
}

val CyberGreen: Color get() = CyberTheme.green
val CyberGreenDim: Color get() = CyberTheme.greenDim
val CyberCyan: Color get() = CyberTheme.cyan
val CyberCyanDim: Color get() = CyberTheme.cyanDim
val CyberBackground: Color get() = CyberTheme.background
val CyberBackgroundDark: Color get() = CyberTheme.backgroundDark
val CyberPanel: Color get() = CyberTheme.greenPanel
val CyberBorder: Color get() = CyberTheme.greenBorder


val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
