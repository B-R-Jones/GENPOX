package com.example.genpox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.genpox.theme.CyberTheme

/**
 * Standardized cyberglass sci-fi Slider component.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoxSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    activeColor: Color = CyberTheme.green,
    inactiveColor: Color = Color.DarkGray
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.height(24.dp),
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        thumb = {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(Color.Black, shape = CutCornerShape(3.dp))
                    .border(1.dp, activeColor, shape = CutCornerShape(3.dp))
                    .padding(2.dp)
                    .background(activeColor, shape = CutCornerShape(1.dp))
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                enabled = enabled,
                colors = SliderDefaults.colors(
                    activeTrackColor = activeColor,
                    inactiveTrackColor = inactiveColor.copy(alpha = 0.5f)
                )
            )
        }
    )
}
