package com.example.genpox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpox.theme.*

@Composable
fun DualPaneConsoleFrame(
    modifier: Modifier = Modifier,
    theme: String = "green", // "green" or "purple"
    flavorTitle: String,
    statusText: String? = null,
    statusColor: Color = CyberGreen,
    statusClickable: (() -> Unit)? = null,
    primaryTitle: String? = null,
    primaryContent: @Composable ColumnScope.() -> Unit,
    secondaryTitle: String? = null,
    secondaryContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    val activeBorder = if (theme == "purple") Color(0xFFA855F7).copy(alpha = 0.4f) else CyberBorder
    val activePanel = if (theme == "purple") Color(0xFF10071C) else CyberPanel
    val headerColor = if (theme == "purple") Color(0xFFA855F7) else CyberGreenDim

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Universal Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = flavorTitle.uppercase(),
                color = headerColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default
            )
            if (statusText != null) {
                val textModifier = if (statusClickable != null) {
                    Modifier.clickable { statusClickable() }
                } else {
                    Modifier
                }
                Text(
                    text = statusText.uppercase(),
                    color = statusColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Default,
                    modifier = textModifier
                )
            }
        }

        // Primary Pane Card
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
                if (primaryTitle != null) {
                    Text(
                        text = primaryTitle.uppercase(),
                        color = Color.White,
                        style = Typography.bodyMedium,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Bold
                    )
                }
                primaryContent()
            }
        }

        // Optional Secondary Pane Card
        if (secondaryContent != null) {
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
                    if (secondaryTitle != null) {
                        Text(
                            text = secondaryTitle.uppercase(),
                            color = Color.White,
                            style = Typography.bodyMedium,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    secondaryContent()
                }
            }
        }
    }
}
