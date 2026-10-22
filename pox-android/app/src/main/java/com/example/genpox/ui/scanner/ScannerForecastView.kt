package com.example.genpox.ui.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpox.data.WaveMath
import com.example.genpox.theme.CyberGreen
import com.example.genpox.theme.CyberGreenDim
import com.example.genpox.theme.Typography
import com.example.genpox.ui.main.MainViewModel
import com.example.genpox.ui.main.cyberglass

@Composable
fun ScannerWaveForecastView(viewModel: MainViewModel) {
    val todayWave = remember { WaveMath.getDailyWaveConfig(System.currentTimeMillis()) }
    val tomorrowWave = remember { WaveMath.getDailyWaveConfig(System.currentTimeMillis() + 86400000L) }
    val dayAfterWave = remember { WaveMath.getDailyWaveConfig(System.currentTimeMillis() + 172800000L) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Today's base-pair wave card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .cyberglass(
                    borderColor = if (todayWave.isSuppressed) Color(0xFF990000) else CyberGreen,
                    backgroundColor = if (todayWave.isSuppressed) Color(0xFF1A0000) else Color.Black.copy(alpha = 0.6f),
                )
                .padding(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false),
                ) {
                    Text(
                        text = "⚡",
                        color = if (todayWave.isSuppressed) Color.Red else Color(0xFFFFB300),
                        fontSize = 14.sp,
                    )
                    Column {
                        Text(
                            text = "TODAY'S BASE-PAIR WAVE",
                            color = CyberGreenDim,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (todayWave.isSuppressed) "DORMANT (CONGESTED DECAY)" else "ACTIVE: ${todayWave.pair} WAVE",
                            color = Color.White,
                            style = Typography.bodySmall,
                            fontFamily = FontFamily.Default,
                            fontWeight = FontWeight.Bold,
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
                            fontFamily = FontFamily.Monospace,
                        )
                        Text(
                            text = "1.12x & 1.62x BOOST",
                            color = CyberGreenDim,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
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
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
        }

        // Forecast Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Tomorrow
            Box(
                modifier = Modifier
                    .weight(1f)
                    .cyberglass(
                        borderColor = if (tomorrowWave.isSuppressed) Color(0xFF990000) else Color.DarkGray,
                        backgroundColor = if (tomorrowWave.isSuppressed) Color(0xFF1A0000) else Color.Black.copy(alpha = 0.45f),
                    )
                    .padding(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "TOMORROW BASE-PAIR",
                            color = CyberGreenDim,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (tomorrowWave.isSuppressed) "DORMANT" else "${tomorrowWave.pair} WAVE",
                            color = Color.White,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (!tomorrowWave.isSuppressed) {
                        Text(
                            text = "${tomorrowWave.primary}➔${tomorrowWave.secondary}",
                            color = CyberGreen,
                            style = Typography.labelSmall,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }

            // Day After Tomorrow
            Box(
                modifier = Modifier
                    .weight(1f)
                    .cyberglass(
                        borderColor = if (dayAfterWave.isSuppressed) Color(0xFF990000) else Color.DarkGray,
                        backgroundColor = if (dayAfterWave.isSuppressed) Color(0xFF1A0000) else Color.Black.copy(alpha = 0.45f),
                    )
                    .padding(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "DAY AFTER TOMORROW",
                            color = CyberGreenDim,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (dayAfterWave.isSuppressed) "DORMANT" else "${dayAfterWave.pair} WAVE",
                            color = Color.White,
                            style = Typography.labelSmall,
                            fontFamily = FontFamily.Default,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    if (!dayAfterWave.isSuppressed) {
                        Text(
                            text = "${dayAfterWave.primary}➔${dayAfterWave.secondary}",
                            color = CyberGreen,
                            style = Typography.labelSmall,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
            }
        }
    }
}
