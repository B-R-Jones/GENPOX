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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.genpox.data.Creature
import com.example.genpox.data.GeneSequence
import com.example.genpox.data.WaveMath
import com.example.genpox.theme.*
import com.example.genpox.ui.components.*
import com.google.maps.android.compose.*
import java.util.*

@Composable
fun VaultView(viewModel: MainViewModel) {
    val creatures by viewModel.creatures.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeCreature by viewModel.activeCreature.collectAsStateWithLifecycle()
    val targetSequence by viewModel.targetSequence.collectAsStateWithLifecycle()
    val geneSequences by viewModel.geneSequences.collectAsStateWithLifecycle(initialValue = emptyList())
    val defenderId by viewModel.defenderCreatureId.collectAsStateWithLifecycle()
    val openedFrom by viewModel.creatureCardOpenedFrom.collectAsStateWithLifecycle()
    val harvestingCreatureIds by viewModel.harvestingCreatureIds.collectAsStateWithLifecycle()
    val devForceAnomaly by viewModel.devForceAnomaly.collectAsStateWithLifecycle()
    val activeTags = remember(activeCreature, targetSequence, defenderId, harvestingCreatureIds) {
        activeCreature?.let { getCreatureTags(it, targetSequence, defenderId, harvestingCreatureIds) } ?: emptyList()
    }

    // Local UI states
    var applyLibFilters by remember { mutableStateOf(true) }
    var isFilterPanelExpanded by remember { mutableStateOf(false) }
    var viewingArchiveSearch by remember { mutableStateOf(false) }
    var libSortBy by remember { mutableStateOf("name-asc") }
    var libFilterFaction by remember { mutableStateOf("ALL") }
    var libFilterType by remember { mutableStateOf("ALL") }
    var libFilterTag by remember { mutableStateOf("ALL") }

    val vaultTab by viewModel.vaultSubTab.collectAsStateWithLifecycle()
    var geneToDeconstruct by remember { mutableStateOf<GeneSequence?>(null) }
    var stepSearchSelectedGene by remember { mutableStateOf<String?>(null) }

    val subTabs = remember {
        listOf(
            PoxSubTab("creatures", "CREATURES", icon = { iconColor ->
                WireframeBugIcon(color = iconColor, modifier = Modifier.size(24.dp))
            }),
            PoxSubTab("search", "SEARCH", icon = { iconColor ->
                WireframeStepSearchIcon(color = iconColor, modifier = Modifier.size(24.dp))
            }),
        )
    }

    // Unique types extractor
    val uniqueTypes = remember(creatures) {
        creatures.map { it.type }.distinct().filter { it.isNotEmpty() }
    }

    // Filter and Sort logic
    val filteredSortedCreatures = remember(
        creatures, applyLibFilters, libSortBy, libFilterFaction, libFilterType, libFilterTag, targetSequence, defenderId, harvestingCreatureIds,
    ) {
        var result = creatures
        if (applyLibFilters) {
            if (libFilterFaction != "ALL") {
                result = result.filter { it.faction.equals(libFilterFaction, ignoreCase = true) }
            }
            if (libFilterType != "ALL") {
                result = result.filter { it.type.equals(libFilterType, ignoreCase = true) }
            }
            if (libFilterTag != "ALL") {
                result = result.filter { getCreatureTags(it, targetSequence, defenderId, harvestingCreatureIds).contains(libFilterTag) }
            }
        }

        when (libSortBy) {
            "name-asc" -> result.sortedBy { it.name }
            "name-desc" -> result.sortedByDescending { it.name }
            "type-asc" -> result.sortedBy { it.type }
            "type-desc" -> result.sortedByDescending { it.type }
            "faction-asc" -> result.sortedBy { it.faction }
            "vitality-desc" -> result.sortedByDescending { it.vitality }
            "attack-desc" -> result.sortedByDescending { it.attack }
            "defense-desc" -> result.sortedByDescending { it.defense }
            "speed-desc" -> result.sortedByDescending { it.speed }
            "tags-desc" -> result.sortedByDescending { getCreatureTags(it, targetSequence, defenderId, harvestingCreatureIds).size }
            else -> result
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val isCloseActive = activeCreature != null || viewingArchiveSearch || openedFrom == "message_creature" || openedFrom == "message_gene"
        PoxTabFrame(
            flavorTitle = "G.E.N. P.O.X. SEABED VAULT V0.4",
            statusText = if (isCloseActive) "CLOSE" else "SYSTEMS ON",
            statusColor = if (isCloseActive) CyberRed else CyberGreen,
            onStatusClick = if (isCloseActive) {
                {
                    viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                    if (activeCreature != null) {
                        val prevOpenedFrom = openedFrom
                        viewModel.setActiveCreature(null)
                        if (prevOpenedFrom == "splicer") {
                            viewModel.selectTab("splicer")
                        } else if (prevOpenedFrom == "scanner") {
                            viewModel.selectTab("scanner")
                        } else if (prevOpenedFrom == "message_creature" || prevOpenedFrom == "message_gene") {
                            viewModel.selectTab("network")
                        }
                    } else if (viewingArchiveSearch) {
                        viewingArchiveSearch = false
                    } else if (openedFrom == "message_creature" || openedFrom == "message_gene") {
                        viewModel.setActiveCreature(null, "vault")
                        viewModel.selectTab("network")
                    }
                }
            } else {
                null
            },
            headerTitle = if (activeCreature != null) {
                "${activeCreature!!.name.uppercase()} [${activeCreature!!.id}]"
            } else if (viewingArchiveSearch) {
                "ARCHIVE SEARCH RESULTS"
            } else if (vaultTab == "creatures") {
                "NODE P.O.X. SEQUENCES REGISTRY"
            } else {
                "MOLECULAR STEP-SEARCH DIRECTORY"
            },
            descriptionText = if (activeCreature != null) {
                "${activeCreature!!.type.uppercase()} : Detailed biological telemetry and genome layout."
            } else if (viewingArchiveSearch) {
                "System target matches for active search parameters."
            } else if (vaultTab == "creatures") {
                "Registry of harvested species and custom step-search DNA databanks."
            } else {
                "Step search functions to navigate and analyze stored DNA sequences."
            },
            borderColor = CyberBorder,
            backgroundColor = CyberPanel,
            isScrollable = false,
            subTabs = subTabs,
            activeSubTab = vaultTab,
            onSubTabClick = { id, tag ->
                if (activeCreature != null) {
                    viewModel.setActiveCreature(null)
                }
                if (viewingArchiveSearch) {
                    viewingArchiveSearch = false
                }
                viewModel.setVaultSubTab(id)
            },
            viewModel = viewModel,
            headerBadges = if (activeCreature != null) {
                {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val factionColor = when (activeCreature!!.faction) {
                            "Infection" -> Color(0xFFEF4444)
                            "Mech" -> Color(0xFF60A5FA)
                            "Parasite" -> Color(0xFFA855F7)
                            else -> Color(0xFF00FF41)
                        }
                        Box(
                            modifier = Modifier
                                .background(factionColor, RoundedCornerShape(2.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = activeCreature!!.faction.uppercase(),
                                color = Color.Black,
                                style = Typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                fontSize = 8.5.sp,
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            activeTags.forEach { tag ->
                                TagBadge(tag = tag)
                            }
                        }
                    }
                }
            } else {
                null
            },
        ) {
            if (activeCreature != null) {
                CreatureDetailCard(c = activeCreature!!, viewModel = viewModel)
            } else if (viewingArchiveSearch) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Summary metrics header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "SYSTEM TARGET MATCHES: ",
                                color = CyberGreen,
                                fontSize = 9.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "${filteredSortedCreatures.size}",
                                color = CyberGreen,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = " SPECIMENS",
                                color = CyberGreen,
                                fontSize = 9.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = "STATUS: GEN-VAULT OPEN",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                        )
                    }

                    // List Area
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(filteredSortedCreatures) { item ->
                            val currentTags = remember(item, targetSequence, defenderId, harvestingCreatureIds) {
                                getCreatureTags(item, targetSequence, defenderId, harvestingCreatureIds)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                                    .background(CyberPanel)
                                    .clickable {
                                        viewModel.synthManager.playBeep(700f, 0.05f, "sine")
                                        viewModel.setActiveCreature(item, openedFrom ?: "vault")
                                    }
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                                        .background(Color.Black.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    com.example.genpox.ui.components.CreatureWireframeView(
                                        dna = item.sequence,
                                        faction = item.faction,
                                        modifier = Modifier.fillMaxSize().padding(4.dp),
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.name.uppercase(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        currentTags.forEach { tag ->
                                            TagBadge(tag = tag)
                                        }
                                    }
                                }

                                Text(
                                    text = "DECRYPT >",
                                    color = CyberGreen,
                                    fontSize = 9.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    ) {
                        when (vaultTab) {
                            "creatures" -> {
                                if (creatures.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "GEN-VAULT DATABANKS OFFLINE.\nSEED GENETIC HOSTS IN COMBINATOR.",
                                            color = CyberGreenDim,
                                            style = Typography.bodyMedium,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.Start),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            val activeFiltersCount = (if (libFilterFaction != "ALL") 1 else 0) +
                                                (if (libFilterType != "ALL") 1 else 0) +
                                                (if (libFilterTag != "ALL") 1 else 0)
                                            val exploreText = if (activeFiltersCount > 0) {
                                                "EXPLORE FILTERS ($activeFiltersCount)"
                                            } else {
                                                "EXPLORE FILTERS"
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                                                    .background(Color.Black)
                                                    .clickable {
                                                        viewModel.synthManager.playBeep(520f, 0.05f, "sine")
                                                        isFilterPanelExpanded = !isFilterPanelExpanded
                                                    }
                                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                            ) {
                                                Text(
                                                    text = if (isFilterPanelExpanded) "COLLAPSE FILTERS" else exploreText,
                                                    color = CyberGreen,
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }

                                            Spacer(modifier = Modifier.weight(1f))

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            ) {
                                                Text(
                                                    text = "ACTIVE SELECTIONS: ",
                                                    color = CyberGreen,
                                                    fontSize = 8.sp,
                                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                                Text(
                                                    text = "${filteredSortedCreatures.size} / ${creatures.size}",
                                                    color = CyberGreen,
                                                    fontSize = 8.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                        }

                                        // Collapsible Filter Console
                                        if (isFilterPanelExpanded) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                                                    .background(Color.Black.copy(alpha = 0.6f))
                                                    .padding(8.dp),
                                            ) {
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    ) {
                                                        Box(modifier = Modifier.weight(1f)) {
                                                            PoxDropdown(
                                                                label = "Sort Sequence:",
                                                                selectedOption = libSortBy,
                                                                options = listOf(
                                                                    "name-asc" to "Name (A -> Z)",
                                                                    "name-desc" to "Name (Z -> A)",
                                                                    "type-asc" to "Type (A -> Z)",
                                                                    "type-desc" to "Type (Z -> A)",
                                                                    "faction-asc" to "Faction (A -> Z)",
                                                                    "vitality-desc" to "Vitality (High -> Low)",
                                                                    "attack-desc" to "Attack (High -> Low)",
                                                                    "defense-desc" to "Defense (High -> Low)",
                                                                    "speed-desc" to "Speed (High -> Low)",
                                                                    "tags-desc" to "Tag Density",
                                                                ),
                                                                onOptionSelected = {
                                                                    viewModel.synthManager.playBeep(480f, 0.02f, "sine")
                                                                    libSortBy = it
                                                                },
                                                            )
                                                        }
                                                        Box(modifier = Modifier.weight(1f)) {
                                                            PoxDropdown(
                                                                label = "Faction Classifier:",
                                                                selectedOption = libFilterFaction,
                                                                options = listOf(
                                                                    "ALL" to "All Factions",
                                                                    "Infection" to "Infection",
                                                                    "Mech" to "Mech",
                                                                    "Parasite" to "Parasite",
                                                                    "Containment" to "Containment",
                                                                ),
                                                                onOptionSelected = {
                                                                    viewModel.synthManager.playBeep(480f, 0.02f, "sine")
                                                                    libFilterFaction = it
                                                                },
                                                            )
                                                        }
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    ) {
                                                        Box(modifier = Modifier.weight(1f)) {
                                                            val typeOptions = remember(uniqueTypes) {
                                                                listOf("ALL" to "All Creature Types") + uniqueTypes.map { it to it }
                                                            }
                                                            PoxDropdown(
                                                                label = "Creature Type:",
                                                                selectedOption = libFilterType,
                                                                options = typeOptions,
                                                                onOptionSelected = {
                                                                    viewModel.synthManager.playBeep(480f, 0.02f, "sine")
                                                                    libFilterType = it
                                                                },
                                                            )
                                                        }
                                                        Box(modifier = Modifier.weight(1f)) {
                                                            val tagOptions = listOf(
                                                                "ALL" to "All Tags",
                                                                "FAVORITE" to "Favorite",
                                                                "DEFENDER" to "Defender",
                                                                "AUTO-HACKER" to "Auto-Hacker",
                                                                "FULL COHERENCE" to "Full Coherence",
                                                                "NATURAL" to "Natural",
                                                                "FORCED" to "Forced",
                                                                "ALPHA GENE" to "Alpha Gene",
                                                                "MODIFIED" to "Modified",
                                                                "ORIGINAL" to "Original",
                                                                "TRANSFER-ORIGIN" to "Transfer Origin",
                                                            )
                                                            PoxDropdown(
                                                                label = "Creature Tags:",
                                                                selectedOption = libFilterTag,
                                                                options = tagOptions,
                                                                onOptionSelected = {
                                                                    viewModel.synthManager.playBeep(480f, 0.02f, "sine")
                                                                    libFilterTag = it
                                                                },
                                                            )
                                                        }
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.End,
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(4.dp))
                                                                .background(Color.Black)
                                                                .clickable {
                                                                    viewModel.synthManager.playBeep(420f, 0.1f, "sine")
                                                                    libSortBy = "name-asc"
                                                                    libFilterFaction = "ALL"
                                                                    libFilterType = "ALL"
                                                                    libFilterTag = "ALL"
                                                                }
                                                                .padding(horizontal = 8.dp, vertical = 4.dp),
                                                        ) {
                                                            Text(
                                                                text = "✕ CLEAR FILTERS",
                                                                color = Color(0xFFEF4444),
                                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Base Panel Area
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f)
                                                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                                                .background(Color.Black.copy(alpha = 0.4f))
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                                ) {
                                                    if (applyLibFilters) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.Center,
                                                        ) {
                                                            Text(
                                                                text = "SEQUENCES FOUND: ",
                                                                color = CyberGreen,
                                                                fontSize = 16.sp,
                                                                fontWeight = FontWeight.Black,
                                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                                                textAlign = TextAlign.Center,
                                                            )
                                                            Text(
                                                                text = "${filteredSortedCreatures.size}",
                                                                color = CyberGreen,
                                                                fontSize = 16.sp,
                                                                fontWeight = FontWeight.Black,
                                                                fontFamily = FontFamily.Monospace,
                                                                textAlign = TextAlign.Center,
                                                            )
                                                        }
                                                        Text(
                                                            text = "Matching sequence(s) loaded; click button below to view",
                                                            color = CyberGreenDim,
                                                            fontSize = 10.sp,
                                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                                            textAlign = TextAlign.Center,
                                                        )
                                                    } else {
                                                        Text(
                                                            text = "NO FILTERS APPLIED",
                                                            color = Color.Red,
                                                            fontSize = 16.sp,
                                                            fontWeight = FontWeight.Black,
                                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                                            textAlign = TextAlign.Center,
                                                        )
                                                        Text(
                                                            text = "Enable filters or reconfigure search parameters using the filter options above.",
                                                            color = CyberGreenDim,
                                                            fontSize = 10.sp,
                                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                                            textAlign = TextAlign.Center,
                                                        )
                                                    }
                                                }

                                                Button(
                                                    onClick = {
                                                        viewModel.synthManager.playBeep(800f, 0.08f, "sine")
                                                        viewingArchiveSearch = true
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth(0.8f)
                                                        .height(44.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = CyberGreen,
                                                        contentColor = Color.Black,
                                                    ),
                                                    shape = RoundedCornerShape(4.dp),
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    ) {
                                                        WireframeSearchIcon(Color.Black)
                                                        Text(
                                                            text = "VIEW SEQUENCES",
                                                            style = Typography.labelLarge,
                                                            fontWeight = FontWeight.Black,
                                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Warning banner
                                        Text(
                                            text = "WARNING: Trading creatures with other emulators within 30ft transfers custody. Transferred specimens are cleared permanently from memory sectors upon accepted linkage.",
                                            color = CyberGreenDim,
                                            fontSize = 8.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                            lineHeight = 11.sp,
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                        )
                                    }
                                }
                            }
                            "search" -> {
                                StepSearchView(
                                    viewModel = viewModel,
                                    activeColor = CyberGreen,
                                    activeColorDim = CyberGreenDim,
                                    activeBorder = CyberBorder,
                                    activePanel = CyberPanel,
                                    isAnomaly = false,
                                    onSelectGene = { stepSearchSelectedGene = it },
                                    onClose = {
                                        viewModel.setVaultSubTab("creatures")
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    onDeconstructGene = { gene ->
                                        geneToDeconstruct = gene
                                    },
                                )
                            }
                        }
                    }

                    if (devForceAnomaly) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Button(
                                onClick = {
                                    viewModel.synthManager.playCombinatorTick()
                                    viewModel.clearDevBases()
                                },
                                modifier = Modifier.weight(1f).height(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red,
                                    contentColor = Color.Black,
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                Text(
                                    text = "CLEAR STOCK",
                                    color = Color.Black,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Default,
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.synthManager.playCombinatorTick()
                                    viewModel.clearDevGenes()
                                },
                                modifier = Modifier.weight(1f).height(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red,
                                    contentColor = Color.Black,
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                Text(
                                    text = "CLEAR GENES",
                                    color = Color.Black,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Default,
                                )
                            }
                            Button(
                                onClick = {
                                    viewModel.synthManager.playCombinatorTick()
                                    viewModel.clearDevCreatures()
                                },
                                modifier = Modifier.weight(1f).height(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Red,
                                    contentColor = Color.Black,
                                ),
                                shape = RoundedCornerShape(4.dp),
                                contentPadding = PaddingValues(0.dp),
                            ) {
                                Text(
                                    text = "CLEAR CREATURES",
                                    color = Color.Black,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Default,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Step Search Gene Details Popup
        stepSearchSelectedGene?.let { gene ->
            StepSearchGeneDetailsPopup(
                viewModel = viewModel,
                gene = gene,
                activeColor = CyberGreen,
                activePanel = CyberPanel,
                onClose = { stepSearchSelectedGene = null },
                onDeconstructGene = { geneSeq ->
                    geneToDeconstruct = geneSeq
                    stepSearchSelectedGene = null
                },
            )
        }
    }

    // Recycling Dialog
    if (geneToDeconstruct != null) {
        GeneDeconstructPopup(
            viewModel = viewModel,
            gene = geneToDeconstruct!!,
            onConfirm = { count ->
                viewModel.decomposeGeneBlock(geneToDeconstruct!!.sequence, count)
                geneToDeconstruct = null
            },
            onDismiss = {
                geneToDeconstruct = null
            },
        )
    }
}

@Composable
fun CreatureDetailCard(
    c: Creature,
    viewModel: MainViewModel,
) {
    var showEnlargedQr by remember { mutableStateOf(false) }
    var showWireframeModel by remember { mutableStateOf(false) }
    val qrContent = remember(c) { viewModel.encodeCreatureToBase64(c) }

    // Effective stats computations scaled by telomeres
    val telomeres = c.telomeres
    val factor = 0.25f + 0.75f * (telomeres / 100f)
    val effVitality = maxOf(10, Math.round(c.vitality * factor))
    val effAttack = maxOf(5, Math.round(c.attack * factor))
    val effDefense = maxOf(5, Math.round(c.defense * factor))
    val effSpeed = maxOf(5, Math.round(c.speed * factor))
    val isDegraded = telomeres < 100

    val geneSequences by viewModel.geneSequences.collectAsStateWithLifecycle(initialValue = emptyList())
    val targetSequence by viewModel.targetSequence.collectAsStateWithLifecycle()
    val defenderId by viewModel.defenderCreatureId.collectAsStateWithLifecycle()
    val openedFrom by viewModel.creatureCardOpenedFrom.collectAsStateWithLifecycle()
    val harvestingCreatureIds by viewModel.harvestingCreatureIds.collectAsStateWithLifecycle()
    val activeTags = remember(c, targetSequence, defenderId, harvestingCreatureIds) {
        getCreatureTags(c, targetSequence, defenderId, harvestingCreatureIds)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Sub-panel layout: Side-by-side ASCII art and QR code
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Box 1: BIO-PHYSICAL RECON
            Column(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                    .background(Color.Black)
                    .clickable {
                        viewModel.synthManager.playBeep(520f, 0.05f, "sine")
                        showWireframeModel = true
                    }
                    .padding(8.dp)
                    .height(115.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "BIO-PHYSICAL RECON",
                    style = Typography.labelSmall,
                    color = CyberGreenDim,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                )
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    com.example.genpox.ui.components.CreatureWireframeView(
                        dna = c.sequence,
                        faction = c.faction,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            // Box 2: GENETIC QR COUPLING
            Column(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                    .background(Color.Black)
                    .clickable {
                        viewModel.synthManager.playBeep(880f, 0.05f, "sine")
                        showEnlargedQr = true
                        viewModel.decreaseCreatureTelomeres(c.id, 15, "QR coupling utilization")
                    }
                    .padding(8.dp)
                    .height(115.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "GENETIC QR COUPLING",
                    style = Typography.labelSmall,
                    color = CyberGreenDim,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    textAlign = TextAlign.Center,
                )
                QrRevealVisual(
                    modifier = Modifier
                        .size(60.dp)
                        .padding(vertical = 2.dp),
                )
                Text(
                    text = "[ CLICK TO ENLARGE ]",
                    style = Typography.labelSmall,
                    color = Color.Gray,
                    fontSize = 6.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Box 3: ARMAMENT DESIGNATION
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "ARMAMENT DESIGNATION",
                    style = Typography.labelSmall,
                    color = CyberGreenDim,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                )
                Text(
                    text = c.primaryWeapon,
                    style = Typography.bodyMedium,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "\"${c.lore}\"",
                    style = Typography.bodySmall,
                    color = CyberGreen,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    lineHeight = 14.sp,
                )
            }
        }

        // DYNAMIC HARVESTING / DISPATCH MODULE (Placed just above Vitality)
        val isHarvesting = remember(harvestingCreatureIds, c.id) {
            harvestingCreatureIds.contains(c.id)
        }
        val selectedAnomaly by viewModel.selectedAnomaly.collectAsStateWithLifecycle()

        if (isHarvesting) {
            HarvestingCountdownTimer(viewModel = viewModel, creatureId = c.id)
        } else if (openedFrom == "message_creature") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(10.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SECURE MESSAGE ATTACHMENT UPLINK",
                        color = CyberCyan,
                        style = Typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = "ATTACH SPECIMEN ${c.name} TO ACTIVE TRANSMISSION COMPILER.",
                        color = Color.LightGray,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Default,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Button(
                        onClick = {
                            viewModel.synthManager.playBeep(600f, 0.05f, "sine")
                            viewModel.setComposerAttachedCreature(c)
                            viewModel.setActiveCreature(null, "vault")
                            viewModel.selectTab("network")
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CyberCyan,
                            contentColor = Color.Black,
                        ),
                        shape = RoundedCornerShape(4.dp),
                    ) {
                        Text(
                            text = "ATTACH SPECIMEN",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Default,
                        )
                    }
                }
            }
        } else if (openedFrom == "scanner") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .border(1.dp, Color(0xFFA855F7), RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(10.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ANOMALY HARVEST TRANSMISSION LINK",
                        color = Color(0xFFA855F7),
                        style = Typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )

                    if (selectedAnomaly != null) {
                        val lockedAnom = selectedAnomaly!!
                        Text(
                            text = "Ready to dispatch to Anomaly ${lockedAnom.id} (Gene: ${lockedAnom.gene}). Dispatch duration scaled by Speed stat.",
                            color = Color.LightGray,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Default,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Button(
                            onClick = {
                                viewModel.synthManager.playBeep(600f, 0.05f, "sine")
                                viewModel.dispatchMission(c, lockedAnom)
                                viewModel.setSelectedAnomalyId(null)
                                viewModel.setActiveCreature(null)
                                viewModel.selectTab("scanner")
                            },
                            modifier = Modifier.fillMaxWidth().height(42.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFA855F7), // Purple
                                contentColor = Color.Black,
                            ),
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                text = "DISPATCH SEQUENCE TO ANOMALY",
                                color = Color.Black,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Default,
                            )
                        }
                    } else {
                        Text(
                            text = "NO ANOMALY TARGET LOCKED. SCAN FOR ANOMALIES FIRST.",
                            color = Color.Gray,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Default,
                        )
                    }
                }
            }
        }

        // Box 4: COMBAT TELEMETRY STATS & TELOMERES
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // HP
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        WireframeHeart(color = Color(0xFFEF4444))
                        Text(
                            text = "VITALITY:",
                            color = CyberGreen,
                            style = Typography.labelSmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                        )
                    }
                    Text(
                        text = buildString {
                            append("$effVitality HP")
                            if (isDegraded) append(" (${c.vitality})")
                        },
                        color = if (isDegraded) Color(0xFFEF4444) else Color.White,
                        style = Typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                    )
                }

                // Aggression
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        WireframeClaws(color = Color(0xFFF59E0B))
                        Text(
                            text = "AGGRESSION:",
                            color = CyberGreen,
                            style = Typography.labelSmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(6.dp)
                                .background(Color(0xFF262626), RoundedCornerShape(3.dp)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((effAttack / 100f).coerceIn(0f, 1f))
                                    .background(Color(0xFFF59E0B), RoundedCornerShape(3.dp)),
                            )
                        }
                        Text(
                            text = buildString {
                                append("$effAttack")
                                if (isDegraded) append(" (${c.attack})")
                            },
                            color = if (isDegraded) Color(0xFFEF4444) else Color.White,
                            style = Typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }

                // Block Shells
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        WireframeShield(color = Color(0xFF60A5FA))
                        Text(
                            text = "BLOCK SHELLS:",
                            color = CyberGreen,
                            style = Typography.labelSmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(6.dp)
                                .background(Color(0xFF262626), RoundedCornerShape(3.dp)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((effDefense / 100f).coerceIn(0f, 1f))
                                    .background(Color(0xFF60A5FA), RoundedCornerShape(3.dp)),
                            )
                        }
                        Text(
                            text = buildString {
                                append("$effDefense")
                                if (isDegraded) append(" (${c.defense})")
                            },
                            color = if (isDegraded) Color(0xFFEF4444) else Color.White,
                            style = Typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }

                // Speed Rate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        WireframeLightning(color = Color(0xFFFBBF24))
                        Text(
                            text = "SPEED RATE:",
                            color = CyberGreen,
                            style = Typography.labelSmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(6.dp)
                                .background(Color(0xFF262626), RoundedCornerShape(3.dp)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((effSpeed / 100f).coerceIn(0f, 1f))
                                    .background(Color(0xFFFBBF24), RoundedCornerShape(3.dp)),
                            )
                        }
                        Text(
                            text = buildString {
                                append("$effSpeed")
                                if (isDegraded) append(" (${c.speed})")
                            },
                            color = if (isDegraded) Color(0xFFEF4444) else Color.White,
                            style = Typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }

                // Telomeres
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            WireframeDna(color = Color(0xFF22C55E))
                            Text(
                                text = "TELOMERES:",
                                color = Color(0xFF22C55E),
                                style = Typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                            )
                        }
                        Text(
                            text = "$telomeres%",
                            color = Color.White,
                            style = Typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    val telomereColor = when {
                        telomeres > 65 -> Color(0xFF22C55E)
                        telomeres > 30 -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .background(Color(0xFF262626), RoundedCornerShape(3.dp)),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((telomeres / 100f).coerceIn(0f, 1f))
                                .background(telomereColor, RoundedCornerShape(3.dp)),
                        )
                    }
                }
            }
        }

        // Box 5: 64-CHARACTER INTEGRATION SEQUENCE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "64-CHARACTER INTEGRATION SEQUENCE",
                    style = Typography.labelSmall,
                    color = CyberGreenDim,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                )
                val sequenceBlocks = (0 until (c.sequence.length / 8)).map { i ->
                    c.sequence.substring(i * 8, minOf(c.sequence.length, (i + 1) * 8))
                }
                val sequenceRows = sequenceBlocks.chunked(4)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    sequenceRows.forEachIndexed { rowIndex, rowBlocks ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            rowBlocks.forEachIndexed { colIndex, block ->
                                val blockIdx = rowIndex * 4 + colIndex
                                val isAnom = WaveMath.isAnomalousGene(block)
                                val color = when {
                                    isAnom -> Color(0xFFC084FC)
                                    blockIdx % 4 == 0 -> Color(0xFF00FF41)
                                    blockIdx % 4 == 1 -> Color(0xFFFBBF24)
                                    blockIdx % 4 == 2 -> Color(0xFF60A5FA)
                                    else -> Color(0xFFC084FC)
                                }
                                val borderStroke = if (isAnom) BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f)) else null
                                val bg = if (isAnom) Color(0xFF1E0B36) else Color.Transparent
                                Box(
                                    modifier = Modifier
                                        .then(if (borderStroke != null) Modifier.border(borderStroke, RoundedCornerShape(4.dp)) else Modifier)
                                        .background(bg, RoundedCornerShape(2.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                ) {
                                    Text(
                                        text = block,
                                        color = color,
                                        style = Typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        letterSpacing = 1.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (c.isMutated && c.originalSequence != null) {
            DnaComparisonGrid(
                original = c.originalSequence,
                current = c.sequence,
            )
        }

        // Box 6: GENE HARVESTING MATRIX
        var selectedGeneIndex by remember { mutableStateOf<Int?>(null) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF7F1D1D), RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        WireframeWarning(color = Color(0xFFEF4444))
                        Text(
                            text = "GENE HARVESTING MATRIX",
                            style = Typography.labelSmall,
                            color = Color(0xFFF87171),
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                        )
                    }
                    Text(
                        text = "1 EXTRACTABLE NODE LIMIT",
                        style = Typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 7.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    )
                }

                Text(
                    text = "Select exactly one gene below to harvest, placing it into your Gen-Vault. Doing so will destroy the remaining genes.",
                    style = Typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    color = Color.LightGray,
                    fontSize = 9.sp,
                    lineHeight = 13.sp,
                )

                val numBlocks = c.sequence.length / 8
                val matrixBlocks = (0 until numBlocks).map { i ->
                    Pair(i, c.sequence.substring(i * 8, minOf(c.sequence.length, (i + 1) * 8)))
                }
                val matrixRows = matrixBlocks.chunked(4)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    matrixRows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            rowItems.forEach { (i, gene) ->
                                val isSelected = selectedGeneIndex == i
                                val isAnom = WaveMath.isAnomalousGene(gene)
                                val ownedCount = geneSequences.find { it.sequence == gene }?.count ?: 0
                                val isNew = ownedCount == 0

                                val borderStroke = when {
                                    isSelected -> if (isAnom) BorderStroke(1.dp, Color(0xFFA855F7)) else BorderStroke(1.dp, Color(0xFFEF4444))
                                    isAnom -> BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.5f))
                                    isNew -> BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
                                    else -> BorderStroke(1.dp, Color(0xFF262626))
                                }

                                val bg = when {
                                    isSelected -> if (isAnom) Color(0xFF1E0B36) else Color(0xFF7F1D1D).copy(alpha = 0.2f)
                                    isAnom -> Color(0xFF1E0B36).copy(alpha = 0.4f)
                                    isNew -> Color(0xFF78350F).copy(alpha = 0.1f)
                                    else -> Color.Transparent
                                }

                                val textColor = when {
                                    isSelected -> if (isAnom) Color(0xFFD8B4FE) else Color(0xFF00FF41)
                                    isAnom -> Color(0xFFD8B4FE)
                                    isNew -> Color(0xFFFBBF24)
                                    else -> Color.Gray
                                }

                                Box(
                                    modifier = Modifier
                                        .border(borderStroke, RoundedCornerShape(4.dp))
                                        .background(bg, RoundedCornerShape(4.dp))
                                        .clickable {
                                            viewModel.synthManager.playBeep((600 + i * 50).toFloat(), 0.05f, "sine")
                                            selectedGeneIndex = if (isSelected) null else i
                                        }
                                        .padding(horizontal = 4.dp, vertical = 6.dp)
                                        .width(72.dp)
                                        .height(42.dp),
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Text(
                                            text = "#${i + 1}",
                                            color = Color.DarkGray,
                                            fontSize = 7.sp,
                                            style = Typography.labelSmall,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                            modifier = Modifier.align(Alignment.TopStart),
                                        )
                                        if (isNew && !isSelected) {
                                            Text(
                                                text = "NEW",
                                                color = Color(0xFFFBBF24),
                                                fontSize = 6.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                                modifier = Modifier.align(Alignment.TopEnd),
                                            )
                                        }
                                        Text(
                                            text = gene,
                                            color = textColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 0.5.sp,
                                            modifier = Modifier.align(Alignment.BottomCenter),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedGeneIndex != null) {
                    val idx = selectedGeneIndex!!
                    val geneToHarvest = c.sequence.substring(idx * 8, minOf(c.sequence.length, (idx + 1) * 8))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .height(1.dp)
                                .fillMaxWidth()
                                .background(Color(0xFF7F1D1D).copy(alpha = 0.3f)),
                        )
                        Text(
                            text = "CONVERT GENE \"$geneToHarvest\" INTO ARCHIVE STOCK. THIS CREATURE WILL BE PURGED.",
                            color = Color(0xFFF87171),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                            lineHeight = 12.sp,
                        )
                        Button(
                            onClick = {
                                viewModel.incinerateCreature(c, geneToHarvest)
                            },
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB91C1C), contentColor = Color.White),
                            shape = RoundedCornerShape(4.dp),
                        ) {
                            Text(
                                text = "ACTIVATE HARVEST INCINERATION",
                                style = Typography.labelSmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }

        // Box 7: GENE SEQUENCE EXTENSION LAB
        val lastFour = c.sequence.takeLast(4)
        val prefixToMatch = lastFour.reversed()
        var extensionSearchPrefix by remember(c.sequence) { mutableStateOf(prefixToMatch) }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF78350F), RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val activeExtensionsCount = maxOf(0, (c.sequence.length - 64) / 8)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        WireframeSparkle(color = Color(0xFFFBBF24))
                        Text(
                            text = "GENE SEQUENCE EXTENSION LAB",
                            style = Typography.labelSmall,
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                        )
                    }
                    Text(
                        text = "EXTENSIONS: $activeExtensionsCount / 2 MAXIMUM",
                        style = Typography.labelSmall,
                        color = Color.Gray,
                        fontSize = 7.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    )
                }

                Text(
                    text = "Append up to two extra genes to this P.O.X. sequence to further enhance it. G.E.N. P.O.X. synthesized & approved genes are guaranteed to improve any P.O.X. sequence. WARNING: Appending anomalous genetics will flag the offending sequence!",
                    style = Typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    color = Color.LightGray,
                    fontSize = 9.sp,
                    lineHeight = 13.sp,
                )

                if (c.sequence.length < 80) {
                    val matchingGenes = geneSequences.filter { it.count > 0 && it.sequence.startsWith(prefixToMatch) }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFF78350F), RoundedCornerShape(4.dp))
                                .background(Color(0xFF78350F).copy(alpha = 0.1f))
                                .padding(8.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                WireframeDna(color = Color(0xFFFBBF24))
                                Text(
                                    text = "GENE ALIGNMENT RULE: Next gene must start with \"$prefixToMatch\" (reverses target's suffix \"$lastFour\").",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 8.5.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                    lineHeight = 12.sp,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        if (matchingGenes.isNotEmpty()) {
                            val activeStep = extensionSearchPrefix.length / 2
                            val stepLabels = listOf("1-2bp", "3-4bp", "5-6bp", "7-8bp")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                stepLabels.forEachIndexed { idx, stepLabel ->
                                    val isCompleted = activeStep > idx
                                    val isActive = activeStep == idx
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .border(
                                                1.dp,
                                                if (isActive) {
                                                    Color(0xFFFBBF24)
                                                } else if (isCompleted) {
                                                    Color(0xFF78350F)
                                                } else {
                                                    Color.DarkGray
                                                },
                                                RoundedCornerShape(4.dp),
                                            )
                                            .background(if (isActive) Color(0xFFFBBF24).copy(alpha = 0.15f) else Color.Transparent)
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = stepLabel,
                                            style = Typography.labelSmall,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                            color = if (isActive || isCompleted) Color(0xFFFBBF24) else Color.Gray,
                                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 8.5.sp,
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF78350F), RoundedCornerShape(4.dp))
                                    .background(Color.Black)
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    for (i in 0 until 4) {
                                        val block = if (extensionSearchPrefix.length >= (i + 1) * 2) {
                                            extensionSearchPrefix.substring(i * 2, (i + 1) * 2)
                                        } else {
                                            "_"
                                        }
                                        val isFilled = extensionSearchPrefix.length >= (i + 1) * 2
                                        val color = if (isFilled) Color(0xFFFBBF24) else Color.DarkGray
                                        Box(
                                            modifier = Modifier
                                                .border(1.dp, color, RoundedCornerShape(4.dp))
                                                .background(if (isFilled) Color(0xFF78350F).copy(alpha = 0.15f) else Color.Transparent)
                                                .padding(horizontal = 6.dp, vertical = 4.dp),
                                        ) {
                                            Text(
                                                text = block,
                                                color = color,
                                                style = Typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                            )
                                        }
                                    }
                                }

                                if (extensionSearchPrefix.length > 4) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Button(
                                            onClick = {
                                                viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                                                extensionSearchPrefix = extensionSearchPrefix.dropLast(2)
                                            },
                                            modifier = Modifier.height(28.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color(0xFFFBBF24)),
                                            border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                        ) {
                                            Text(
                                                text = "BACK",
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                viewModel.synthManager.playBeep(440f, 0.05f, "sine")
                                                extensionSearchPrefix = prefixToMatch
                                            },
                                            modifier = Modifier.height(28.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.Red),
                                            border = BorderStroke(1.dp, Color.Red),
                                            shape = RoundedCornerShape(4.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp),
                                        ) {
                                            Text(
                                                text = "RESET",
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                            }

                            if (extensionSearchPrefix.length < 8) {
                                val options = if (extensionSearchPrefix.length == 4) {
                                    matchingGenes.map { it.sequence.substring(4, 6) }.distinct()
                                } else {
                                    matchingGenes.filter { it.sequence.startsWith(extensionSearchPrefix) }.map { it.sequence.substring(6, 8) }.distinct()
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "SELECT NEXT 2-BASE PAIRS OPTIONS:",
                                        style = Typography.labelSmall,
                                        color = Color(0xFFFBBF24),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                    )
                                    androidx.compose.foundation.layout.FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        options.forEach { opt ->
                                            Box(
                                                modifier = Modifier
                                                    .border(BorderStroke(1.dp, Color(0xFF262626)), RoundedCornerShape(4.dp))
                                                    .background(Color.Transparent, RoundedCornerShape(4.dp))
                                                    .clickable {
                                                        viewModel.synthManager.playBeep(580f, 0.05f, "sine")
                                                        extensionSearchPrefix += opt
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                                    .width(52.dp),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = opt,
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                val finalGene = extensionSearchPrefix
                                val ownedCount = geneSequences.find { it.sequence == finalGene }?.count ?: 0
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .height(1.dp)
                                            .fillMaxWidth()
                                            .background(Color(0xFF78350F).copy(alpha = 0.3f)),
                                    )
                                    Text(
                                        text = "APPEND GENE \"$finalGene\" (INVENTORY: $ownedCount AVAILABLE) TO INSTANCE RECTIFIER BLOCK.",
                                        color = Color(0xFFFBBF24),
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                        lineHeight = 12.sp,
                                    )
                                    Button(
                                        onClick = {
                                            viewModel.appendGeneToActiveCreature(c, finalGene)
                                        },
                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706), contentColor = Color.Black),
                                        shape = RoundedCornerShape(4.dp),
                                    ) {
                                        Text(
                                            text = "APPLY EXTENSION",
                                            style = Typography.labelSmall,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF262626), RoundedCornerShape(4.dp))
                                    .background(Color(0xFF0A0A0A))
                                    .padding(8.dp),
                            ) {
                                Text(
                                    text = buildString {
                                        append("✕ No matching gene blocks in stock that start with \"$prefixToMatch\".\n")
                                        append("Please visit the DNA COMBINATOR tab to synthesize patterns starting with this prefix.")
                                    },
                                    color = Color(0xFFEA580C),
                                    fontSize = 8.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                    lineHeight = 12.sp,
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF065F46), RoundedCornerShape(4.dp))
                            .background(Color(0xFF065F46).copy(alpha = 0.15f))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "▲ STABILIZATION INTEGRITY UNLOCKED",
                                color = Color(0xFF10B981),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                            )
                            Text(
                                text = "Specimen sequence has been appended with exactly 2 maximum additional genes. Signal capacity stabilized.",
                                color = Color(0xFF047857),
                                fontSize = 8.5.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }

        // Box 8: UNLOCKED SPECIAL ACTIONS (Conditional)
        val unlockedMoves = remember(c.sequence) { viewModel.getUnlockedMoves(c.sequence) }
        if (unlockedMoves.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF78350F), RoundedCornerShape(4.dp))
                    .background(Color(0xFF78350F).copy(alpha = 0.1f))
                    .padding(10.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        WireframeSparkle(color = Color(0xFFFBBF24))
                        Text(
                            text = "UNLOCKED MUTANT SPECIAL ACTIONS (${unlockedMoves.size}):",
                            style = Typography.labelSmall,
                            color = Color(0xFFFBBF24),
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        unlockedMoves.forEach { mv ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Color(0xFF78350F), RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.4f))
                                    .padding(6.dp),
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = "[${mv.type.uppercase()}] ${mv.name}",
                                            color = Color(0xFFFBBF24),
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                        )
                                        Text(
                                            text = "ACTIVE IN HACKS",
                                            color = Color.Gray,
                                            fontSize = 7.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                        )
                                    }
                                    Text(
                                        text = mv.description,
                                        color = Color(0xFF00FF41),
                                        fontSize = 8.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Box 9: Footer Actions
        val defenderId by viewModel.defenderCreatureId.collectAsStateWithLifecycle()
        val isDefender = defenderId == c.id

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Button(
                    onClick = { viewModel.toggleDefenderCreature(c.id) },
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDefender) Color(0xFF3B82F6) else Color.Transparent,
                        contentColor = if (isDefender) Color.Black else Color.Gray,
                    ),
                    border = if (isDefender) null else BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        WireframeShield(
                            color = if (isDefender) Color.Black else Color.Gray,
                            modifier = Modifier.size(10.dp),
                        )
                        Text(
                            text = if (isDefender) "DEFENDER READY" else "DEFENDER",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Button(
                    onClick = { viewModel.toggleAutoHackerCreature(c) },
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (c.isAutoHacker) Color(0xFFD97706) else Color.Transparent,
                        contentColor = if (c.isAutoHacker) Color.Black else Color.Gray,
                    ),
                    border = if (c.isAutoHacker) null else BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        WireframeClaws(
                            color = if (c.isAutoHacker) Color.Black else Color.Gray,
                            modifier = Modifier.size(10.dp),
                        )
                        Text(
                            text = if (c.isAutoHacker) "AUTO-HACKER READY" else "AUTO-HACKER",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Button(
                    onClick = { viewModel.toggleFavoriteCreature(c) },
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (c.isFavorite) Color(0xFFEAB308) else Color.Transparent,
                        contentColor = if (c.isFavorite) Color.Black else Color.Gray,
                    ),
                    border = if (c.isFavorite) null else BorderStroke(1.dp, CyberBorder),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        WireframeStar(
                            color = if (c.isFavorite) Color.Black else Color.Gray,
                            filled = c.isFavorite,
                        )
                        Text(
                            text = if (c.isFavorite) "FAVORITED" else "FAVORITE",
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                Button(
                    onClick = { viewModel.synthManager.playCreatureSequenceAudio(c.sequence) },
                    modifier = Modifier.weight(1f).height(38.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = CyberGreen,
                    ),
                    border = BorderStroke(1.dp, CyberGreen),
                    shape = RoundedCornerShape(4.dp),
                ) {
                    Text(
                        text = "AUDIO EMIT",
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }

    // Click-to-enlarge full screen QR coupling transponder pop-up dialog
    if (showEnlargedQr) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showEnlargedQr = false }) {
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .border(2.dp, CyberGreen, RoundedCornerShape(8.dp))
                    .background(Color.Black)
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "GENETIC TRANSPONDER DATA",
                        color = CyberGreen,
                        style = Typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    )
                    QrCodeImage(
                        content = qrContent,
                        sizeDp = 180,
                    )
                    Box(
                        modifier = Modifier
                            .background(CyberRed, RoundedCornerShape(4.dp))
                            .clickable { showEnlargedQr = false }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "CLOSE",
                            color = Color.Black,
                            style = Typography.labelSmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }

    if (showWireframeModel) {
        val fColor = when (c.faction) {
            "Infection" -> Color(0xFFEF4444)
            "Mech" -> Color(0xFF60A5FA)
            "Parasite" -> Color(0xFFA855F7)
            else -> Color(0xFF00FF41)
        }
        androidx.compose.ui.window.Dialog(onDismissRequest = { showWireframeModel = false }) {
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .border(2.dp, fColor, RoundedCornerShape(8.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                // Background: full-frame scanner static + 3D creature
                com.example.genpox.ui.components.CreatureWireframeView(
                    dna = c.sequence,
                    faction = c.faction,
                    modifier = Modifier.fillMaxSize(),
                )

                // Foreground: Text overlay elements
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "3D HOLO-STRUCT RECON",
                        color = Color.White,
                        style = Typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .background(CyberRed, RoundedCornerShape(4.dp))
                            .clickable { showWireframeModel = false }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text = "CLOSE",
                            color = Color.Black,
                            style = Typography.labelSmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

fun getCoherence(seq: String, target: String): String {
    if (seq.isEmpty() || target.isEmpty()) return "none"
    val seq64 = seq.take(64)
    val target64 = target.take(64)
    if (seq64 == target64) return "full"

    var alignedMatches = 0
    for (i in 0 until 8) {
        val start = i * 8
        val end = (i + 1) * 8
        if (start < seq64.length && end <= seq64.length && start < target64.length && end <= target64.length) {
            val seqGene = seq64.substring(start, end)
            val tgtGene = target64.substring(start, end)
            if (seqGene == tgtGene) {
                alignedMatches++
            }
        }
    }
    if (alignedMatches == 8) return "full"
    if (alignedMatches >= 1) return "partial"

    for (i in 0 until 8) {
        val start = i * 8
        val end = (i + 1) * 8
        if (start < target64.length && end <= target64.length) {
            val tgtGene = target64.substring(start, end)
            if (seq.contains(tgtGene)) {
                return "partial"
            }
        }
    }
    return "none"
}

fun getCreatureTags(
    item: Creature,
    targetSequence: String,
    defenderId: String? = null,
    harvestingCreatureIds: Set<String> = emptySet(),
): List<String> {
    val tags = mutableListOf<String>()
    if (item.isFavorite) tags.add("FAVORITE")
    if (defenderId == item.id) tags.add("DEFENDER")
    if (item.isAutoHacker) tags.add("AUTO-HACKER")
    if (harvestingCreatureIds.contains(item.id)) tags.add("HARVESTING")

    if (item.isFullCoherence) {
        tags.add("FULL COHERENCE")
        if (item.coherenceType == "Natural") {
            tags.add("NATURAL")
        } else if (item.coherenceType == "Forced") {
            tags.add("FORCED")
        }
    }

    if (item.attack >= 75 || item.defense >= 75 || item.vitality >= 75 || item.speed >= 75) {
        tags.add("ALPHA GENE")
    }
    if (item.appendedGenes.isNotEmpty()) {
        tags.add("MODIFIED")
    }
    if (item.isMutated) {
        tags.add("MUTATED")
    }
    if (item.origin == "Created") {
        tags.add("ORIGINAL")
    } else if (item.origin.startsWith("Traded")) {
        tags.add("TRANSFER-ORIGIN")
    }
    return tags
}

@Composable
private fun PoxDropdown(
    label: String,
    selectedOption: String,
    options: List<Pair<String, String>>,
    onOptionSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            color = CyberGreenDim,
            style = Typography.labelSmall,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
                .background(Color.Black)
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = options.find { it.first == selectedOption }?.second?.uppercase() ?: selectedOption.uppercase(),
                    color = Color.White,
                    style = Typography.bodySmall,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                    fontSize = 10.sp,
                )
                Text(
                    text = "▼",
                    color = CyberGreen,
                    fontSize = 8.sp,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier
                    .background(Color.Black)
                    .border(1.dp, CyberBorder, RoundedCornerShape(4.dp)),
            ) {
                options.forEach { (value, displayName) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = displayName.uppercase(),
                                color = Color.White,
                                style = Typography.bodySmall,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                            )
                        },
                        onClick = {
                            onOptionSelected(value)
                            expanded = false
                        },
                        modifier = Modifier.background(Color.Black),
                    )
                }
            }
        }
    }
}
