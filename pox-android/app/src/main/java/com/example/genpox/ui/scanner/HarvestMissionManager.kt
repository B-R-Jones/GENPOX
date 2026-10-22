package com.example.genpox.ui.scanner

import com.example.genpox.audio.PoxSynthManager
import com.example.genpox.data.*
import com.example.genpox.data.repository.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Encapsulates harvest mission management: dispatching expeditions, recalling missions,
 * simulating per-tick hazard conflict resolutions, and biological mutation wells.
 */
class HarvestMissionManager(
    private val missionRepository: MissionRepository,
    private val creatureRepository: CreatureRepository,
    private val geneStockRepository: GeneStockRepository,
    private val preferencesRepository: UserPreferencesRepository,
    private val synthManager: PoxSynthManager,
    private val getUserLocation: () -> Pair<Double, Double>,
    private val getRawAnomalies: () -> List<PoxAnomaly>,
    private val getCelestialWells: () -> List<CelestialWell>,
    private val getReactorSalt: () -> Float,
    private val setTrackedMissionId: (String?) -> Unit,
    private val getTrackedMissionId: () -> String?,
    private val logEmitter: (String) -> Unit,
) {

    fun hasCoherenceShield(creature: Creature): Boolean {
        if (WaveMath.getAnomalousBenefits(creature.sequence).any { it.id == "COHERENCE_SHIELD" }) {
            return true
        }
        creature.appendedGenes.forEach { gene ->
            if (WaveMath.isAnomalousGene(gene) && WaveMath.getBenefitForAnomalousGene(gene).id == "COHERENCE_SHIELD") {
                return true
            }
        }
        return false
    }

    fun dispatchMission(scope: CoroutineScope, creature: Creature, anomaly: PoxAnomaly) {
        scope.launch {
            val wave = WaveMath.getDailyWaveConfig(System.currentTimeMillis())
            val (userLat, userLng) = getUserLocation()
            val boundaryRadius = anomaly.getBoundaryRadiusForPlayer(userLat, userLng)
            val phaseFraction = wave.lunarAge / DeploymentEngine.LUNAR_MONTH_DAYS
            val lunarPhaseScale = (1.0 - kotlin.math.cos(phaseFraction * 2.0 * Math.PI)) / 2.0
            val lunarResistanceMod = 0.7 + 0.6 * lunarPhaseScale
            val rBase = boundaryRadius * 0.1
            val rAnom = rBase * lunarResistanceMod

            val resonanceMod = DeploymentEngine.getSynodicResonanceMod(creature.faction, wave.phaseName)
            val effectiveDefense = DeploymentEngine.calculateEffectiveDefense(creature.defense, creature.faction, wave.phaseName)

            val stalledDepth = DeploymentEngine.calculateStallDepth(effectiveDefense, boundaryRadius, wave.lunarAge)
            val dispatchDistance = boundaryRadius * (1.0 - stalledDepth / 100.0)

            val mutationInterval = DeploymentEngine.calculateMutationInterval(stalledDepth, wave.lunarAge)

            val t = if (anomaly.distance > 0.0) (dispatchDistance / anomaly.distance).coerceIn(0.0, 1.0) else 0.0
            val landingLat = anomaly.lat + t * (userLat - anomaly.lat)
            val landingLng = anomaly.lng + t * (userLng - anomaly.lng)

            val combinedDensity = DeploymentEngine.calculateCombinedWaveDensity(landingLat, landingLng, getRawAnomalies())

            val descentDistance = boundaryRadius * (stalledDepth / 100.0)
            val densityShift = 0.2 * (lunarPhaseScale - 0.5)
            val effectiveDensity = (combinedDensity + densityShift).coerceIn(-0.33, 0.33)

            val hasShield = hasCoherenceShield(creature)

            val metrics = DeploymentEngine.calculateTransitMetrics(
                creatureSpeed = creature.speed,
                anomalyDistance = anomaly.distance,
                boundaryRadius = boundaryRadius,
                stalledDepth = stalledDepth,
                effectiveDensity = effectiveDensity,
                hasCoherenceShield = hasShield,
            )

            val travelDuration = metrics.travelDuration
            val descentDuration = metrics.descentDuration
            val harvestDuration = metrics.harvestDuration
            val ascentDuration = metrics.ascentDuration
            val transitBackDuration = metrics.returnDuration
            val calculatedDuration = metrics.totalDuration

            val densityVal = Math.round(combinedDensity * 100.0).toInt()
            val effDensityVal = Math.round(effectiveDensity * 100.0).toInt()
            val missionLogsList = listOf(
                "[LAUNCH] Specimen \"${creature.name}\" dispatched to Anomaly ${anomaly.id}.",
                "[LANDING_COORD] $landingLat,$landingLng",
                "[TELEMETRY] Synodic Resonance: ${if (resonanceMod >= 0) "+" else ""}$resonanceMod DEF (${wave.phaseName}).",
                "[TELEMETRY] Effective Defense: $effectiveDefense | Anomaly Resistance: ${String.format(java.util.Locale.US, "%.1f", rAnom)} (Base: ${rBase.toInt()}, Lunar: ${String.format(java.util.Locale.US, "%.2f", lunarResistanceMod)}x).",
                "[TELEMETRY] Stalled Depth resolved: ${stalledDepth.toInt()}% (${String.format(java.util.Locale.US, "%.1f", dispatchDistance)}ft from epicenter).",
                "[TELEMETRY] Combined Wave Density -> Base: ${if (densityVal >= 0) "+" else ""}$densityVal% | Active Shifted: ${if (effDensityVal >= 0) "+" else ""}$effDensityVal%.",
                "[TELEMETRY] Coherence Shield status: ${if (hasShield) "ACTIVE (Drag Imm.)" else "INACTIVE"}.",
                "[TELEMETRY] Durations -> Travel: ${travelDuration}s | Descent: ${descentDuration}s | Harvest: ${harvestDuration}s | Ascent: ${ascentDuration}s | Return: ${transitBackDuration}s.",
                "[TELEMETRY] Mutation Well active. Expected base decay: every ${mutationInterval}s.",
            )

            val mission = HarvestMission(
                id = "MSN-" + UUID.randomUUID().toString().take(6),
                creatureId = creature.id,
                creatureName = creature.name,
                creatureFaction = creature.faction,
                lat = anomaly.lat,
                lng = anomaly.lng,
                startTime = System.currentTimeMillis(),
                totalDuration = calculatedDuration,
                harvestedPayload = listOf(anomaly.gene),
                isCompleted = false,
                isReturned = false,
                dispatchDistance = dispatchDistance,
                stalledDepth = stalledDepth,
                originalSequence = creature.sequence,
                elapsedSeconds = 0L,
                missionLogs = missionLogsList,
                phase = "TRAVEL",
                travelDuration = travelDuration,
                descentDuration = descentDuration,
                harvestDuration = harvestDuration,
                ascentDuration = ascentDuration,
                transitBackDuration = transitBackDuration,
                currentPhaseElapsed = 0L,
            )

            missionRepository.insertMission(mission)
            setTrackedMissionId(mission.id)
            logEmitter("DSP: Dispatched \"${creature.name}\" to harvest spot [${anomaly.gene}]")
            synthManager.playBeep(520f, 0.3f, "triangle")
        }
    }

    suspend fun recallMission(mission: HarvestMission) {
        val landingLog = mission.missionLogs.find { it.startsWith("[LANDING_COORD] ") }
        val (landingLat, landingLng) = if (landingLog != null) {
            val coords = landingLog.substringAfter("[LANDING_COORD] ").split(",")
            coords[0].toDouble() to coords[1].toDouble()
        } else {
            mission.lat to mission.lng
        }

        val otherAnom = getRawAnomalies().find { anom ->
            (Math.abs(anom.lat - mission.lat) > 0.0001 || Math.abs(anom.lng - mission.lng) > 0.0001) &&
                calculateDistanceInFeet(landingLat, landingLng, anom.lat, anom.lng) <= anom.getBoundaryRadiusForPlayer(landingLat, landingLng)
        }

        fun isGeneSequence(s: String): Boolean = s.length == 8 && s.all { it in "AGTC" }

        val primaryGene = mission.harvestedPayload.find { isGeneSequence(it) } ?: "AGTCGTAC"
        var updatedPrimaryGene = primaryGene
        val finalLogs = mission.missionLogs.toMutableList()

        if (otherAnom != null) {
            val dA = calculateDistanceInFeet(landingLat, landingLng, mission.lat, mission.lng)
            val dB = calculateDistanceInFeet(landingLat, landingLng, otherAnom.lat, otherAnom.lng)

            val baseSpilloverChance = 0.125
            val factor = 1.0 - (dB / (dA + dB)).coerceIn(0.0, 1.0)
            val spilloverChance = baseSpilloverChance * factor

            val roll = Math.random()
            finalLogs.add("[TELEMETRY] Adjacent anomaly boundary overlap: ${otherAnom.id} (crosstalk active).")
            if (roll <= spilloverChance) {
                val targetGene = primaryGene
                val overlapGene = otherAnom.gene
                if (targetGene.length == 8 && overlapGene.length == 8) {
                    val hybridGene = targetGene.substring(0, 4) + overlapGene.substring(4, 8)
                    updatedPrimaryGene = hybridGene
                    finalLogs.add("[TELEMETRY] Hybridization success: $hybridGene (Target $targetGene Weld Overlap $overlapGene).")
                } else {
                    updatedPrimaryGene = overlapGene
                    finalLogs.add("[TELEMETRY] Crosstalk spillover: Harvested adjacent gene $overlapGene.")
                }
            } else {
                finalLogs.add("[TELEMETRY] Crosstalk failed. Target signature remains dominant.")
            }
        }

        val finalPayload = mission.harvestedPayload.toMutableList()
        val primaryIndex = finalPayload.indexOf(primaryGene)
        if (primaryIndex != -1) {
            finalPayload[primaryIndex] = updatedPrimaryGene
        } else {
            finalPayload.add(0, updatedPrimaryGene)
        }

        val bonusGene = WaveMath.generateAnomalousGene()
        finalPayload.add(bonusGene)

        val genesToInsert = mutableListOf<String>()
        var deltaA = 0L
        var deltaG = 0L
        var deltaT = 0L
        var deltaC = 0L

        finalPayload.forEach { item ->
            if (isGeneSequence(item)) {
                genesToInsert.add(item)
            } else {
                when (item.uppercase()) {
                    "STOCK_A" -> deltaA++
                    "STOCK_G" -> deltaG++
                    "STOCK_T" -> deltaT++
                    "STOCK_C" -> deltaC++
                }
            }
        }

        val toInsertOrUpdate = mutableListOf<GeneSequence>()
        genesToInsert.distinct().forEach { gene ->
            val tm = GeneticsEngine.calculateMeltingTemperature(gene, getReactorSalt().toDouble())
            val mfe = GeneticsEngine.calculateMinimumFreeEnergy(gene)
            toInsertOrUpdate.add(
                GeneSequence(
                    sequence = gene,
                    count = finalPayload.count { it == gene },
                    discoveredAt = System.currentTimeMillis(),
                    averageQScore = 30.0,
                    meltingTemp = tm,
                    minimumFreeEnergy = mfe,
                ),
            )
        }
        geneStockRepository.insertGeneSequences(toInsertOrUpdate)

        if (deltaA > 0 || deltaG > 0 || deltaT > 0 || deltaC > 0) {
            val currentA = preferencesRepository.rawStockA.first()
            val currentG = preferencesRepository.rawStockG.first()
            val currentT = preferencesRepository.rawStockT.first()
            val currentC = preferencesRepository.rawStockC.first()

            val newA = currentA + deltaA
            val newG = currentG + deltaG
            val newT = currentT + deltaT
            val newC = currentC + deltaC

            preferencesRepository.saveRawStocks(newA, newG, newT, newC)
            finalLogs.add("[RESOURCES] Siphoned resources deposited to stockpile: A: +$deltaA, G: +$deltaG, T: +$deltaT, C: +$deltaC")
        }

        val updated = mission.copy(isReturned = true, harvestedPayload = finalPayload, missionLogs = finalLogs)
        missionRepository.insertMission(updated)

        if (getTrackedMissionId() == mission.id) {
            setTrackedMissionId(null)
        }

        logEmitter("RCL: Mission returned. Payload processed: ${finalPayload.joinToString()}")
        synthManager.playSynthesisSuccess()
    }

    fun recallMission(scope: CoroutineScope, mission: HarvestMission) {
        scope.launch {
            recallMission(mission)
        }
    }

    fun recallAllActiveMissions(scope: CoroutineScope) {
        scope.launch {
            try {
                val active = missionRepository.activeMissions.first()
                val ongoing = active.filter { !it.isReturned }
                if (ongoing.isEmpty()) {
                    logEmitter("DEV: No active harvesters found to recall.")
                    return@launch
                }
                ongoing.forEach { m ->
                    val updated = m.copy(
                        isCompleted = true,
                        phase = "COMPLETED",
                        currentPhaseElapsed = 0L,
                    )
                    recallMission(updated)
                }
                logEmitter("DEV: Recalled ${ongoing.size} active harvesters.")
            } catch (e: Exception) {
                logEmitter("DEV ERROR: Failed to recall harvesters: ${e.message}")
            }
        }
    }

    suspend fun tickMissions(activeMissionsList: List<HarvestMission>) {
        if (activeMissionsList.isEmpty()) return

        for (m in activeMissionsList) {
            if (m.isReturned) continue

            val nextElapsed = m.elapsedSeconds + 1
            val currentPhaseElapsed = m.currentPhaseElapsed + 1

            var nextPhase = m.phase
            var nextPhaseElapsed = currentPhaseElapsed
            val phaseLogs = mutableListOf<String>()

            when (m.phase) {
                "TRAVEL" -> {
                    if (currentPhaseElapsed >= m.travelDuration) {
                        nextPhase = "DESCENT"
                        nextPhaseElapsed = 0L
                        phaseLogs.add("[TELEMETRY] Arrived at boundary. Initiating descent.")
                    }
                }
                "DESCENT" -> {
                    if (currentPhaseElapsed >= m.descentDuration) {
                        nextPhase = "HARVESTING"
                        nextPhaseElapsed = 0L
                        phaseLogs.add("[TELEMETRY] Depth reached. Initiating extraction.")
                    }
                }
                "HARVESTING" -> {
                    if (currentPhaseElapsed >= m.harvestDuration) {
                        nextPhase = "ASCENT"
                        nextPhaseElapsed = 0L
                        phaseLogs.add("[TELEMETRY] Extraction complete. Commencing ascent.")
                    }
                }
                "ASCENT" -> {
                    if (currentPhaseElapsed >= m.ascentDuration) {
                        nextPhase = "TRANST_BACK"
                        nextPhaseElapsed = 0L
                        phaseLogs.add("[TELEMETRY] Boundary reached. Transitioning to transit.")
                    }
                }
                "TRANST_BACK" -> {
                    if (currentPhaseElapsed >= m.transitBackDuration) {
                        nextPhase = "COMPLETED"
                        nextPhaseElapsed = 0L
                        phaseLogs.add("[COMPLETE] Payload returned.")
                    }
                }
            }

            val isCompleted = nextPhase == "COMPLETED"

            val waveConfig = WaveMath.getDailyWaveConfig(System.currentTimeMillis())
            val waveCouplingVal = WaveMath.getSpectrumWaveCoupling(System.currentTimeMillis())
            val mutationInterval = DeploymentEngine.calculateMutationInterval(m.stalledDepth, waveConfig.lunarAge)

            val newLogs = m.missionLogs.toMutableList()
            val newHarvestedPayload = m.harvestedPayload.toMutableList()
            phaseLogs.forEach { log ->
                newLogs.add(log)
            }

            var resolvedPhase = nextPhase
            var resolvedPhaseElapsed = nextPhaseElapsed
            var resolvedCompleted = isCompleted

            if (!resolvedCompleted && (m.phase == "DESCENT" || m.phase == "HARVESTING" || m.phase == "ASCENT")) {
                val creature = creatureRepository.getCreatureById(m.creatureId)
                if (creature != null) {
                    val landingLog = m.missionLogs.find { it.startsWith("[LANDING_COORD] ") }
                    val (landingLat, landingLng) = if (landingLog != null) {
                        val coords = landingLog.substringAfter("[LANDING_COORD] ").split(",")
                        coords[0].toDouble() to coords[1].toDouble()
                    } else {
                        m.lat to m.lng
                    }

                    val anomaly = getRawAnomalies().find { Math.abs(it.lat - m.lat) < 0.0001 && Math.abs(it.lng - m.lng) < 0.0001 }
                        ?: PoxAnomaly(
                            id = "ANM-MUTATED",
                            name = "Mutated Anomaly",
                            lat = m.lat,
                            lng = m.lng,
                            gene = m.harvestedPayload.firstOrNull() ?: "AGTCGTAC",
                            faction = "Anomaly",
                            distance = 0.0,
                            heatZoneDiameter = 500.0 * 3.28084,
                            density = 0.0,
                        )

                    val overlappingWells = getCelestialWells().filter { well -> well.contains(landingLat, landingLng) }

                    var currentCreature: Creature = creature
                    val isLoggingTick = nextElapsed % 5 == 0L

                    val anomalyResult = ConflictEngine.simulateCreatureVsEnvironment(
                        creature = currentCreature,
                        hazard = anomaly,
                        userLat = landingLat,
                        userLng = landingLng,
                        totalTicks = 1,
                        lunarAge = waveConfig.lunarAge,
                        waveCoupling = waveCouplingVal,
                    )
                    currentCreature = currentCreature.copy(telomeres = anomalyResult.finalTelomeres)

                    anomalyResult.logs.forEach { log ->
                        if (log.startsWith("Tick ") && isLoggingTick) {
                            newLogs.add("[HAZARD] Anomaly: $log")
                        } else if (log.contains("Coherence Shield") && currentPhaseElapsed == 1L) {
                            newLogs.add(log)
                        }
                    }

                    val isHarvestingCompleteTick = m.phase == "HARVESTING" && nextPhase == "ASCENT"
                    if (isHarvestingCompleteTick && anomalyResult.harvestedPayload.isNotEmpty()) {
                        newHarvestedPayload.addAll(anomalyResult.harvestedPayload)
                    }

                    var survived = anomalyResult.survived
                    if (survived) {
                        for (well in overlappingWells) {
                            val wellResult = ConflictEngine.simulateCreatureVsEnvironment(
                                creature = currentCreature,
                                hazard = well,
                                userLat = landingLat,
                                userLng = landingLng,
                                totalTicks = 1,
                                lunarAge = waveConfig.lunarAge,
                                waveCoupling = waveCouplingVal,
                            )
                            currentCreature = currentCreature.copy(telomeres = wellResult.finalTelomeres)

                            wellResult.logs.forEach { log ->
                                if (log.startsWith("Tick ") && isLoggingTick) {
                                    newLogs.add("[HAZARD] ${well.name}: $log")
                                } else if (log.contains("Coherence Shield") && currentPhaseElapsed == 1L) {
                                    newLogs.add(log)
                                }
                            }

                            if (isHarvestingCompleteTick && wellResult.harvestedPayload.isNotEmpty()) {
                                newHarvestedPayload.addAll(wellResult.harvestedPayload)
                            }

                            if (!wellResult.survived) {
                                survived = false
                                break
                            }
                        }
                    }

                    creatureRepository.insertCreature(currentCreature)

                    if (currentCreature.telomeres <= 0) {
                        newLogs.add("[FATAL] Specimen's telomeres completely decayed under extreme environmental hazard. Mission aborted.")
                        resolvedPhase = "COMPLETED"
                        resolvedPhaseElapsed = 0L
                        resolvedCompleted = true
                    }

                    if (!resolvedCompleted && nextElapsed % mutationInterval == 0L) {
                        val bases = listOf("A", "G", "T", "C")
                        val chars = currentCreature.sequence.toCharArray()
                        val mutationIndex = (Math.random() * chars.size).toInt()
                        val oldBase = chars[mutationIndex].toString()
                        val choices = bases.filter { it != oldBase }
                        val newBase = choices[(Math.random() * choices.size).toInt()]
                        chars[mutationIndex] = newBase[0]
                        val mutatedSequence = String(chars)

                        val proc = compileDeterministicOffline(mutatedSequence)
                        val updatedCreature = currentCreature.copy(
                            sequence = mutatedSequence,
                            vitality = proc.vitality,
                            attack = proc.attack,
                            defense = proc.defense,
                            speed = proc.speed,
                            faction = proc.faction,
                            type = proc.type,
                            primaryWeapon = proc.primaryWeapon,
                            lore = proc.lore,
                            isMutated = true,
                            originalSequence = currentCreature.originalSequence ?: m.originalSequence ?: currentCreature.sequence,
                        )
                        creatureRepository.insertCreature(updatedCreature)
                        newLogs.add("[MUTATION] Pos $mutationIndex: $oldBase -> $newBase. Stats re-calculated.")
                    }
                }
            }

            val updatedMission = m.copy(
                elapsedSeconds = nextElapsed,
                isCompleted = resolvedCompleted,
                missionLogs = newLogs,
                phase = resolvedPhase,
                currentPhaseElapsed = resolvedPhaseElapsed,
                harvestedPayload = newHarvestedPayload,
            )
            if (resolvedCompleted) {
                recallMission(updatedMission)
            } else {
                missionRepository.insertMission(updatedMission)
            }
        }
    }

    fun compileDeterministicOffline(sequence: String, reagent: String? = null): Creature {
        val upperSeq = sequence.uppercase()
        var aCount = 0
        var gCount = 0
        var tCount = 0
        var cCount = 0
        for (char in upperSeq) {
            when (char) {
                'A' -> aCount++
                'G' -> gCount++
                'T' -> tCount++
                'C' -> cCount++
            }
        }

        var aCountFaction = aCount
        var gCountFaction = gCount
        var tCountFaction = tCount
        var cCountFaction = cCount

        when (reagent) {
            "REAGENT_ALIGN_CONTAINMENT" -> cCountFaction += 15
            "REAGENT_ALIGN_INFECTION" -> aCountFaction += 15
            "REAGENT_ALIGN_MECH" -> gCountFaction += 15
            "REAGENT_ALIGN_PARASITE" -> tCountFaction += 15
        }

        val faction = when (maxOf(aCountFaction, gCountFaction, tCountFaction, cCountFaction)) {
            aCountFaction -> "Infection"
            gCountFaction -> "Mech"
            tCountFaction -> "Parasite"
            else -> "Containment"
        }

        val type = when (faction) {
            "Infection" -> "Acidic Virus Swarm"
            "Mech" -> "Heavy Metal Shell"
            "Parasite" -> "Neural Parasite"
            else -> "Bio-Containment Unit"
        }

        val baseSeed = upperSeq.hashCode()
        val random = java.util.Random(baseSeed.toLong())

        val vitality = 100 + aCount * 5
        val attack = 20 + gCount * 2
        val defense = 20 + cCount * 2
        val speed = 20 + tCount * 2

        val name = when (faction) {
            "Infection" -> "Toxipod-${random.nextInt(99)}"
            "Mech" -> "Chitin-Shell-${random.nextInt(99)}"
            "Parasite" -> "CyberVex-${random.nextInt(99)}"
            else -> "SectorGuard-${random.nextInt(99)}"
        }

        val weapon = when (faction) {
            "Infection" -> "Acid Spurt-Needle"
            "Mech" -> "Titanium Piston Jaw"
            "Parasite" -> "Synapse Siphon"
            else -> "Plasma Shield Mesh"
        }

        val lore = "Deterministic offline specimen. Gene sequence frequencies reveal highly stable $faction bindings, evolving standard operational designation as a $type."

        val grid = StringBuilder()
        for (r in 0 until 5) {
            for (col in 0 until 5) {
                val valRand = random.nextInt(4)
                val char = when (valRand) {
                    0 -> "."
                    1 -> "o"
                    2 -> "x"
                    else -> "#"
                }
                grid.append(char)
            }
            if (r < 4) grid.append("\n")
        }

        return Creature(
            id = UUID.randomUUID().toString(),
            sequence = sequence,
            name = name,
            faction = faction,
            type = type,
            vitality = vitality,
            attack = attack,
            defense = defense,
            speed = speed,
            primaryWeapon = weapon,
            lore = lore,
            asciiArt = grid.toString(),
            discoveredAt = System.currentTimeMillis(),
            origin = "Local Compiled Unit",
        )
    }

    private fun calculateDistanceInFeet(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double = GeoMath.calculateHaversineDistanceInFeet(lat1, lng1, lat2, lng2)
}
