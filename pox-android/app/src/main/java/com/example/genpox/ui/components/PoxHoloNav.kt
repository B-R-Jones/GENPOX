package com.example.genpox.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.genpox.theme.*
import com.example.genpox.ui.main.MainViewModel
import kotlinx.coroutines.delay

/**
 * Data class representing a sub-tab in the GENPOX UI deck.
 */
data class PoxSubTab(
    val id: String,
    val tag: String,
    val icon: @Composable (Color) -> Unit
)

/**
 * Generic configuration for buttons in the unified holo-nav deck.
 */
data class HoloNavButton(
    val id: String = "",
    val borderColor: Color = CyberGreen,
    val glowColor: Color = Color.Transparent,
    val backgroundColor: Color = Color(0xFF0F172A).copy(alpha = 0.85f),
    val enabled: Boolean = true,
    val onClick: () -> Unit,
    val content: @Composable (Color) -> Unit
)

/**
 * Unified, highly flexible, gesture-driven holo-navigation deck container.
 * Merges single-row sub-tabs and layered multi-row action decks.
 * Supports:
 * - Horizontal swipe: stack/unstack buttons.
 * - Vertical swipe: cycle rows (when multiple rows exist).
 * - Row fading: inactive rows fade to alpha = 0 during stacking.
 * - Bouncy spring animations.
 */
@Composable
fun PoxUnifiedHoloNav(
    modifier: Modifier = Modifier,
    rows: List<List<HoloNavButton>>,
    activeButtonId: String = "",
    allowStacking: Boolean = true,
    viewModel: MainViewModel
) {
    if (rows.isEmpty()) return

    var isStacked by remember { mutableStateOf(false) }
    var activeRowIndex by remember { mutableStateOf(0) }

    // Smoothly animate the stack fraction with a bouncy feel
    val stackedProgress by animateFloatAsState(
        targetValue = if (isStacked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "stackedProgress"
    )

    // Infinite bouncing animation while offscreen to indicate swipeability
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")
    val stackedBounceOffset by if (isStacked) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -8f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "stackedBounce"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    // Periodic attraction bounce for the inactive row(s) when unstacked
    val verticalBounceAnimatable = remember { Animatable(0f) }
    LaunchedEffect(rows.size, isStacked) {
        if (rows.size > 1 && !isStacked) {
            while (true) {
                delay(6000L)
                verticalBounceAnimatable.animateTo(
                    targetValue = -8f,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                )
                verticalBounceAnimatable.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        } else {
            verticalBounceAnimatable.snapTo(0f)
        }
    }
    val verticalBounceOffset = verticalBounceAnimatable.value

    val maxRowSize = rows.maxOfOrNull { it.size } ?: 0
    val containerWidth = (maxRowSize * 54 - 8).dp

    Box(
        modifier = modifier
            .zIndex(10f)
            .size(width = containerWidth, height = 46.dp)
            .pointerInput(rows.size) {
                var totalDragX = 0f
                var totalDragY = 0f
                detectDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDragEnd = {
                        val absX = kotlin.math.abs(totalDragX)
                        val absY = kotlin.math.abs(totalDragY)
                        if (allowStacking && absX > absY && absX > 50f) {
                            isStacked = totalDragX > 0f
                        } else if (rows.size > 1 && absY > absX && absY > 50f && !isStacked) {
                            activeRowIndex = (activeRowIndex + 1) % rows.size
                            viewModel.synthManager.playCombinatorTick()
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y
                    }
                )
            }
    ) {
        rows.forEachIndexed { rowIndex, rowButtons ->
            val isActive = rowIndex == activeRowIndex
            val isActiveProgress by animateFloatAsState(
                targetValue = if (isActive) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "isActiveProgress_$rowIndex"
            )

            val scale = 0.85f + 0.15f * isActiveProgress
            val baseOffsetY = -24f * (1f - isActiveProgress)
            val currentBounceY = verticalBounceOffset * (1f - isActiveProgress)
            val offsetY = (baseOffsetY + currentBounceY).dp

            val alpha = if (isActive) 1f else 0.6f * (1f - stackedProgress)
            val zIndex = if (isActive) 1f else 0f

            val M = rowButtons.size
            rowButtons.forEachIndexed { buttonIndex, button ->
                val normalX = buttonIndex * 54f
                val stackedX = (M - 1) * 54f - (M - 1 - buttonIndex) * 3f
                val offscreenShift = 64f

                val targetX = normalX + (stackedX - normalX) * stackedProgress
                val currentShift = offscreenShift * stackedProgress
                val finalX = targetX + currentShift + (stackedBounceOffset * stackedProgress)

                val isButtonActive = activeButtonId.isNotEmpty() && activeButtonId == button.id
                val buttonAlphaFactor = if (isStacked && buttonIndex < M - 1) 0.5f else 1f
                val finalAlpha = alpha * buttonAlphaFactor

                PoxHoloButton(
                    borderColor = button.borderColor,
                    glowColor = button.glowColor,
                    backgroundColor = button.backgroundColor,
                    modifier = Modifier
                        .offset(x = finalX.dp, y = offsetY)
                        .graphicsLayer {
                            this.alpha = finalAlpha
                            this.scaleX = scale
                            this.scaleY = scale
                        }
                        .zIndex(zIndex),
                    enabled = if (isStacked) (buttonIndex == M - 1) else button.enabled,
                    onClick = {
                        if (isStacked) {
                            viewModel.synthManager.playCombinatorTick()
                            isStacked = false
                        } else {
                            button.onClick()
                        }
                    }
                ) {
                    button.content(if (isButtonActive) Color.Black else button.borderColor)
                }
            }
        }
    }
}

/**
 * Standard reusable sub-tab navigation deck (Holo-Nav).
 * Retains the original signature as a zero-overhead delegate mapping PoxSubTab into PoxUnifiedHoloNav.
 */
@Composable
fun PoxHoloNav(
    modifier: Modifier = Modifier,
    subTabs: List<PoxSubTab>,
    activeSubTab: String,
    onSubTabClick: (String, String) -> Unit,
    viewModel: MainViewModel,
    activeColor: Color = CyberGreen,
    inactiveColor: Color = CyberGreenDim
) {
    val buttons = subTabs.map { tab ->
        val isActive = activeSubTab == tab.id
        HoloNavButton(
            id = tab.id,
            borderColor = if (isActive) activeColor else inactiveColor.copy(alpha = 0.4f),
            glowColor = if (isActive) activeColor.copy(alpha = 0.15f) else Color.Transparent,
            backgroundColor = if (isActive) activeColor else Color(0xFF0F172A).copy(alpha = 0.85f),
            enabled = true,
            onClick = {
                if (activeSubTab != tab.id) {
                    viewModel.synthManager.playCombinatorTick()
                    onSubTabClick(tab.id, tab.tag)
                }
            },
            content = { iconColor ->
                tab.icon(iconColor)
            }
        )
    }

    PoxUnifiedHoloNav(
        modifier = modifier,
        rows = listOf(buttons),
        activeButtonId = activeSubTab,
        allowStacking = true,
        viewModel = viewModel
    )
}
