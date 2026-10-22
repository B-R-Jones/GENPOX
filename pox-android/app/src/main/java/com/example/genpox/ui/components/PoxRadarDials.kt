package com.example.genpox.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.genpox.ui.main.MainViewModel
import com.example.genpox.ui.main.ScannerRadarState

/**
 * Tactical Radar HoloNav Dials & Siphoning HUD.
 *
 * Provides proximity siphoning telemetry, North/Recenter alignment,
 * and a unified Circular 2D / 3D Mode Toggle Disc.
 */
@Composable
fun PoxRadarDials(
    state: ScannerRadarState,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val activeSiphonFont by viewModel.activeSiphonedFont.collectAsStateWithLifecycle()
    val siphonedRate by viewModel.siphonedRate.collectAsStateWithLifecycle()

    // Base bottom offset floats 8.dp above PoxHoloNav top boundary
    val baseBottom = 202.dp

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Proximity Siphoning Banner
        if (activeSiphonFont != null && siphonedRate > 0) {
            val font = activeSiphonFont!!
            val baseName = when (font.baseType) {
                'A' -> "ADENINE (A)"
                'G' -> "GUANINE (G)"
                'T' -> "THYMINE (T)"
                'C' -> "CYTOSINE (C)"
                else -> "UNKNOWN"
            }
            val baseColor = when (font.baseType) {
                'A' -> Color(0xFF38BDF8)
                'G' -> Color(0xFF34D399)
                'T' -> Color(0xFFFB7185)
                'C' -> Color(0xFFFBBF24)
                else -> Color.Cyan
            }
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 60.dp)
                    .border(1.dp, baseColor.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PROXIMITY SIPHONING ACTIVE",
                        color = baseColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "GATHER RATE: +$siphonedRate UNITS/SEC -> $baseName",
                        color = Color.White,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }

        // 2. North / Recenter Reset Button (Aligned bottom-end)
        NorthResetButton(
            onReset = {
                viewModel.synthManager.playBeep(880f, 0.08f, "sine")
                state.resetToNorthTop()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = baseBottom, end = 76.dp),
        )

        // 3. Circular 2D / 3D HoloNav Mode Toggle
        HoloNav2D3DToggle(
            is3DMode = state.is3DMode,
            onToggle = {
                viewModel.synthManager.playBeep(if (state.is3DMode) 440f else 880f, 0.05f, "sine")
                state.toggle2D3D()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = baseBottom, end = 20.dp),
        )
    }
}

/**
 * Circular 2D / 3D Mode Toggle Disc.
 *
 * Toggles between locked 2D Top-Down perspective (90° tilt, North rotation)
 * and full 3D Isometric perspective (45° tilt).
 */
@Composable
fun HoloNav2D3DToggle(
    is3DMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cyanColor = Color(0xFF00E5FF)
    val cyberGreen = Color(0xFF00FF66)
    val neonAmber = Color(0xFFFFB300)

    val activeColor = if (is3DMode) neonAmber else cyanColor
    val modeText = if (is3DMode) "3D" else "2D"
    val subText = if (is3DMode) "ISO" else "TOP"

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.75f))
            .border(1.5.dp, activeColor.copy(alpha = 0.85f), CircleShape)
            .clickable { onToggle() },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 1.dp.toPx()
            val radius = size.minDimension / 2f - 3.dp.toPx()
            drawCircle(
                color = activeColor.copy(alpha = 0.25f),
                radius = radius,
                style = Stroke(width = strokeWidth),
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = modeText,
                color = activeColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                lineHeight = 13.sp,
            )
            Text(
                text = subText,
                color = if (is3DMode) Color.White.copy(alpha = 0.8f) else cyberGreen.copy(alpha = 0.8f),
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                lineHeight = 7.sp,
            )
        }
    }
}

/**
 * North / Orientation Reset Button.
 *
 * Snaps camera rotation to North (0°) and centers panning on the player.
 */
@Composable
fun NorthResetButton(
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cyberGreen = Color(0xFF00FF66)
    val cyanColor = Color(0xFF00E5FF)

    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.75f))
            .border(1.dp, cyanColor.copy(alpha = 0.8f), CircleShape)
            .clickable { onReset() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "▲",
                color = cyberGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                lineHeight = 10.sp,
            )
            Text(
                text = "N",
                color = cyanColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                lineHeight = 11.sp,
            )
        }
    }
}
