package com.example.genpox.ui.main

import android.content.Context
import androidx.annotation.Keep
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpox.data.Creature
import com.example.genpox.data.GeneSequence
import com.example.genpox.data.HarvestMission
import com.example.genpox.data.WaveMath
import com.example.genpox.data.BuildingStructure
import com.example.genpox.theme.*
import com.example.genpox.ui.components.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.absoluteValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.SystemFontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.Immutable


@Composable
fun SingleDnaDotGrid(
    sequence: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val columns = 8
    val rows = (sequence.length + columns - 1) / columns

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label.uppercase(),
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Default,
            modifier = Modifier.padding(bottom = 2.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (r in 0 until rows) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (c in 0 until columns) {
                        val idx = r * columns + c
                        if (idx < sequence.length) {
                            val baseChar = sequence[idx]
                            
                            val blockStart = (idx / 8) * 8
                            val blockEnd = minOf(sequence.length, blockStart + 8)
                            val geneBlock = sequence.substring(blockStart, blockEnd)
                            
                            val isAnomalous = geneBlock.any { it in "XZYW?!$%&@#" }
                            val dotColor = if (isAnomalous) {
                                Color(0xFFA855F7)
                            } else {
                                when (baseChar.uppercaseChar()) {
                                    'A' -> Color(0xFF00FF41)
                                    'G' -> Color(0xFFFBBF24)
                                    'T' -> Color(0xFF60A5FA)
                                    'C' -> Color(0xFFEC4899)
                                    else -> Color(0xFF404040)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(dotColor)
                            )
                        } else {
                            Box(modifier = Modifier.size(10.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DnaComparisonGrid(original: String, current: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "DNA MUTATION TELEMETRY SCAN",
            style = Typography.labelSmall,
            color = Color(0xFFF97316),
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Default,
            modifier = Modifier.align(Alignment.Start)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Top
        ) {
            SingleDnaDotGrid(
                sequence = original,
                label = "SEQ. SCAN: Original",
                modifier = Modifier.weight(1f)
            )

            SingleDnaDotGrid(
                sequence = current,
                label = "SEQ. SCAN: Mutated",
                modifier = Modifier.weight(1f)
            )
        }
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
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
            Text(
                text = "[ TAP TO REVEAL ]",
                color = Color.Gray,
                fontSize = 5.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }
    }
}
