package com.example.genpox.ui.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.genpox.theme.*
import com.example.genpox.ui.components.*
import com.google.maps.android.compose.*
import java.util.*

// ==========================================
// 5. FORECAST VIEW
// ==========================================
@Composable
fun ForecastView(viewModel: MainViewModel) {
    val dailyBounties by viewModel.dailyBounties.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = "DAILY GENE FORECASTS", style = Typography.titleMedium, color = CyberGreen)
        Text(
            text = "SCAN TARGET TELEMETRIES HARVEST PATHWAYS:",
            style = Typography.labelSmall,
            color = CyberGreenDim,
        )

        // Draw forecast grids
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            items(dailyBounties.take(28)) { bounty ->
                val first8 = bounty.take(8)
                Box(
                    modifier = Modifier
                        .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                        .background(CyberPanel)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = first8, style = Typography.bodySmall, fontSize = 9.sp, color = CyberGreen)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = "CO coherence", style = Typography.bodySmall, fontSize = 6.sp, color = CyberGreenDim)
                    }
                }
            }
        }
    }
}
