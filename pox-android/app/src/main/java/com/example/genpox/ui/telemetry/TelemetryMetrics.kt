package com.example.genpox.ui.telemetry

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpox.theme.CyberBorder
import com.example.genpox.theme.CyberGreen
import com.example.genpox.theme.CyberGreenDim
import com.example.genpox.theme.CyberPanel

data class TelemetryMetric(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val formula: String,
    val description: String,
    val effects: List<String>,
)

fun factionColorForCategory(category: String): Color = when (category) {
    "COSMIC & LUNAR" -> Color(0xFF22D3EE)
    "GENETIC SYNTHESIS" -> Color(0xFFA855F7)
    "REACTOR & ANOMALY PHYSICS" -> Color(0xFFFBBF24)
    else -> Color(0xFFEF4444)
}

@Composable
fun TelemetryMetricRow(
    metric: TelemetryMetric,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .background(CyberPanel)
            .clickable { onClick() }
            .padding(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = metric.title,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Default,
                )
                Text(
                    text = metric.category,
                    color = factionColorForCategory(metric.category),
                    fontSize = 7.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Default,
                )
            }
            Text(
                text = metric.summary,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Default,
                lineHeight = 11.sp,
            )
            Text(
                text = "CLICK FOR FORMULAS & SIMULATORS ➔",
                color = CyberGreen,
                fontSize = 7.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default,
            )
        }
    }
}

@Composable
fun TelemetryDetailScreen(
    metric: TelemetryMetric,
    onBack: () -> Unit,
    interactiveWidgetContent: @Composable (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .border(1.dp, CyberGreen, RoundedCornerShape(2.dp))
                .background(Color.Black)
                .clickable { onBack() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .align(Alignment.Start),
        ) {
            Text(
                text = "✕ BACK TO LIBRARY",
                color = CyberGreen,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = metric.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Default,
            )
            Text(
                text = "[ CATEGORY: ${metric.category} ]",
                color = factionColorForCategory(metric.category),
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                .background(CyberPanel)
                .padding(8.dp),
        ) {
            Text(
                text = metric.description,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 9.sp,
                fontFamily = FontFamily.Default,
                lineHeight = 12.sp,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "MATHEMATICAL EXPRESSIONS:",
                color = CyberGreenDim,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF222222), RoundedCornerShape(4.dp))
                    .background(Color(0xFF030303))
                    .padding(8.dp),
            ) {
                Text(
                    text = metric.formula,
                    color = Color(0xFF00FF41),
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 12.sp,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "INTERACTIVE SIMULATOR & GRAPH:",
                color = CyberGreenDim,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                    .background(Color.Black)
                    .padding(8.dp),
            ) {
                interactiveWidgetContent(metric.id)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "SYSTEM PHENOTYPIC EFFECTS:",
                color = CyberGreenDim,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Default,
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                metric.effects.forEach { effect ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(text = "•", color = CyberGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = effect,
                            color = Color.LightGray,
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Default,
                            lineHeight = 11.sp,
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(55.dp))
    }
}
