package com.example.genpox.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.genpox.theme.*
import com.example.genpox.ui.main.MainViewModel
import com.example.genpox.ui.main.cyberglass

/**
 * Data class representing a sub-tab in the GENPOX UI deck.
 */
data class PoxSubTab(
    val id: String,
    val tag: String,
    val icon: @Composable (Color) -> Unit
)

/**
 * Standard reusable sub-tab navigation deck (Holo-Nav).
 * Styled with holographic square buttons, active glow, and audio feedback.
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
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        subTabs.forEach { tab ->
            val isActive = activeSubTab == tab.id
            val borderColor = if (isActive) activeColor else inactiveColor.copy(alpha = 0.4f)
            val glowColor = if (isActive) activeColor.copy(alpha = 0.15f) else Color.Transparent
            val backColor = if (isActive) activeColor else Color(0xFF0F172A).copy(alpha = 0.85f)
            val iconColor = if (isActive) Color.Black else activeColor

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .cyberglass(
                        borderColor = borderColor,
                        glowColor = glowColor,
                        backgroundColor = backColor
                    )
                    .clickable {
                        if (activeSubTab != tab.id) {
                            viewModel.synthManager.playCombinatorTick()
                            onSubTabClick(tab.id, tab.tag)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                tab.icon(iconColor)
            }
        }
    }
}
