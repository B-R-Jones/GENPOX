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
import com.example.genpox.data.BiophysicsEngine
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
// SUB-DIAGNOSTIC POPUPS
// ==========================================
@Composable
fun GeneDetailsPopup(
    viewModel: MainViewModel,
    gene: String,
    activeColor: Color,
    activePanel: Color,
    onClose: () -> Unit
) {
    val isAnom = WaveMath.isAnomalousGene(gene)
    val benefit = if (isAnom) WaveMath.getBenefitForAnomalousGene(gene) else null
    
    AlertDialog(
        onDismissRequest = onClose,
        modifier = Modifier.cyberglass(
            borderColor = if (isAnom) CyberTheme.purple else activeColor,
            backgroundColor = if (isAnom) CyberTheme.purplePanel else activePanel
        ),
        containerColor = Color.Transparent,
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
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isAnom) Color(0xFFD8B4FE) else activeColor
                )

                // Biophysical parameters
                val geneSequence = remember(gene, viewModel.geneSequences.collectAsState().value) {
                    viewModel.geneSequences.value.find { it.sequence == gene }
                }
                val tm = remember(gene, geneSequence) {
                    geneSequence?.meltingTemp ?: BiophysicsEngine.calculateMeltingTemperature(gene, viewModel.reactorSalt.value.toDouble())
                }
                val mfe = remember(gene, geneSequence) {
                    geneSequence?.minimumFreeEnergy ?: BiophysicsEngine.calculateMinimumFreeEnergy(gene)
                }
                val qScore = remember(gene, geneSequence) {
                    geneSequence?.averageQScore ?: 30.0
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, (if (isAnom) Color(0xFFD8B4FE) else activeColor).copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "THERMODYNAMIC Tm: " + String.format(Locale.US, "%.2f°C", tm),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "MIN FREE ENERGY: " + String.format(Locale.US, "%.2f kcal/mol", mfe),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ACCURATE Q-SCORE: " + String.format(Locale.US, "%.1f (Phred)", qScore),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

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
                        color = activeColor.copy(alpha = 0.6f),
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
            PoxButton(
                text = "✕ CLOSE",
                onClick = onClose,
                buttonType = PoxButtonType.RED_DANGER,
                buttonSize = PoxButtonSize.COMPACT,
                sound = PoxButtonSound.BEEP_DEFAULT,
                viewModel = viewModel
            )
        },
        dismissButton = {
            PoxButton(
                text = "TARGET IN BIO-LAB",
                onClick = {
                    viewModel.setTargetSynthesisSequence(gene)
                    onClose()
                },
                buttonType = PoxButtonType.CYAN_CELESTIAL,
                buttonSize = PoxButtonSize.COMPACT,
                sound = PoxButtonSound.BEEP_DEFAULT,
                viewModel = viewModel
            )
        }
    )
}

@Composable
fun StepSearchGeneDetailsPopup(
    viewModel: MainViewModel,
    gene: String,
    activeColor: Color,
    activePanel: Color,
    onClose: () -> Unit,
    onDeconstructGene: ((GeneSequence) -> Unit)? = null
) {
    val isAnom = WaveMath.isAnomalousGene(gene)
    val benefit = if (isAnom) WaveMath.getBenefitForAnomalousGene(gene) else null
    
    AlertDialog(
        onDismissRequest = onClose,
        modifier = Modifier.cyberglass(
            borderColor = if (isAnom) CyberTheme.purple else activeColor,
            backgroundColor = if (isAnom) CyberTheme.purplePanel else activePanel
        ),
        containerColor = Color.Transparent,
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
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isAnom) Color(0xFFD8B4FE) else activeColor
                )

                // Biophysical parameters
                val geneSequence = remember(gene, viewModel.geneSequences.collectAsState().value) {
                    viewModel.geneSequences.value.find { it.sequence == gene }
                }
                val tm = remember(gene, geneSequence) {
                    geneSequence?.meltingTemp ?: BiophysicsEngine.calculateMeltingTemperature(gene, viewModel.reactorSalt.value.toDouble())
                }
                val mfe = remember(gene, geneSequence) {
                    geneSequence?.minimumFreeEnergy ?: BiophysicsEngine.calculateMinimumFreeEnergy(gene)
                }
                val qScore = remember(gene, geneSequence) {
                    geneSequence?.averageQScore ?: 30.0
                }
                val count = geneSequence?.count ?: 0

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, (if (isAnom) Color(0xFFD8B4FE) else activeColor).copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "THERMODYNAMIC Tm: " + String.format(Locale.US, "%.2f°C", tm),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "MIN FREE ENERGY: " + String.format(Locale.US, "%.2f kcal/mol", mfe),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ACCURATE Q-SCORE: " + String.format(Locale.US, "%.1f (Phred)", qScore),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

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

                if (geneSequence != null && count > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    PoxButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "♺ DECONSTRUCT GENE BLOCK",
                        onClick = { onDeconstructGene?.invoke(geneSequence) },
                        buttonType = PoxButtonType.RED_DANGER,
                        buttonSize = PoxButtonSize.STANDARD,
                        sound = PoxButtonSound.BEEP_HIGH,
                        viewModel = viewModel
                    )
                }
            }
        },
        confirmButton = {
            PoxButton(
                text = "✕ CLOSE",
                onClick = onClose,
                buttonType = PoxButtonType.RED_DANGER,
                buttonSize = PoxButtonSize.COMPACT,
                sound = PoxButtonSound.BEEP_DEFAULT,
                viewModel = viewModel
            )
        },
        dismissButton = {
            PoxButton(
                text = "TARGET IN BIO-LAB",
                onClick = {
                    viewModel.setTargetSynthesisSequence(gene)
                    onClose()
                },
                buttonType = PoxButtonType.CYAN_CELESTIAL,
                buttonSize = PoxButtonSize.COMPACT,
                sound = PoxButtonSound.BEEP_DEFAULT,
                viewModel = viewModel
            )
        }
    )
}

@Composable
fun VaultGeneDetailsPopup(
    viewModel: MainViewModel,
    gene: String,
    onClose: () -> Unit
) {
    val benefit = WaveMath.getBenefitForAnomalousGene(gene)
    val isAnom = WaveMath.isAnomalousGene(gene)
    
    AlertDialog(
        onDismissRequest = onClose,
        modifier = Modifier.cyberglass(
            borderColor = CyberTheme.purple,
            backgroundColor = CyberTheme.purplePanel
        ),
        containerColor = Color.Transparent,
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
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD8B4FE)
                )

                // Biophysical parameters
                val geneSequence = remember(gene, viewModel.geneSequences.collectAsState().value) {
                    viewModel.geneSequences.value.find { it.sequence == gene }
                }
                val tm = remember(gene, geneSequence) {
                    geneSequence?.meltingTemp ?: BiophysicsEngine.calculateMeltingTemperature(gene, viewModel.reactorSalt.value.toDouble())
                }
                val mfe = remember(gene, geneSequence) {
                    geneSequence?.minimumFreeEnergy ?: BiophysicsEngine.calculateMinimumFreeEnergy(gene)
                }
                val qScore = remember(gene, geneSequence) {
                    geneSequence?.averageQScore ?: 30.0
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .border(1.dp, Color(0xFFD8B4FE).copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "THERMODYNAMIC Tm: " + String.format(Locale.US, "%.2f°C", tm),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "MIN FREE ENERGY: " + String.format(Locale.US, "%.2f kcal/mol", mfe),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ACCURATE Q-SCORE: " + String.format(Locale.US, "%.1f (Phred)", qScore),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

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
            PoxButton(
                text = "✕ CLOSE",
                onClick = onClose,
                buttonType = PoxButtonType.RED_DANGER,
                buttonSize = PoxButtonSize.COMPACT,
                sound = PoxButtonSound.BEEP_DEFAULT,
                viewModel = viewModel
            )
        },
        dismissButton = {
            PoxButton(
                text = "TARGET IN BIO-LAB",
                onClick = {
                    viewModel.setTargetSynthesisSequence(gene)
                    onClose()
                },
                buttonType = PoxButtonType.CYAN_CELESTIAL,
                buttonSize = PoxButtonSize.COMPACT,
                sound = PoxButtonSound.BEEP_DEFAULT,
                viewModel = viewModel
            )
        }
    )
}

@Composable
fun GeneDeconstructPopup(
    viewModel: MainViewModel,
    gene: GeneSequence,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var deconstructCount by remember { mutableStateOf(1) }

    var aCount = 0
    var gCount = 0
    var tCount = 0
    var cCount = 0
    gene.sequence.forEach { char ->
        when (char) {
            'A' -> aCount++
            'G' -> gCount++
            'T' -> tCount++
            'C' -> cCount++
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.cyberglass(
            borderColor = Color(0xFFEF4444),
            backgroundColor = Color(0xFF150808)
        ),
        containerColor = Color.Transparent,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "DECONSTRUCT GENE BLOCK",
                    color = Color.White,
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Default
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ARE YOU SURE YOU WANT TO RECYCLE STANDARD GENE SEQUENCE BLOCK:",
                    color = Color.LightGray,
                    style = Typography.bodySmall,
                    fontSize = 10.sp
                )
                Text(
                    text = gene.sequence,
                    color = CyberGreen,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "BASE YIELD (PER BLOCK):",
                    color = Color.Gray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Pair("A", aCount),
                        Pair("G", gCount),
                        Pair("T", tCount),
                        Pair("C", cCount)
                    ).forEach { (base, count) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, CyberBorder.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = base, color = CyberGreen, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                Text(text = "+${count}", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                if (gene.count > 1) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "QUANTITY TO DECONSTRUCT: $deconstructCount / ${gene.count}",
                        color = Color.LightGray,
                        style = Typography.labelSmall
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .cyberglass(borderColor = CyberBorder, backgroundColor = CyberPanel)
                                .clickable { if (deconstructCount > 1) deconstructCount-- },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("-", color = CyberGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        
                        Text(
                            text = "$deconstructCount",
                            color = Color.White,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .cyberglass(borderColor = CyberBorder, backgroundColor = CyberPanel)
                                .clickable { if (deconstructCount < gene.count) deconstructCount++ },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+", color = CyberGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (gene.count > 1) {
                    PoxButton(
                        text = "♺ DECONSTRUCT ALL",
                        onClick = { onConfirm(gene.count) },
                        buttonType = PoxButtonType.RED_DANGER,
                        buttonSize = PoxButtonSize.COMPACT,
                        sound = PoxButtonSound.BEEP_DEFAULT,
                        viewModel = viewModel
                    )
                }
                PoxButton(
                    text = "✓ DECONSTRUCT",
                    onClick = { onConfirm(deconstructCount) },
                    buttonType = PoxButtonType.RED_MUTED,
                    buttonSize = PoxButtonSize.COMPACT,
                    sound = PoxButtonSound.BEEP_DEFAULT,
                    viewModel = viewModel
                )
            }
        },
        dismissButton = {
            PoxButton(
                text = "✕ CANCEL",
                onClick = onDismiss,
                buttonType = PoxButtonType.GRAY_UTILITY,
                buttonSize = PoxButtonSize.COMPACT,
                sound = PoxButtonSound.BEEP_DEFAULT,
                viewModel = viewModel
            )
        }
    )
}
