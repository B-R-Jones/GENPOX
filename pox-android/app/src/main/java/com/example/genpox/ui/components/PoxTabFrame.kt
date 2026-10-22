package com.example.genpox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.genpox.theme.*
import com.example.genpox.ui.main.MainViewModel
import com.example.genpox.ui.main.cyberglass
import com.example.genpox.ui.main.getActiveReagentColor

/**
 * Standard reusable frame layout for GENPOX UI tabs.
 * Standardizes styling, typography, corner wireframes, and headers.
 */
@Composable
fun PoxTabFrame(
    modifier: Modifier = Modifier,
    flavorTitle: String,
    statusText: String = "ONLINE",
    statusColor: Color = CyberGreen,
    headerTitle: String,
    descriptionText: String,
    borderColor: Color = CyberBorder,
    backgroundColor: Color = CyberPanel,
    onStatusClick: (() -> Unit)? = null,
    isScrollable: Boolean = true,
    subTabs: List<PoxSubTab> = emptyList(),
    activeSubTab: String = "",
    onSubTabClick: ((String, String) -> Unit)? = null,
    viewModel: MainViewModel? = null,
    drawSubTabs: Boolean = true,
    drawFrame: Boolean = true,
    showHeader: Boolean = true,
    useDynamicReagentColor: Boolean = false,
    headerBadges: (@Composable () -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val selectedReagentState = if (useDynamicReagentColor) viewModel?.selectedReagent?.collectAsStateWithLifecycle() else null
    val selectedReagent = selectedReagentState?.value
    val dynamicColor = if (useDynamicReagentColor && viewModel != null) getActiveReagentColor(selectedReagent) else null
    val baseBorderColor = dynamicColor ?: if (borderColor == CyberBorder) CyberGreen else borderColor
    val resolvedBorder = baseBorderColor.copy(alpha = 0.35f)

    val scrollModifier = if (isScrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (drawFrame) {
                    Modifier.cyberglass(borderColor = resolvedBorder, backgroundColor = backgroundColor)
                        .padding(12.dp)
                } else {
                    Modifier
                },
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(scrollModifier)
                .padding(bottom = if (subTabs.isNotEmpty()) 56.dp else 0.dp)
                .then(
                    if (!drawFrame) {
                        Modifier.padding(12.dp)
                    } else {
                        Modifier
                    },
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            if (showHeader) {
                // Top header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = flavorTitle.uppercase(),
                        color = when (resolvedBorder) {
                            CyberBorder -> CyberGreenDim
                            CyberTheme.purpleBorder -> CyberTheme.purpleDim
                            CyberTheme.cyanBorder -> CyberTheme.cyanDim
                            CyberTheme.redBorder -> CyberTheme.redDim
                            else -> baseBorderColor.copy(alpha = 0.6f)
                        },
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default,
                    )
                    if (statusText == "CLOSE") {
                        Box(
                            modifier = Modifier
                                .background(CyberRed, RoundedCornerShape(4.dp))
                                .clickable { onStatusClick?.invoke() }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "CLOSE",
                                color = Color.Black,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    } else {
                        Text(
                            text = statusText.uppercase(),
                            color = statusColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Default,
                            modifier = if (onStatusClick != null) Modifier.clickable(onClick = onStatusClick) else Modifier,
                        )
                    }
                }

                // Middle main title row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .requiredHeight(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = headerTitle.uppercase(),
                        color = Color.White,
                        style = Typography.bodyMedium,
                        fontFamily = FontFamily.Default,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                }

                // Bottom flavor/description text
                Text(
                    text = descriptionText,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (resolvedBorder) {
                        CyberBorder -> CyberGreen.copy(alpha = 0.8f)
                        CyberTheme.purpleBorder -> CyberTheme.purple.copy(alpha = 0.8f)
                        CyberTheme.cyanBorder -> CyberTheme.cyan.copy(alpha = 0.8f)
                        CyberTheme.redBorder -> CyberTheme.red.copy(alpha = 0.8f)
                        else -> baseBorderColor.copy(alpha = 0.8f)
                    },
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(bottom = if (content != null && headerBadges == null) 4.dp else 0.dp),
                )

                if (headerBadges != null) {
                    Box(modifier = Modifier.padding(bottom = if (content != null) 4.dp else 0.dp)) {
                        headerBadges()
                    }
                }
            }

            // Optional slot for inner elements
            if (content != null) {
                content()
            }
        }

        if (drawSubTabs && subTabs.isNotEmpty() && onSubTabClick != null && viewModel != null) {
            val activeColor = baseBorderColor
            val inactiveColor = baseBorderColor.copy(alpha = 0.6f)
            PoxHoloNav(
                modifier = Modifier.align(Alignment.BottomEnd),
                subTabs = subTabs,
                activeSubTab = activeSubTab,
                onSubTabClick = onSubTabClick,
                viewModel = viewModel,
                activeColor = activeColor,
                inactiveColor = inactiveColor,
            )
        }
    }
}
