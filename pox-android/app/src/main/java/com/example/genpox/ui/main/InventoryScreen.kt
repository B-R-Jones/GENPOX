package com.example.genpox.ui.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.genpox.theme.*
import com.example.genpox.ui.components.*
import com.google.maps.android.compose.*
import java.text.SimpleDateFormat
import java.util.*

// ==========================================
// 6. INVENTORY VIEW
// ==========================================
@Composable
fun InventoryView(viewModel: MainViewModel) {
    val geneSequences by viewModel.geneSequences.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = "TRANSCIEVER GENE INVENTORY", style = Typography.titleMedium, color = CyberGreen)

        if (geneSequences.isEmpty()) {
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = "NO GENE GENES RESOLVED.\nHARVEST TARGETS VIA MAP SCANNER.",
                    color = CyberGreenDim,
                    style = Typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(geneSequences) { seq ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                            .background(CyberPanel)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(text = "BLOCK: ${seq.sequence}", style = Typography.bodyMedium, fontWeight = FontWeight.Bold, color = CyberGreen)
                            Text(
                                text = "ACQUIRED: " + SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(seq.discoveredAt)),
                                style = Typography.bodySmall,
                                color = CyberGreenDim,
                            )
                        }
                        Text(
                            text = "QTY: ${seq.count}",
                            style = Typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = CyberGreen,
                        )
                    }
                }
            }
        }
    }
}
