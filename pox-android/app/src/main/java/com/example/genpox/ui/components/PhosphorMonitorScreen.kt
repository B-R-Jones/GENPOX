package com.example.genpox.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.example.genpox.theme.CyberGreen
import com.example.genpox.theme.CyberGreenDim
import com.example.genpox.theme.CyberPanel
import com.example.genpox.theme.CyberBorder
import com.example.genpox.theme.CyberTheme
import com.example.genpox.theme.Typography
import com.example.genpox.ui.main.MainViewModel
import com.example.genpox.ui.main.getActiveReagentColor
import java.util.Random
import kotlin.math.sqrt
import kotlin.math.cos
import kotlin.math.sin

/**
 * A modifier that applies a real-time retro CRT phosphor-monitor filter to any Composable content.
 * It coordinates color tones with the provided baseColor.
 */
fun Modifier.phosphorMonitor(
    baseColor: Color = CyberGreen,
    scanlineOpacity: Float = 0.25f,
    grilleOpacity: Float = 0.15f,
    bloomIntensity: Float = 0.3f,
    curvatureBezel: Float = 0.05f, // Amount of screen curvature bulge simulation
    jitterSpeed: Float = 0.5f,     // Animation hum speed
    glitchIntensity: Float = 0.1f, // Static ticks & horizontal sync aberration
    timeMs: Long = 0L
): Modifier = this.then(
    Modifier.drawWithContent {
        // 1. Draw the underlying Composable content
        drawContent()

        val w = size.width
        val h = size.height
        if (w <= 0 || h <= 0) return@drawWithContent

        // 2. Draw CRT glass bezel & screen curvature vignette shading
        drawCrtVignette(w, h, baseColor, curvatureBezel)

        // 3. Draw Scanlines
        if (scanlineOpacity > 0f) {
            val scanlineSpacing = 4.dp.toPx()
            val scanlineBrush = Brush.verticalGradient(
                0f to Color.Transparent,
                0.5f to Color.Black.copy(alpha = scanlineOpacity),
                1f to Color.Transparent,
                startY = 0f,
                endY = scanlineSpacing,
                tileMode = TileMode.Repeated
            )
            drawRect(brush = scanlineBrush, size = size, blendMode = BlendMode.Multiply)
        }

        // 4. Draw Aperture Grille (vertical lines simulating phosphor columns)
        if (grilleOpacity > 0f) {
            val grilleSpacing = 3.dp.toPx()
            val grilleBrush = Brush.horizontalGradient(
                0f to Color.Transparent,
                0.5f to Color.Black.copy(alpha = grilleOpacity),
                1f to Color.Transparent,
                startX = 0f,
                endX = grilleSpacing,
                tileMode = TileMode.Repeated
            )
            drawRect(brush = grilleBrush, size = size, blendMode = BlendMode.Multiply)
        }

        // 5. Draw Phosphor Bloom / Halo (faint overlay drawn with additive blend)
        if (bloomIntensity > 0f) {
            // Draw a slightly offset glow layer to simulate excited glass bleeding
            drawRect(
                color = baseColor.copy(alpha = bloomIntensity * 0.12f),
                size = size,
                blendMode = BlendMode.Plus
            )
        }

        // 6. Draw Jitter / Glitch Artifacts & Screen Noise
        if (glitchIntensity > 0f && timeMs > 0L) {
            val rand = Random(timeMs / 150)
            
            // Random analog sync-tear lines
            if (rand.nextFloat() < 0.2f * glitchIntensity) {
                val lineY = rand.nextFloat() * h
                val lineH = rand.nextFloat() * 4.dp.toPx() + 1.dp.toPx()
                drawRect(
                    color = baseColor.copy(alpha = rand.nextFloat() * 0.15f * glitchIntensity),
                    topLeft = Offset(0f, lineY),
                    size = Size(w, lineH),
                    blendMode = BlendMode.Plus
                )
            }

            // CRT Snow/Phosphor particle static ticks
            if (rand.nextFloat() < 0.4f) {
                val ticksCount = (4 * glitchIntensity).toInt().coerceAtLeast(1)
                for (i in 0 until ticksCount) {
                    val px = rand.nextFloat() * w
                    val py = rand.nextFloat() * h
                    val tickLen = rand.nextFloat() * 8.dp.toPx() + 2.dp.toPx()
                    drawLine(
                        color = Color.White.copy(alpha = rand.nextFloat() * 0.3f),
                        start = Offset(px, py),
                        end = Offset(px + tickLen, py),
                        strokeWidth = 1.dp.toPx(),
                        blendMode = BlendMode.Plus
                    )
                }
            }
        }

        // 7. Draw Glass Glare reflection (diagonal light reflection overlay)
        val glareBrush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.02f),
                Color.Transparent,
                Color.Transparent
            ),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )
        drawRect(brush = glareBrush, size = size, blendMode = BlendMode.Plus)
    }
)

/**
 * Helper to draw vignette / bulge shading mimicking glass curvature
 */
private fun DrawScope.drawCrtVignette(w: Float, h: Float, color: Color, curveFactor: Float) {
    if (curveFactor <= 0f) return
    
    // Draw corner vignette darkening
    val radialVignette = Brush.radialGradient(
        colors = listOf(
            Color.Transparent,
            Color.Black.copy(alpha = 0.3f),
            Color.Black.copy(alpha = 0.9f)
        ),
        center = Offset(w / 2f, h / 2f),
        radius = sqrt(w * w + h * h) / 2f
    )
    drawRect(brush = radialVignette, size = size, blendMode = BlendMode.Multiply)


}

/**
 * Generates a ColorMatrix for mapping grayscale image channels into a specific base color ramp,
 * with high-excitation white highlights.
 */
fun getPhosphorColorMatrix(baseColor: Color): ColorMatrix {
    val r = baseColor.red
    val g = baseColor.green
    val b = baseColor.blue
    
    // Convert source pixels to luminance (0.2126R + 0.7152G + 0.0722B),
    // then scale and project onto target R, G, B channels to map monochromatic tone.
    return ColorMatrix(floatArrayOf(
        0.2126f * r, 0.7152f * r, 0.0722f * r, 0f, 0f,
        0.2126f * g, 0.7152f * g, 0.0722f * g, 0f, 0f,
        0.2126f * b, 0.7152f * b, 0.0722f * b, 0f, 0f,
        0f,          0f,          0f,          1f, 0f
    ))
}

/**
 * Lightweight Sobel Filter Edge Detection on Android Bitmaps.
 * Converted to return a white-on-black binary edge map which can be passed to getPhosphorColorMatrix.
 */
fun applySobelEdgeDetection(src: Bitmap, threshold: Float = 0.12f): Bitmap {
    val width = src.width
    val height = src.height
    val dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    
    val pixels = IntArray(width * height)
    src.getPixels(pixels, 0, width, 0, 0, width, height)
    
    val gray = FloatArray(width * height)
    for (i in pixels.indices) {
        val p = pixels[i]
        val rVal = (p shr 16) and 0xFF
        val gVal = (p shr 8) and 0xFF
        val bVal = p and 0xFF
        // Compute Rec. 709 luminance
        gray[i] = 0.2126f * rVal + 0.7152f * gVal + 0.0722f * bVal
    }
    
    val outPixels = IntArray(width * height)
    
    for (y in 1 until height - 1) {
        for (x in 1 until width - 1) {
            val idx = y * width + x
            
            // Grab 3x3 grid around pixel
            val v00 = gray[(y - 1) * width + (x - 1)]
            val v01 = gray[(y - 1) * width + x]
            val v02 = gray[(y - 1) * width + (x + 1)]
            
            val v10 = gray[y * width + (x - 1)]
            val v12 = gray[y * width + (x + 1)]
            
            val v20 = gray[(y + 1) * width + (x - 1)]
            val v21 = gray[(y + 1) * width + x]
            val v22 = gray[(y + 1) * width + (x + 1)]
            
            // Sobel horizontal (Gx) and vertical (Gy) gradients
            val gx = (v02 + 2f * v12 + v22) - (v00 + 2f * v10 + v20)
            val gy = (v20 + 2f * v21 + v22) - (v00 + 2f * v01 + v02)
            
            val gMag = sqrt(gx * gx + gy * gy)
            
            if (gMag > threshold * 255f) {
                outPixels[idx] = AndroidColor.WHITE
            } else {
                outPixels[idx] = AndroidColor.BLACK
            }
        }
    }
    
    dest.setPixels(outPixels, 0, width, 0, 0, width, height)
    return dest
}

/**
 * A customizable mock scene generator that draws grid lines, text readouts, and circular gauges
 * directly to a Canvas. Used to show real-time Phosphor Monitor results when no custom image is loaded.
 */
@Composable
fun PhosphorMockRadarScreen(
    baseColor: Color,
    timeMs: Long,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_anim")
    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_angle"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val maxRadius = minOf(w, h) * 0.4f

        // Draw HUD alignment grid lines
        val gridCount = 8
        for (i in 1..gridCount) {
            val gx = (w / gridCount) * i
            drawLine(
                color = baseColor.copy(alpha = 0.08f),
                start = Offset(gx, 0f),
                end = Offset(gx, h),
                strokeWidth = 1.dp.toPx()
            )
            val gy = (h / gridCount) * i
            drawLine(
                color = baseColor.copy(alpha = 0.08f),
                start = Offset(0f, gy),
                end = Offset(w, gy),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw concentric radar circles
        for (r in listOf(0.25f, 0.5f, 0.75f, 1.0f)) {
            drawCircle(
                color = baseColor.copy(alpha = 0.2f),
                radius = maxRadius * r,
                center = Offset(cx, cy),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Draw crosshair axes
        drawLine(
            color = baseColor.copy(alpha = 0.3f),
            start = Offset(cx - maxRadius * 1.1f, cy),
            end = Offset(cx + maxRadius * 1.1f, cy),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = baseColor.copy(alpha = 0.3f),
            start = Offset(cx, cy - maxRadius * 1.1f),
            end = Offset(cx, cy + maxRadius * 1.1f),
            strokeWidth = 1.dp.toPx()
        )

        // Draw sweeping radar ray
        val angleRad = Math.toRadians(radarAngle.toDouble())
        val rx = cx + maxRadius * cos(angleRad).toFloat()
        val ry = cy + maxRadius * sin(angleRad).toFloat()
        drawLine(
            color = baseColor.copy(alpha = 0.7f),
            start = Offset(cx, cy),
            end = Offset(rx, ry),
            strokeWidth = 2.dp.toPx()
        )

        // Draw simulated blip points
        val blips = listOf(
            Offset(cx + maxRadius * 0.4f, cy - maxRadius * 0.3f) to 0.8f,
            Offset(cx - maxRadius * 0.6f, cy + maxRadius * 0.2f) to 0.4f,
            Offset(cx + maxRadius * 0.1f, cy + maxRadius * 0.5f) to 0.9f
        )
        for ((pos, alpha) in blips) {
            drawCircle(
                color = baseColor.copy(alpha = alpha),
                radius = 6.dp.toPx(),
                center = pos
            )
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.8f),
                radius = 2.dp.toPx(),
                center = pos
            )
        }
    }
}

/**
 * Creates a static high-contrast vector-like calibration pattern in memory.
 */
fun createSampleBitmap(): Bitmap {
    val bmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    val paint = android.graphics.Paint().apply {
        color = AndroidColor.WHITE
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }
    
    // Background clear
    canvas.drawColor(AndroidColor.BLACK)
    
    // Draw calibration circles and crosses
    canvas.drawCircle(128f, 128f, 100f, paint)
    canvas.drawCircle(128f, 128f, 60f, paint)
    paint.strokeWidth = 1.5f
    canvas.drawLine(128f, 10f, 128f, 246f, paint)
    canvas.drawLine(10f, 128f, 246f, 128f, paint)
    
    // Draw some box outlines
    canvas.drawRect(48f, 48f, 208f, 208f, paint)
    
    // Draw text readout details
    paint.color = AndroidColor.WHITE
    paint.style = android.graphics.Paint.Style.FILL
    paint.textSize = 20f
    paint.typeface = android.graphics.Typeface.MONOSPACE
    canvas.drawText("GENPOX CRT", 62f, 96f, paint)
    canvas.drawText("TEST PATTERN", 54f, 172f, paint)
    
    return bmp
}

@Composable
fun PhosphorMonitorSettingsView(viewModel: MainViewModel) {
    val scanlineOpacity by viewModel.crtScanlineOpacity.collectAsState()
    val grilleOpacity by viewModel.crtGrilleOpacity.collectAsState()
    val bloomIntensity by viewModel.crtBloomIntensity.collectAsState()
    val curvatureBezel by viewModel.crtCurvatureBezel.collectAsState()
    val glitchIntensity by viewModel.crtGlitchIntensity.collectAsState()
    var isVectorMode by remember { mutableStateOf(false) }
    var isAnimatedSource by remember { mutableStateOf(true) }
    
    // Theme Color indexing
    var selectedColorIndex by remember { mutableStateOf(0) }
    
    // Continuous time hum animation ticker
    val infiniteTransition = rememberInfiniteTransition(label = "jitter_time")
    val timeMsFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 100000f,
        animationSpec = infiniteRepeatable(
            animation = tween(100000, easing = LinearEasing)
        ),
        label = "time_ms"
    )
    val timeMs = timeMsFloat.toLong()

    // Base colors mapped from GENPOX core palettes & active reagent slot
    val activeReagent by viewModel.selectedReagent.collectAsState()
    val colors = listOf(
        CyberTheme.green to "CYBER-GREEN",
        CyberTheme.purple to "CYBER-PURPLE",
        CyberTheme.cyan to "CYBER-CYAN",
        CyberTheme.red to "CYBER-RED",
        getActiveReagentColor(activeReagent) to "ACTIVE REAGENT"
    )
    val baseColor = colors[selectedColorIndex].first

    // Cache dynamic test bitmaps
    val context = LocalContext.current
    val baseBmp = remember {
        android.graphics.BitmapFactory.decodeResource(
            context.resources,
            com.example.genpox.R.drawable.venus_art
        ) ?: createSampleBitmap()
    }
    val edgeBmp = remember(baseBmp) { applySobelEdgeDetection(baseBmp, 0.12f) }
    
    val displayBmp = if (isVectorMode) edgeBmp else baseBmp
    val displayImageBitmap = remember(displayBmp) { displayBmp.asImageBitmap() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "PHOSPHOR CRT MONITOR EMULATOR", style = Typography.titleMedium, color = CyberGreen)
        Text(
            text = "Fine-tune cathode-ray emission, grid masks, phosphor decay bloom, and edge-extracted vector graphics.",
            style = Typography.bodySmall,
            color = CyberGreenDim
        )

        // 1. Live CRT Display Preview Box (4:3 aspect ratio bezel)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.33f)
                .background(Color.Black)
        ) {
            if (isAnimatedSource) {
                // Animated Vector Radar Feed
                PhosphorMockRadarScreen(baseColor = baseColor, timeMs = timeMs)
            } else {
                // Static Glyph Pattern
                androidx.compose.foundation.Image(
                    bitmap = displayImageBitmap,
                    contentDescription = "CRT Source",
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = ColorFilter.colorMatrix(getPhosphorColorMatrix(baseColor))
                )
            }
        }

        // 2. Monitor Configuration Controls Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                .background(CyberPanel)
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "MONITOR CONFIGURATION CONTROLS", style = Typography.bodyMedium, fontWeight = FontWeight.Bold, color = CyberGreen)

                // Input source select row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Input Signal Source:", style = Typography.bodySmall, color = CyberGreenDim)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(true to "HUD RADAR", false to "GLYPH MAP").forEach { (animated, label) ->
                            val active = isAnimatedSource == animated
                            Button(
                                onClick = {
                                    viewModel.synthManager.playCombinatorTick()
                                    isAnimatedSource = animated
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (active) baseColor else CyberPanel,
                                    contentColor = if (active) Color.Black else baseColor.copy(alpha = 0.6f)
                                ),
                                border = BorderStroke(1.dp, baseColor.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(2.dp),
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text(label, style = Typography.labelSmall, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Vector/Outline Mode toggle (only active for static patterns to show edge detection)
                if (!isAnimatedSource) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Vector CRT Outline Mode (Sobel):", style = Typography.bodySmall, color = CyberGreenDim)
                        Switch(
                            checked = isVectorMode,
                            onCheckedChange = {
                                viewModel.synthManager.playCombinatorTick()
                                isVectorMode = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = baseColor,
                                uncheckedThumbColor = baseColor.copy(alpha = 0.5f),
                                uncheckedTrackColor = Color.Black
                            )
                        )
                    }
                }

                // Tint selections (color coordinated to base themes)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Select Monitor Phosphor Tint:", style = Typography.bodySmall, color = CyberGreenDim)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        colors.forEachIndexed { idx, (color, name) ->
                            val active = selectedColorIndex == idx
                            Button(
                                onClick = {
                                    viewModel.synthManager.playCombinatorTick()
                                    selectedColorIndex = idx
                                },
                                modifier = Modifier.weight(1f).height(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (active) color else CyberPanel,
                                    contentColor = if (active) Color.Black else color.copy(alpha = 0.7f)
                                ),
                                border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(2.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp)
                            ) {
                                Text(name.take(6), style = Typography.labelSmall, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. Physical Parameters Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                .background(CyberPanel)
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "PHYSICAL EMISSION PARAMETERS", style = Typography.bodyMedium, fontWeight = FontWeight.Bold, color = CyberGreen)
                
                // Scanline opacity
                Text(text = "Scanline Grid Density (${(scanlineOpacity * 100).toInt()}%):", style = Typography.bodySmall, color = CyberGreenDim)
                PoxSlider(
                    value = scanlineOpacity,
                    onValueChange = {
                        viewModel.setCrtScanlineOpacity(it)
                        CyberTheme.savePreset(context, "active_colors")
                    },
                    activeColor = baseColor
                )

                // Grille opacity
                Text(text = "Aperture Grille Mask (${(grilleOpacity * 100).toInt()}%):", style = Typography.bodySmall, color = CyberGreenDim)
                PoxSlider(
                    value = grilleOpacity,
                    onValueChange = {
                        viewModel.setCrtGrilleOpacity(it)
                        CyberTheme.savePreset(context, "active_colors")
                    },
                    activeColor = baseColor
                )

                // Bloom strength
                Text(text = "Phosphor Glow Bleeding (${(bloomIntensity * 100).toInt()}%):", style = Typography.bodySmall, color = CyberGreenDim)
                PoxSlider(
                    value = bloomIntensity,
                    onValueChange = {
                        viewModel.setCrtBloomIntensity(it)
                        CyberTheme.savePreset(context, "active_colors")
                    },
                    activeColor = baseColor
                )

                // Curvature bezel factor
                Text(text = "CRT Tube Glass Curvature (${(curvatureBezel * 1000).toInt()}):", style = Typography.bodySmall, color = CyberGreenDim)
                PoxSlider(
                    value = curvatureBezel,
                    onValueChange = {
                        viewModel.setCrtCurvatureBezel(it)
                        CyberTheme.savePreset(context, "active_colors")
                    },
                    valueRange = 0f..0.15f,
                    activeColor = baseColor
                )

                // Sync Jitter
                Text(text = "Analog Jitter & Noise (${(glitchIntensity * 100).toInt()}%):", style = Typography.bodySmall, color = CyberGreenDim)
                PoxSlider(
                    value = glitchIntensity,
                    onValueChange = {
                        viewModel.setCrtGlitchIntensity(it)
                        CyberTheme.savePreset(context, "active_colors")
                    },
                    activeColor = baseColor
                )
            }
        }
    }
}
