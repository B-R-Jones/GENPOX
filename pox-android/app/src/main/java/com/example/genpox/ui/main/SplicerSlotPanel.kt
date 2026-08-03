package com.example.genpox.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpox.data.GeneSequence
import com.example.genpox.data.WaveMath
import com.example.genpox.theme.CyberBorder
import com.example.genpox.theme.CyberGreen
import com.example.genpox.theme.CyberGreenDim
import com.example.genpox.theme.CyberRed
import com.example.genpox.theme.Typography

@Composable
fun SplicerSlotPanel(
    viewModel: MainViewModel,
    activeSlotSelection: Int?,
    targetSequence: String,
    slotSequenceFilter: String,
    inventoryGenes: List<GeneSequence>,
    isWide: Boolean
) {
    if (activeSlotSelection == null) return

    val expected = targetSequence.substring(activeSlotSelection * 8, (activeSlotSelection + 1) * 8)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ASSIGN GENE BLOCK TO SLOT #${activeSlotSelection + 1}",
                color = CyberGreenDim,
                fontSize = 9.sp,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { viewModel.selectSplicerSlot(null) },
                modifier = Modifier.height(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberRed,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text(
                    text = "CLOSE",
                    color = Color.Black,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Expected gene info block
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CyberGreen.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                .padding(6.dp)
        ) {
            Text(
                text = "REQUIRED SEGMENT FOR SLOT #${activeSlotSelection + 1}",
                color = CyberGreenDim,
                fontSize = 9.sp,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = expected,
                color = Color(0xFF22D3EE),
                style = Typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
        }

        val matchingGene = inventoryGenes.find { it.sequence == expected && it.count > 0 }

        if (matchingGene != null) {
            // Stock panel matching Required Segment block style
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberGreen.copy(alpha = 0.05f), RoundedCornerShape(4.dp))
                    .border(1.dp, CyberGreenDim, RoundedCornerShape(4.dp))
                    .padding(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AVAILABLE STOCK",
                        color = CyberGreenDim,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Default,
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
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "x${matchingGene.count}",
                        color = CyberGreen,
                        style = Typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Button(
                onClick = { viewModel.assignGeneToSlot(matchingGene.sequence) },
                modifier = Modifier.fillMaxWidth().height(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberGreen,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "LOAD GENE BLOCK",
                    color = Color.Black,
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
                    .background(Color(0x15EF4444), RoundedCornerShape(4.dp))
                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(4.dp))
                    .padding(6.dp)
            ) {
                Text(
                    text = "AVAILABLE STOCK",
                    color = Color(0xFFEF4444).copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Default,
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

        Spacer(modifier = Modifier.height(6.dp))

        Button(
            onClick = { viewModel.setTargetSynthesisSequence(expected) },
            modifier = Modifier.fillMaxWidth().height(32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color(0xFF22D3EE)
            ),
            border = BorderStroke(1.dp, Color(0xFF22D3EE)),
            shape = RoundedCornerShape(4.dp),
            contentPadding = PaddingValues(0.dp)
        ) {
            Text(
                text = "TARGET IN BIO-LAB",
                color = Color(0xFF22D3EE),
                style = Typography.bodySmall,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 8.5.sp
            )
        }
    }
}
