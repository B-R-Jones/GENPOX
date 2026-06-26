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
import androidx.compose.ui.geometry.CornerRadius
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
fun WireframeMutated(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(14.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 1.2.dp.toPx()
        drawLine(color, Offset(w * 0.2f, h * 0.2f), Offset(w * 0.45f, h * 0.45f), strokeWidth = stroke)
        drawLine(color, Offset(w * 0.55f, h * 0.55f), Offset(w * 0.8f, h * 0.8f), strokeWidth = stroke)
        drawLine(color, Offset(w * 0.8f, h * 0.2f), Offset(w * 0.2f, h * 0.8f), strokeWidth = stroke)
        drawLine(color, Offset(w * 0.35f, h * 0.35f), Offset(w * 0.65f, h * 0.35f), strokeWidth = 0.8.dp.toPx())
    }
}

@Composable
fun HoloDnaSlotIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val strokeW = 1.2.dp.toPx()
        
        // Draw the outer square box
        drawRect(
            color = color,
            topLeft = Offset(0f, 0f),
            size = Size(w, h),
            style = Stroke(width = strokeW)
        )
        
        // Draw the vertical DNA strand inside the box
        val cx = w / 2f
        val amplitude = w * 0.2f
        val steps = 12
        val wave1 = mutableListOf<Offset>()
        val wave2 = mutableListOf<Offset>()
        for (i in 0..steps) {
            val y = h * 0.15f + (h * 0.7f) * (i.toFloat() / steps)
            val angle = (i.toFloat() / steps) * 2.2 * Math.PI
            val xOffset = amplitude * sin(angle).toFloat()
            wave1.add(Offset(cx + xOffset, y))
            wave2.add(Offset(cx - xOffset, y))
        }
        
        for (i in 0 until steps) {
            drawLine(color, wave1[i], wave1[i+1], strokeWidth = strokeW)
            drawLine(color, wave2[i], wave2[i+1], strokeWidth = strokeW)
        }
        
        for (i in listOf(2, 6, 10)) {
            drawLine(color, wave1[i], wave2[i], strokeWidth = strokeW * 0.7f)
        }
    }
}

@Composable
fun HoloDnaForceIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val strokeW = 1.2.dp.toPx()
        val cx = w / 2f
        
        // Draw the vertical DNA strand in the center
        val amplitude = w * 0.15f
        val steps = 12
        val wave1 = mutableListOf<Offset>()
        val wave2 = mutableListOf<Offset>()
        for (i in 0..steps) {
            val y = h * 0.15f + (h * 0.7f) * (i.toFloat() / steps)
            val angle = (i.toFloat() / steps) * 2.2 * Math.PI
            val xOffset = amplitude * sin(angle).toFloat()
            wave1.add(Offset(cx + xOffset, y))
            wave2.add(Offset(cx - xOffset, y))
        }
        
        for (i in 0 until steps) {
            drawLine(color, wave1[i], wave1[i+1], strokeWidth = strokeW)
            drawLine(color, wave2[i], wave2[i+1], strokeWidth = strokeW)
        }
        for (i in listOf(2, 6, 10)) {
            drawLine(color, wave1[i], wave2[i], strokeWidth = strokeW * 0.7f)
        }
        
        // Draw two inward facing chevrons on the left and right sides
        val leftPath = Path().apply {
            moveTo(w * 0.1f, h * 0.35f)
            lineTo(w * 0.25f, h * 0.5f)
            lineTo(w * 0.1f, h * 0.65f)
        }
        val rightPath = Path().apply {
            moveTo(w * 0.9f, h * 0.35f)
            lineTo(w * 0.75f, h * 0.5f)
            lineTo(w * 0.9f, h * 0.65f)
        }
        
        drawPath(leftPath, color, style = Stroke(width = strokeW))
        drawPath(rightPath, color, style = Stroke(width = strokeW))
    }
}

@Composable
fun HoloDnaAutoLoopIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val strokeW = 1.2.dp.toPx()
        val cx = w / 2f
        val cy = h / 2f
        
        // Draw the vertical DNA strand in the center (scaled down slightly to fit loop arrows)
        val amplitude = w * 0.11f
        val steps = 10
        val wave1 = mutableListOf<Offset>()
        val wave2 = mutableListOf<Offset>()
        for (i in 0..steps) {
            val y = h * 0.22f + (h * 0.56f) * (i.toFloat() / steps)
            val angle = (i.toFloat() / steps) * 2.0 * Math.PI
            val xOffset = amplitude * sin(angle).toFloat()
            wave1.add(Offset(cx + xOffset, y))
            wave2.add(Offset(cx - xOffset, y))
        }
        
        for (i in 0 until steps) {
            drawLine(color, wave1[i], wave1[i+1], strokeWidth = strokeW)
            drawLine(color, wave2[i], wave2[i+1], strokeWidth = strokeW)
        }
        for (i in listOf(2, 5, 8)) {
            drawLine(color, wave1[i], wave2[i], strokeWidth = strokeW * 0.7f)
        }
        
        // Draw two inward facing chevrons on the left and right sides (scaled down to fit)
        val leftPath = Path().apply {
            moveTo(w * 0.22f, h * 0.38f)
            lineTo(w * 0.32f, h * 0.5f)
            lineTo(w * 0.22f, h * 0.62f)
        }
        val rightPath = Path().apply {
            moveTo(w * 0.78f, h * 0.38f)
            lineTo(w * 0.68f, h * 0.5f)
            lineTo(w * 0.78f, h * 0.62f)
        }
        
        drawPath(leftPath, color, style = Stroke(width = strokeW))
        drawPath(rightPath, color, style = Stroke(width = strokeW))

        // Draw two semi-circular loop arrows
        val arcPadding = 2.dp.toPx()
        val arcSize = Size(w - arcPadding * 2, h - arcPadding * 2)
        val arcTopLeft = Offset(arcPadding, arcPadding)
        val rx = (w - arcPadding * 2) / 2f
        
        // Left arc: starts at 100 degrees, sweeps 170 degrees (clockwise to 270 degrees)
        drawArc(
            color = color,
            startAngle = 100f,
            sweepAngle = 170f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
        
        // Right arc: starts at 280 degrees, sweeps 170 degrees (clockwise to 90 degrees)
        drawArc(
            color = color,
            startAngle = 280f,
            sweepAngle = 170f,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcSize,
            style = Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )

        // Arrowheads:
        // Top arrowhead at 270 degrees (pointing right)
        val topArrowPath = Path().apply {
            moveTo(cx - 3.5.dp.toPx(), cy - rx - 2.dp.toPx())
            lineTo(cx, cy - rx)
            lineTo(cx - 3.5.dp.toPx(), cy - rx + 2.dp.toPx())
        }
        // Bottom arrowhead at 90 degrees (pointing left)
        val bottomArrowPath = Path().apply {
            moveTo(cx + 3.5.dp.toPx(), cy + rx - 2.dp.toPx())
            lineTo(cx, cy + rx)
            lineTo(cx + 3.5.dp.toPx(), cy + rx + 2.dp.toPx())
        }

        drawPath(topArrowPath, color, style = Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        drawPath(bottomArrowPath, color, style = Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}

@Composable
fun HoloDnaAutoSlotIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val strokeW = 1.2.dp.toPx()
        
        // Draw the slot box (U-shape container)
        val boxPath = Path().apply {
            moveTo(w * 0.25f, h * 0.45f)
            lineTo(w * 0.25f, h * 0.8f)
            lineTo(w * 0.75f, h * 0.8f)
            lineTo(w * 0.75f, h * 0.45f)
        }
        drawPath(boxPath, color, style = Stroke(width = strokeW))
        
        // Draw the down-pointing arrow
        val arrowPath = Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            lineTo(w * 0.5f, h * 0.6f)
            // Arrowhead at the end pointing down
            moveTo(w * 0.38f, h * 0.48f)
            lineTo(w * 0.5f, h * 0.6f)
            lineTo(w * 0.62f, h * 0.48f)
        }
        drawPath(arrowPath, color, style = Stroke(width = strokeW, cap = androidx.compose.ui.graphics.StrokeCap.Round))
    }
}

@Composable
fun TagBadge(tag: String, modifier: Modifier = Modifier) {
    val (color, content) = when (tag) {
        "FAVORITE" -> Color(0xFFFFB300) to @Composable { WireframeStar(Color(0xFFFFB300), filled = true, modifier = modifier.size(10.dp)) }
        "DEFENDER" -> Color(0xFF60A5FA) to @Composable { WireframeShield(Color(0xFF60A5FA), modifier = modifier.size(10.dp)) }
        "AUTO-HACKER" -> Color(0xFFFBBF24) to @Composable { WireframeGear(Color(0xFFFBBF24), modifier = modifier.size(10.dp)) }
        "HARVESTING" -> Color(0xFF00E1FF) to @Composable { WireframeGalaxy(Color(0xFF00E1FF), modifier = modifier.size(10.dp)) }
        "FULL COHERENCE" -> CyberGreen to @Composable { WireframeDna(CyberGreen, modifier = modifier.size(10.dp)) }
        "NATURAL" -> Color(0xFF10B981) to @Composable { WireframeNatural(Color(0xFF10B981), modifier = modifier.size(10.dp)) }
        "FORCED" -> Color(0xFFF59E0B) to @Composable { WireframeForced(Color(0xFFF59E0B), modifier = modifier.size(10.dp)) }
        "ALPHA GENE" -> Color(0xFFFFB300) to @Composable { WireframeLightning(Color(0xFFFFB300), modifier = modifier.size(10.dp)) }
        "MODIFIED" -> Color(0xFFC084FC) to @Composable { WireframeSparkle(Color(0xFFC084FC), modifier = modifier.size(10.dp)) }
        "ORIGINAL" -> CyberGreen to @Composable { WireframeOriginal(CyberGreen, modifier = modifier.size(10.dp)) }
        "TRANSFER-ORIGIN" -> Color(0xFFEF4444) to @Composable { WireframeTransfer(Color(0xFFEF4444), modifier = modifier.size(10.dp)) }
        "MUTATED" -> Color(0xFFF97316) to @Composable { WireframeMutated(Color(0xFFF97316), modifier = modifier.size(10.dp)) }
        else -> return
    }
    
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(2.dp))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            content()
        }
    }
}

@Composable
fun WireframePickaxeIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val strokeW = 1.5.dp.toPx()

        // 1. Draw the handle: from bottom-left (w * 0.15f, h * 0.85f) to top-right (w * 0.65f, h * 0.35f)
        drawLine(
            color = color,
            start = Offset(w * 0.15f, h * 0.85f),
            end = Offset(w * 0.65f, h * 0.35f),
            strokeWidth = strokeW
        )

        // 2. Draw the pickaxe head: a curved diamond-like hollow blade.
        val headPath = Path().apply {
            // Start at the left tip (tapered)
            moveTo(w * 0.2f, h * 0.35f)
            // Outer curve to the top/center attachment area
            quadraticTo(w * 0.45f, h * 0.2f, w * 0.7f, h * 0.3f)
            // Outer curve to the right tip (tapered)
            quadraticTo(w * 0.8f, h * 0.55f, w * 0.65f, h * 0.8f)
            // Inner curve back to the bottom/center attachment area
            quadraticTo(w * 0.72f, h * 0.52f, w * 0.63f, h * 0.37f)
            // Inner curve back to the left tip
            quadraticTo(w * 0.43f, h * 0.28f, w * 0.2f, h * 0.35f)
            close()
        }
        drawPath(headPath, color, style = Stroke(width = strokeW))

        // 3. Draw a small square cap/mounting collar at the intersection
        drawRect(
            color = color,
            topLeft = Offset(w * 0.61f, h * 0.29f),
            size = Size(w * 0.08f, h * 0.08f),
            style = Stroke(width = strokeW * 0.7f)
        )
    }
}

@Composable
fun WireframeBugIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val strokeW = 1.5.dp.toPx()

        // Body (central vertical capsule)
        drawRoundRect(
            color = color,
            topLeft = Offset(cx - w * 0.15f, cy - h * 0.25f),
            size = Size(w * 0.3f, h * 0.5f),
            cornerRadius = CornerRadius(w * 0.1f, h * 0.1f),
            style = Stroke(width = strokeW)
        )

        // Head
        drawCircle(
            color = color,
            radius = w * 0.1f,
            center = Offset(cx, cy - h * 0.35f),
            style = Stroke(width = strokeW)
        )

        // Antennae
        drawLine(color, Offset(cx - w * 0.05f, cy - h * 0.45f), Offset(cx - w * 0.18f, cy - h * 0.55f), strokeWidth = strokeW)
        drawLine(color, Offset(cx + w * 0.05f, cy - h * 0.45f), Offset(cx + w * 0.18f, cy - h * 0.55f), strokeWidth = strokeW)

        // Legs (left side)
        drawLine(color, Offset(cx - w * 0.15f, cy - h * 0.15f), Offset(cx - w * 0.38f, cy - h * 0.25f), strokeWidth = strokeW)
        drawLine(color, Offset(cx - w * 0.15f, cy), Offset(cx - w * 0.42f, cy), strokeWidth = strokeW)
        drawLine(color, Offset(cx - w * 0.15f, cy + h * 0.15f), Offset(cx - w * 0.38f, cy + h * 0.25f), strokeWidth = strokeW)

        // Legs (right side)
        drawLine(color, Offset(cx + w * 0.15f, cy - h * 0.15f), Offset(cx + w * 0.38f, cy - h * 0.25f), strokeWidth = strokeW)
        drawLine(color, Offset(cx + w * 0.15f, cy), Offset(cx + w * 0.42f, cy), strokeWidth = strokeW)
        drawLine(color, Offset(cx + w * 0.15f, cy + h * 0.15f), Offset(cx + w * 0.38f, cy + h * 0.25f), strokeWidth = strokeW)
    }
}

@Composable
fun WireframeStepSearchIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val strokeW = 1.5.dp.toPx()

        // 1. Draw zig-zag staircase (stretching from bottom-left to top-right)
        val stairPath = Path().apply {
            moveTo(w * 0.15f, h * 0.85f)
            lineTo(w * 0.35f, h * 0.85f)
            lineTo(w * 0.35f, h * 0.62f)
            lineTo(w * 0.55f, h * 0.62f)
            lineTo(w * 0.55f, h * 0.38f)
            lineTo(w * 0.75f, h * 0.38f)
            lineTo(w * 0.75f, h * 0.15f)
            lineTo(w * 0.90f, h * 0.15f)
        }
        drawPath(
            path = stairPath,
            color = color.copy(alpha = 0.5f),
            style = Stroke(width = strokeW)
        )

        // 2. Draw magnifying glass over it
        val glassCenter = Offset(w * 0.42f, h * 0.42f)
        val glassRadius = w * 0.22f

        // Lens circle
        drawCircle(
            color = color,
            radius = glassRadius,
            center = glassCenter,
            style = Stroke(width = strokeW)
        )

        // Handle
        drawLine(
            color = color,
            start = Offset(glassCenter.x + glassRadius * 0.707f, glassCenter.y + glassRadius * 0.707f),
            end = Offset(w * 0.82f, h * 0.82f),
            strokeWidth = strokeW + 0.5.dp.toPx()
        )
    }
}

@Composable
fun WireframeReactorParametersIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val strokeW = 1.5.dp.toPx()

        // 1. Draw a gear (shifted down and left to make room for bubbles in upper right)
        // Center at (w * 0.42f, h * 0.58f)
        val cx = w * 0.42f
        val cy = h * 0.58f
        val rOuter = w * 0.28f
        val rInner = w * 0.14f
        val center = Offset(cx, cy)

        drawCircle(color, radius = rInner, center = center, style = Stroke(width = strokeW * 0.8f))
        drawCircle(color, radius = rOuter, center = center, style = Stroke(width = strokeW * 0.8f))
        for (i in 0 until 6) {
            val angle = i * Math.PI / 3
            val start = Offset(
                (cx + rOuter * cos(angle)).toFloat(),
                (cy + rOuter * sin(angle)).toFloat()
            )
            val end = Offset(
                (cx + (rOuter + w * 0.08f) * cos(angle)).toFloat(),
                (cy + (rOuter + w * 0.08f) * sin(angle)).toFloat()
            )
            drawLine(color, start, end, strokeWidth = strokeW)
        }

        // 2. Draw bubbles in the upper right
        // Three small circles of slightly different sizes grouped to the upper right
        drawCircle(color, radius = w * 0.08f, center = Offset(w * 0.78f, h * 0.26f), style = Stroke(width = strokeW * 0.8f))
        drawCircle(color, radius = w * 0.05f, center = Offset(w * 0.63f, h * 0.18f), style = Stroke(width = strokeW * 0.8f))
        drawCircle(color, radius = w * 0.04f, center = Offset(w * 0.84f, h * 0.42f), style = Stroke(width = strokeW * 0.8f))
    }
}

@Composable
fun WireframeCloudIcon(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val strokeW = 1.5.dp.toPx()
        
        val path = Path().apply {
            moveTo(w * 0.25f, h * 0.7f)
            // Left arch
            cubicTo(w * 0.1f, h * 0.7f, w * 0.1f, h * 0.45f, w * 0.28f, h * 0.45f)
            // Top/middle arch
            cubicTo(w * 0.3f, h * 0.22f, w * 0.7f, h * 0.22f, w * 0.72f, h * 0.45f)
            // Right arch
            cubicTo(w * 0.9f, h * 0.45f, w * 0.9f, h * 0.7f, w * 0.75f, h * 0.7f)
            close()
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = strokeW)
        )
    }
}


