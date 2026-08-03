package com.example.genpox.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.genpox.theme.Typography

@Composable
fun SplicerTerminalLogView(
    viewModel: MainViewModel,
    forcedConstructionLogs: List<String>,
    isForcedLoopActive: Boolean,
    activeBorder: Color
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {

            val listState = rememberLazyListState()
            LaunchedEffect(forcedConstructionLogs.size) {
                if (forcedConstructionLogs.isNotEmpty()) {
                    listState.animateScrollToItem(forcedConstructionLogs.size - 1)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF050505))
                    .border(1.dp, activeBorder, RoundedCornerShape(4.dp))
                    .padding(6.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(forcedConstructionLogs) { log ->
                    val isAlert = log.contains("FAILED") || log.contains("WARNING") || log.contains("Failed") || log.contains("Sacrificed")
                    Text(
                        text = log,
                        color = if (isAlert) Color(0xFFFCA5A5) else Color(0xFF34D399),
                        style = Typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isForcedLoopActive) {
            Button(
                onClick = {
                    viewModel.synthManager.playReject()
                    viewModel.setIsForcedLoopActive(false)
                },
                modifier = Modifier.fillMaxWidth().height(34.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Red
                ),
                border = BorderStroke(1.dp, Color.Red),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "✕ EXIT AUTO-SYNTHESIS LOOP CASCADE",
                    color = Color.Red,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Default
                )
            }
        }
    }
}
