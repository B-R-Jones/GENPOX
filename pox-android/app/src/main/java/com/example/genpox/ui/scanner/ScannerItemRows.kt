package com.example.genpox.ui.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpox.data.CelestialWell
import com.example.genpox.theme.CyberGreenDim
import com.example.genpox.theme.CyberPanel
import com.example.genpox.theme.Typography
import com.example.genpox.ui.main.MainViewModel
import com.example.genpox.ui.main.PoxBaseFont

@Composable
fun FontItemRow(
    font: PoxBaseFont,
    userLat: Double,
    userLng: Double,
    viewModel: MainViewModel,
    onClick: () -> Unit = {},
) {
    val fontDist = viewModel.calculateDistanceInFeet(userLat, userLng, font.lat, font.lng)
    val latDiff = font.lat - userLat
    val lngDiff = font.lng - userLng
    val direction = remember(latDiff, lngDiff) {
        val angle = Math.toDegrees(Math.atan2(latDiff, lngDiff))
        val normalizedAngle = (angle + 360) % 360
        when {
            normalizedAngle >= 337.5 || normalizedAngle < 22.5 -> "E"
            normalizedAngle >= 22.5 && normalizedAngle < 67.5 -> "NE"
            normalizedAngle >= 67.5 && normalizedAngle < 112.5 -> "N"
            normalizedAngle >= 112.5 && normalizedAngle < 157.5 -> "NW"
            normalizedAngle >= 157.5 && normalizedAngle < 202.5 -> "W"
            normalizedAngle >= 202.5 && normalizedAngle < 247.5 -> "SW"
            normalizedAngle >= 247.5 && normalizedAngle < 292.5 -> "S"
            else -> "SE"
        }
    }

    val isNear = fontDist <= font.maxRadiusFeet
    val remaining = ((font.durationMs - (System.currentTimeMillis() - font.spawnTime)) / 1000L).coerceAtLeast(0L)
    val typeStr = if (font.isInverted) "VOID SINK" else "CODON INLET"
    val cardColor = if (font.isInverted) {
        Color(0xFF8B5CF6)
    } else {
        when (font.baseType) {
            'A' -> Color(0xFF38BDF8)
            'G' -> Color(0xFF34D399)
            'T' -> Color(0xFFFB7185)
            'C' -> Color(0xFFFBBF24)
            else -> Color.Cyan
        }
    }

    val activityText = if (isNear) {
        val rate = font.getGatherAmount(fontDist)
        if (font.isInverted) {
            "VOID ACTIVE: -$rate STOCKS/S (${remaining}S LEFT)"
        } else {
            "SIPHON ACTIVE: +$rate UNITS/SEC (${remaining}S LEFT)"
        }
    } else {
        "OUT OF RANGE (${remaining}S LEFT)"
    }
    val activityColor = if (isNear) {
        if (font.isInverted) Color(0xFFFBBF24) else Color(0xFF34D399)
    } else {
        Color.Red
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                viewModel.synthManager.playBeep(480f, 0.05f, "sine")
                onClick()
            }
            .border(1.dp, cardColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .background(CyberPanel)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(cardColor),
                )
                Text(
                    text = font.id.uppercase(),
                    style = Typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = "[$typeStr]",
                    style = Typography.labelSmall,
                    fontSize = 8.sp,
                    color = cardColor,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "DIST: ${fontDist.toInt()} FT",
                    style = Typography.bodySmall,
                    color = CyberGreenDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                )
                Text(
                    text = "DIR: $direction",
                    style = Typography.bodySmall,
                    color = CyberGreenDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                )
                Text(
                    text = "COVERAGE: ${font.maxRadiusFeet.toInt()} FT",
                    style = Typography.bodySmall,
                    color = CyberGreenDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                )
            }

            Text(
                text = activityText.uppercase(),
                color = activityColor,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }

        Box(
            modifier = Modifier
                .border(1.dp, cardColor.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 6.dp, vertical = 3.dp),
        ) {
            Text(
                text = if (font.isInverted) "VOID" else font.baseType.toString(),
                color = cardColor,
                style = Typography.bodySmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
fun WellItemRow(
    well: CelestialWell,
    userLat: Double,
    userLng: Double,
    viewModel: MainViewModel,
    onClick: () -> Unit = {},
) {
    val wellDist = viewModel.calculateDistanceInFeet(userLat, userLng, well.lat, well.lng)
    val latDiff = well.lat - userLat
    val lngDiff = well.lng - userLng
    val direction = remember(latDiff, lngDiff) {
        val angle = Math.toDegrees(Math.atan2(latDiff, lngDiff))
        val normalizedAngle = (angle + 360) % 360
        when {
            normalizedAngle >= 337.5 || normalizedAngle < 22.5 -> "E"
            normalizedAngle >= 22.5 && normalizedAngle < 67.5 -> "NE"
            normalizedAngle >= 67.5 && normalizedAngle < 112.5 -> "N"
            normalizedAngle >= 112.5 && normalizedAngle < 157.5 -> "NW"
            normalizedAngle >= 157.5 && normalizedAngle < 202.5 -> "W"
            normalizedAngle >= 202.5 && normalizedAngle < 247.5 -> "SW"
            normalizedAngle >= 247.5 && normalizedAngle < 292.5 -> "S"
            else -> "SE"
        }
    }

    val inRange = wellDist <= well.radiusFeet
    val wellColor = when (well.type) {
        "GREENHOUSE" -> Color(0xFFEF4444)
        "MAGNETOSPHERIC" -> Color(0xFFFBBF24)
        "GRAVITATIONAL" -> Color(0xFF8B5CF6)
        else -> Color.Cyan
    }

    val activityText = if (inRange) "ENVIRONMENTAL HAZARD TENSION ACTIVE" else "SAFE BOUNDARY"
    val activityColor = if (inRange) Color(0xFFEF4444) else Color(0xFF34D399)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                viewModel.synthManager.playBeep(480f, 0.05f, "sine")
                onClick()
            }
            .border(1.dp, wellColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .background(CyberPanel)
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(wellColor),
                )
                Text(
                    text = well.name.uppercase(),
                    style = Typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Text(
                    text = "[${well.type}]",
                    style = Typography.labelSmall,
                    fontSize = 8.sp,
                    color = wellColor,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = "DIST: ${wellDist.toInt()} FT",
                    style = Typography.bodySmall,
                    color = CyberGreenDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                )
                Text(
                    text = "DIR: $direction",
                    style = Typography.bodySmall,
                    color = CyberGreenDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                )
                Text(
                    text = "RADIUS: ${well.radiusFeet.toInt()} FT",
                    style = Typography.bodySmall,
                    color = CyberGreenDim,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                )
            }

            Text(
                text = activityText.uppercase(),
                color = activityColor,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }

        Box(
            modifier = Modifier
                .border(1.dp, wellColor.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = 6.dp, vertical = 3.dp),
        ) {
            Text(
                text = well.type.take(4).uppercase(),
                color = wellColor,
                style = Typography.bodySmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            )
        }
    }
}
