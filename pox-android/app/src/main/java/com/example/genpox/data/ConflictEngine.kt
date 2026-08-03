package com.example.genpox.data

import java.util.Locale
import java.util.Random

object ConflictEngine {

    data class Combatant(
        val name: String,
        val faction: String,
        val sequence: String,
        val maxHp: Int,
        var currentHp: Int,
        val attack: Int,
        val defense: Int,
        val speed: Int,
        val anomalousBenefits: List<WaveMath.AnomalousBenefit> = emptyList(),
        val moveset: List<String> = emptyList(),
        var isEvadedNext: Boolean = false,
        var isDeflectedThisRound: Boolean = false
    )

    data class CombatResult(
        val winnerName: String,
        val winnerFaction: String,
        val wasAttackerVictor: Boolean,
        val totalTurns: Int,
        val logs: List<String>,
        val finalAttackerHp: Int,
        val finalDefenderHp: Int
    )

    data class EnvironmentSimulationResult(
        val survived: Boolean,
        val finalTelomeres: Int,
        val ticksSpent: Int,
        val decaySuffered: Double,
        val harvestedPayload: List<String>,
        val logs: List<String>
    )

    data class HackingResult(
        val success: Boolean,
        val logs: List<String>,
        val siphonedPayload: List<String>,
        val finalHackerTelomeres: Int,
        val directCombatResult: CombatResult? = null
    )

    /**
     * Initializes a Combatant state from a Creature model.
     */
    fun initializeCombatant(creature: Creature, phaseName: String, waveCoupling: Double): Combatant {
        val factor = 0.25 + 0.75 * (creature.telomeres.toDouble() / 100.0)
        val maxHp = maxOf(10, Math.round(creature.vitality * factor).toInt())
        val attack = maxOf(5, Math.round(creature.attack * factor).toInt())
        val defenseBase = maxOf(5, Math.round(creature.defense * factor).toInt())
        val speed = maxOf(5, Math.round(creature.speed * factor).toInt())

        val defense = DeploymentEngine.calculateEffectiveDefense(defenseBase, creature.faction, phaseName)
        val benefits = WaveMath.getAnomalousBenefits(creature.sequence)

        val moves = mutableListOf<String>()
        if (creature.sequence.length >= 72) {
            val block1 = creature.sequence.substring(64, 72)
            val countAG = block1.count { it == 'A' || it == 'G' }
            val countTC = block1.count { it == 'T' || it == 'C' }
            val slot1Move = if (countAG >= countTC) "BIO_DRAIN_REPAIR" else "QUANTUM_ESCAPE_DEVIATION"
            moves.add(slot1Move)

            if (creature.sequence.length >= 80) {
                val slot2Move = if (slot1Move == "BIO_DRAIN_REPAIR") "ELECTROMAGNETIC_SHELL_DEFLECT" else "MICRO_PHAGE_EXTRACTION"
                moves.add(slot2Move)
            }
        }

        return Combatant(
            name = creature.name,
            faction = creature.faction,
            sequence = creature.sequence,
            maxHp = maxHp,
            currentHp = maxHp,
            attack = attack,
            defense = defense,
            speed = speed,
            anomalousBenefits = benefits,
            moveset = moves
        )
    }

    /**
     * Initializes an Anomaly Core combatant based on metadata.
     */
    fun initializeAnomalyCore(anomaly: PoxAnomaly): Combatant {
        val maxHp = (120 + anomaly.density * 60 + anomaly.heatZoneDiameter / 10).toInt().coerceIn(100, 250)
        val attack = (20 + anomaly.density * 40).toInt().coerceIn(10, 99)
        val defense = (20 + anomaly.density * 40).toInt().coerceIn(10, 99)
        val speed = (20 + anomaly.density * 40).toInt().coerceIn(10, 99)
        val repeatedSeq = anomaly.gene.repeat(10)
        val benefits = WaveMath.getAnomalousBenefits(repeatedSeq)

        val moves = mutableListOf<String>()
        val block1 = repeatedSeq.substring(64, 72)
        val countAG = block1.count { it == 'A' || it == 'G' }
        val countTC = block1.count { it == 'T' || it == 'C' }
        val slot1Move = if (countAG >= countTC) "BIO_DRAIN_REPAIR" else "QUANTUM_ESCAPE_DEVIATION"
        moves.add(slot1Move)
        val slot2Move = if (slot1Move == "BIO_DRAIN_REPAIR") "ELECTROMAGNETIC_SHELL_DEFLECT" else "MICRO_PHAGE_EXTRACTION"
        moves.add(slot2Move)

        return Combatant(
            name = "Anomaly Core ${anomaly.name}",
            faction = anomaly.faction,
            sequence = repeatedSeq,
            maxHp = maxHp,
            currentHp = maxHp,
            attack = attack,
            defense = defense,
            speed = speed,
            anomalousBenefits = benefits,
            moveset = moves
        )
    }

    /**
     * PIPELINE 1: Creature vs. Creature (Standard Arena Turn-Based)
     */
    fun simulateCreatureVsCreature(
        attackerCreature: Creature,
        defenderCreature: Creature,
        phaseName: String,
        waveCoupling: Double,
        seed: Long = 42L
    ): CombatResult {
        val random = Random(seed)
        val attacker = initializeCombatant(attackerCreature, phaseName, waveCoupling)
        val defender = initializeCombatant(defenderCreature, phaseName, waveCoupling)

        val logs = mutableListOf<String>()
        logs.add("Combat Initiated: Attacker ${attacker.name} (${attacker.faction}) vs Defender ${defender.name} (${defender.faction}).")
        logs.add("Attacker effective stats: HP ${attacker.maxHp}, ATK ${attacker.attack}, DEF ${attacker.defense}, SPD ${attacker.speed}.")
        logs.add("Defender effective stats: HP ${defender.maxHp}, ATK ${defender.attack}, DEF ${defender.defense}, SPD ${defender.speed}.")

        var turnCount = 0
        while (attacker.currentHp > 0 && defender.currentHp > 0 && turnCount < 50) {
            turnCount++
            logs.add("--- Round $turnCount ---")
            attacker.isDeflectedThisRound = false
            defender.isDeflectedThisRound = false

            val first: Combatant
            val second: Combatant
            if (attacker.speed > defender.speed) {
                first = attacker
                second = defender
            } else if (attacker.speed < defender.speed) {
                first = defender
                second = attacker
            } else {
                first = attacker
                second = defender
            }

            // First actor turn
            executeActorTurn(first, second, turnCount, phaseName, waveCoupling, logs, random)
            if (second.currentHp <= 0) {
                resolveSelfDestruct(second, first, turnCount, phaseName, waveCoupling, logs)
                break
            }

            // Second actor turn
            executeActorTurn(second, first, turnCount, phaseName, waveCoupling, logs, random)
            if (first.currentHp <= 0) {
                resolveSelfDestruct(first, second, turnCount, phaseName, waveCoupling, logs)
                break
            }
        }

        val wasAttackerVictor = attacker.currentHp > 0 && defender.currentHp <= 0
        val winner = if (attacker.currentHp > 0) attacker else defender
        logs.add("Combat Concluded in $turnCount rounds. Winner: ${winner.name} (${winner.faction}).")

        return CombatResult(
            winnerName = winner.name,
            winnerFaction = winner.faction,
            wasAttackerVictor = wasAttackerVictor,
            totalTurns = turnCount,
            logs = logs,
            finalAttackerHp = attacker.currentHp,
            finalDefenderHp = defender.currentHp
        )
    }

    private fun executeActorTurn(
        actor: Combatant,
        target: Combatant,
        turnCount: Int,
        phaseName: String,
        waveCoupling: Double,
        logs: MutableList<String>,
        random: Random
    ) {
        val activePerks = actor.anomalousBenefits.filter {
            isPerkTriggered(it.triggerIndex, actor.currentHp, actor.maxHp, turnCount, phaseName, waveCoupling)
        }
        val targetActivePerks = target.anomalousBenefits.filter {
            isPerkTriggered(it.triggerIndex, target.currentHp, target.maxHp, turnCount, phaseName, waveCoupling)
        }

        // Active Moveset Decision
        val action: String = when {
            actor.moveset.contains("MICRO_PHAGE_EXTRACTION") && actor.currentHp < actor.maxHp * 0.80 && random.nextDouble() < 0.60 -> "MICRO_PHAGE_EXTRACTION"
            actor.moveset.contains("BIO_DRAIN_REPAIR") && actor.currentHp < actor.maxHp * 0.80 && random.nextDouble() < 0.60 -> "BIO_DRAIN_REPAIR"
            actor.moveset.contains("ELECTROMAGNETIC_SHELL_DEFLECT") && !actor.isDeflectedThisRound && random.nextDouble() < 0.40 -> "ELECTROMAGNETIC_SHELL_DEFLECT"
            actor.moveset.contains("QUANTUM_ESCAPE_DEVIATION") && !actor.isEvadedNext && random.nextDouble() < 0.40 -> "QUANTUM_ESCAPE_DEVIATION"
            actor.moveset.isNotEmpty() && random.nextDouble() < 0.35 -> actor.moveset[random.nextInt(actor.moveset.size)]
            else -> "NORMAL_ATTACK"
        }

        when (action) {
            "NORMAL_ATTACK" -> {
                val speedEvasionChance = maxOf(0.0, (target.speed - actor.speed).toDouble()) * 0.25
                val phaseShiftPerk = targetActivePerks.find { it.id == "PHASE_SHIFT" }
                val phaseShiftChance = phaseShiftPerk?.chance ?: 0.0

                when {
                    target.isDeflectedThisRound -> {
                        logs.add("${actor.name} attacks ${target.name}, but it was DEFLECTED by Electromagnetic Shell Deflect.")
                    }
                    target.isEvadedNext -> {
                        target.isEvadedNext = false
                        logs.add("${actor.name} attacks ${target.name}, but it was EVADED by Quantum Escape Deviation.")
                    }
                    phaseShiftChance > 0.0 && random.nextDouble() * 100.0 < phaseShiftChance -> {
                        logs.add("[PERK] ${actor.name} attacks ${target.name}, but it was EVADED by Phase Shift!")
                    }
                    speedEvasionChance > 0.0 && random.nextDouble() * 100.0 < speedEvasionChance -> {
                        logs.add("${actor.name} attacks ${target.name}, but it was EVADED due to speed difference.")
                    }
                    else -> {
                        var outgoing = actor.attack.toDouble()
                        val doubleStrikePerk = activePerks.find { it.id == "DOUBLE_STRIKE" }
                        if (doubleStrikePerk != null) {
                            outgoing *= doubleStrikePerk.magnitude
                            logs.add("[PERK] Double Strike amplified attack damage by ${String.format(Locale.US, "%.2f", doubleStrikePerk.magnitude)}x.")
                        }

                        val damage = maxOf(1, Math.round(outgoing).toInt() - target.defense)
                        target.currentHp = maxOf(0, target.currentHp - damage)
                        logs.add("${actor.name} attacks ${target.name} for $damage damage. (${target.name} HP: ${target.currentHp}/${target.maxHp})")

                        val regenPerk = activePerks.find { it.id == "HEALTH_REGEN" }
                        if (regenPerk != null) {
                            val heal = regenPerk.magnitude.toInt()
                            actor.currentHp = minOf(actor.maxHp, actor.currentHp + heal)
                            logs.add("[PERK] Health Regen restored $heal HP to ${actor.name}. (${actor.name} HP: ${actor.currentHp}/${actor.maxHp})")
                        }
                    }
                }
            }
            "BIO_DRAIN_REPAIR", "MICRO_PHAGE_EXTRACTION" -> {
                val phaseShiftPerk = targetActivePerks.find { it.id == "PHASE_SHIFT" }
                val phaseShiftChance = phaseShiftPerk?.chance ?: 0.0

                when {
                    target.isDeflectedThisRound -> {
                        logs.add("${actor.name} attempts to siphon ${target.name}, but it was DEFLECTED by Electromagnetic Shell Deflect.")
                    }
                    target.isEvadedNext -> {
                        target.isEvadedNext = false
                        logs.add("${actor.name} attempts to siphon ${target.name}, but it was EVADED by Quantum Escape Deviation.")
                    }
                    phaseShiftChance > 0.0 && random.nextDouble() * 100.0 < phaseShiftChance -> {
                        logs.add("[PERK] ${actor.name} attempts to siphon ${target.name}, but it was EVADED by Phase Shift!")
                    }
                    else -> {
                        val damage = 35
                        target.currentHp = maxOf(0, target.currentHp - damage)
                        val healed = minOf(actor.maxHp - actor.currentHp, damage)
                        actor.currentHp += healed
                        val moveName = if (action == "BIO_DRAIN_REPAIR") "Bio-Drain Repair" else "Micro-Phage Extraction"
                        logs.add("${actor.name} executed $moveName, siphoning $damage HP from ${target.name} and mending $healed HP. (${target.name} HP: ${target.currentHp}/${target.maxHp})")
                    }
                }
            }
            "QUANTUM_ESCAPE_DEVIATION" -> {
                actor.isEvadedNext = true
                logs.add("${actor.name} executed Quantum Escape Deviation, preparing an evasive shift for the next attack.")
            }
            "ELECTROMAGNETIC_SHELL_DEFLECT" -> {
                actor.isDeflectedThisRound = true
                logs.add("${actor.name} executed Electromagnetic Shell Deflect, erecting a deflection shield for this round.")
            }
        }
    }

    private fun resolveSelfDestruct(
        defeated: Combatant,
        victor: Combatant,
        turnCount: Int,
        phaseName: String,
        waveCoupling: Double,
        logs: MutableList<String>
    ) {
        val activePerks = defeated.anomalousBenefits.filter {
            isPerkTriggered(it.triggerIndex, defeated.currentHp, defeated.maxHp, turnCount, phaseName, waveCoupling)
        }
        val selfDestructPerk = activePerks.find { it.id == "SELF_DESTRUCT" }
        if (selfDestructPerk != null) {
            val flatDmg = selfDestructPerk.magnitude.toInt()
            victor.currentHp = maxOf(0, victor.currentHp - flatDmg)
            logs.add("[PERK] ${defeated.name} triggered Self-Destruct, dealing $flatDmg flat damage to ${victor.name}! (${victor.name} HP: ${victor.currentHp}/${victor.maxHp})")
        }
    }

    private fun isPerkTriggered(
        triggerIndex: Int,
        currentHp: Int,
        maxHp: Int,
        turnCount: Int,
        phaseName: String,
        waveCoupling: Double
    ): Boolean {
        return when (triggerIndex) {
            0 -> true
            1 -> phaseName.contains("New", true) || phaseName.contains("Crescent", true) || phaseName.contains("Quarter", true)
            2 -> phaseName.contains("Full", true) || phaseName.contains("Gibbous", true)
            3 -> currentHp < maxHp * 0.40
            4 -> currentHp > maxHp * 0.70
            5 -> turnCount <= 3
            6 -> turnCount > 6
            7 -> waveCoupling > 82.0
            else -> true
        }
    }

    /**
     * PIPELINE 2: Creature vs. Environment (Geographical Hazard Simulation)
     */
    fun simulateCreatureVsEnvironment(
        creature: Creature,
        hazard: GeographicHazard,
        userLat: Double,
        userLng: Double,
        totalTicks: Int,
        lunarAge: Double,
        waveCoupling: Double
    ): EnvironmentSimulationResult {
        val logs = mutableListOf<String>()
        val siphonedPayload = mutableListOf<String>()
        var currentTelomeres = creature.telomeres
        var totalDecaySuffered = 0.0

        val inside = hazard.contains(userLat, userLng)
        if (!inside) {
            logs.add("Coordinates ($userLat, $userLng) are outside the hazard ${hazard.id} boundary. Sim aborted.")
            return EnvironmentSimulationResult(
                survived = true,
                finalTelomeres = currentTelomeres,
                ticksSpent = 0,
                decaySuffered = 0.0,
                harvestedPayload = emptyList(),
                logs = logs
            )
        }

        val benefits = WaveMath.getAnomalousBenefits(creature.sequence)
        val hasCoherenceShield = benefits.any { it.id == "COHERENCE_SHIELD" }

        val ticksToSimulate = if (hasCoherenceShield) {
            val reduced = maxOf(1, (totalTicks * 0.7).toInt())
            logs.add("[PERK] Coherence Shield active! Environmental drag bypassed. Total ticks reduced from $totalTicks to $reduced.")
            reduced
        } else {
            totalTicks
        }

        val resistance = when (hazard.hazardType.uppercase()) {
            "HEAT" -> creature.heatResistance
            "RADIATION" -> creature.radResistance
            "KINETIC" -> creature.kineticResistance
            else -> 0.0
        }
        logs.add("Environment Hazard: ${hazard.id} (${hazard.hazardType}). Base Resistance: ${(resistance * 100).toInt()}%.")

        for (tick in 1..ticksToSimulate) {
            val dist = hazard.distanceTo(userLat, userLng)
            val radius = if (hazard is PoxAnomaly) {
                hazard.getBoundaryRadiusForPlayer(userLat, userLng)
            } else if (hazard is CelestialWell) {
                hazard.radiusFeet
            } else {
                1000.0
            }
            val ratio = if (radius > 0.0) (dist / radius).coerceIn(0.0, 1.0) else 1.0
            val hazardPower = hazard.getHazardPowerAt(userLat, userLng, lunarAge)

            val baseDamage = 8.0 * (1.0 - ratio) * hazardPower
            val finalDamage = baseDamage * (1.0 - resistance)
            currentTelomeres = maxOf(0, currentTelomeres - Math.round(finalDamage).toInt())
            totalDecaySuffered += finalDamage

            if (tick == 1 || tick == ticksToSimulate || tick % 10 == 0) {
                logs.add("Tick $tick: Suffered ${String.format(Locale.US, "%.2f", finalDamage)} decay. Telomeres: $currentTelomeres%.")
            }

            if (currentTelomeres <= 0) {
                logs.add("Tick $tick: Telomeres depleted to 0%. Specimen disintegrated under extreme environmental hazard.")
                break
            }
        }

        val survived = currentTelomeres > 0
        if (survived) {
            logs.add("Specimen survived the hazard dispatch run. Final Telomeres: $currentTelomeres%.")
            val harvestBoostPerk = benefits.find { it.id == "HARVEST_BOOST" }
            val dailyConfig = WaveMath.getDailyWaveConfig(System.currentTimeMillis())
            val isHarvestBoostActive = harvestBoostPerk != null && isPerkTriggered(
                harvestBoostPerk.triggerIndex,
                100, 100,
                ticksToSimulate,
                dailyConfig.phaseName,
                waveCoupling
            )

            val payload = hazard.generateHarvestPayload(creature, waveCoupling, isHarvestBoostActive)
            siphonedPayload.addAll(payload)
            logs.add("Harvest operation complete. Extraction payload collected: $payload")
        }

        return EnvironmentSimulationResult(
            survived = survived,
            finalTelomeres = currentTelomeres,
            ticksSpent = ticksToSimulate,
            decaySuffered = totalDecaySuffered,
            harvestedPayload = siphonedPayload,
            logs = logs
        )
    }

    /**
     * PIPELINE 3: Creature vs. Player Node (Firewall Hacking)
     */
    fun simulateCreatureVsPlayerNode(
        hacker: Creature,
        targetNodeId: String,
        defender: Creature?,
        vaultAutoDefenders: List<Creature>,
        baseFirewallComplexity: Double,
        phaseName: String,
        waveCoupling: Double,
        seed: Long = 42L
    ): HackingResult {
        val logs = mutableListOf<String>()
        val siphoned = mutableListOf<String>()

        var combatResult: CombatResult? = null
        var hackerTelomeres = hacker.telomeres

        if (defender != null) {
            logs.add("Hacking attempt on Node $targetNodeId detected. Defender interception triggered!")
            val result = simulateCreatureVsCreature(hacker, defender, phaseName, waveCoupling, seed)
            combatResult = result
            logs.addAll(result.logs)

            if (!result.wasAttackerVictor) {
                hackerTelomeres = maxOf(0, hacker.telomeres - 15)
                logs.add("Hacker defeated in battle. Decryption failed. Feedback shorted hacker telomeres to $hackerTelomeres%.")
                return HackingResult(
                    success = false,
                    logs = logs,
                    siphonedPayload = emptyList(),
                    finalHackerTelomeres = hackerTelomeres,
                    directCombatResult = combatResult
                )
            } else {
                logs.add("Defender node security defeated. Initiating node firewall compromise phase.")
            }
        }

        // Firewall Complexity Aggregation
        val vaultAutoDefenderSum = vaultAutoDefenders.filter { it.isAutoDefender }.sumOf { it.defense.toDouble() }
        val firewallComplexity = baseFirewallComplexity + vaultAutoDefenderSum * 2.0
        logs.add("Target Firewall Complexity: $firewallComplexity (Base: $baseFirewallComplexity, Vault Auto-Defender Defense Sum: $vaultAutoDefenderSum).")

        // Decryption Rate Calculations
        val factor = 0.25 + 0.75 * (hackerTelomeres.toDouble() / 100.0)
        val scaledSpeed = maxOf(5.0, Math.round(hacker.speed * factor).toDouble())
        val scaledAttack = maxOf(5.0, Math.round(hacker.attack * factor).toDouble())
        var decryptionRate = (scaledSpeed * 0.4 + scaledAttack * 0.2) / firewallComplexity

        // Heliocentric alignment modifiers
        val alignmentBase = when (hacker.faction) {
            "Infection" -> 'A'
            "Mech" -> 'G'
            "Parasite" -> 'T'
            else -> 'C'
        }
        val alignState = WaveMath.getBaseAlignmentState(alignmentBase, WaveMath.getAdjustedTime())
        val alignMult = when (alignState) {
            WaveMath.AlignmentState.CONJUNCTION -> 1.20
            WaveMath.AlignmentState.OPPOSITION -> 0.90
            WaveMath.AlignmentState.QUADRATURE -> 1.10
            else -> 1.0
        }
        decryptionRate *= alignMult
        logs.add("Planetary resonance alignment for base $alignmentBase is $alignState. Rate multiplier: ${alignMult}x.")

        var progress = 0.0
        var ticks = 0
        while (progress < 100.0 && ticks < 30) {
            ticks++
            progress += decryptionRate * 15.0
        }

        logs.add("Decryption phase ended after $ticks cycles. Final Progress: ${String.format(Locale.US, "%.1f", progress)}%.")

        val success = progress >= 100.0
        if (success) {
            logs.add("Terminal decryption success! Compromised node data block.")
            val randomSeq = WaveMath.generateWaveGeneBlock(null, Random(seed))
            siphoned.add(randomSeq)
        } else {
            hackerTelomeres = maxOf(0, hackerTelomeres - 10)
            logs.add("Decryption failed due to trace detection. Feedback decay shorted hacker telomeres to $hackerTelomeres%.")
        }

        return HackingResult(
            success = success,
            logs = logs,
            siphonedPayload = siphoned,
            finalHackerTelomeres = hackerTelomeres,
            directCombatResult = combatResult
        )
    }
}
