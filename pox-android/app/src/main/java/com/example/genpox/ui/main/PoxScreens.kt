package com.example.genpox.ui.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.geometry.Offset

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpox.data.Creature
import com.example.genpox.data.GeneSequence
import com.example.genpox.data.HarvestMission
import com.example.genpox.data.WaveMath
import com.example.genpox.theme.*
import com.example.genpox.ui.components.CanvasMetrics
import com.example.genpox.ui.components.NodeCrystalCanvas
import com.example.genpox.ui.components.MapStyle
import com.example.genpox.ui.components.QrCodeImage
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

// ==========================================
// CUSTOM WIREFRAME RETRO ICONS & VISUALS
// ==========================================
@Composable
fun WireframeHeart(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.85f)
            cubicTo(w * 0.1f, h * 0.55f, w * 0.05f, h * 0.2f, w * 0.3f, h * 0.15f)
            cubicTo(w * 0.45f, h * 0.15f, w * 0.5f, h * 0.3f, w * 0.5f, h * 0.3f)
            cubicTo(w * 0.5f, h * 0.3f, w * 0.55f, h * 0.15f, w * 0.7f, h * 0.15f)
            cubicTo(w * 0.95f, h * 0.2f, w * 0.9f, h * 0.55f, w * 0.5f, h * 0.85f)
            close()
        }
        drawPath(path, color, style = Stroke(width = 1.5.dp.toPx()))
    }
}

@Composable
fun WireframeLightning(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.6f, h * 0.05f)
            lineTo(w * 0.25f, h * 0.55f)
            lineTo(w * 0.55f, h * 0.55f)
            lineTo(w * 0.4f, h * 0.95f)
            lineTo(w * 0.75f, h * 0.45f)
            lineTo(w * 0.45f, h * 0.45f)
            close()
        }
        drawPath(path, color, style = Stroke(width = 1.5.dp.toPx()))
    }
}

@Composable
fun WireframeClaws(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.2.dp.toPx()
        // Draw 3 slanting curved lines representing scratch marks / claws
        val path1 = Path().apply {
            moveTo(w * 0.25f, h * 0.2f)
            quadraticTo(w * 0.4f, h * 0.5f, w * 0.3f, h * 0.8f)
        }
        val path2 = Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            quadraticTo(w * 0.6f, h * 0.5f, w * 0.5f, h * 0.85f)
        }
        val path3 = Path().apply {
            moveTo(w * 0.75f, h * 0.2f)
            quadraticTo(w * 0.8f, h * 0.5f, w * 0.7f, h * 0.8f)
        }
        drawPath(path1, color, style = Stroke(width = stroke))
        drawPath(path2, color, style = Stroke(width = stroke))
        drawPath(path3, color, style = Stroke(width = stroke))
    }
}

@Composable
fun WireframeShield(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.05f)
            lineTo(w * 0.85f, h * 0.15f)
            lineTo(w * 0.85f, h * 0.5f)
            quadraticTo(w * 0.85f, h * 0.75f, w * 0.5f, h * 0.95f)
            quadraticTo(w * 0.15f, h * 0.75f, w * 0.15f, h * 0.5f)
            lineTo(w * 0.15f, h * 0.15f)
            close()
        }
        drawPath(path, color, style = Stroke(width = 1.5.dp.toPx()))
    }
}

@Composable
fun WireframeGalaxy(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val center = Offset(w * 0.5f, h * 0.5f)
        drawCircle(color, radius = w * 0.12f, center = center)
        
        val armPath1 = Path()
        val armPath2 = Path()
        
        for (i in 0..30) {
            val t = i / 30f
            val angle = t * 2f * Math.PI.toFloat()
            val radius = w * 0.12f + t * w * 0.35f
            val x = (center.x + radius * cos(angle)).toFloat()
            val y = (center.y + radius * sin(angle)).toFloat()
            if (i == 0) armPath1.moveTo(x, y) else armPath1.lineTo(x, y)
        }
        
        for (i in 0..30) {
            val t = i / 30f
            val angle = t * 2f * Math.PI.toFloat() + Math.PI.toFloat()
            val radius = w * 0.12f + t * w * 0.35f
            val x = (center.x + radius * cos(angle)).toFloat()
            val y = (center.y + radius * sin(angle)).toFloat()
            if (i == 0) armPath2.moveTo(x, y) else armPath2.lineTo(x, y)
        }
        
        drawPath(armPath1, color, style = Stroke(width = 1.2.dp.toPx()))
        drawPath(armPath2, color, style = Stroke(width = 1.2.dp.toPx()))
    }
}

@Composable
fun WireframeSpeed(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.4f, h * 0.2f)
            lineTo(w * 0.8f, h * 0.5f)
            lineTo(w * 0.4f, h * 0.8f)
            lineTo(w * 0.1f, h * 0.5f)
            close()
        }
        drawPath(path, color, style = Stroke(width = 1.5.dp.toPx()))
        drawLine(color, Offset(w * 0.3f, h * 0.35f), Offset(w * 0.05f, h * 0.35f), strokeWidth = 1.2.dp.toPx())
        drawLine(color, Offset(w * 0.3f, h * 0.65f), Offset(w * 0.05f, h * 0.65f), strokeWidth = 1.2.dp.toPx())
    }
}

@Composable
fun WireframeDna(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val steps = 10
        val wave1 = mutableListOf<Offset>()
        val wave2 = mutableListOf<Offset>()
        for (i in 0..steps) {
            val x = w * (i.toFloat() / steps)
            val angle = (i.toFloat() / steps) * 2 * Math.PI
            val yOffset = (h * 0.25f * sin(angle)).toFloat()
            wave1.add(Offset(x, h * 0.5f + yOffset))
            wave2.add(Offset(x, h * 0.5f - yOffset))
        }
        for (i in 0 until steps) {
            drawLine(color, wave1[i], wave1[i+1], strokeWidth = 1.5.dp.toPx())
            drawLine(color, wave2[i], wave2[i+1], strokeWidth = 1.5.dp.toPx())
        }
        for (i in listOf(2, 5, 8)) {
            drawLine(color, wave1[i], wave2[i], strokeWidth = 1.dp.toPx())
        }
    }
}

@Composable
fun WireframeWarning(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.1f)
            lineTo(w * 0.9f, h * 0.85f)
            lineTo(w * 0.1f, h * 0.85f)
            close()
        }
        drawPath(path, color, style = Stroke(width = 1.5.dp.toPx()))
        drawLine(color, Offset(w * 0.5f, h * 0.35f), Offset(w * 0.5f, h * 0.65f), strokeWidth = 1.5.dp.toPx())
        drawCircle(color, radius = 1.dp.toPx(), center = Offset(w * 0.5f, h * 0.77f))
    }
}

@Composable
fun WireframeSparkle(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, 0f)
            quadraticTo(w * 0.5f, h * 0.5f, w, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.5f, w * 0.5f, h)
            quadraticTo(w * 0.5f, h * 0.5f, 0f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.5f, w * 0.5f, 0f)
        }
        drawPath(path, color, style = Stroke(width = 1.5.dp.toPx()))
    }
}

@Composable
fun WireframeGear(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val rOuter = w * 0.35f
        val rInner = w * 0.18f
        val center = Offset(w * 0.5f, h * 0.5f)
        drawCircle(color, radius = rInner, center = center, style = Stroke(width = 1.2.dp.toPx()))
        drawCircle(color, radius = rOuter, center = center, style = Stroke(width = 1.2.dp.toPx()))
        for (i in 0 until 6) {
            val angle = i * Math.PI / 3
            val start = Offset(
                (center.x + rOuter * cos(angle)).toFloat(),
                (center.y + rOuter * sin(angle)).toFloat()
            )
            val end = Offset(
                (center.x + (rOuter + w * 0.12f) * cos(angle)).toFloat(),
                (center.y + (rOuter + w * 0.12f) * sin(angle)).toFloat()
            )
            drawLine(color, start, end, strokeWidth = 1.5.dp.toPx())
        }
    }
}

@Composable
fun WireframeStar(color: Color, filled: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val center = Offset(w * 0.5f, h * 0.5f)
        val path = Path().apply {
            val numPoints = 5
            val rOuter = w * 0.45f
            val rInner = w * 0.2f
            for (i in 0 until numPoints * 2) {
                val r = if (i % 2 == 0) rOuter else rInner
                val angle = i * Math.PI / numPoints - Math.PI / 2
                val x = (center.x + r * cos(angle)).toFloat()
                val y = (center.y + r * sin(angle)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        if (filled) {
            drawPath(path, color)
        } else {
            drawPath(path, color, style = Stroke(width = 1.2.dp.toPx()))
        }
    }
}

@Composable
fun WireframeOriginal(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val r = w * 0.4f
        drawCircle(color, radius = r, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = 1.2.dp.toPx()))
        drawCircle(color, radius = w * 0.15f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = 1.2.dp.toPx()))
    }
}

@Composable
fun WireframeTransfer(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val y1 = h * 0.35f
        val y2 = h * 0.65f
        val stroke = 1.2.dp.toPx()
        
        drawLine(color, Offset(w * 0.2f, y1), Offset(w * 0.8f, y1), strokeWidth = stroke)
        drawLine(color, Offset(w * 0.8f, y1), Offset(w * 0.6f, y1 - h * 0.15f), strokeWidth = stroke)
        drawLine(color, Offset(w * 0.8f, y1), Offset(w * 0.6f, y1 + h * 0.15f), strokeWidth = stroke)

        drawLine(color, Offset(w * 0.8f, y2), Offset(w * 0.2f, y2), strokeWidth = stroke)
        drawLine(color, Offset(w * 0.2f, y2), Offset(w * 0.4f, y2 - h * 0.15f), strokeWidth = stroke)
        drawLine(color, Offset(w * 0.2f, y2), Offset(w * 0.4f, y2 + h * 0.15f), strokeWidth = stroke)
    }
}

@Composable
fun WireframeNatural(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.85f)
            cubicTo(w * 0.15f, h * 0.55f, w * 0.2f, h * 0.15f, w * 0.5f, h * 0.15f)
            cubicTo(w * 0.8f, h * 0.15f, w * 0.85f, h * 0.55f, w * 0.5f, h * 0.85f)
            close()
        }
        drawPath(path, color, style = Stroke(width = 1.2.dp.toPx()))
        drawLine(color, Offset(w * 0.5f, h * 0.85f), Offset(w * 0.5f, h * 0.15f), strokeWidth = 1.0.dp.toPx())
    }
}

@Composable
fun WireframeForced(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.2.dp.toPx()
        val path = Path().apply {
            moveTo(w * 0.2f, h * 0.25f)
            lineTo(w * 0.8f, h * 0.25f)
            lineTo(w * 0.8f, h * 0.5f)
            lineTo(w * 0.6f, h * 0.5f)
            lineTo(w * 0.6f, h * 0.85f)
            lineTo(w * 0.4f, h * 0.85f)
            lineTo(w * 0.4f, h * 0.5f)
            lineTo(w * 0.2f, h * 0.5f)
            close()
        }
        drawPath(path, color, style = Stroke(width = stroke))
    }
}

@Composable
fun TagBadge(tag: String, modifier: Modifier = Modifier) {
    val (color, content) = when (tag) {
        "FAVORITE" -> Color(0xFFFFB300) to @Composable { WireframeStar(Color(0xFFFFB300), filled = true, modifier = modifier.size(10.dp)) }
        "DEFENDER" -> Color(0xFF60A5FA) to @Composable { WireframeShield(Color(0xFF60A5FA), modifier = modifier.size(10.dp)) }
        "AUTO-HACKER" -> Color(0xFFFBBF24) to @Composable { WireframeGear(Color(0xFFFBBF24), modifier = modifier.size(10.dp)) }
        "FULL COHERENCE" -> CyberGreen to @Composable { WireframeDna(CyberGreen, modifier = modifier.size(10.dp)) }
        "NATURAL" -> Color(0xFF10B981) to @Composable { WireframeNatural(Color(0xFF10B981), modifier = modifier.size(10.dp)) }
        "FORCED" -> Color(0xFFF59E0B) to @Composable { WireframeForced(Color(0xFFF59E0B), modifier = modifier.size(10.dp)) }
        "ALPHA GENE" -> Color(0xFFFFB300) to @Composable { WireframeLightning(Color(0xFFFFB300), modifier = modifier.size(10.dp)) }
        "MODIFIED" -> Color(0xFFC084FC) to @Composable { WireframeSparkle(Color(0xFFC084FC), modifier = modifier.size(10.dp)) }
        "ORIGINAL" -> CyberGreen to @Composable { WireframeOriginal(CyberGreen, modifier = modifier.size(10.dp)) }
        "TRANSFER-ORIGIN" -> Color(0xFFEF4444) to @Composable { WireframeTransfer(Color(0xFFEF4444), modifier = modifier.size(10.dp)) }
        else -> return
    }
    
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        content()
    }
}

@Composable
fun QrRevealVisual(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLineY"
    )

    Box(
        modifier = modifier
            .border(1.dp, CyberBorder.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val tickLen = w * 0.12f
            val strokeW = 1.5.dp.toPx()
            
            drawLine(CyberGreenDim, Offset(0f, 0f), Offset(tickLen, 0f), strokeWidth = strokeW)
            drawLine(CyberGreenDim, Offset(0f, 0f), Offset(0f, tickLen), strokeWidth = strokeW)
            drawLine(CyberGreenDim, Offset(w, 0f), Offset(w - tickLen, 0f), strokeWidth = strokeW)
            drawLine(CyberGreenDim, Offset(w, 0f), Offset(w, tickLen), strokeWidth = strokeW)
            drawLine(CyberGreenDim, Offset(0f, h), Offset(tickLen, h), strokeWidth = strokeW)
            drawLine(CyberGreenDim, Offset(0f, h), Offset(0f, h - tickLen), strokeWidth = strokeW)
            drawLine(CyberGreenDim, Offset(w, h), Offset(w - tickLen, h), strokeWidth = strokeW)
            drawLine(CyberGreenDim, Offset(w, h), Offset(w, h - tickLen), strokeWidth = strokeW)
            
            drawCircle(
                color = CyberGreenDim.copy(alpha = 0.3f),
                radius = w * 0.35f,
                center = center,
                style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f))
            )
            drawCircle(
                color = CyberGreen.copy(alpha = 0.1f),
                radius = w * 0.2f,
                center = center
            )
            
            val chLen = w * 0.08f
            drawLine(CyberGreenDim, Offset(center.x - w * 0.38f, center.y), Offset(center.x - w * 0.28f, center.y), strokeWidth = 1.dp.toPx())
            drawLine(CyberGreenDim, Offset(center.x + w * 0.28f, center.y), Offset(center.x + w * 0.38f, center.y), strokeWidth = 1.dp.toPx())
            drawLine(CyberGreenDim, Offset(center.x, center.y - h * 0.38f), Offset(center.x, center.y - h * 0.28f), strokeWidth = 1.dp.toPx())
            drawLine(CyberGreenDim, Offset(center.x, center.y + h * 0.28f), Offset(center.x, center.y + h * 0.38f), strokeWidth = 1.dp.toPx())
            
            val scanY = h * scanProgress
            drawLine(
                color = CyberGreen,
                start = Offset(0f, scanY),
                end = Offset(w, scanY),
                strokeWidth = 1.5.dp.toPx()
            )
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(CyberGreen.copy(alpha = 0.15f), Color.Transparent),
                    startY = scanY,
                    endY = minOf(h, scanY + 12.dp.toPx())
                ),
                topLeft = Offset(0f, scanY),
                size = androidx.compose.ui.geometry.Size(w, minOf(h - scanY, 12.dp.toPx()))
            )
        }
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "ENCRYPTED",
                color = CyberGreen,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
            Text(
                text = "[ TAP TO REVEAL ]",
                color = Color.Gray,
                fontSize = 5.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==========================================
// 1. COMBINATOR VIEW (MAIN SCENE)
// ==========================================

// ==========================================
// 1. COMBINATOR VIEW (BIO-LAB TAB SCENE)
// ==========================================
@Composable
fun CombinatorView(viewModel: MainViewModel) {
    val bioLabSubTab by viewModel.bioLabSubTab.collectAsState()
    val idleTime by viewModel.idleTime.collectAsState()
    val boostSecondsLeft by viewModel.boostSecondsLeft.collectAsState()
    val anomalyEngineActive by viewModel.anomalyEngineActive.collectAsState()
    val discoveredPacketsLog by viewModel.discoveredPacketsLog.collectAsState()
    val scrollingGene by viewModel.scrollingGene.collectAsState()
    val grandTotalStandardNucleotides by viewModel.grandTotalStandardNucleotides.collectAsState()
    val geneSequences by viewModel.geneSequences.collectAsState()
    val devForceAnomaly by viewModel.devForceAnomaly.collectAsState()

    // UI colors and themes based on subtab
    val activeColor = if (bioLabSubTab == "pox") CyberGreen else Color(0xFFA855F7)
    val activeColorDim = if (bioLabSubTab == "pox") CyberGreenDim else Color(0xFF701A75)
    val activeBorder = if (bioLabSubTab == "pox") CyberBorder else Color(0xFF4A125E)
    val activePanel = if (bioLabSubTab == "pox") CyberPanel else Color(0xFF150B24)

    // Local Overlay Visibility States
    var showStepSearchOverlay by remember { mutableStateOf(false) }
    var showPacketLogOverlay by remember { mutableStateOf(false) }
    var showAnomalyVaultOverlay by remember { mutableStateOf(false) }

    // Step Search local states
    var stepSearchPrefix by remember { mutableStateOf("") }
    var viewStepSearchMatchesOnly by remember { mutableStateOf(false) }
    var stepSearchSelectedGene by remember { mutableStateOf<String?>(null) }

    // Packet Log local states
    var selectedPacketByGene by remember { mutableStateOf<String?>(null) }
    var packetLogQuery by remember { mutableStateOf("") }

    // Selected anomalous gene details (within Anomaly Vault overlay)
    var selectedAnomalousGene by remember { mutableStateOf<String?>(null) }

    // Wave config calculations
    val wave = WaveMath.getDailyWaveConfig(System.currentTimeMillis())
    
    // Auto-generate metrics for NodeCrystalCanvas based on synodic daily wave configuration
    val metrics = remember(wave, bioLabSubTab) {
        var widthG = 0.5f
        var widthA = 0.5f
        var widthT = 0.5f
        var widthC = 0.5f
        
        when (wave.primary) {
            "G" -> widthG = wave.primaryMultiplier.toFloat()
            "A" -> widthA = wave.primaryMultiplier.toFloat()
            "T" -> widthT = wave.primaryMultiplier.toFloat()
            "C" -> widthC = wave.primaryMultiplier.toFloat()
        }
        when (wave.secondary) {
            "G" -> widthG = wave.secondaryMultiplier.toFloat()
            "A" -> widthA = wave.secondaryMultiplier.toFloat()
            "T" -> widthT = wave.secondaryMultiplier.toFloat()
            "C" -> widthC = wave.secondaryMultiplier.toFloat()
        }
        
        val scale = 0.5f
        CanvasMetrics(
            subnodeDepthG = 0.5f,
            subnodeDepthA = 0.5f,
            subnodeDepthT = 0.5f,
            subnodeDepthC = 0.5f,
            totalNodeDepth = 0.6f,
            subnodeWidthG = widthG * scale,
            subnodeWidthA = widthA * scale,
            subnodeWidthT = widthT * scale,
            subnodeWidthC = widthC * scale,
            totalNodeWidth = 0.8f,
            colorR = if (bioLabSubTab == "pox") 0 else 168,
            colorG = if (bioLabSubTab == "pox") 255 else 85,
            colorB = if (bioLabSubTab == "pox") 102 else 247
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main Screen Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // 2. MAIN REACTOR CARD (representing Left Pane)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, activeBorder, RoundedCornerShape(4.dp))
                    .background(activePanel)
                    .padding(12.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    if (bioLabSubTab == "pox") {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "[ G.E.N. P.O.X. TIDE POOL REACTOR V2.4 ]",
                                color = CyberGreenDim,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Default
                            )
                            Text(
                                text = "SYSTEMS ON",
                                color = CyberGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Default
                            )
                        }

                        // Title
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .requiredHeight(24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SINGLE-NODE CYBERNETIC SYNTHESIZER",
                                color = Color.White,
                                style = Typography.bodyMedium,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Top-Level Counts section
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "UNIQUE GENE IDS",
                                    color = CyberGreenDim,
                                    style = Typography.labelSmall,
                                    fontFamily = FontFamily.Default,
                                    fontSize = 9.sp
                                )
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⬢ ",
                                        color = CyberGreen,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${geneSequences.size}",
                                        color = Color.White,
                                        style = Typography.bodyLarge,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(35.dp)
                                    .background(CyberBorder)
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp)
                            ) {
                                Text(
                                    text = "MULTI-COUNT GENE IDS",
                                    color = CyberGreenDim,
                                    style = Typography.labelSmall,
                                    fontFamily = FontFamily.Default,
                                    fontSize = 9.sp
                                )
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⬢ ",
                                        color = CyberGreen,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${geneSequences.filter { it.count > 1 }.size}",
                                        color = Color.White,
                                        style = Typography.bodyLarge,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Today's base-pair wave card
                        val todayWave = WaveMath.getDailyWaveConfig(System.currentTimeMillis())
                        val tomorrowWave = WaveMath.getDailyWaveConfig(System.currentTimeMillis() + 86400000L)
                        val dayAfterWave = WaveMath.getDailyWaveConfig(System.currentTimeMillis() + 172800000L)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (todayWave.isSuppressed) Color(0xFF990000).copy(alpha = 0.8f) else CyberGreen,
                                    RoundedCornerShape(4.dp)
                                )
                                .background(if (todayWave.isSuppressed) Color(0xFF1A0000) else Color.Black.copy(alpha = 0.6f))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Text(
                                        text = "~",
                                        color = if (todayWave.isSuppressed) Color.Red else Color(0xFFFFB300),
                                        fontSize = 14.sp
                                    )
                                    Column {
                                        Text(
                                            text = "TODAY'S BASE-PAIR WAVE",
                                            color = CyberGreenDim,
                                            style = Typography.labelSmall,
                                            fontFamily = FontFamily.Default,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (todayWave.isSuppressed) "DORMANT (CONGESTED DECAY)" else "ACTIVE: ${todayWave.pair.uppercase()} WAVE",
                                            color = Color.White,
                                            style = Typography.bodySmall,
                                            fontFamily = FontFamily.Default,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (!todayWave.isSuppressed) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${todayWave.primary} ➔ ${todayWave.secondary}",
                                            color = CyberGreen,
                                            style = Typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                        Text(
                                            text = "1.12X & 1.62X BOOST",
                                            color = CyberGreenDim,
                                            style = Typography.labelSmall,
                                            fontFamily = FontFamily.Default,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "NULL",
                                        color = Color.Red,
                                        style = Typography.labelSmall,
                                        fontFamily = FontFamily.Default,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(Color(0xFF330000), RoundedCornerShape(2.dp))
                                            .border(1.dp, Color.Red, RoundedCornerShape(2.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Forecast Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Tomorrow
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (tomorrowWave.isSuppressed) Color(0xFF990000).copy(alpha = 0.6f) else Color.DarkGray,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .background(if (tomorrowWave.isSuppressed) Color(0xFF1A0000) else Color.Black.copy(alpha = 0.45f))
                                    .padding(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "TOMORROW BASE-PAIR",
                                            color = CyberGreenDim,
                                            style = Typography.labelSmall,
                                            fontFamily = FontFamily.Default,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (tomorrowWave.isSuppressed) "DORMANT" else "${tomorrowWave.pair.uppercase()} WAVE",
                                            color = Color.White,
                                            style = Typography.labelSmall,
                                            fontFamily = FontFamily.Default,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (!tomorrowWave.isSuppressed) {
                                        Text(
                                            text = "${tomorrowWave.primary}➔${tomorrowWave.secondary}",
                                            color = CyberGreen,
                                            style = Typography.labelSmall,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                }
                            }

                            // Day After Tomorrow
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (dayAfterWave.isSuppressed) Color(0xFF990000).copy(alpha = 0.6f) else Color.DarkGray,
                                        RoundedCornerShape(4.dp)
                                    )
                                    .background(if (dayAfterWave.isSuppressed) Color(0xFF1A0000) else Color.Black.copy(alpha = 0.45f))
                                    .padding(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "DAY AFTER TOMORROW",
                                            color = CyberGreenDim,
                                            style = Typography.labelSmall,
                                            fontFamily = FontFamily.Default,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (dayAfterWave.isSuppressed) "DORMANT" else "${dayAfterWave.pair.uppercase()} WAVE",
                                            color = Color.White,
                                            style = Typography.labelSmall,
                                            fontFamily = FontFamily.Default,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (!dayAfterWave.isSuppressed) {
                                        Text(
                                            text = "${dayAfterWave.primary}➔${dayAfterWave.secondary}",
                                            color = CyberGreen,
                                            style = Typography.labelSmall,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Anomaly tab content
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "[ WARNING: UNKNOWN REACTOR ]",
                                color = Color(0xFFA855F7),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Default
                            )
                            Text(
                                text = if (anomalyEngineActive) "SYSTEMS ON" else "SYSTEMS OFF",
                                color = if (anomalyEngineActive) Color(0xFFD8B4FE) else Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Default,
                                modifier = Modifier.clickable {
                                    viewModel.setAnomalyEngineActive(!anomalyEngineActive)
                                }
                            )
                        }

                        // Compact Title
                        Row(
                            modifier = Modifier.fillMaxWidth().requiredHeight(24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GENETIC ANOMALY HARMONIZER",
                                color = Color.White,
                                style = Typography.bodyMedium,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Top-Level Counts section for Anomaly tab (standardized padding)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF4A125E), RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        viewModel.synthManager.playBeep(600f, 0.05f, "sine")
                                        showAnomalyVaultOverlay = true
                                    }
                             ) {
                                Text(
                                    text = "ANOMALOUS GENE IDS",
                                    color = Color(0xFFA855F7),
                                    style = Typography.labelSmall,
                                    fontFamily = FontFamily.Default,
                                    fontSize = 9.sp
                                )
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⬢ ",
                                        color = Color(0xFFA855F7),
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${geneSequences.filter { WaveMath.isAnomalousGene(it.sequence) }.size}",
                                        color = Color.White,
                                        style = Typography.bodyLarge,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(35.dp)
                                    .background(Color(0xFF4A125E))
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 12.dp)
                            ) {
                                Text(
                                    text = "TOTAL GENE STOCK",
                                    color = Color(0xFFA855F7),
                                    style = Typography.labelSmall,
                                    fontFamily = FontFamily.Default,
                                    fontSize = 9.sp
                                )
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⬢ ",
                                        color = CyberGreen,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${geneSequences.size}",
                                        color = Color.White,
                                        style = Typography.bodyLarge,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Compact Info panel
                        val meetsRequirement = grandTotalStandardNucleotides >= 250000L
                        val formattedCount = String.format(Locale.US, "%,d", grandTotalStandardNucleotides)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (meetsRequirement) Color(0xFFA855F7) else Color(0xFF4A125E),
                                    RoundedCornerShape(4.dp)
                                )
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Text(
                                        text = "~",
                                        color = Color(0xFFA855F7),
                                        fontSize = 14.sp
                                    )
                                    Column {
                                        Text(
                                            text = "ANOMALOUS RESOURCE & LOAD",
                                            color = Color(0xFFD8B4FE),
                                            style = Typography.labelSmall,
                                            fontFamily = FontFamily.Default,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "$formattedCount / 250,000 NUCLEOTIDES",
                                            color = Color.White,
                                            style = Typography.bodySmall,
                                            fontFamily = FontFamily.Default,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "RATE: -10K/LOOP",
                                        color = Color(0xFFA855F7),
                                        style = Typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                    Text(
                                        text = "RESETS STABILITY",
                                        color = Color(0xFFD8B4FE),
                                        style = Typography.labelSmall,
                                        fontFamily = FontFamily.Default,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Anomaly Forecast Grid
                        val coupling = WaveMath.getSpectrumWaveCoupling(System.currentTimeMillis())
                        val chanceMetrics = WaveMath.getAnomalyEngineSuccessChance(grandTotalStandardNucleotides, coupling)
                        val formattedFinalChance = String.format(Locale.US, "%.3f%%", chanceMetrics.finalChance)
                        val formattedModifier = String.format(Locale.US, "%+.3f%%", chanceMetrics.harmonicModifier)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Left: Consolidation Chance
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        Color(0xFF4A125E),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .padding(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "CONSOLIDATION CHANCE",
                                            color = Color(0xFFD8B4FE),
                                            style = Typography.labelSmall,
                                            fontFamily = FontFamily.Default,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "SUCCESS PROB",
                                            color = Color.White,
                                            style = Typography.labelSmall,
                                            fontFamily = FontFamily.Default,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = formattedFinalChance,
                                        color = Color(0xFFA855F7),
                                        style = Typography.labelSmall,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            }

                            // Right: Spectrum Coupling
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        Color(0xFF4A125E),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .padding(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "SPECTRUM COUPLING",
                                            color = Color(0xFFD8B4FE),
                                            style = Typography.labelSmall,
                                            fontFamily = FontFamily.Default,
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "HARMONIC MODIFIER",
                                            color = Color.White,
                                            style = Typography.labelSmall,
                                            fontFamily = FontFamily.Default,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = formattedModifier,
                                        color = Color(0xFFA855F7),
                                        style = Typography.labelSmall,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    // Ticker Box
                    val displayText = if (bioLabSubTab == "anomaly") {
                        if (!anomalyEngineActive) {
                            "--------"
                        } else {
                            val syms = "XZYW?!$%&@#"
                            scrollingGene.mapIndexed { i, char ->
                                val idx = (char.code + i) % syms.length
                                syms[idx]
                            }.joinToString("")
                        }
                    } else {
                        scrollingGene
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .border(2.dp, activeColor, RoundedCornerShape(4.dp))
                            .background(Color.Black)
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayText,
                            color = activeColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            letterSpacing = 2.sp
                        )
                    }

                    // Progress bar
                    val isBoosted = boostSecondsLeft > 0
                    val totalCycle = if (isBoosted) 8 else 16
                    val progress = if (bioLabSubTab == "anomaly" && !anomalyEngineActive) {
                        0f
                    } else {
                        ((totalCycle - idleTime).toFloat() / totalCycle.toFloat()).coerceIn(0f, 1f)
                    }

                    val progressColor = if (bioLabSubTab == "anomaly") {
                        if (anomalyEngineActive) Color(0xFFA855F7) else Color.DarkGray
                    } else {
                        CyberGreen
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .border(1.dp, activeBorder, RoundedCornerShape(4.dp))
                            .clip(RoundedCornerShape(4.dp)),
                        color = progressColor,
                        trackColor = Color.Black
                    )

                    // Timer text and booster status
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (bioLabSubTab == "anomaly") {
                                if (anomalyEngineActive) "ANOMALOUS CONSOLIDATION IN: ${idleTime}S" else "ANOMALOUS CONSOLIDATION: IDLE"
                            } else {
                                "GENE ARRAY READY IN: ${idleTime}S"
                            },
                            color = if (bioLabSubTab == "anomaly") {
                                if (anomalyEngineActive) Color(0xFFA855F7) else Color.Gray
                            } else {
                                CyberGreenDim
                            },
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            style = Typography.bodySmall
                        )

                        if (boostSecondsLeft > 0 && bioLabSubTab == "pox") {
                            Text(
                                text = String.format(Locale.US, "REACTOR BOOST ACTIVE: %02d:%02d REMAINING", boostSecondsLeft / 60, boostSecondsLeft % 60),
                                color = CyberGreen,
                                fontWeight = FontWeight.Bold,
                                style = Typography.labelSmall,
                                fontFamily = FontFamily.Default,
                                modifier = Modifier
                                    .background(CyberGreen.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                                    .border(1.dp, CyberGreen.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // 5. ACTION NAVIGATION OVERLAY BUTTONS
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { showStepSearchOverlay = true },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black.copy(alpha = 0.4f), contentColor = activeColor),
                            border = BorderStroke(1.dp, activeBorder),
                            shape = RoundedCornerShape(2.dp),
                            contentPadding = PaddingValues(vertical = 0.dp)
                        ) {
                            Text(
                                text = "MOLECULAR STEP-SEARCH",
                                style = Typography.bodyMedium,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.synthManager.playBeep(650f, 0.05f, "sine")
                        if (bioLabSubTab == "anomaly") {
                            showAnomalyVaultOverlay = true
                        } else {
                            showPacketLogOverlay = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (bioLabSubTab == "anomaly") Color(0xFF701A75).copy(alpha = 0.2f) else Color(0xFF003CFF).copy(alpha = 0.2f),
                        contentColor = if (bioLabSubTab == "anomaly") Color(0xFFD8B4FE) else Color(0xFF00E1FF)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (bioLabSubTab == "anomaly") Color(0xFFA855F7).copy(alpha = 0.5f) else Color(0xFF003CFF).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(2.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = if (bioLabSubTab == "anomaly") "VIEW ANOMALY DISCOVERY LOG" else "VIEW GENE SYNTHESIS LOG",
                        style = Typography.labelSmall,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = { viewModel.triggerManualAcceleration() },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (bioLabSubTab == "anomaly") Color(0xFF701A75).copy(alpha = 0.2f) else CyberGreenDim.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (bioLabSubTab == "anomaly") Color(0xFFA855F7).copy(alpha = 0.8f) else CyberGreen.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(2.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "MANUAL ACCELERATION (-2S)",
                        style = Typography.labelSmall,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // SUB-TAB SWITCHER (P.O.X. Reactor & Anomaly Engine)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, activeBorder, RoundedCornerShape(4.dp))
                        .background(activePanel)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = { viewModel.setBioLabSubTab("pox") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (bioLabSubTab == "pox") CyberGreen else Color.Transparent,
                            contentColor = if (bioLabSubTab == "pox") Color.Black else CyberGreenDim
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text(
                            text = "P.O.X. REACTOR",
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { viewModel.setBioLabSubTab("anomaly") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (bioLabSubTab == "anomaly") Color(0xFFA855F7) else Color.Transparent,
                            contentColor = if (bioLabSubTab == "anomaly") Color.White else Color(0xFF701A75)
                        ),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Text(
                            text = "ANOMALY ENGINE",
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (devForceAnomaly) {
                    Button(
                        onClick = { viewModel.addDevGenes() },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x30F97316),
                            contentColor = Color(0xFFF97316)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFF97316)),
                        shape = RoundedCornerShape(2.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "DEV: INJECT 10K GENES",
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ==========================================
        // OVERLAY 1: MOLECULAR STEP-SEARCH
        // ==========================================
        if (showStepSearchOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .border(2.dp, activeColor, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "MOLECULAR STEP-SEARCH DIRECTORY",
                            style = Typography.bodyMedium,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            color = activeColor
                        )
                        Text(
                            text = "✕ CLOSE",
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                            modifier = Modifier.clickable {
                                showStepSearchOverlay = false
                                stepSearchPrefix = ""
                                viewStepSearchMatchesOnly = false
                                stepSearchSelectedGene = null
                            }
                        )
                    }

                    // Progress indicators (1-2bp, 3-4bp, 5-6bp, 7-8bp or character-by-character)
                    val isAnomaly = bioLabSubTab == "anomaly"
                    val stepSize = if (isAnomaly) 1 else 2
                    val maxSteps = if (isAnomaly) 8 else 4
                    val activeStep = stepSearchPrefix.length / stepSize
                    val stepLabels = if (isAnomaly) {
                        listOf("1bp", "2bp", "3bp", "4bp", "5bp", "6bp", "7bp", "8bp")
                    } else {
                        listOf("1-2bp", "3-4bp", "5-6bp", "7-8bp")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        stepLabels.forEachIndexed { idx, stepLabel ->
                            val isCompleted = activeStep > idx
                            val isActive = activeStep == idx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(
                                        1.dp,
                                        if (isActive) activeColor else if (isCompleted) activeColorDim.copy(alpha = 0.5f) else Color.DarkGray,
                                        RoundedCornerShape(2.dp)
                                    )
                                    .background(if (isActive) activeColor.copy(alpha = 0.15f) else Color.Transparent)
                                    .padding(vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stepLabel,
                                    style = Typography.labelSmall,
                                    fontFamily = FontFamily.Default,
                                    color = if (isActive || isCompleted) activeColor else Color.Gray,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = if (isAnomaly) 7.5.sp else 9.sp
                                )
                            }
                        }
                    }

                    // Query & Controls
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, activeBorder, RoundedCornerShape(4.dp))
                            .background(activePanel)
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "PREFIX: ",
                                    style = Typography.labelSmall,
                                    fontFamily = FontFamily.Default,
                                    color = activeColorDim
                                )
                                if (isAnomaly) {
                                    for (i in 0 until 8) {
                                        val charStr = if (stepSearchPrefix.length > i) {
                                            stepSearchPrefix[i].toString()
                                        } else {
                                            "•"
                                        }
                                        val isCurrent = activeStep == i
                                        Text(
                                            text = charStr,
                                            style = Typography.bodyMedium,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) Color.White else if (charStr != "•") activeColor else Color.DarkGray
                                        )
                                    }
                                } else {
                                    for (i in 0 until 4) {
                                        val block = if (stepSearchPrefix.length >= (i + 1) * 2) {
                                            stepSearchPrefix.substring(i * 2, (i + 1) * 2)
                                        } else {
                                            "••"
                                        }
                                        val isCurrent = activeStep == i
                                        Text(
                                            text = block,
                                            style = Typography.bodyMedium,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCurrent) Color.White else if (block != "••") activeColor else Color.DarkGray
                                        )
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (activeStep > 0) {
                                    Text(
                                        text = "UNDO",
                                        style = Typography.labelSmall,
                                        fontFamily = FontFamily.Default,
                                        color = Color.Yellow,
                                        modifier = Modifier.clickable {
                                            stepSearchPrefix = stepSearchPrefix.dropLast(stepSize)
                                            viewStepSearchMatchesOnly = false
                                            viewModel.synthManager.playCombinatorTick()
                                        }
                                    )
                                }
                                Text(
                                    text = "RESET",
                                    style = Typography.labelSmall,
                                    fontFamily = FontFamily.Default,
                                    color = Color.Yellow,
                                    modifier = Modifier.clickable {
                                        stepSearchPrefix = ""
                                        viewStepSearchMatchesOnly = false
                                        stepSearchSelectedGene = null
                                        viewModel.synthManager.playReject()
                                    }
                                )
                            }
                        }
                    }

                    // Main display area: Grid or list of matches
                    val allInventoryGenes = geneSequences.map { it.sequence }
                    val isDone = activeStep == maxSteps

                    if (isDone || viewStepSearchMatchesOnly) {
                        // Matches View
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "RESOLVED STOCK MATCHES:",
                                    style = Typography.labelSmall,
                                    fontFamily = FontFamily.Default,
                                    color = activeColorDim
                                )
                                if (!isDone) {
                                    Text(
                                        text = "BACK TO GRID",
                                        style = Typography.labelSmall,
                                        fontFamily = FontFamily.Default,
                                        color = activeColor,
                                        modifier = Modifier.clickable { viewStepSearchMatchesOnly = false }
                                    )
                                }
                            }
                            
                            val matches = allInventoryGenes.filter { it.startsWith(stepSearchPrefix) }
                            
                            if (matches.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "NO MATCHING STANDARD OR ANOMALOUS GENES FOUND.",
                                        color = activeColorDim,
                                        style = Typography.bodySmall,
                                        fontFamily = FontFamily.Default
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize().padding(top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(matches) { matchSeq ->
                                        val count = geneSequences.find { it.sequence == matchSeq }?.count ?: 0
                                        val isAnom = WaveMath.isAnomalousGene(matchSeq)
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(
                                                    1.dp,
                                                    if (isAnom) Color(0xFFA855F7).copy(alpha = 0.5f) else activeBorder,
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .background(if (isAnom) Color(0xFF1E0B36) else activePanel)
                                                .clickable {
                                                    stepSearchSelectedGene = matchSeq
                                                    viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                                                }
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = matchSeq,
                                                    style = Typography.bodyMedium,
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isAnom) Color(0xFFD8B4FE) else activeColor
                                                )
                                                Text(
                                                    text = if (isAnom) "ANOMALY SECTOR" else "STANDARD GENE",
                                                    style = Typography.labelSmall,
                                                    fontFamily = FontFamily.Default,
                                                    color = if (isAnom) Color(0xFFA855F7) else activeColorDim
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "QTY: ",
                                                    style = Typography.bodyMedium,
                                                    fontFamily = FontFamily.Default,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isAnom) Color(0xFFD8B4FE) else activeColor
                                                )
                                                Text(
                                                    text = "x$count",
                                                    style = Typography.bodyMedium,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isAnom) Color(0xFFD8B4FE) else activeColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Grid View (Select Couple or Base)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isAnomaly) "SELECT ANOMALY BASE TO FILTER:" else "SELECT SEQUENCE COUPLE TO FILTER:",
                                style = Typography.labelSmall,
                                fontFamily = FontFamily.Default,
                                color = activeColorDim,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            if (isAnomaly) {
                                val options = listOf("X", "Z", "Y", "W", "?", "!", "$", "%", "&", "@", "#")
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(4),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(options) { base ->
                                        val tentative = stepSearchPrefix + base
                                        val matchCount = allInventoryGenes.count { it.startsWith(tentative) }
                                        val isEnabled = matchCount > 0
                                        
                                        Box(
                                            modifier = Modifier
                                                .border(
                                                    1.dp,
                                                    if (isEnabled) activeColor else Color.DarkGray.copy(alpha = 0.2f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .background(if (isEnabled) activePanel else Color.Transparent)
                                                .clickable(enabled = isEnabled) {
                                                    stepSearchPrefix = tentative
                                                    viewModel.synthManager.playCombinatorTick()
                                                }
                                                .padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = base,
                                                    style = Typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isEnabled) Color.White else Color.Gray.copy(alpha = 0.3f),
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                )
                                                if (isEnabled) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "x",
                                                            style = Typography.labelSmall,
                                                            fontFamily = FontFamily.Default,
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = activeColor
                                                        )
                                                        Text(
                                                            text = "$matchCount",
                                                            style = Typography.labelSmall,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = activeColor
                                                        )
                                                        Text(
                                                            text = " TYPES",
                                                            style = Typography.labelSmall,
                                                            fontFamily = FontFamily.Default,
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = activeColor
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                val couples = listOf("AA", "AC", "AG", "AT", "CA", "CC", "CG", "CT", "GA", "GC", "GG", "GT", "TA", "TC", "TG", "TT")
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(4),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(couples) { couple ->
                                        val tentative = stepSearchPrefix + couple
                                        val matchCount = allInventoryGenes.count { it.startsWith(tentative) }
                                        val isEnabled = matchCount > 0
                                        
                                        Box(
                                            modifier = Modifier
                                                .border(
                                                    1.dp,
                                                    if (isEnabled) activeColor else Color.DarkGray.copy(alpha = 0.2f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .background(if (isEnabled) activePanel else Color.Transparent)
                                                .clickable(enabled = isEnabled) {
                                                    stepSearchPrefix = tentative
                                                    viewModel.synthManager.playCombinatorTick()
                                                }
                                                .padding(6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = couple,
                                                    style = Typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isEnabled) Color.White else Color.Gray.copy(alpha = 0.3f),
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                )
                                                if (isEnabled) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = "x",
                                                            style = Typography.labelSmall,
                                                            fontFamily = FontFamily.Default,
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = activeColor
                                                        )
                                                        Text(
                                                            text = "$matchCount",
                                                            style = Typography.labelSmall,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = activeColor
                                                        )
                                                        Text(
                                                            text = " TYPES",
                                                            style = Typography.labelSmall,
                                                            fontFamily = FontFamily.Default,
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = activeColor
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Intermediate matches shortcut button
                    if (!isDone && stepSearchPrefix.isNotEmpty() && !viewStepSearchMatchesOnly) {
                        Button(
                            onClick = { viewStepSearchMatchesOnly = true },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = activeColorDim.copy(alpha = 0.15f), contentColor = activeColor),
                            border = BorderStroke(1.dp, activeColorDim.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "VIEW INTERMEDIATE MATCHES",
                                style = Typography.labelSmall,
                                fontFamily = FontFamily.Default
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // OVERLAY 2: BATCH PACKET LOG
        // ==========================================
        if (showPacketLogOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF020D04))
                    .border(1.dp, Color(0xCC00FF41), RoundedCornerShape(6.dp))
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header / Title bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "●",
                                color = Color(0xFF00FF41),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "[ GENE SYNTHESIS LOG ]",
                                color = Color(0xFF00FF41),
                                style = Typography.bodyMedium,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // CLEAR ALL Button
                            Box(
                                modifier = Modifier
                                    .border(1.dp, Color.Yellow, RoundedCornerShape(4.dp))
                                    .background(Color.Transparent)
                                    .clickable {
                                        viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                                        viewModel.clearDiscoveredPacketsLog()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✕ CLEAR ALL",
                                    color = Color.Yellow,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Default,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // CLOSE Button
                            Box(
                                modifier = Modifier
                                    .border(1.dp, Color.Red, RoundedCornerShape(4.dp))
                                    .background(Color.Transparent)
                                    .clickable {
                                        viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                                        showPacketLogOverlay = false
                                        packetLogQuery = ""
                                        selectedPacketByGene = null
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "✕ CLOSE",
                                    color = Color.Red,
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Default,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Info Bar
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.2f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "alpha"
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF00FF41), RoundedCornerShape(4.dp))
                            .background(Color.Transparent)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF00FF41).copy(alpha = alpha))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SELECT ANY GENE BLOCK TO LOAD DETAILED ANALYSIS",
                            color = Color(0xFF00FF41),
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Logs List
                    val filteredPackets = discoveredPacketsLog.filter { packet ->
                        packetLogQuery.isEmpty() || packet.genes.any { it.contains(packetLogQuery.uppercase()) }
                    }

                    if (filteredPackets.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "NO SYNTHESIS PACKETS RECORDED IN THIS SECTOR.",
                                color = Color(0xFF00FF41).copy(alpha = 0.5f),
                                style = Typography.bodySmall,
                                fontFamily = FontFamily.Default,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(filteredPackets) { pIdx, packet ->
                                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(packet.timestamp))
                                val uniqueCount = packet.newGenes.size
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            1.dp,
                                            Color(0xFF00FF41).copy(alpha = 0.25f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .background(Color.Black.copy(alpha = 0.4f))
                                        .padding(8.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        // Packet Header
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "#",
                                                    style = Typography.labelSmall,
                                                    fontFamily = FontFamily.Default,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF00FF41)
                                                )
                                                Text(
                                                    text = "${discoveredPacketsLog.size - pIdx}",
                                                    style = Typography.labelSmall,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF00FF41)
                                                )
                                                Text(
                                                    text = " PACKET SPLICED",
                                                    style = Typography.labelSmall,
                                                    fontFamily = FontFamily.Default,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF00FF41)
                                                )
                                            }
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = "$uniqueCount",
                                                        style = Typography.labelSmall,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF22D3EE)
                                                    )
                                                    Text(
                                                        text = " NEW GENES",
                                                        style = Typography.labelSmall,
                                                        fontFamily = FontFamily.Default,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF22D3EE)
                                                    )
                                                }
                                                Text(
                                                    text = timeStr,
                                                    style = Typography.labelSmall,
                                                    color = Color.Gray,
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                )
                                            }
                                        }

                                        // Genes display
                                        if (packet.isAnomalous) {
                                            val anomalousGene = packet.genes.firstOrNull() ?: "DECAYED!"
                                            val isDecayed = anomalousGene == "DECAYED!"
                                            val isNew = packet.newGenes.contains(anomalousGene)
                                            
                                            val borderAlpha = if (isNew) alpha else 0.0f
                                            val borderStroke = if (isNew) BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = borderAlpha)) else null
                                            
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .then(if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(2.dp)) else Modifier)
                                                    .background(Color.Transparent)
                                                    .clickable(enabled = !isDecayed) {
                                                        selectedPacketByGene = anomalousGene
                                                        viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                                                    }
                                                    .padding(12.dp),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = anomalousGene,
                                                        style = Typography.bodyMedium,
                                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDecayed) Color.Red else Color(0xFFA855F7).copy(alpha = if (isNew) alpha else 1f)
                                                    )
                                                    if (!isDecayed) {
                                                        Text(
                                                            text = "SECURED [DIAGNOSE]",
                                                            style = Typography.labelSmall,
                                                            fontFamily = FontFamily.Default,
                                                            fontSize = 7.5.sp,
                                                            color = Color(0xFFD8B4FE)
                                                        )
                                                    } else {
                                                        Text(
                                                            text = "DECOMPOSED",
                                                            style = Typography.labelSmall,
                                                            fontFamily = FontFamily.Default,
                                                            fontSize = 7.5.sp,
                                                            color = Color.Red
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                for (row in 0 until 2) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        for (col in 0 until 4) {
                                                            val idx = row * 4 + col
                                                            val gene = packet.genes.getOrNull(idx)
                                                            if (gene != null) {
                                                                val isNew = packet.newGenes.contains(gene)
                                                                val borderStroke = if (isNew) BorderStroke(1.dp, Color(0xFF22D3EE).copy(alpha = alpha)) else null
                                                                
                                                                Box(
                                                                    modifier = Modifier
                                                                        .weight(1f)
                                                                        .then(if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(2.dp)) else Modifier)
                                                                        .background(Color.Transparent)
                                                                        .clickable {
                                                                            selectedPacketByGene = gene
                                                                            viewModel.synthManager.playBeep(330f, 0.04f, "sine")
                                                                        }
                                                                        .padding(vertical = 12.dp),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(
                                                                        text = gene,
                                                                        style = Typography.labelSmall,
                                                                        fontSize = 7.5.sp,
                                                                        color = if (isNew) Color(0xFF22D3EE).copy(alpha = alpha) else Color(0xFF00FF41),
                                                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                                        fontWeight = FontWeight.Bold
                                                                    )
                                                                }
                                                            } else {
                                                                Spacer(modifier = Modifier.weight(1f))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Footer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFF00FF41).copy(alpha = alpha))
                            )
                            Text(
                                text = "STANDARD GENE SEARCH ACTIVE",
                                color = Color(0xFF00FF41).copy(alpha = 0.8f),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "SECURE CONNECTION: AIS-DEV-ENV",
                            color = Color(0xFF005511),
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ==========================================
        // OVERLAY 3: DECRYPTED ANOMALY VAULT
        // ==========================================
        if (showAnomalyVaultOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .border(2.dp, Color(0xFFA855F7), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ANOMALY DISCOVERY LOG",
                            style = Typography.bodyMedium,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD8B4FE)
                        )
                        Text(
                            text = "✕ CLOSE",
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red,
                            modifier = Modifier.clickable {
                                showAnomalyVaultOverlay = false
                                selectedAnomalousGene = null
                            }
                        )
                    }

                    // Get all anomalous genes
                    val anomalousGenes = geneSequences.filter { WaveMath.isAnomalousGene(it.sequence) }

                    if (anomalousGenes.isEmpty()) {
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text(
                                text = "ANOMALY DISCOVERY LOG EMPTY.\nENGAGE ANOMALY ENGINE TO ATTAIN ANOMALOUS GENES.",
                                color = Color(0xFF701A75),
                                style = Typography.bodySmall,
                                fontFamily = FontFamily.Default,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(anomalousGenes) { gene ->
                                val benefit = WaveMath.getBenefitForAnomalousGene(gene.sequence)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFF4A125E), RoundedCornerShape(4.dp))
                                        .background(Color(0xFF1E0B36))
                                        .clickable {
                                            selectedAnomalousGene = gene.sequence
                                            viewModel.synthManager.playBeep(587f, 0.05f, "triangle")
                                        }
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = gene.sequence,
                                                style = Typography.bodyMedium,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFD8B4FE)
                                            )
                                            Text(
                                                text = benefit.name,
                                                style = Typography.bodySmall,
                                                fontFamily = FontFamily.Default,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFA855F7)
                                            )
                                            Text(
                                                text = benefit.description,
                                                style = Typography.labelSmall,
                                                fontFamily = FontFamily.Default,
                                                color = Color(0xFFCCC2DC),
                                                lineHeight = 12.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "QTY: ",
                                                style = Typography.bodyMedium,
                                                fontFamily = FontFamily.Default,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFD8B4FE)
                                            )
                                            Text(
                                                text = "x${gene.count}",
                                                style = Typography.bodyMedium,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFD8B4FE)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // SUB-DIAGNOSTIC POPUP: GENE DETAILS
        // ==========================================
        selectedPacketByGene?.let { gene ->
            val isAnom = WaveMath.isAnomalousGene(gene)
            val benefit = if (isAnom) WaveMath.getBenefitForAnomalousGene(gene) else null
            
            AlertDialog(
                onDismissRequest = { selectedPacketByGene = null },
                containerColor = if (isAnom) Color(0xFF150B24) else activePanel,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isAnom) {
                            WireframeGalaxy(
                                color = Color(0xFFD8B4FE),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "ANOMALOUS GENE",
                                style = Typography.titleMedium,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD8B4FE)
                            )
                        } else {
                            WireframeDna(
                                color = activeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "STANDARD GENE",
                                style = Typography.titleMedium,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                                color = activeColor
                            )
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "SEQUENCE: $gene",
                            style = Typography.bodyMedium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isAnom) Color(0xFFD8B4FE) else activeColor
                        )

                        if (isAnom && benefit != null) {
                            Text(
                                text = "CLASSIFICATION: SYNODIC ANOMALOUS UNIT",
                                style = Typography.bodySmall,
                                fontFamily = FontFamily.Default,
                                color = Color(0xFFA855F7),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "PASSIVE ACTION: ${benefit.name.uppercase()}",
                                style = Typography.bodySmall,
                                fontFamily = FontFamily.Default,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = benefit.description.uppercase(),
                                style = Typography.labelSmall,
                                fontFamily = FontFamily.Default,
                                color = Color.LightGray,
                                lineHeight = 12.sp
                            )
                        } else {
                            Text(
                                text = "CLASSIFICATION: STABLE NUCLEOTIDE UNIT",
                                style = Typography.bodySmall,
                                fontFamily = FontFamily.Default,
                                color = activeColorDim,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "THIS SYNTHETIC BASE HAS SUCCESSFULLY BONDED WITH ORGANIC NUCLEOTIDES IN THE REACTOR. STABILITY OF FURTHER SPLICING WILL NOT BE ADVERSELY AFFECTED.",
                                style = Typography.labelSmall,
                                fontFamily = FontFamily.Default,
                                color = Color.LightGray,
                                lineHeight = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { selectedPacketByGene = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAnom) Color(0xFFA855F7) else activeColor,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("CLOSE DIAGNOSTIC", style = Typography.labelSmall, fontFamily = FontFamily.Default)
                    }
                }
            )
        }

        // ==========================================
        // SUB-DIAGNOSTIC POPUP: STEP-SEARCH GENE DETAILS
        // ==========================================
        stepSearchSelectedGene?.let { gene ->
            val isAnom = WaveMath.isAnomalousGene(gene)
            val benefit = if (isAnom) WaveMath.getBenefitForAnomalousGene(gene) else null
            
            AlertDialog(
                onDismissRequest = { stepSearchSelectedGene = null },
                containerColor = if (isAnom) Color(0xFF150B24) else activePanel,
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isAnom) {
                            WireframeGalaxy(
                                color = Color(0xFFD8B4FE),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "DECRYPTED ANOMALY MOLECULE",
                                style = Typography.titleMedium,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD8B4FE)
                            )
                        } else {
                            WireframeDna(
                                color = activeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "MOLECULAR STOCK DETAILED VIEW",
                                style = Typography.titleMedium,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                                color = activeColor
                            )
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "SEQUENCE: $gene",
                            style = Typography.bodyMedium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isAnom) Color(0xFFD8B4FE) else activeColor
                        )

                        if (isAnom && benefit != null) {
                            Text(
                                text = "TACTICAL PERK: ${benefit.name.uppercase()}",
                                style = Typography.bodySmall,
                                fontFamily = FontFamily.Default,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = benefit.description.uppercase(),
                                style = Typography.labelSmall,
                                fontFamily = FontFamily.Default,
                                color = Color.LightGray,
                                lineHeight = 12.sp
                            )
                        } else {
                            Text(
                                text = "STANDARD STABLE SEQUENCE. IDEAL FOR CREATURE TELOMERE REINFORCEMENT OPERATIONS.",
                                style = Typography.labelSmall,
                                fontFamily = FontFamily.Default,
                                color = Color.LightGray,
                                lineHeight = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { stepSearchSelectedGene = null },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAnom) Color(0xFFA855F7) else activeColor,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("DISMISS", style = Typography.labelSmall, fontFamily = FontFamily.Default)
                    }
                }
            )
        }

        // ==========================================
        // SUB-DIAGNOSTIC POPUP: VAULT GENE DETAILS
        // ==========================================
        selectedAnomalousGene?.let { gene ->
            val benefit = WaveMath.getBenefitForAnomalousGene(gene)
            
            AlertDialog(
                onDismissRequest = { selectedAnomalousGene = null },
                containerColor = Color(0xFF150B24),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WireframeGalaxy(
                            color = Color(0xFFD8B4FE),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "ANOMALY QUANTUM DECRYPT",
                            style = Typography.titleMedium,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD8B4FE)
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "SEQUENCE: $gene",
                            style = Typography.bodyMedium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD8B4FE)
                        )
                        Text(
                            text = "PERK ID: ${benefit.id.uppercase()}",
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            color = Color(0xFFA855F7)
                        )
                        Text(
                            text = "DESIGNATION: ${benefit.name.uppercase()}",
                            style = Typography.bodySmall,
                            fontFamily = FontFamily.Default,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = benefit.description.uppercase(),
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            color = Color.LightGray,
                            lineHeight = 12.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { selectedAnomalousGene = null },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7), contentColor = Color.White),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("DISMISS", style = Typography.labelSmall, fontFamily = FontFamily.Default)
                    }
                }
            )
        }
    }
}

// ==========================================
// 2. SPLICER VIEW
// ==========================================
@Composable
fun SplicerView(viewModel: MainViewModel) {
    val targetSequence by viewModel.targetSequence.collectAsState()
    val splicerSlots by viewModel.splicerSlots.collectAsState()
    val activeSlotSelection by viewModel.activeSlotSelection.collectAsState()
    val slotSequenceFilter by viewModel.slotSequenceFilter.collectAsState()
    val isReactorFrozen by viewModel.isReactorFrozen.collectAsState()
    val reactorFreezeTimeLeft by viewModel.reactorFreezeTimeLeft.collectAsState()
    val isForcedConstructionActive by viewModel.isForcedConstructionActive.collectAsState()
    val isForcedLoopActive by viewModel.isForcedLoopActive.collectAsState()
    val forcedConstructionLogs by viewModel.forcedConstructionLogs.collectAsState()
    val isSplicing by viewModel.isSplicing.collectAsState()
    val splicingProgress by viewModel.splicingProgress.collectAsState()
    val inventoryGenes by viewModel.geneSequences.collectAsState()

    if (isForcedConstructionActive || isSplicing) {
        SplicerLeftPanel(
            viewModel = viewModel,
            targetSequence = targetSequence,
            splicerSlots = splicerSlots,
            activeSlotSelection = activeSlotSelection,
            isReactorFrozen = isReactorFrozen,
            reactorFreezeTimeLeft = reactorFreezeTimeLeft,
            isForcedConstructionActive = isForcedConstructionActive,
            isForcedLoopActive = isForcedLoopActive,
            forcedConstructionLogs = forcedConstructionLogs,
            isSplicing = isSplicing,
            splicingProgress = splicingProgress,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWide = maxWidth > 600.dp
            if (isWide) {
                Row(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1.2f).fillMaxHeight()) {
                        SplicerLeftPanel(
                            viewModel = viewModel,
                            targetSequence = targetSequence,
                            splicerSlots = splicerSlots,
                            activeSlotSelection = activeSlotSelection,
                            isReactorFrozen = isReactorFrozen,
                            reactorFreezeTimeLeft = reactorFreezeTimeLeft,
                            isForcedConstructionActive = isForcedConstructionActive,
                            isForcedLoopActive = isForcedLoopActive,
                            forcedConstructionLogs = forcedConstructionLogs,
                            isSplicing = isSplicing,
                            splicingProgress = splicingProgress,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(modifier = Modifier.weight(0.8f).fillMaxHeight()) {
                        SplicerRightPanel(
                            viewModel = viewModel,
                            activeSlotSelection = activeSlotSelection,
                            targetSequence = targetSequence,
                            slotSequenceFilter = slotSequenceFilter,
                            inventoryGenes = inventoryGenes,
                            isWide = isWide
                        )
                    }
                }
            } else {
                val rightPanelHeight = if (activeSlotSelection != null) 140.dp else 75.dp
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                        SplicerLeftPanel(
                            viewModel = viewModel,
                            targetSequence = targetSequence,
                            splicerSlots = splicerSlots,
                            activeSlotSelection = activeSlotSelection,
                            isReactorFrozen = isReactorFrozen,
                            reactorFreezeTimeLeft = reactorFreezeTimeLeft,
                            isForcedConstructionActive = isForcedConstructionActive,
                            isForcedLoopActive = isForcedLoopActive,
                            forcedConstructionLogs = forcedConstructionLogs,
                            isSplicing = isSplicing,
                            splicingProgress = splicingProgress,
                            modifier = Modifier.fillMaxWidth().wrapContentHeight()
                        )
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(rightPanelHeight)) {
                        SplicerRightPanel(
                            viewModel = viewModel,
                            activeSlotSelection = activeSlotSelection,
                            targetSequence = targetSequence,
                            slotSequenceFilter = slotSequenceFilter,
                            inventoryGenes = inventoryGenes,
                            isWide = isWide
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SplicerLeftPanel(
    viewModel: MainViewModel,
    targetSequence: String,
    splicerSlots: List<String?>,
    activeSlotSelection: Int?,
    isReactorFrozen: Boolean,
    reactorFreezeTimeLeft: Float,
    isForcedConstructionActive: Boolean,
    isForcedLoopActive: Boolean,
    forcedConstructionLogs: List<String>,
    isSplicing: Boolean,
    splicingProgress: Int,
    modifier: Modifier = Modifier
) {
    if (isForcedConstructionActive) {
        // Emergency Forced Compile Terminal Screen
        Column(
            modifier = modifier
                .border(1.dp, Color(0xFF991B1B), RoundedCornerShape(4.dp))
                .background(Color.Black)
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "[ FORCED SEQUENCING ACTIVE ]",
                        color = Color.Red,
                        style = Typography.labelSmall,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "P.O.X. REACTOR ACTIVE (OVERRIDE)",
                    color = Color.White,
                    style = Typography.bodyMedium,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                // CRT scrolling terminal logs container
                val displayedLogs = remember { mutableStateListOf<String>() }
                LaunchedEffect(forcedConstructionLogs) {
                    if (forcedConstructionLogs.isEmpty()) {
                        displayedLogs.clear()
                    } else {
                        val isNewRun = displayedLogs.size > forcedConstructionLogs.size ||
                                (displayedLogs.isNotEmpty() && forcedConstructionLogs.firstOrNull() != displayedLogs.firstOrNull())
                        val startIdx = if (isNewRun) {
                            displayedLogs.clear()
                            0
                        } else {
                            displayedLogs.size
                        }
                        for (i in startIdx until forcedConstructionLogs.size) {
                            delay(150L)
                            displayedLogs.add(forcedConstructionLogs[i])
                        }
                    }
                }

                val listState = rememberLazyListState()
                LaunchedEffect(displayedLogs.size) {
                    if (displayedLogs.isNotEmpty()) {
                        listState.animateScrollToItem(displayedLogs.size - 1)
                    }
                }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF050505))
                        .border(1.dp, Color(0xFF2D0A0A), RoundedCornerShape(2.dp))
                        .padding(6.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(displayedLogs) { log ->
                        val isAlert = log.contains("FAILED") || log.contains("WARNING") || log.contains("Failed") || log.contains("Sacrificed")
                        Text(
                            text = log,
                            color = if (isAlert) Color(0xFFFCA5A5) else Color(0xFF34D399),
                            style = Typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!isForcedLoopActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x20EF4444))
                            .border(1.dp, Color(0x50EF4444), RoundedCornerShape(2.dp))
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.Red)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "P.O.X. REACTOR FROZEN: ${reactorFreezeTimeLeft}s REMAINING",
                            color = Color(0xFFFCA5A5),
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (isForcedLoopActive) {
                    Button(
                        onClick = { viewModel.setIsForcedLoopActive(false) },
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x30B91C1C), contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        shape = RoundedCornerShape(2.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "✕ EXIT AUTO-SYNTHESIS LOOP CASCADE",
                            style = Typography.bodySmall,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    } else if (isSplicing) {
        // Splicing Morphogenesis Screen
        Column(
            modifier = modifier
                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                .background(CyberPanel)
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val infiniteTransition = rememberInfiniteTransition()
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )

            // Custom rotating Canvas double helix DNA animation
            Canvas(modifier = Modifier.size(130.dp).rotate(rotation)) {
                val w = size.width
                val h = size.height
                val points = 8
                val amplitude = 36f
                for (i in 0..points) {
                    val t = i.toFloat() / points
                    val y = t * h
                    val angle = t * 2 * Math.PI.toFloat()
                    val x1 = w / 2 + amplitude * sin(angle)
                    val x2 = w / 2 - amplitude * sin(angle)

                    val slotIdx = if (i == points) points - 1 else i
                    val segmentProgressThreshold = (slotIdx + 1) * 12.5f
                    val isSegmentLoaded = splicingProgress >= segmentProgressThreshold

                    val targetSeg = if (slotIdx * 8 + 8 <= targetSequence.length) {
                        targetSequence.substring(slotIdx * 8, slotIdx * 8 + 8)
                    } else {
                        "--------"
                    }
                    val isAnom = WaveMath.isAnomalousGene(targetSeg)

                    val segmentColor = if (isAnom) {
                        Color(0xFFA855F7)
                    } else {
                        when (slotIdx % 4) {
                            0 -> CyberGreen
                            1 -> Color(0xFFFBBF24)
                            2 -> Color(0xFF60A5FA)
                            else -> Color(0xFFC084FC)
                        }
                    }

                    val colorNode1 = if (isSegmentLoaded) {
                        if (sin(angle) > 0) segmentColor else Color(0xFFA855F7)
                    } else {
                        Color.DarkGray
                    }

                    val colorNode2 = if (isSegmentLoaded) {
                        if (sin(angle) > 0) Color(0xFFA855F7) else segmentColor
                    } else {
                        Color.DarkGray
                    }

                    val lineColor = if (isSegmentLoaded) {
                        segmentColor.copy(alpha = 0.4f)
                    } else {
                        Color.DarkGray.copy(alpha = 0.2f)
                    }

                    drawLine(
                        color = lineColor,
                        start = Offset(x1, y),
                        end = Offset(x2, y),
                        strokeWidth = 1.5.dp.toPx()
                    )
                    drawCircle(color = colorNode1, radius = 5.dp.toPx(), center = Offset(x1, y))
                    drawCircle(color = colorNode2, radius = 5.dp.toPx(), center = Offset(x2, y))
                }
            }


            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "P.O.X. REACTOR ENGAGED",
                color = CyberGreen,
                style = Typography.bodyMedium,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Sequencing P.O.X. genome using synthesized genes...",
                color = CyberGreenDim,
                style = Typography.bodySmall,
                fontFamily = FontFamily.Default,
                textAlign = TextAlign.Center,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Synthesis Assembly Matrix
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .border(1.dp, CyberBorder.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SYNTHESIS ASSEMBLY MATRIX",
                    color = CyberGreenDim,
                    style = Typography.labelSmall,
                    fontFamily = FontFamily.Default,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                for (i in 0 until 8) {
                    val segmentProgressThreshold = (i + 1) * 12.5f
                    val isSegmentLoaded = splicingProgress >= segmentProgressThreshold
                    val targetSeg = if (i * 8 + 8 <= targetSequence.length) {
                        targetSequence.substring(i * 8, i * 8 + 8)
                    } else {
                        "--------"
                    }
                    val isAnom = WaveMath.isAnomalousGene(targetSeg)
                    val color = if (isAnom) {
                        Color(0xFFA855F7)
                    } else {
                        when (i % 4) {
                            0 -> CyberGreen
                            1 -> Color(0xFFFBBF24)
                            2 -> Color(0xFF60A5FA)
                            else -> Color(0xFFC084FC)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(26.dp)
                            .border(
                                width = 1.dp,
                                color = if (isSegmentLoaded) color.copy(alpha = 0.6f) else Color.DarkGray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(2.dp)
                            )
                            .background(if (isSegmentLoaded) color.copy(alpha = 0.08f) else Color.Transparent)
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SLOT #${i + 1}",
                            color = if (isSegmentLoaded) color.copy(alpha = 0.8f) else Color.Gray,
                            style = Typography.bodySmall,
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )

                        if (isSegmentLoaded) {
                            Text(
                                text = targetSeg,
                                color = color,
                                style = Typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp
                            )
                        } else {
                            Text(
                                text = "SPLICING SEQUENCE...",
                                color = Color.DarkGray,
                                style = Typography.bodySmall,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Default
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "SPLICING PACKETS BUFFER: $splicingProgress%",
                color = CyberGreenDim,
                fontFamily = FontFamily.Default,
                style = Typography.labelSmall
            )
        }
    } else {
        // Standard Re-sequencing grid layout panel
        Column(
            modifier = modifier
                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                .background(CyberPanel)
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "[ G.E.N. P.O.X. E-MERGE SEQUENCER v1.7 ]",
                        color = CyberGreenDim,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default
                    )
                    Text(
                        text = "SYSTEMS ON",
                        color = CyberGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default
                    )
                }

                Text(
                    text = "SINGLE-NODE SEQUENCING",
                    color = Color.White,
                    style = Typography.bodyMedium,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Fill all slots with stockpiled genes to assemble the target genome.",
                    color = CyberGreenDim,
                    style = Typography.bodySmall,
                    fontFamily = FontFamily.Default,
                    fontSize = 10.sp
                )

                // 8 Slots grid (arranged as 4 cols, 2 rows)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black)
                        .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                        .padding(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (idx in 0..3) {
                            Box(modifier = Modifier.weight(1f)) {
                                SplicerSlotCell(
                                    idx = idx,
                                    slot = splicerSlots[idx],
                                    isSelected = activeSlotSelection == idx,
                                    onClick = { viewModel.selectSplicerSlot(idx) },
                                    onEject = { viewModel.ejectGeneFromSlot(idx) }
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (idx in 4..7) {
                            Box(modifier = Modifier.weight(1f)) {
                                SplicerSlotCell(
                                    idx = idx,
                                    slot = splicerSlots[idx],
                                    isSelected = activeSlotSelection == idx,
                                    onClick = { viewModel.selectSplicerSlot(idx) },
                                    onEject = { viewModel.ejectGeneFromSlot(idx) }
                                )
                            }
                        }
                    }
                }

                // Target genome required sequence
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF050C06))
                        .border(1.dp, Color(0x4000FF41), RoundedCornerShape(4.dp))
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "[ REQUIRED TARGET SEQUENCE ]",
                            color = CyberGreen,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "64-CHAR GENOME GOAL",
                            color = CyberGreenDim,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 8.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    // Renders 8 blocks of 8 bases in a centered 2x4 grid
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (row in 0 until 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (col in 0 until 4) {
                                    val i = row * 4 + col
                                    val segment = targetSequence.substring(i * 8, (i + 1) * 8)
                                    val isAnom = WaveMath.isAnomalousGene(segment)
                                    val color = if (isAnom) {
                                        Color(0xFFA855F7)
                                    } else {
                                        when (i % 4) {
                                            0 -> CyberGreen
                                            1 -> Color(0xFFFBBF24)
                                            2 -> Color(0xFF60A5FA)
                                            else -> Color(0xFFC084FC)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                color = if (isAnom) Color(0x20A855F7) else Color.Transparent,
                                                shape = RoundedCornerShape(2.dp)
                                            )
                                            .border(
                                                width = if (isAnom) 1.dp else 0.dp,
                                                color = if (isAnom) Color(0x30A855F7) else Color.Transparent,
                                                shape = RoundedCornerShape(2.dp)
                                            )
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = segment,
                                            color = color,
                                            style = Typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            letterSpacing = 1.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Current spliced specimen DNA
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF050C06))
                        .border(1.dp, Color(0x4000FF41), RoundedCornerShape(4.dp))
                        .padding(6.dp)
                ) {
                    Text(
                        text = "[ CURRENT SPLICED SEQUENCE ]",
                        color = CyberGreen,
                        style = Typography.labelSmall,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (row in 0 until 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (col in 0 until 4) {
                                    val i = row * 4 + col
                                    val slotGene = splicerSlots.getOrNull(i)
                                    val segment = slotGene ?: "--------"
                                    val isAnom = if (slotGene != null) {
                                        val targetSegment = targetSequence.substring(i * 8, (i + 1) * 8)
                                        WaveMath.isAnomalousGene(targetSegment)
                                    } else false
                                    
                                    val color = if (slotGene == null) {
                                        Color.Gray
                                    } else if (isAnom) {
                                        Color(0xFFA855F7)
                                    } else {
                                        when (i % 4) {
                                            0 -> CyberGreen
                                            1 -> Color(0xFFFBBF24)
                                            2 -> Color(0xFF60A5FA)
                                            else -> Color(0xFFC084FC)
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                color = if (isAnom) Color(0x20A855F7) else Color.Transparent,
                                                shape = RoundedCornerShape(2.dp)
                                            )
                                            .border(
                                                width = if (isAnom) 1.dp else 0.dp,
                                                color = if (isAnom) Color(0x30A855F7) else Color.Transparent,
                                                shape = RoundedCornerShape(2.dp)
                                            )
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = segment,
                                            color = color,
                                            style = Typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            letterSpacing = 1.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Synthesizer Actions and Forced Emergency Overrides
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.autofillSplicerSlots() },
                        modifier = Modifier.height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0x8000FF41)),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text(text = "AUTO SLOT", style = Typography.bodySmall, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold)
                    }

                    val hasEmpty = splicerSlots.contains(null)
                    Button(
                        onClick = { viewModel.constructSplicedCreature() },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasEmpty) Color(0x1000FF41) else CyberGreen,
                            contentColor = if (hasEmpty) CyberGreenDim else Color.Black
                        ),
                        border = BorderStroke(1.dp, if (hasEmpty) Color(0x3000FF41) else CyberBorder),
                        shape = RoundedCornerShape(2.dp),
                        enabled = !hasEmpty
                    ) {
                        Text(
                            text = if (hasEmpty) "FILL EMPTY SLOTS OR FORCE" else "SEQUENCE P.O.X. GENOME",
                            style = Typography.bodySmall,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { viewModel.startForcedConstruction() },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x20B91C1C), contentColor = Color(0xFFEF4444)),
                        border = BorderStroke(1.dp, Color(0xFF7F1D1D)),
                        shape = RoundedCornerShape(2.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(text = "FORCE SINGLE", style = Typography.bodySmall, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { viewModel.setIsForcedLoopActive(true); viewModel.startForcedConstruction() },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x30B91C1C), contentColor = Color(0xFFFCA5A5)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        shape = RoundedCornerShape(2.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(text = "FORCE AUTO-LOOP", style = Typography.bodySmall, fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                Text(
                    text = "WARNING: FREEZES BIO-LAB REACTOR FOR 8S; GENE BLOCK ATTRITION IS 37.5%",
                    color = Color(0xFFFCA5A5),
                    style = Typography.bodySmall,
                    fontFamily = FontFamily.Default,
                    fontSize = 8.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                val devForceAnomaly by viewModel.devForceAnomaly.collectAsState()
                if (devForceAnomaly) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { viewModel.devInjectMissingTargetGenes() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x20A855F7),
                            contentColor = Color(0xFFD8B4FE)
                        ),
                        border = BorderStroke(1.dp, Color(0x80A855F7)),
                        shape = RoundedCornerShape(2.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "DEV: INJECT MISSING GENES",
                            style = Typography.bodySmall,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SplicerSlotCell(
    idx: Int,
    slot: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEject: () -> Unit
) {
    val isAnom = slot?.let { WaveMath.isAnomalousGene(it) } ?: false
    val borderStroke = when {
        isSelected -> BorderStroke(1.5.dp, if (isAnom) Color(0xFFA855F7) else CyberGreen)
        slot != null -> BorderStroke(1.dp, if (isAnom) Color(0xFF701A75) else Color(0xFF064E3B))
        else -> BorderStroke(1.dp, Color(0x3000FF41))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .border(borderStroke, RoundedCornerShape(4.dp))
            .background(
                if (isSelected) {
                    if (isAnom) Color(0x30A855F7) else Color(0x2000FF41)
                } else {
                    if (isAnom) Color(0x15A855F7) else Color(0xFF080808)
                }
            )
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        Text(
            text = "#${idx + 1}",
            color = if (isAnom) Color(0xFFC084FC) else CyberGreenDim,
            style = Typography.bodySmall,
            fontFamily = FontFamily.Default,
            fontSize = 8.sp,
            modifier = Modifier.align(Alignment.TopStart)
        )

        if (slot != null) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = slot.take(4),
                    color = if (isAnom) Color(0xFFD8B4FE) else CyberGreen,
                    style = Typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
                Text(
                    text = slot.substring(4),
                    color = if (isAnom) Color(0xFFA855F7) else CyberGreenDim,
                    style = Typography.bodySmall,
                    fontSize = 8.sp,
                    letterSpacing = 1.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(12.dp)
                    .align(Alignment.TopEnd)
                    .clickable { onEject() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    color = Color.Red,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text(
                text = "----",
                color = Color(0xFF1B2B1B),
                style = Typography.bodySmall,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun SplicerRightPanel(
    viewModel: MainViewModel,
    activeSlotSelection: Int?,
    targetSequence: String,
    slotSequenceFilter: String,
    inventoryGenes: List<GeneSequence>,
    isWide: Boolean
) {
    if (activeSlotSelection != null) {
        val expected = targetSequence.substring(activeSlotSelection * 8, (activeSlotSelection + 1) * 8)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                .background(CyberPanel)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ASSIGN GENE BLOCK TO SLOT #${activeSlotSelection + 1}",
                    color = CyberGreen,
                    style = Typography.labelSmall,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "[ CANCEL ]",
                    color = Color.Red,
                    style = Typography.labelSmall,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { viewModel.selectSplicerSlot(null) }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            // Expected gene info block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x2000FF41))
                    .border(1.dp, Color(0x5000FF41), RoundedCornerShape(2.dp))
                    .padding(4.dp)
            ) {
                Text(
                    text = "[ REQUIRED SEGMENT FOR SLOT #${activeSlotSelection + 1} ]",
                    color = CyberGreen,
                    style = Typography.labelSmall,
                    fontFamily = FontFamily.Default,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = expected,
                    color = Color(0xFF22D3EE),
                    style = Typography.bodyMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            val matchingGene = inventoryGenes.find { it.sequence == expected && it.count > 0 }

            if (matchingGene != null) {
                // Stock panel matching Required Segment block style
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x1010B981))
                        .border(1.dp, Color(0xFF059669), RoundedCornerShape(2.dp))
                        .padding(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "[ AVAILABLE STOCK ]",
                            color = Color(0xFF34D399),
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "MUTATE READY",
                            color = Color(0xFF34D399),
                            style = Typography.bodySmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = matchingGene.sequence,
                            color = CyberGreen,
                            style = Typography.bodyMedium,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "x${matchingGene.count}",
                            color = CyberGreen,
                            style = Typography.bodySmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = { viewModel.assignGeneToSlot(matchingGene.sequence) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(2.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "LOAD GENE BLOCK",
                        style = Typography.bodySmall,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                }
            } else {
                // No stock panel matching Required Segment block style
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x15EF4444))
                        .border(1.dp, Color(0x50EF4444), RoundedCornerShape(2.dp))
                        .padding(6.dp)
                ) {
                    Text(
                        text = "[ AVAILABLE STOCK ]",
                        color = Color(0xFFFCA5A5),
                        style = Typography.labelSmall,
                        fontFamily = FontFamily.Default,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "NO COMPATIBLE SEGMENT FOUND",
                        color = Color.Red,
                        style = Typography.bodyMedium,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    } else {
        // Empty State Panel
        Column(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                .background(CyberPanel)
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Folder representation
            Text(
                text = "[ NO ACTIVE CONSTRUCTOR SLOT SELECTION ]",
                color = CyberGreen,
                style = Typography.bodySmall,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isWide) "CHOOSE A GENE SLOT ON THE LEFT PANEL TO DISPLAY COMPATIBLE GENES"
                       else "CHOOSE A GENE SLOT ABOVE TO DISPLAY COMPATIBLE GENES",
                color = CyberGreenDim,
                style = Typography.bodySmall,
                fontFamily = FontFamily.Default,
                fontSize = 9.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==========================================
// 3. GEN-VAULT (LIBRARY) VIEW
// ==========================================
@Composable
fun VaultView(viewModel: MainViewModel) {
    val creatures by viewModel.creatures.collectAsState()
    val activeCreature by viewModel.activeCreature.collectAsState()
    val targetSequence by viewModel.targetSequence.collectAsState()
    val geneSequences by viewModel.geneSequences.collectAsState()
    val discoveredPacketsLog by viewModel.discoveredPacketsLog.collectAsState()
    val defenderId by viewModel.defenderCreatureId.collectAsState()

    // Local UI states
    var applyLibFilters by remember { mutableStateOf(false) }
    var isFilterPanelExpanded by remember { mutableStateOf(false) }
    var viewingArchiveSearch by remember { mutableStateOf(false) }
    var libSortBy by remember { mutableStateOf("name-asc") }
    var libFilterFaction by remember { mutableStateOf("ALL") }
    var libFilterType by remember { mutableStateOf("ALL") }
    var libFilterTag by remember { mutableStateOf("ALL") }

    // Unique types extractor
    val uniqueTypes = remember(creatures) {
        creatures.map { it.type }.distinct().filter { it.isNotEmpty() }
    }

    // Filter and Sort logic
    val filteredSortedCreatures = remember(
        creatures, applyLibFilters, libSortBy, libFilterFaction, libFilterType, libFilterTag, targetSequence, defenderId
    ) {
        var result = creatures
        if (applyLibFilters) {
            if (libFilterFaction != "ALL") {
                result = result.filter { it.faction.equals(libFilterFaction, ignoreCase = true) }
            }
            if (libFilterType != "ALL") {
                result = result.filter { it.type.equals(libFilterType, ignoreCase = true) }
            }
            if (libFilterTag != "ALL") {
                result = result.filter { getCreatureTags(it, targetSequence, defenderId).contains(libFilterTag) }
            }
        }

        when (libSortBy) {
            "name-asc" -> result.sortedBy { it.name }
            "name-desc" -> result.sortedByDescending { it.name }
            "type-asc" -> result.sortedBy { it.type }
            "type-desc" -> result.sortedByDescending { it.type }
            "faction-asc" -> result.sortedBy { it.faction }
            "vitality-desc" -> result.sortedByDescending { it.vitality }
            "attack-desc" -> result.sortedByDescending { it.attack }
            "defense-desc" -> result.sortedByDescending { it.defense }
            "speed-desc" -> result.sortedByDescending { it.speed }
            "tags-desc" -> result.sortedByDescending { getCreatureTags(it, targetSequence, defenderId).size }
            else -> result
        }
    }

    if (creatures.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "GEN-VAULT DATABANKS OFFLINE.\nSEED GENETIC HOSTS IN COMBINATOR.",
                color = CyberGreenDim,
                style = Typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (activeCreature == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                    .background(CyberPanel)
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                // 1. Registry Header (Aligned stacked layout for mobile screens)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "[ G.E.N. P.O.X. SEABED VAULT v0.4 ]",
                                color = CyberGreenDim,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "SYSTEMS ON",
                                color = CyberGreen,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "NODE P.O.X. SEQUENCES REGISTRY",
                            color = Color.White,
                            style = Typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                        )
                        Text(
                            text = "View or filter your spliced P.O.X. sequences below",
                            color = CyberGreenDim,
                            fontSize = 8.5.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
 
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.Start),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .border(1.dp, if (applyLibFilters) CyberGreen else Color.DarkGray, RoundedCornerShape(2.dp))
                                .background(if (applyLibFilters) Color(0xFF00FF41).copy(alpha = 0.15f) else Color.Black)
                                .clickable {
                                    viewModel.synthManager.playBeep(480f, 0.05f, "sine")
                                    applyLibFilters = !applyLibFilters
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(RoundedCornerShape(2.5.dp))
                                        .background(if (applyLibFilters) CyberGreen else Color.DarkGray)
                                )
                                Text(
                                    text = if (applyLibFilters) "FILTERS ACTIVE" else "FILTERS BYPASSED",
                                    color = if (applyLibFilters) CyberGreen else Color.Gray,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
 
                        val activeFiltersCount = (if (libFilterFaction != "ALL") 1 else 0) +
                                (if (libFilterType != "ALL") 1 else 0) +
                                (if (libFilterTag != "ALL") 1 else 0)
                        val exploreText = if (activeFiltersCount > 0) {
                            "◆ EXPLORE FILTERS ($activeFiltersCount)"
                        } else {
                            "◆ EXPLORE FILTERS"
                        }
 
                        Box(
                            modifier = Modifier
                                .border(1.dp, CyberBorder, RoundedCornerShape(2.dp))
                                .background(Color.Black)
                                .clickable {
                                    viewModel.synthManager.playBeep(520f, 0.05f, "sine")
                                    isFilterPanelExpanded = !isFilterPanelExpanded
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isFilterPanelExpanded) "◆ COLLAPSE FILTERS" else exploreText,
                                color = CyberGreen,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "ACTIVE SELECTIONS: ",
                                color = CyberGreen,
                                fontSize = 8.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${filteredSortedCreatures.size} / ${creatures.size}",
                                color = CyberGreen,
                                fontSize = 8.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 3. Collapsible Filter Console
                if (isFilterPanelExpanded) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    PoxDropdown(
                                        label = "Sort Sequence:",
                                        selectedOption = libSortBy,
                                        options = listOf(
                                            "name-asc" to "Name (A ➔ Z)",
                                            "name-desc" to "Name (Z ➔ A)",
                                            "type-asc" to "Type (A ➔ Z)",
                                            "type-desc" to "Type (Z ➔ A)",
                                            "faction-asc" to "Faction (A ➔ Z)",
                                            "vitality-desc" to "Vitality (High ➔ Low)",
                                            "attack-desc" to "Attack (High ➔ Low)",
                                            "defense-desc" to "Defense (High ➔ Low)",
                                            "speed-desc" to "Speed (High ➔ Low)",
                                            "tags-desc" to "Tag Density"
                                        ),
                                        onOptionSelected = {
                                            viewModel.synthManager.playBeep(480f, 0.02f, "sine")
                                            libSortBy = it
                                        }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    PoxDropdown(
                                        label = "Faction Classifier:",
                                        selectedOption = libFilterFaction,
                                        options = listOf(
                                            "ALL" to "All Factions",
                                            "Infection" to "Infection",
                                            "Mech" to "Mech",
                                            "Parasite" to "Parasite",
                                            "Containment" to "Containment"
                                        ),
                                        onOptionSelected = {
                                            viewModel.synthManager.playBeep(480f, 0.02f, "sine")
                                            libFilterFaction = it
                                        }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    val typeOptions = remember(uniqueTypes) {
                                        listOf("ALL" to "All Creature Types") + uniqueTypes.map { it to it }
                                    }
                                    PoxDropdown(
                                        label = "Creature Type:",
                                        selectedOption = libFilterType,
                                        options = typeOptions,
                                        onOptionSelected = {
                                            viewModel.synthManager.playBeep(480f, 0.02f, "sine")
                                            libFilterType = it
                                        }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    val tagOptions = listOf(
                                        "ALL" to "All Tags",
                                        "FAVORITE" to "Favorite",
                                        "DEFENDER" to "Defender",
                                        "AUTO-HACKER" to "Auto-Hacker",
                                        "FULL COHERENCE" to "Full Coherence",
                                        "NATURAL" to "Natural",
                                        "FORCED" to "Forced",
                                        "ALPHA GENE" to "Alpha Gene",
                                        "MODIFIED" to "Modified",
                                        "ORIGINAL" to "Original",
                                        "TRANSFER-ORIGIN" to "Transfer Origin"
                                    )
                                    PoxDropdown(
                                        label = "Creature Tags:",
                                        selectedOption = libFilterTag,
                                        options = tagOptions,
                                        onOptionSelected = {
                                            viewModel.synthManager.playBeep(480f, 0.02f, "sine")
                                            libFilterTag = it
                                        }
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, Color(0xFF990000), RoundedCornerShape(2.dp))
                                        .background(Color.Black)
                                        .clickable {
                                            viewModel.synthManager.playBeep(420f, 0.1f, "sine")
                                            libSortBy = "name-asc"
                                            libFilterFaction = "ALL"
                                            libFilterType = "ALL"
                                            libFilterTag = "ALL"
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "✕ CLEAR FILTERS",
                                        color = Color.Red,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Base Panel Area (Step 1)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (applyLibFilters) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "SEQUENCES FOUND: ",
                                        color = CyberGreen,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "${filteredSortedCreatures.size}",
                                        color = CyberGreen,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Text(
                                    text = "Matching sequence(s) loaded; click button below to view",
                                    color = CyberGreenDim,
                                    fontSize = 10.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = "NO FILTERS APPLIED",
                                    color = Color.Red,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Enable filters or reconfigure search parameters using the filter options above.",
                                    color = CyberGreenDim,
                                    fontSize = 10.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.synthManager.playBeep(800f, 0.08f, "sine")
                                viewingArchiveSearch = true
                            },
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberGreen,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Canvas(modifier = Modifier.size(12.dp)) {
                                    drawCircle(
                                        color = Color.Black,
                                        radius = 4.dp.toPx(),
                                        style = Stroke(width = 1.5.dp.toPx()),
                                        center = Offset(5.dp.toPx(), 5.dp.toPx())
                                    )
                                    drawLine(
                                        color = Color.Black,
                                        start = Offset(7.5.dp.toPx(), 7.5.dp.toPx()),
                                        end = Offset(11.dp.toPx(), 11.dp.toPx()),
                                        strokeWidth = 1.5.dp.toPx()
                                    )
                                }
                                Text(
                                    text = "VIEW SEQUENCES",
                                    style = Typography.labelLarge,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                                )
                            }
                        }
                    }
                }

                    // 5. Warning banner
                    Text(
                        text = "WARNING: Trading creatures with other emulators within 30ft transfers custody. Transferred specimens are cleared permanently from memory sectors upon accepted linkage.",
                        color = CyberGreenDim,
                        fontSize = 8.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                        lineHeight = 11.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    )
                }
            }
        } else {
            val c = activeCreature!!
            CreatureDetailCard(c = c, viewModel = viewModel)
        }

        // Overlay is drawn on top when viewingArchiveSearch is true and activeCreature is null
        if (viewingArchiveSearch && activeCreature == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .border(2.dp, CyberGreen, RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Title bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF071A09))
                            .border(1.dp, CyberBorder)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(CyberGreen)
                            )
                            Text(
                                text = "[ P.O.X. SEQUENCE DIRECTORY ]",
                                color = CyberGreen,
                                fontSize = 10.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .border(1.dp, Color(0xFF990000), RoundedCornerShape(2.dp))
                                .background(Color.Black)
                                .clickable {
                                    viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                                    viewingArchiveSearch = false
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "✕ CLOSE",
                                color = Color.Red,
                                fontSize = 8.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Summary metrics header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SYSTEM TARGET MATCHES: ",
                                color = CyberGreen,
                                fontSize = 9.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${filteredSortedCreatures.size}",
                                color = CyberGreen,
                                fontSize = 9.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = " SPECIMENS",
                                color = CyberGreen,
                                fontSize = 9.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "STATUS: GEN-VAULT OPEN",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                        )
                    }

                    // List Area
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(1.dp, CyberBorder)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredSortedCreatures) { item ->
                            val currentTags = remember(item, targetSequence, defenderId) {
                                getCreatureTags(item, targetSequence, defenderId)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CyberBorder, RoundedCornerShape(2.dp))
                                    .background(CyberPanel)
                                    .clickable {
                                        viewModel.synthManager.playBeep(700f, 0.05f, "sine")
                                        viewModel.setActiveCreature(item)
                                    }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = item.name.uppercase(),
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            currentTags.forEach { tag ->
                                                TagBadge(tag = tag)
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.padding(top = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.type.uppercase(),
                                            color = CyberGreenDim,
                                            fontSize = 8.5.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                                        )
                                        Text(
                                            text = "SECTOR: ${item.faction.uppercase()}",
                                            color = CyberGreenDim,
                                            fontSize = 8.5.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                                        )
                                    }
                                }

                                Text(
                                    text = "DECRYPT >",
                                    color = CyberGreen,
                                    fontSize = 9.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreatureDetailCard(
    c: Creature,
    viewModel: MainViewModel
) {
    var showEnlargedQr by remember { mutableStateOf(false) }
    val qrContent = remember(c) { viewModel.encodeCreatureToBase64(c) }

    // Effective stats computations scaled by telomeres
    val telomeres = c.telomeres
    val factor = 0.25f + 0.75f * (telomeres / 100f)
    val effVitality = maxOf(10, Math.round(c.vitality * factor))
    val effAttack = maxOf(5, Math.round(c.attack * factor))
    val effDefense = maxOf(5, Math.round(c.defense * factor))
    val effSpeed = maxOf(5, Math.round(c.speed * factor))
    val isDegraded = telomeres < 100

    val geneSequences by viewModel.geneSequences.collectAsState()
    val targetSequence by viewModel.targetSequence.collectAsState()
    val defenderId by viewModel.defenderCreatureId.collectAsState()
    val openedFrom by viewModel.creatureCardOpenedFrom.collectAsState()
    val activeTags = remember(c, targetSequence, defenderId) {
        getCreatureTags(c, targetSequence, defenderId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // New top-row flavor header with close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "[ G.E.N. P.O.X. SEABED VAULT v0.4 ]",
                color = CyberGreenDim,
                fontSize = 9.sp,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold
            )

            Box(
                modifier = Modifier
                    .border(1.dp, Color(0xFF990000), RoundedCornerShape(2.dp))
                    .background(Color.Black)
                    .clickable {
                        viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                        viewModel.setActiveCreature(null)
                        if (openedFrom == "splicer") {
                            viewModel.selectTab("splicer")
                        } else if (openedFrom == "scanner") {
                            viewModel.selectTab("transceiver")
                        }
                    }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "✕ CLOSE",
                    color = Color.Red,
                    fontSize = 8.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Sub-container with name, type, and badges
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = c.name.uppercase(),
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    color = Color.White,
                    fontSize = 15.sp
                )
                Text(
                    text = c.id,
                    style = Typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = CyberGreen,
                    fontSize = 9.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Creature Type
            Text(
                text = c.type.uppercase(),
                style = Typography.bodySmall,
                color = CyberGreenDim,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                fontSize = 9.5.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Badges
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val factionColor = when (c.faction) {
                    "Infection" -> Color(0xFFEF4444)
                    "Mech" -> Color(0xFF60A5FA)
                    "Parasite" -> Color(0xFFA855F7)
                    else -> Color(0xFF00FF41)
                }
                Box(
                    modifier = Modifier
                        .background(factionColor, RoundedCornerShape(2.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = c.faction.uppercase(),
                        color = Color.Black,
                        style = Typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                        fontSize = 8.5.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    activeTags.forEach { tag ->
                        TagBadge(tag = tag)
                    }
                }
            }
        }

        // Sub-panel layout: Side-by-side ASCII art and QR code
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Box 1: BIO-PHYSICAL RECON
            Column(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, CyberBorder.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .background(Color.Black)
                    .padding(8.dp)
                    .height(115.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "BIO-PHYSICAL RECON",
                    style = Typography.labelSmall,
                    color = CyberGreenDim,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = c.asciiArt.lines().joinToString("\n") { it.trim() },
                        style = Typography.bodySmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = CyberGreen,
                        lineHeight = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Box 2: GENETIC QR COUPLING
            Column(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, CyberBorder.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .background(Color.Black)
                    .clickable {
                        viewModel.synthManager.playBeep(880f, 0.05f, "sine")
                        showEnlargedQr = true
                        viewModel.decreaseCreatureTelomeres(c.id, 15, "QR coupling utilization")
                    }
                    .padding(8.dp)
                    .height(115.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "GENETIC QR COUPLING",
                    style = Typography.labelSmall,
                    color = CyberGreenDim,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    textAlign = TextAlign.Center
                )
                QrRevealVisual(
                    modifier = Modifier
                        .size(60.dp)
                        .padding(vertical = 2.dp)
                )
                Text(
                    text = "[ CLICK TO ENLARGE ]",
                    style = Typography.labelSmall,
                    color = Color.Gray,
                    fontSize = 6.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Box 3: ARMAMENT DESIGNATION
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "ARMAMENT DESIGNATION",
                    style = Typography.labelSmall,
                    color = CyberGreenDim,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                )
                Text(
                    text = c.primaryWeapon,
                    style = Typography.bodyMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "\"${c.lore}\"",
                    style = Typography.bodySmall,
                    color = CyberGreen,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    lineHeight = 14.sp
                )
            }
        }

        // Box 4: COMBAT TELEMETRY STATS & TELOMERES
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // HP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        WireframeHeart(color = Color(0xFFEF4444))
                        Text(
                            text = "VITALITY:",
                            color = CyberGreen,
                            style = Typography.labelSmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                        )
                    }
                    Text(
                        text = buildString {
                            append("$effVitality HP")
                            if (isDegraded) append(" (${c.vitality})")
                        },
                        color = if (isDegraded) Color(0xFFEF4444) else Color.White,
                        style = Typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }

                // Aggression
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        WireframeClaws(color = Color(0xFFF59E0B))
                        Text(
                            text = "AGGRESSION:",
                            color = CyberGreen,
                            style = Typography.labelSmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(6.dp)
                                .background(Color(0xFF262626), RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((effAttack / 100f).coerceIn(0f, 1f))
                                    .background(Color(0xFFF59E0B), RoundedCornerShape(3.dp))
                            )
                        }
                        Text(
                            text = buildString {
                                append("$effAttack")
                                if (isDegraded) append(" (${c.attack})")
                            },
                            color = if (isDegraded) Color(0xFFEF4444) else Color.White,
                            style = Typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }

                // Block Shells
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        WireframeShield(color = Color(0xFF60A5FA))
                        Text(
                            text = "BLOCK SHELLS:",
                            color = CyberGreen,
                            style = Typography.labelSmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(6.dp)
                                .background(Color(0xFF262626), RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((effDefense / 100f).coerceIn(0f, 1f))
                                    .background(Color(0xFF60A5FA), RoundedCornerShape(3.dp))
                            )
                        }
                        Text(
                            text = buildString {
                                append("$effDefense")
                                if (isDegraded) append(" (${c.defense})")
                            },
                            color = if (isDegraded) Color(0xFFEF4444) else Color.White,
                            style = Typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }

                // Speed Rate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        WireframeLightning(color = Color(0xFFFBBF24))
                        Text(
                            text = "SPEED RATE:",
                            color = CyberGreen,
                            style = Typography.labelSmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(6.dp)
                                .background(Color(0xFF262626), RoundedCornerShape(3.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((effSpeed / 100f).coerceIn(0f, 1f))
                                    .background(Color(0xFFFBBF24), RoundedCornerShape(3.dp))
                            )
                        }
                        Text(
                            text = buildString {
                                append("$effSpeed")
                                if (isDegraded) append(" (${c.speed})")
                            },
                            color = if (isDegraded) Color(0xFFEF4444) else Color.White,
                            style = Typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }

                // Telomeres
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            WireframeDna(color = Color(0xFF22C55E))
                            Text(
                                text = "TELOMERES:",
                                color = Color(0xFF22C55E),
                                style = Typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                            )
                        }
                        Text(
                            text = "$telomeres%",
                            color = Color.White,
                            style = Typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                    val telomereColor = when {
                        telomeres > 65 -> Color(0xFF22C55E)
                        telomeres > 30 -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(Color(0xFF262626), RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((telomeres / 100f).coerceIn(0f, 1f))
                                .background(telomereColor, RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
        }

        // Box 5: 64-CHARACTER INTEGRATION SEQUENCE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "64-CHARACTER INTEGRATION SEQUENCE",
                    style = Typography.labelSmall,
                    color = CyberGreenDim,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                )
                val sequenceBlocks = (0 until (c.sequence.length / 8)).map { i ->
                    c.sequence.substring(i * 8, minOf(c.sequence.length, (i + 1) * 8))
                }
                val sequenceRows = sequenceBlocks.chunked(4)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    sequenceRows.forEachIndexed { rowIndex, rowBlocks ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowBlocks.forEachIndexed { colIndex, block ->
                                val blockIdx = rowIndex * 4 + colIndex
                                val isAnom = WaveMath.isAnomalousGene(block)
                                val color = when {
                                    isAnom -> Color(0xFFC084FC)
                                    blockIdx % 4 == 0 -> Color(0xFF00FF41)
                                    blockIdx % 4 == 1 -> Color(0xFFFBBF24)
                                    blockIdx % 4 == 2 -> Color(0xFF60A5FA)
                                    else -> Color(0xFFC084FC)
                                }
                                val borderStroke = if (isAnom) BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f)) else null
                                val bg = if (isAnom) Color(0xFF1E0B36) else Color.Transparent
                                Box(
                                    modifier = Modifier
                                        .then(if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(2.dp)) else Modifier)
                                        .background(bg, RoundedCornerShape(2.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = block,
                                        color = color,
                                        style = Typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Box 6: GENE HARVESTING MATRIX
        var selectedGeneIndex by remember { mutableStateOf<Int?>(null) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF7F1D1D).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        WireframeWarning(color = Color(0xFFEF4444))
                        Text(
                            text = "GENE HARVESTING MATRIX",
                            style = Typography.labelSmall,
                            color = Color(0xFFF87171),
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                        )
                    }
                    Text(
                        text = "1 EXTRACTABLE NODE LIMIT",
                        style = Typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 7.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                    )
                }

                Text(
                    text = "Select exactly one gene below to harvest, placing it into your Gen-Vault. Doing so will destroy the remaining genes.",
                    style = Typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    color = Color.LightGray,
                    fontSize = 9.sp,
                    lineHeight = 13.sp
                )

                val numBlocks = c.sequence.length / 8
                val matrixBlocks = (0 until numBlocks).map { i ->
                    Pair(i, c.sequence.substring(i * 8, minOf(c.sequence.length, (i + 1) * 8)))
                }
                val matrixRows = matrixBlocks.chunked(4)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    matrixRows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            rowItems.forEach { (i, gene) ->
                                val isSelected = selectedGeneIndex == i
                                val isAnom = WaveMath.isAnomalousGene(gene)
                                val ownedCount = geneSequences.find { it.sequence == gene }?.count ?: 0
                                val isNew = ownedCount == 0

                                val borderStroke = when {
                                    isSelected -> if (isAnom) BorderStroke(1.dp, Color(0xFFA855F7)) else BorderStroke(1.dp, Color(0xFFEF4444))
                                    isAnom -> BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f))
                                    isNew -> BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                                    else -> BorderStroke(1.dp, Color(0xFF262626))
                                }

                                val bg = when {
                                    isSelected -> if (isAnom) Color(0xFF1E0B36) else Color(0xFF7F1D1D).copy(alpha = 0.2f)
                                    isAnom -> Color(0xFF1E0B36).copy(alpha = 0.4f)
                                    isNew -> Color(0xFF78350F).copy(alpha = 0.1f)
                                    else -> Color.Transparent
                                }

                                val textColor = when {
                                    isSelected -> if (isAnom) Color(0xFFD8B4FE) else Color(0xFF00FF41)
                                    isAnom -> Color(0xFFD8B4FE)
                                    isNew -> Color(0xFFFBBF24)
                                    else -> Color.Gray
                                }

                                Box(
                                    modifier = Modifier
                                        .border(borderStroke, RoundedCornerShape(4.dp))
                                        .background(bg, RoundedCornerShape(4.dp))
                                        .clickable {
                                            viewModel.synthManager.playBeep((600 + i * 50).toFloat(), 0.05f, "sine")
                                            selectedGeneIndex = if (isSelected) null else i
                                        }
                                        .padding(horizontal = 4.dp, vertical = 6.dp)
                                        .width(72.dp)
                                        .height(42.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Text(
                                            text = "#${i + 1}",
                                            color = Color.DarkGray,
                                            fontSize = 7.sp,
                                            style = Typography.labelSmall,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                            modifier = Modifier.align(Alignment.TopStart)
                                        )
                                        if (isNew && !isSelected) {
                                            Text(
                                                text = "NEW",
                                                color = Color(0xFFFBBF24),
                                                fontSize = 6.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                                modifier = Modifier.align(Alignment.TopEnd)
                                            )
                                        }
                                        Text(
                                            text = gene,
                                            color = textColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            letterSpacing = 0.5.sp,
                                            modifier = Modifier.align(Alignment.BottomCenter)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedGeneIndex != null) {
                    val idx = selectedGeneIndex!!
                    val geneToHarvest = c.sequence.substring(idx * 8, minOf(c.sequence.length, (idx + 1) * 8))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .height(1.dp)
                                .fillMaxWidth()
                                .background(Color(0xFF7F1D1D).copy(alpha = 0.3f))
                        )
                        Text(
                            text = "CONVERT GENE \"$geneToHarvest\" INTO ARCHIVE STOCK. THIS CREATURE WILL BE PURGED.",
                            color = Color(0xFFF87171),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                            lineHeight = 12.sp
                        )
                        Button(
                            onClick = {
                                viewModel.incinerateCreature(c, geneToHarvest)
                            },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C), contentColor = Color.White),
                            shape = RoundedCornerShape(2.dp)
                        ) {
                            Text(
                                text = "ACTIVATE HARVEST INCINERATION",
                                style = Typography.labelSmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Box 7: GENE SEQUENCE EXTENSION LAB
        val lastFour = c.sequence.takeLast(4)
        val prefixToMatch = lastFour.reversed()
        var extensionSearchPrefix by remember(c.sequence) { mutableStateOf(prefixToMatch) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF78350F).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val activeExtensionsCount = maxOf(0, (c.sequence.length - 64) / 8)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        WireframeSparkle(color = Color(0xFFFBBF24))
                        Text(
                            text = "GENE SEQUENCE EXTENSION LAB",
                            style = Typography.labelSmall,
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                        )
                    }
                    Text(
                        text = "EXTENSIONS: $activeExtensionsCount / 2 MAXIMUM",
                        style = Typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 7.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                    )
                }

                Text(
                    text = "Append up to two extra genes to this P.O.X. sequence to further enhance it. G.E.N. P.O.X. synthesized & approved genes are guaranteed to improve any P.O.X. sequence. WARNING: Appending anomalous genetics will flag the offending sequence!",
                    style = Typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    color = Color.LightGray,
                    fontSize = 9.sp,
                    lineHeight = 13.sp
                )

                if (c.sequence.length < 80) {
                    val matchingGenes = geneSequences.filter { it.count > 0 && it.sequence.startsWith(prefixToMatch) }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF78350F).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .background(Color(0xFF78350F).copy(alpha = 0.1f))
                                .padding(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                WireframeDna(color = Color(0xFFFBBF24))
                                Text(
                                    text = "GENE ALIGNMENT RULE: Next gene must start with \"$prefixToMatch\" (reverses target's suffix \"$lastFour\").",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 8.5.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                    lineHeight = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (matchingGenes.isNotEmpty()) {
                            val activeStep = extensionSearchPrefix.length / 2
                            val stepLabels = listOf("1-2bp", "3-4bp", "5-6bp", "7-8bp")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                stepLabels.forEachIndexed { idx, stepLabel ->
                                    val isCompleted = activeStep > idx
                                    val isActive = activeStep == idx
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(
                                                1.dp,
                                                if (isActive) Color(0xFFFBBF24) else if (isCompleted) Color(0xFF78350F).copy(alpha = 0.5f) else Color.DarkGray,
                                                RoundedCornerShape(2.dp)
                                            )
                                            .background(if (isActive) Color(0xFFFBBF24).copy(alpha = 0.15f) else Color.Transparent)
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stepLabel,
                                            style = Typography.labelSmall,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                            color = if (isActive || isCompleted) Color(0xFFFBBF24) else Color.Gray,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 8.5.sp
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF78350F).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                    .background(Color.Black)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    for (i in 0 until 4) {
                                        val block = if (extensionSearchPrefix.length >= (i + 1) * 2) {
                                            extensionSearchPrefix.substring(i * 2, (i + 1) * 2)
                                        } else {
                                            "_"
                                        }
                                        val isFilled = extensionSearchPrefix.length >= (i + 1) * 2
                                        val color = if (isFilled) Color(0xFFFBBF24) else Color.DarkGray
                                        Box(
                                            modifier = Modifier
                                                .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
                                                .background(if (isFilled) Color(0xFF78350F).copy(alpha = 0.15f) else Color.Transparent)
                                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = block,
                                                color = color,
                                                style = Typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            )
                                        }
                                    }
                                }

                                if (extensionSearchPrefix.length > 4) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Button(
                                            onClick = {
                                                viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                                                extensionSearchPrefix = extensionSearchPrefix.dropLast(2)
                                            },
                                            modifier = Modifier.height(28.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color(0xFFFBBF24)),
                                            border = BorderStroke(1.dp, Color(0xFF78350F)),
                                            shape = RoundedCornerShape(2.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text(
                                                text = "BACK",
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                                                extensionSearchPrefix = prefixToMatch
                                            },
                                            modifier = Modifier.height(28.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Red),
                                            border = BorderStroke(1.dp, Color(0xFF78350F)),
                                            shape = RoundedCornerShape(2.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp)
                                        ) {
                                            Text(
                                                text = "RESET",
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            if (extensionSearchPrefix.length < 8) {
                                val options = if (extensionSearchPrefix.length == 4) {
                                    matchingGenes.map { it.sequence.substring(4, 6) }.distinct()
                                } else {
                                    matchingGenes.filter { it.sequence.startsWith(extensionSearchPrefix) }.map { it.sequence.substring(6, 8) }.distinct()
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "SELECT NEXT 2-BASE PAIRS OPTIONS:",
                                        style = Typography.labelSmall,
                                        color = Color(0xFFFBBF24),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                                    )
                                    androidx.compose.foundation.layout.FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        options.forEach { opt ->
                                            Box(
                                                modifier = Modifier
                                                    .border(BorderStroke(1.dp, Color(0xFF262626)), RoundedCornerShape(4.dp))
                                                    .background(Color.Transparent, RoundedCornerShape(4.dp))
                                                    .clickable {
                                                        viewModel.synthManager.playBeep(580f, 0.05f, "sine")
                                                        extensionSearchPrefix += opt
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                                    .width(52.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = opt,
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                val finalGene = extensionSearchPrefix
                                val ownedCount = geneSequences.find { it.sequence == finalGene }?.count ?: 0
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .height(1.dp)
                                            .fillMaxWidth()
                                            .background(Color(0xFF78350F).copy(alpha = 0.3f))
                                    )
                                    Text(
                                        text = "APPEND GENE \"$finalGene\" (INVENTORY: $ownedCount AVAILABLE) TO INSTANCE RECTIFIER BLOCK.",
                                        color = Color(0xFFFBBF24),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                        lineHeight = 12.sp
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.appendGeneToActiveCreature(c, finalGene)
                                        },
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706), contentColor = Color.Black),
                                        shape = RoundedCornerShape(2.dp)
                                    ) {
                                        Text(
                                            text = "APPLY EXTENSION",
                                            style = Typography.labelSmall,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF262626), RoundedCornerShape(4.dp))
                                    .background(Color(0xFF0A0A0A))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = buildString {
                                        append("✕ No matching gene blocks in stock that start with \"$prefixToMatch\".\n")
                                        append("Please visit the DNA COMBINATOR tab to synthesize patterns starting with this prefix.")
                                    },
                                    color = Color(0xFFEA580C),
                                    fontSize = 8.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                    lineHeight = 12.sp
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF065F46).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                            .background(Color(0xFF065F46).copy(alpha = 0.15f))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "▲ STABILIZATION INTEGRITY UNLOCKED",
                                color = Color(0xFF10B981),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                            )
                            Text(
                                text = "Specimen sequence has been appended with exactly 2 maximum additional genes. Signal capacity stabilized.",
                                color = Color(0xFF047857),
                                fontSize = 8.5.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Box 8: UNLOCKED SPECIAL ACTIONS (Conditional)
        val unlockedMoves = remember(c.sequence) { viewModel.getUnlockedMoves(c.sequence) }
        if (unlockedMoves.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF78350F).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .background(Color(0xFF78350F).copy(alpha = 0.1f))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        WireframeSparkle(color = Color(0xFFFBBF24))
                        Text(
                            text = "UNLOCKED MUTANT SPECIAL ACTIONS (${unlockedMoves.size}):",
                            style = Typography.labelSmall,
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        unlockedMoves.forEach { mv ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF78350F).copy(alpha = 0.2f))
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .padding(6.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "[${mv.type.uppercase()}] ${mv.name}",
                                            color = Color(0xFFFBBF24),
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                                        )
                                        Text(
                                            text = "ACTIVE IN HACKS",
                                            color = Color.Gray,
                                            fontSize = 7.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                                        )
                                    }
                                    Text(
                                        text = mv.description,
                                        color = Color(0xFF00FF41),
                                        fontSize = 8.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Box 9: Footer Actions
        val defenderId by viewModel.defenderCreatureId.collectAsState()
        val isDefender = defenderId == c.id

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { viewModel.toggleDefenderCreature(c.id) },
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDefender) Color(0xFF1E3A8A).copy(alpha = 0.6f) else Color.Black,
                        contentColor = if (isDefender) Color(0xFF60A5FA) else Color.Gray
                    ),
                    border = BorderStroke(1.dp, if (isDefender) Color(0xFF3B82F6) else Color(0xFF262626)),
                    shape = RoundedCornerShape(2.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        WireframeShield(
                            color = if (isDefender) Color(0xFF60A5FA) else Color.Gray,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = if (isDefender) "DEFENDER READY" else "DEFENDER",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = { viewModel.toggleAutoHackerCreature(c) },
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (c.isAutoHacker) Color(0xFF78350F).copy(alpha = 0.6f) else Color.Black,
                        contentColor = if (c.isAutoHacker) Color(0xFFFBBF24) else Color.Gray
                    ),
                    border = BorderStroke(1.dp, if (c.isAutoHacker) Color(0xFFD97706) else Color(0xFF262626)),
                    shape = RoundedCornerShape(2.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        WireframeClaws(
                            color = if (c.isAutoHacker) Color(0xFFFBBF24) else Color.Gray,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = if (c.isAutoHacker) "AUTO-HACKER READY" else "AUTO-HACKER",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = { viewModel.toggleFavoriteCreature(c) },
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (c.isFavorite) Color(0xFF713F12).copy(alpha = 0.6f) else Color.Black,
                        contentColor = if (c.isFavorite) Color(0xFFEAB308) else Color.Gray
                    ),
                    border = BorderStroke(1.dp, if (c.isFavorite) Color(0xFFCA8A04) else Color(0xFF262626)),
                    shape = RoundedCornerShape(2.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        WireframeStar(
                            color = if (c.isFavorite) Color(0xFFEAB308) else Color.Gray,
                            filled = c.isFavorite
                        )
                        Text(
                            text = if (c.isFavorite) "FAVORITED" else "FAVORITE",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = { viewModel.synthManager.playCreatureSequenceAudio(c.sequence) },
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00FF41).copy(alpha = 0.1f),
                        contentColor = Color(0xFF00FF41)
                    ),
                    border = BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(2.dp)
                ) {
                    Text(
                        text = "AUDIO EMIT",
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // Click-to-enlarge full screen QR coupling transponder pop-up dialog
    if (showEnlargedQr) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showEnlargedQr = false }) {
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .border(2.dp, CyberGreen, RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "GENETIC TRANSPONDER DATA",
                        color = CyberGreen,
                        style = Typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                    )
                    QrCodeImage(
                        content = qrContent,
                        sizeDp = 180
                    )
                    Text(
                        text = "CLOSE",
                        color = Color.Red,
                        style = Typography.labelSmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showEnlargedQr = false }
                    )
                }
            }
        }
    }
}

private fun getCoherence(seq: String, target: String): String {
    if (seq.isEmpty() || target.isEmpty()) return "none"
    val seq64 = seq.take(64)
    val target64 = target.take(64)
    if (seq64 == target64) return "full"

    var alignedMatches = 0
    for (i in 0 until 8) {
        val start = i * 8
        val end = (i + 1) * 8
        if (start < seq64.length && end <= seq64.length && start < target64.length && end <= target64.length) {
            val seqGene = seq64.substring(start, end)
            val tgtGene = target64.substring(start, end)
            if (seqGene == tgtGene) {
                alignedMatches++
            }
        }
    }
    if (alignedMatches == 8) return "full"
    if (alignedMatches >= 1) return "partial"

    for (i in 0 until 8) {
        val start = i * 8
        val end = (i + 1) * 8
        if (start < target64.length && end <= target64.length) {
            val tgtGene = target64.substring(start, end)
            if (seq.contains(tgtGene)) {
                return "partial"
            }
        }
    }
    return "none"
}

private fun getCreatureTags(
    item: Creature,
    targetSequence: String,
    defenderId: String? = null
): List<String> {
    val tags = mutableListOf<String>()
    if (item.isFavorite) tags.add("FAVORITE")
    if (defenderId == item.id) tags.add("DEFENDER")
    if (item.isAutoHacker) tags.add("AUTO-HACKER")

    if (item.isFullCoherence) {
        tags.add("FULL COHERENCE")
        if (item.coherenceType == "Natural") {
            tags.add("NATURAL")
        } else if (item.coherenceType == "Forced") {
            tags.add("FORCED")
        }
    }

    if (item.attack >= 75 || item.defense >= 75 || item.vitality >= 75 || item.speed >= 75) {
        tags.add("ALPHA GENE")
    }
    if (item.appendedGenes.isNotEmpty()) {
        tags.add("MODIFIED")
    }
    if (item.origin == "Created") {
        tags.add("ORIGINAL")
    } else if (item.origin.startsWith("Traded")) {
        tags.add("TRANSFER-ORIGIN")
    }
    return tags
}

@Composable
private fun PoxDropdown(
    label: String,
    selectedOption: String,
    options: List<Pair<String, String>>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            color = CyberGreenDim,
            style = Typography.labelSmall,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                .background(Color.Black)
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = options.find { it.first == selectedOption }?.second?.uppercase() ?: selectedOption.uppercase(),
                    color = Color.White,
                    style = Typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    fontSize = 10.sp
                )
                Text(
                    text = "▼",
                    color = CyberGreen,
                    fontSize = 8.sp
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(Color.Black)
                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
            ) {
                options.forEach { (value, displayName) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = displayName.uppercase(),
                                color = Color.White,
                                style = Typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default
                            )
                        },
                        onClick = {
                            onOptionSelected(value)
                            expanded = false
                        },
                        modifier = Modifier.background(Color.Black)
                    )
                }
            }
        }
    }
}

// ==========================================
// 4. SCANNER VIEW (styled MAP or fallback RADAR)
// ==========================================
@Composable
fun ScannerView(viewModel: MainViewModel) {
    val mapVisible = remember { mutableStateOf(false) } // Toggle to holographic radar by default for retro look

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "TACTICAL AREA SCANNER", style = Typography.titleMedium, color = CyberGreen)
            Text(
                text = if (mapVisible.value) "HOLO-RADAR" else "GEOMAP",
                style = Typography.labelSmall,
                color = CyberGreen,
                modifier = Modifier
                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                    .clickable { mapVisible.value = !mapVisible.value }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        if (mapVisible.value) {
            // Real map screen container
            GoogleMapsScanner(viewModel)
        } else {
            // Holographic radar sweep view
            HolographicRadarScanner(viewModel)
        }

        // Active harvest missions list
        Text(text = "ACTIVE HARVEST DISPATCHES:", style = Typography.labelSmall, color = CyberGreenDim)
        val activeMissions by viewModel.activeMissions.collectAsState()

        if (activeMissions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "NO HARVEST SHUTTLES DISPATCHED.", style = Typography.bodySmall, color = CyberGreenDim)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(0.4f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(activeMissions) { mission ->
                    val elapsed = (System.currentTimeMillis() - mission.startTime) / 1000f
                    val progress = (elapsed / mission.totalDuration).coerceIn(0f, 1f)
                    val isDone = progress >= 1f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                            .background(CyberPanel)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "${mission.creatureName} (${mission.creatureFaction})", style = Typography.bodySmall, fontWeight = FontWeight.Bold, color = CyberGreen)
                            Text(text = "GENE DEST: ${mission.harvestedGenes.joinToString()}", style = Typography.bodySmall, color = CyberGreenDim)
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                color = CyberGreen,
                                trackColor = Color.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.recallMission(mission) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDone) CyberGreen else CyberPanel,
                                contentColor = if (isDone) Color.Black else CyberGreenDim
                            ),
                            shape = RoundedCornerShape(4.dp),
                            border = BorderStroke(1.dp, CyberBorder),
                            enabled = isDone,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text(text = "RECALL", style = Typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

// 4a. Google Maps wrapper
@Composable
fun GoogleMapsScanner(viewModel: MainViewModel) {
    val lat by viewModel.latitude.collectAsState()
    val lng by viewModel.longitude.collectAsState()
    val scanRadius by viewModel.scanRadius.collectAsState()
    val anomalies by viewModel.anomalies.collectAsState()
    val creatures by viewModel.creatures.collectAsState()

    val location = LatLng(lat, lng)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(location, 17f)
    }

    val selectedAnomaly = remember { mutableStateOf<PoxAnomaly?>(null) }

    LaunchedEffect(lat, lng) {
        cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 17f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapStyleOptions = MapStyleOptions(MapStyle.JSON_STYLE)
            )
        ) {
            // Draw center player location
            Marker(
                state = MarkerState(position = location),
                title = "PLAYER TRANSMITTER BEACON"
            )

            // Draw Anomalies
            anomalies.forEach { anomaly ->
                Marker(
                    state = MarkerState(position = LatLng(anomaly.lat, anomaly.lng)),
                    title = "${anomaly.name} (${anomaly.gene})",
                    onClick = {
                        selectedAnomaly.value = anomaly
                        viewModel.synthManager.playBeep(770f, 0.05f, "square")
                        false
                    }
                )
            }
        }

        // Selected anomaly popup HUD overlay
        selectedAnomaly.value?.let { anomaly ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .border(1.dp, CyberGreen, RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = anomaly.name, style = Typography.bodySmall, fontWeight = FontWeight.Bold, color = CyberGreen)
                        Text(
                            text = "CLOSE",
                            style = Typography.labelSmall,
                            color = Color.Red,
                            modifier = Modifier.clickable { selectedAnomaly.value = null }
                        )
                    }
                    Text(text = "GENE SIGNATURE: ${anomaly.gene}", style = Typography.bodySmall, color = CyberGreenDim)
                    Text(text = "DISTANCE: ${anomaly.distance.toInt()} FT | SECTOR: ${anomaly.faction}", style = Typography.bodySmall, color = CyberGreenDim)

                    if (creatures.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "DISPATCH FOR PROBE:", style = Typography.labelSmall, color = CyberGreenDim)
                            val listState = remember { mutableStateOf(false) }

                            Button(
                                onClick = { listState.value = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPanel, contentColor = CyberGreen),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(text = "CHOOSE UNIT", style = Typography.labelSmall)
                            }

                            if (listState.value) {
                                AlertDialog(
                                    onDismissRequest = { listState.value = false },
                                    containerColor = CyberPanel,
                                    title = { Text("SELECT EXPEDITION UNIT", style = Typography.titleMedium, color = CyberGreen) },
                                    text = {
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(creatures) { c ->
                                                Text(
                                                    text = c.name,
                                                    style = Typography.bodyMedium,
                                                    color = CyberGreen,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            viewModel.dispatchMission(c, anomaly)
                                                            listState.value = false
                                                            selectedAnomaly.value = null
                                                        }
                                                        .padding(vertical = 8.dp)
                                                )
                                            }
                                        }
                                    },
                                    confirmButton = {}
                                )
                            }
                        }
                    } else {
                        Text(text = "NO COMPILED CREATURES AVAILABLE TO DISPATCH.", style = Typography.bodySmall, color = Color.Red)
                    }
                }
            }
        }
    }
}

// 4b. Custom holographic radar view fallback (Premium retro visual)
@Composable
fun HolographicRadarScanner(viewModel: MainViewModel) {
    val anomalies by viewModel.anomalies.collectAsState()
    val creatures by viewModel.creatures.collectAsState()
    val selectedAnomaly = remember { mutableStateOf<PoxAnomaly?>(null) }

    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(190.dp)) {
            val cx = size.width / 2
            val cy = size.height / 2
            val maxR = size.width / 2 * 0.9f

            // Radar concentric rings
            drawCircle(Color(0xFF00FF41).copy(alpha = 0.15f), radius = maxR, center = Offset(cx, cy), style = Stroke(width = 1f))
            drawCircle(Color(0xFF00FF41).copy(alpha = 0.1f), radius = maxR * 0.6f, center = Offset(cx, cy), style = Stroke(width = 0.5f))
            drawCircle(Color(0xFF00FF41).copy(alpha = 0.05f), radius = maxR * 0.3f, center = Offset(cx, cy), style = Stroke(width = 0.5f))

            // Crosshair lines
            drawLine(Color(0xFF00FF41).copy(alpha = 0.15f), Offset(cx - maxR, cy), Offset(cx + maxR, cy), strokeWidth = 0.5f)
            drawLine(Color(0xFF00FF41).copy(alpha = 0.15f), Offset(cx, cy - maxR), Offset(cx, cy + maxR), strokeWidth = 0.5f)

            // Dynamic sweep ray
            val radians = Math.toRadians(sweepAngle.toDouble())
            val sweepX = cx + maxR * cos(radians).toFloat()
            val sweepY = cy + maxR * sin(radians).toFloat()
            drawLine(
                brush = Brush.sweepGradient(
                    colors = listOf(CyberGreen.copy(alpha = 0.4f), Color.Transparent),
                    center = Offset(cx, cy)
                ),
                start = Offset(cx, cy),
                end = Offset(sweepX, sweepY),
                strokeWidth = 2f
            )

            // Draw center beacon player transceiver
            drawCircle(CyberGreen, radius = 3.5f, center = Offset(cx, cy))
        }

        // Clickable interactive blips matching anomalies
        Box(modifier = Modifier.size(190.dp)) {
            anomalies.forEachIndexed { idx, anomaly ->
                val angleOffset = idx * 72f
                val distanceFraction = 0.2f + (idx * 0.15f)
                val rad = Math.toRadians(angleOffset.toDouble())
                val x = 95 + (85 * distanceFraction * cos(rad)).toFloat()
                val y = 95 + (85 * distanceFraction * sin(rad)).toFloat()

                val factionColor = when (anomaly.faction) {
                    "Infection" -> Color.Red
                    "Mech" -> Color.Yellow
                    "Parasite" -> Color(0xFFA855F7)
                    else -> Color.Cyan
                }

                Box(
                    modifier = Modifier
                        .offset(x.dp - 6.dp, y.dp - 6.dp)
                        .size(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(factionColor.copy(alpha = 0.8f))
                        .clickable {
                            selectedAnomaly.value = anomaly
                            viewModel.synthManager.playBeep(660f, 0.05f, "sine")
                        }
                )
            }
        }

        // Anomaly overlay
        selectedAnomaly.value?.let { anomaly ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.95f))
                    .border(1.dp, CyberGreen, RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = anomaly.name, style = Typography.bodySmall, fontWeight = FontWeight.Bold, color = CyberGreen)
                        Text(
                            text = "CLOSE",
                            style = Typography.labelSmall,
                            color = Color.Red,
                            modifier = Modifier.clickable { selectedAnomaly.value = null }
                        )
                    }
                    Text(text = "GENE COORD: ${anomaly.gene}", style = Typography.bodySmall, color = CyberGreenDim)
                    Text(text = "SECTOR: ${anomaly.faction} | DIST: ${anomaly.distance.toInt()} FT", style = Typography.bodySmall, color = CyberGreenDim)

                    if (creatures.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "DEPLOY EXPEDITION UNIT:", style = Typography.labelSmall, color = CyberGreenDim)
                            val listState = remember { mutableStateOf(false) }

                            Button(
                                onClick = { listState.value = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPanel, contentColor = CyberGreen),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(text = "SELECT UNIT", style = Typography.labelSmall)
                            }

                            if (listState.value) {
                                AlertDialog(
                                    onDismissRequest = { listState.value = false },
                                    containerColor = CyberPanel,
                                    title = { Text("CHOOSE EXPEDITION UNIT", style = Typography.titleMedium, color = CyberGreen) },
                                    text = {
                                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(creatures) { c ->
                                                Text(
                                                    text = c.name,
                                                    style = Typography.bodyMedium,
                                                    color = CyberGreen,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            viewModel.dispatchMission(c, anomaly)
                                                            listState.value = false
                                                            selectedAnomaly.value = null
                                                        }
                                                        .padding(vertical = 8.dp)
                                                )
                                            }
                                        }
                                    },
                                    confirmButton = {}
                                )
                            }
                        }
                    } else {
                        Text(text = "NO TELEMETRY HOSTS TO DEPLOY.", style = Typography.bodySmall, color = Color.Red)
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. FORECAST VIEW
// ==========================================
@Composable
fun ForecastView(viewModel: MainViewModel) {
    val dailyBounties by viewModel.dailyBounties.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "DAILY GENE FORECASTS", style = Typography.titleMedium, color = CyberGreen)
        Text(
            text = "SCAN TARGET TELEMETRIES HARVEST PATHWAYS:",
            style = Typography.labelSmall,
            color = CyberGreenDim
        )

        // Draw forecast grids
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(dailyBounties.take(28)) { bounty ->
                val first8 = bounty.take(8)
                Box(
                    modifier = Modifier
                        .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                        .background(CyberPanel)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = first8, style = Typography.bodySmall, fontSize = 9.sp, color = CyberGreen)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "CO coherence", style = Typography.bodySmall, fontSize = 6.sp, color = CyberGreenDim)
                    }
                }
            }
        }
    }
}

// ==========================================
// 6. INVENTORY VIEW
// ==========================================
@Composable
fun InventoryView(viewModel: MainViewModel) {
    val geneSequences by viewModel.geneSequences.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = "TRANSCIEVER GENE INVENTORY", style = Typography.titleMedium, color = CyberGreen)

        if (geneSequences.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "NO GENE GENES RESOLVED.\nHARVEST TARGETS VIA MAP SCANNER.",
                    color = CyberGreenDim,
                    style = Typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(geneSequences) { seq ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                            .background(CyberPanel)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "BLOCK: ${seq.sequence}", style = Typography.bodyMedium, fontWeight = FontWeight.Bold, color = CyberGreen)
                            Text(
                                text = "ACQUIRED: " + SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(seq.discoveredAt)),
                                style = Typography.bodySmall,
                                color = CyberGreenDim
                            )
                        }
                        Text(
                            text = "QTY: ${seq.count}",
                            style = Typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyberGreen
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. SETTINGS VIEW
// ==========================================
@Composable
fun SettingsView(viewModel: MainViewModel) {
    val apiKey by viewModel.geminiApiKey.collectAsState()
    val mute by viewModel.muteSound.collectAsState()
    val textState = remember { mutableStateOf("") }

    LaunchedEffect(apiKey) {
        textState.value = apiKey
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "SYSTEM CONFIGURATION", style = Typography.titleMedium, color = CyberGreen)

        // API Key Settings Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                .background(CyberPanel)
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "GEMINI API CREDENTIALS", style = Typography.bodyMedium, fontWeight = FontWeight.Bold, color = CyberGreen)
                Text(text = "Paste your Google AI Studio API Key to unlock high-fidelity creature compiling.", style = Typography.bodySmall, color = CyberGreenDim)

                TextField(
                    value = textState.value,
                    onValueChange = { textState.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = CyberGreen,
                        unfocusedTextColor = CyberGreen,
                        focusedBorderColor = CyberGreen,
                        unfocusedBorderColor = CyberBorder,
                        focusedContainerColor = Color.Black,
                        unfocusedContainerColor = Color.Black
                    ),
                    placeholder = { Text("Paste AIStudio API Key...", color = CyberGreenDim, style = Typography.bodySmall) }
                )

                Button(
                    onClick = { viewModel.saveApiKey(textState.value) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("SAVE KEY TO DATASTORE", style = Typography.labelSmall)
                }
            }
        }

        // General settings
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                .background(CyberPanel)
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "SOUND MANAGEMENT", style = Typography.bodyMedium, fontWeight = FontWeight.Bold, color = CyberGreen)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "MUTE retro synthesis chimes:", style = Typography.bodySmall, color = CyberGreenDim)
                    Switch(
                        checked = mute,
                        onCheckedChange = { viewModel.setMute(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = CyberGreen,
                            uncheckedThumbColor = CyberGreenDim,
                            uncheckedTrackColor = Color.Black
                        )
                    )
                }
            }
        }
    }
}
