package com.example.genpox.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.genpox.ui.main.cyberglass

/**
 * Standard reusable holographic navigation button.
 * Size: 46.dp, styled with cyberglass modifier.
 */
@Composable
fun PoxHoloButton(
    modifier: Modifier = Modifier,
    borderColor: Color,
    glowColor: Color = Color.Transparent,
    backgroundColor: Color = Color(0xFF0F172A).copy(alpha = 0.85f),
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(46.dp)
            .cyberglass(
                borderColor = borderColor,
                glowColor = glowColor,
                backgroundColor = backgroundColor
            )
            .clickable(enabled = enabled) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
