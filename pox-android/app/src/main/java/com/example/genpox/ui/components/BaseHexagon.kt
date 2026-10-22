package com.example.genpox.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpox.data.GraphicsMath

@Composable
fun BaseHexagon(
    char: Char,
    isPlaceholder: Boolean,
    sizeDp: Dp,
    modifier: Modifier = Modifier,
) {
    val color = when (char) {
        'A' -> Color(0xFF38BDF8) // Cyan
        'G' -> Color(0xFF34D399) // Green
        'T' -> Color(0xFFFB7185) // Rose/Red
        'C' -> Color(0xFFFBBF24) // Yellow
        else -> Color.Gray
    }
    val hexColor = if (isPlaceholder) Color.DarkGray.copy(alpha = 0.4f) else color

    Box(
        modifier = modifier.size(sizeDp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val hexRadius = w / 2f
            val center = Offset(w / 2f, h / 2f)

            // Centralized vertex calculation in GraphicsMath
            val vertices = GraphicsMath.calculateHexagonVertices(center.x, center.y, hexRadius)

            val path = Path().apply {
                vertices.forEachIndexed { i, pt ->
                    if (i == 0) moveTo(pt.x, pt.y) else lineTo(pt.x, pt.y)
                }
                close()
            }

            if (!isPlaceholder) {
                drawPath(path, color = hexColor.copy(alpha = 0.15f), style = Fill)
                drawPath(path, color = hexColor, style = Stroke(width = 1.2.dp.toPx()))
            } else {
                drawPath(path, color = hexColor, style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)))
            }
        }
        if (char != '-' && char != '.') {
            Text(
                text = char.toString(),
                color = if (isPlaceholder) Color.Gray else hexColor,
                fontSize = if (sizeDp < 20.dp) {
                    7.sp
                } else if (sizeDp > 26.dp) {
                    11.sp
                } else {
                    10.sp
                },
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
