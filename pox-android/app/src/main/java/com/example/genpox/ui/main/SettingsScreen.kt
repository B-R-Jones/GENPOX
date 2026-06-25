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


// ==========================================
// 7. SETTINGS VIEW
// ==========================================
@Composable
fun ColorSliderRow(
    label: String,
    color: Color,
    onColorChange: (Color) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
            .background(CyberPanel)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(),
                style = Typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = CyberGreen
            )
            Box(
                modifier = Modifier
                    .size(40.dp, 18.dp)
                    .background(color, RoundedCornerShape(2.dp))
                    .border(1.dp, CyberBorder, RoundedCornerShape(2.dp))
            )
        }

        // R Slider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "R: ${(color.red * 255).toInt().toString().padStart(3, ' ')}",
                color = CyberGreenDim,
                style = Typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(42.dp)
            )
            Slider(
                value = color.red,
                onValueChange = { onColorChange(Color(it, color.green, color.blue, 1f)) },
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = CyberGreen,
                    activeTrackColor = CyberGreen,
                    inactiveTrackColor = Color.Black
                )
            )
        }

        // G Slider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "G: ${(color.green * 255).toInt().toString().padStart(3, ' ')}",
                color = CyberGreenDim,
                style = Typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(42.dp)
            )
            Slider(
                value = color.green,
                onValueChange = { onColorChange(Color(color.red, it, color.blue, 1f)) },
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = CyberGreen,
                    activeTrackColor = CyberGreen,
                    inactiveTrackColor = Color.Black
                )
            )
        }

        // B Slider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "B: ${(color.blue * 255).toInt().toString().padStart(3, ' ')}",
                color = CyberGreenDim,
                style = Typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.width(42.dp)
            )
            Slider(
                value = color.blue,
                onValueChange = { onColorChange(Color(color.red, color.green, it, 1f)) },
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = CyberGreen,
                    activeTrackColor = CyberGreen,
                    inactiveTrackColor = Color.Black
                )
            )
        }
    }
}

@Composable
fun PaletteSettingsView(viewModel: MainViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "COLOR & PALETTE MANAGEMENT", style = Typography.titleMedium, color = CyberGreen)

        val context = LocalContext.current

        // Persistent Core Theme Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                .background(CyberPanel)
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "PERSISTENT CORE THEME", style = Typography.bodyMedium, fontWeight = FontWeight.Bold, color = CyberGreen)
                Text(text = "Save your adjustments as the active core theme so they load automatically on launch.", style = Typography.bodySmall, color = CyberGreenDim)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.synthManager.playSynthesisSuccess()
                            CyberTheme.savePreset(context, "active_colors")
                            viewModel.addLog("SYS: Secure Active Colors Saved.")
                        },
                        modifier = Modifier.weight(1f).height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("SAVE TO ACTIVE THEME", style = Typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Preset Slots Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                .background(CyberPanel)
                .padding(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "THEME PRESET SLOTS", style = Typography.bodyMedium, fontWeight = FontWeight.Bold, color = CyberGreen)
                Text(text = "Save and load custom cyber presets.", style = Typography.bodySmall, color = CyberGreenDim)

                listOf(
                    Pair("preset_1", "PRESET ALPHA"),
                    Pair("preset_2", "PRESET BETA"),
                    Pair("preset_3", "PRESET GAMMA")
                ).forEach { preset ->
                    var isSaved by remember { mutableStateOf(context.getSharedPreferences("cyber_theme_presets", Context.MODE_PRIVATE).contains("${preset.first}_green")) }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = preset.second,
                            color = if (isSaved) CyberGreen else CyberGreenDim,
                            style = Typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Button(
                            onClick = {
                                viewModel.synthManager.playCombinatorTick()
                                CyberTheme.savePreset(context, preset.first)
                                isSaved = true
                                viewModel.addLog("SYS: Theme saved to ${preset.second}.")
                            },
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGreenDim, contentColor = Color.Black),
                            shape = RoundedCornerShape(2.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("SAVE", style = Typography.labelSmall, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = {
                                if (isSaved) {
                                    viewModel.synthManager.playSynthesisSuccess()
                                    CyberTheme.loadPreset(context, preset.first)
                                    viewModel.addLog("SYS: Loaded theme from ${preset.second}.")
                                }
                            },
                            enabled = isSaved,
                            modifier = Modifier.height(28.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSaved) CyberGreen else Color.Black,
                                contentColor = if (isSaved) Color.Black else Color.Gray,
                                disabledContainerColor = Color.Black,
                                disabledContentColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(2.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("LOAD", style = Typography.labelSmall, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(text = "CYBER GREEN PALETTE", style = Typography.bodyMedium, fontWeight = FontWeight.Bold, color = CyberGreen)
        ColorSliderRow(label = "Primary Green", color = CyberTheme.green, onColorChange = { CyberTheme.green = it })
        ColorSliderRow(label = "Dim Green", color = CyberTheme.greenDim, onColorChange = { CyberTheme.greenDim = it })
        ColorSliderRow(label = "Green Border", color = CyberTheme.greenBorder, onColorChange = { CyberTheme.greenBorder = it })
        ColorSliderRow(label = "Green Panel", color = CyberTheme.greenPanel, onColorChange = { CyberTheme.greenPanel = it })

        Spacer(modifier = Modifier.height(4.dp))

        Text(text = "CYBER PURPLE PALETTE", style = Typography.bodyMedium, fontWeight = FontWeight.Bold, color = CyberTheme.purple)
        ColorSliderRow(label = "Primary Purple", color = CyberTheme.purple, onColorChange = { CyberTheme.purple = it })
        ColorSliderRow(label = "Dim Purple", color = CyberTheme.purpleDim, onColorChange = { CyberTheme.purpleDim = it })
        ColorSliderRow(label = "Purple Border", color = CyberTheme.purpleBorder, onColorChange = { CyberTheme.purpleBorder = it })
        ColorSliderRow(label = "Purple Panel", color = CyberTheme.purplePanel, onColorChange = { CyberTheme.purplePanel = it })

        Spacer(modifier = Modifier.height(4.dp))

        Text(text = "CYBER CYAN PALETTE", style = Typography.bodyMedium, fontWeight = FontWeight.Bold, color = CyberTheme.cyan)
        ColorSliderRow(label = "Primary Cyan", color = CyberTheme.cyan, onColorChange = { CyberTheme.cyan = it })
        ColorSliderRow(label = "Dim Cyan", color = CyberTheme.cyanDim, onColorChange = { CyberTheme.cyanDim = it })
        ColorSliderRow(label = "Cyan Border", color = CyberTheme.cyanBorder, onColorChange = { CyberTheme.cyanBorder = it })
        ColorSliderRow(label = "Cyan Panel", color = CyberTheme.cyanPanel, onColorChange = { CyberTheme.cyanPanel = it })

        Spacer(modifier = Modifier.height(4.dp))

        Text(text = "CYBER RED PALETTE", style = Typography.bodyMedium, fontWeight = FontWeight.Bold, color = CyberTheme.red)
        ColorSliderRow(label = "Primary Red", color = CyberTheme.red, onColorChange = { CyberTheme.red = it })
        ColorSliderRow(label = "Dim Red", color = CyberTheme.redDim, onColorChange = { CyberTheme.redDim = it })
        ColorSliderRow(label = "Red Border", color = CyberTheme.redBorder, onColorChange = { CyberTheme.redBorder = it })
        ColorSliderRow(label = "Red Panel", color = CyberTheme.redPanel, onColorChange = { CyberTheme.redPanel = it })

        Spacer(modifier = Modifier.height(4.dp))

        Text(text = "GLOBAL BACKGROUND COLORS", style = Typography.bodyMedium, fontWeight = FontWeight.Bold, color = CyberGreen)
        ColorSliderRow(label = "Main Background", color = CyberTheme.background, onColorChange = { CyberTheme.background = it })
        ColorSliderRow(label = "Dark Background", color = CyberTheme.backgroundDark, onColorChange = { CyberTheme.backgroundDark = it })

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                viewModel.synthManager.playCombinatorTick()
                CyberTheme.resetToDefaults()
            },
            modifier = Modifier.fillMaxWidth().height(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen, contentColor = Color.Black),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text("RESET COLOR PALETTES TO DEFAULT", style = Typography.labelSmall, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ==========================================
@Composable
fun SettingsView(viewModel: MainViewModel) {
    var subTab by remember { mutableStateOf("general") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Subtab Selection Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                Pair("general", "GENERAL"),
                Pair("telemetry", "TELEMETRY"),
                Pair("palette", "PALETTE")
            ).forEach { tab ->
                val isActive = subTab == tab.first
                Button(
                    onClick = {
                        viewModel.synthManager.playCombinatorTick()
                        subTab = tab.first
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isActive) CyberGreen else CyberPanel,
                        contentColor = if (isActive) Color.Black else CyberGreenDim
                    ),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, CyberBorder),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = tab.second,
                        style = Typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (subTab == "general") {
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
        } else if (subTab == "telemetry") {
            TelemetryView(viewModel)
        } else if (subTab == "palette") {
            PaletteSettingsView(viewModel)
        }
    }
}
