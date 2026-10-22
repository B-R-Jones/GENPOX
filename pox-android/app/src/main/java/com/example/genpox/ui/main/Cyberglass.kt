package com.example.genpox.ui.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.genpox.theme.*
import com.example.genpox.ui.components.*
import com.google.maps.android.compose.*
import java.util.*

@Composable
fun getActiveReagentColor(selectedReagent: String?): Color = selectedReagent?.let { rId ->
    when (rId) {
        "REAGENT_LIGASE" -> Color(0xFF00FF66)
        "REAGENT_BETAINE" -> Color(0xFFFBBF24)
        "REAGENT_FORMAMIDE" -> Color(0xFFA855F7)
        "REAGENT_TU" -> Color(0xFFEF4444)
        "REAGENT_STAT_VITALITY" -> Color(0xFF38BDF8)
        "REAGENT_STAT_ATTACK" -> Color(0xFF34D399)
        "REAGENT_STAT_DEFENSE" -> Color(0xFFFB7185)
        "REAGENT_STAT_SPEED" -> Color(0xFFFBBF24)
        "REAGENT_ALIGN_CONTAINMENT" -> Color(0xFF38BDF8)
        "REAGENT_ALIGN_INFECTION" -> Color(0xFF34D399)
        "REAGENT_ALIGN_MECH" -> Color(0xFFFB7185)
        "REAGENT_ALIGN_PARASITE" -> Color(0xFFFBBF24)
        "REAGENT_TACTICAL_HACK" -> Color(0xFF84CC16)
        "REAGENT_TACTICAL_DEFEND" -> Color(0xFFCBD5E1)
        "REAGENT_TACTICAL_TEMP" -> Color(0xFFEC4899)
        "REAGENT_VISUAL_GLOW" -> Color(0xFF00FF66)
        "REAGENT_VISUAL_MATRIX" -> Color(0xFF22D3EE)
        "REAGENT_VISUAL_SHIMMER" -> {
            val transition = rememberInfiniteTransition(label = "shimmer_transition")
            val hue by transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 6000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart,
                ),
                label = "shimmer_hue",
            )
            Color.hsv(hue, 0.8f, 0.9f)
        }
        else -> CyberTheme.green
    }
} ?: CyberTheme.green

fun Modifier.cyberglass(
    borderColor: Color,
    glowColor: Color = borderColor.copy(alpha = 0.2f),
    backgroundColor: Color = Color(0xFF0A0A0F).copy(alpha = 0.75f),
): Modifier = this.then(
    Modifier
        .background(backgroundColor, RoundedCornerShape(4.dp))
        .drawBehind {
            val w = size.width
            val h = size.height
            val tick = 8.dp.toPx()
            val stroke = 1.dp.toPx()

            // Draw background outline border (1.dp)
            drawRoundRect(
                color = borderColor.copy(alpha = 0.35f),
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = Size(w - stroke, h - stroke),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                style = Stroke(width = stroke),
            )

            // Top-left
            drawLine(borderColor, Offset(0f, 0f), Offset(tick, 0f), strokeWidth = stroke * 1.5f)
            drawLine(borderColor, Offset(0f, 0f), Offset(0f, tick), strokeWidth = stroke * 1.5f)

            // Top-right
            drawLine(borderColor, Offset(w, 0f), Offset(w - tick, 0f), strokeWidth = stroke * 1.5f)
            drawLine(borderColor, Offset(w, 0f), Offset(w, tick), strokeWidth = stroke * 1.5f)

            // Bottom-left
            drawLine(borderColor, Offset(0f, h), Offset(tick, h), strokeWidth = stroke * 1.5f)
            drawLine(borderColor, Offset(0f, h), Offset(0f, h - tick), strokeWidth = stroke * 1.5f)

            // Bottom-right
            drawLine(borderColor, Offset(w, h), Offset(w - tick, h), strokeWidth = stroke * 1.5f)
            drawLine(borderColor, Offset(w, h), Offset(w, h - tick), strokeWidth = stroke * 1.5f)
        },
)

fun Modifier.cyberglassCircle(
    borderColor: Color,
    glowColor: Color = borderColor.copy(alpha = 0.2f),
    backgroundColor: Color = Color(0xFF0A0A0F).copy(alpha = 0.75f),
): Modifier = this.then(
    Modifier
        .clip(androidx.compose.foundation.shape.CircleShape)
        .background(backgroundColor)
        .border(1.dp, borderColor.copy(alpha = 0.4f), androidx.compose.foundation.shape.CircleShape)
        .drawBehind {
            val w = size.width
            drawCircle(
                color = borderColor.copy(alpha = 0.15f),
                radius = w / 2f - 3.dp.toPx(),
                style = Stroke(width = 0.8.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f)),
            )
        },
)
