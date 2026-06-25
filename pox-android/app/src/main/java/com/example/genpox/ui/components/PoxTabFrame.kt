package com.example.genpox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.genpox.theme.*
import com.example.genpox.ui.main.MainViewModel
import com.example.genpox.ui.main.cyberglass

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
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val scrollModifier = if (isScrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier

    Box(
        modifier = modifier
            .fillMaxSize()
            .cyberglass(borderColor = borderColor, backgroundColor = backgroundColor)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(scrollModifier)
                .padding(bottom = if (subTabs.isNotEmpty()) 56.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Top header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                   text = flavorTitle.uppercase(),
                   color = when (borderColor) {
                       CyberBorder -> CyberGreenDim
                       CyberTheme.purpleBorder -> CyberTheme.purpleDim
                       CyberTheme.cyanBorder -> CyberTheme.cyanDim
                       CyberTheme.redBorder -> CyberTheme.redDim
                       else -> borderColor.copy(alpha = 0.6f)
                   },
                   fontSize = 9.sp,
                   fontWeight = FontWeight.Bold,
                   fontFamily = FontFamily.Default
                )
                Text(
                    text = statusText.uppercase(),
                    color = statusColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Default,
                    modifier = if (onStatusClick != null) Modifier.clickable(onClick = onStatusClick) else Modifier
                )
            }

            // Middle main title row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .requiredHeight(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = headerTitle.uppercase(),
                    color = Color.White,
                    style = Typography.bodyMedium,
                    fontFamily = FontFamily.Default,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            // Bottom flavor/description text
            Text(
                text = descriptionText,
                style = MaterialTheme.typography.bodySmall,
                color = when (borderColor) {
                    CyberBorder -> CyberGreen.copy(alpha = 0.8f)
                    CyberTheme.purpleBorder -> CyberTheme.purple.copy(alpha = 0.8f)
                    CyberTheme.cyanBorder -> CyberTheme.cyan.copy(alpha = 0.8f)
                    CyberTheme.redBorder -> CyberTheme.red.copy(alpha = 0.8f)
                    else -> borderColor.copy(alpha = 0.8f)
                },
                fontSize = 10.sp,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(bottom = if (content != null) 4.dp else 0.dp)
            )

            // Optional slot for inner elements
            if (content != null) {
                content()
            }
        }

        if (subTabs.isNotEmpty() && onSubTabClick != null && viewModel != null) {
            val activeColor = if (borderColor == CyberBorder) CyberGreen else borderColor
            val inactiveColor = if (borderColor == CyberBorder) CyberGreenDim else borderColor.copy(alpha = 0.6f)
            PoxHoloNav(
                modifier = Modifier.align(Alignment.BottomEnd),
                subTabs = subTabs,
                activeSubTab = activeSubTab,
                onSubTabClick = onSubTabClick,
                viewModel = viewModel,
                activeColor = activeColor,
                inactiveColor = inactiveColor
            )
        }
    }
}
