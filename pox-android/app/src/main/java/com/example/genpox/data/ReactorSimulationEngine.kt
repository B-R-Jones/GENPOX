package com.example.genpox.data

import java.util.Random

object ReactorSimulationEngine {

    data class StepResult(
        val step: Int,
        val accumulatedStrand: String,
        val qScore: Double,
        val tm: Double,
        val mfe: Double,
        val isMutated: Boolean = false,
        val mutationMessage: String? = null,
        val isCollapsed: Boolean = false,
        val collapseReason: String = ""
    )

    data class RunResult(
        val finalStrand: String,
        val qScore: Double,
        val tm: Double,
        val mfe: Double,
        val steps: List<StepResult>,
        val isCollapsed: Boolean,
        val collapseReason: String = ""
    )

    suspend fun runSynthesis(
        target: String,
        temp: Float,
        salt: Double,
        poly: String,
        solute: String,
        inletRatioA: Float,
        inletRatioG: Float,
        inletRatioT: Float,
        inletRatioC: Float,
        stepIntervalMs: Long,
        onStep: (StepResult) -> Unit
    ): RunResult {
        val sumInlets = inletRatioA + inletRatioG + inletRatioT + inletRatioC
        val normI_A = if (sumInlets > 0) inletRatioA / sumInlets else 0.25f
        val normI_G = if (sumInlets > 0) inletRatioG / sumInlets else 0.25f
        val normI_T = if (sumInlets > 0) inletRatioT / sumInlets else 0.25f
        val normI_C = if (sumInlets > 0) inletRatioC / sumInlets else 0.25f

        val targetLen = target.length.toDouble()
        val ratioA = target.count { it == 'A' }.toDouble() / targetLen
        val ratioG = target.count { it == 'G' }.toDouble() / targetLen
        val ratioT = target.count { it == 'T' }.toDouble() / targetLen
        val ratioC = target.count { it == 'C' }.toDouble() / targetLen

        val deviation = Math.abs(normI_A - ratioA) +
                        Math.abs(normI_G - ratioG) +
                        Math.abs(normI_T - ratioT) +
                        Math.abs(normI_C - ratioC)
        val penalty = (deviation * 15.0).coerceAtMost(15.0)

        var finalStrand = ""
        var currentQScore = 0.0
        val stepsList = mutableListOf<StepResult>()
        val random = Random()

        for (step in 1..8) {
            kotlinx.coroutines.delay(stepIntervalMs)

            val expectedChar = target[step - 1]
            var incorporatedChar = expectedChar
            var isMutated = false
            var mutationMessage: String? = null

            if (!poly.equals("PFU", ignoreCase = true)) {
                val rawRatio = when (expectedChar) {
                    'A' -> normI_A
                    'G' -> normI_G
                    'T' -> normI_T
                    'C' -> normI_C
                    else -> 1.0f
                }
                val inletRatio = if (poly.equals("TAQ", ignoreCase = true) && temp < 25.0f) {
                    rawRatio * 0.5f
                } else {
                    rawRatio
                }
                val roll = random.nextDouble()
                if (roll > inletRatio) {
                    isMutated = true
                    val options = listOf('A', 'G', 'T', 'C').filter { it != expectedChar }
                    val otherWeights = options.map { o ->
                        when (o) {
                            'A' -> normI_A.toDouble()
                            'G' -> normI_G.toDouble()
                            'T' -> normI_T.toDouble()
                            'C' -> normI_C.toDouble()
                            else -> 0.0
                        }
                    }
                    val sumOthers = otherWeights.sum()
                    incorporatedChar = if (sumOthers > 0.0) {
                        val rOthers = random.nextDouble() * sumOthers
                        var accOthers = 0.0
                        var sel = options.first()
                        for (idx in options.indices) {
                            accOthers += otherWeights[idx]
                            if (rOthers < accOthers) {
                                sel = options[idx]
                                break
                            }
                        }
                        sel
                    } else {
                        options[random.nextInt(options.size)]
                    }
                    mutationMessage = "CROSSTALK INTRUSION: Mismatched inlet ratios caused mutation at step $step! Incorporated $incorporatedChar instead of $expectedChar."
                }
            }

            finalStrand += incorporatedChar
            val tm = BiophysicsEngine.calculateMeltingTemperature(finalStrand, salt)
            val mfe = BiophysicsEngine.calculateMinimumFreeEnergy(finalStrand)

            val baseQ = BiophysicsEngine.calculatePhredQScore(poly, random)
            val targetTmVal = BiophysicsEngine.calculateMeltingTemperature(target, salt)
            val diff = temp.toDouble() - targetTmVal
            val sigma = when (poly.uppercase()) {
                "TAQ" -> 15.0
                "PFU" -> 5.0
                "TTH" -> 25.0
                else -> 15.0
            }
            val eta = Math.exp(-(diff * diff) / (2.0 * sigma * sigma))
            val baseStepQ = (baseQ * eta - penalty).coerceIn(5.0, 40.0)
            val newQ = if (step == 1) baseStepQ else (currentQScore * (step - 1) + baseStepQ) / step
            currentQScore = newQ.coerceIn(5.0, 40.0)

            var failureTriggered = false
            var collapseReason = ""

            val inletVal = when (expectedChar) {
                'A' -> inletRatioA
                'G' -> inletRatioG
                'T' -> inletRatioT
                'C' -> inletRatioC
                else -> 0f
            }
            if (inletVal <= 0.05f) {
                failureTriggered = true
                collapseReason = "inlet deprivation ($expectedChar flow restricted to ${String.format(java.util.Locale.US, "%.0f%%", inletVal * 100)})"
            }

            val stallThreshold = 28.0f + (salt.toFloat() * 30.0f)
            if (!failureTriggered && finalStrand.length >= 4 && temp < stallThreshold && mfe <= -5.0 && solute != "DMSO") {
                failureTriggered = true
                collapseReason = "stalling (GC hairpin secondary fold stabilized by salt at ${String.format(java.util.Locale.US, "%.1f", stallThreshold)}°C)"
            }

            val denatureThreshold = 22.0f + (salt.toFloat() * 30.0f)
            val currentGcPercent = finalStrand.count { it == 'G' || it == 'C' }.toDouble() / finalStrand.length.toDouble()
            if (!failureTriggered && finalStrand.length >= 4 && temp > denatureThreshold && currentGcPercent < 0.40 && solute != "Netropsin") {
                failureTriggered = true
                collapseReason = "denaturation (AT bonds denatured due to low salt stabilization at ${String.format(java.util.Locale.US, "%.1f", denatureThreshold)}°C)"
            }

            if (!failureTriggered) {
                if (poly.equals("PFU", ignoreCase = true) && temp > 40.0f) {
                    failureTriggered = true
                    collapseReason = "enzyme denaturation (PFU denatured at ${String.format(java.util.Locale.US, "%.1f", temp)}°C, limit is 40.0°C)"
                } else if (poly.equals("TAQ", ignoreCase = true) && temp > 45.0f) {
                    failureTriggered = true
                    collapseReason = "enzyme denaturation (TAQ denatured at ${String.format(java.util.Locale.US, "%.1f", temp)}°C, limit is 45.0°C)"
                }
            }

            if (!failureTriggered && poly.equals("TTH", ignoreCase = true) && temp < 25.0f) {
                failureTriggered = true
                collapseReason = "cold inactivation (TTH deactivated at ${String.format(java.util.Locale.US, "%.1f", temp)}°C, limit is 25.0°C)"
            }

            val stepRes = StepResult(
                step = step,
                accumulatedStrand = finalStrand,
                qScore = currentQScore,
                tm = tm,
                mfe = mfe,
                isMutated = isMutated,
                mutationMessage = mutationMessage,
                isCollapsed = failureTriggered,
                collapseReason = collapseReason
            )
            stepsList.add(stepRes)
            onStep(stepRes)

            if (failureTriggered) {
                return RunResult(
                    finalStrand = finalStrand,
                    qScore = currentQScore,
                    tm = tm,
                    mfe = mfe,
                    steps = stepsList,
                    isCollapsed = true,
                    collapseReason = collapseReason
                )
            }
        }

        return RunResult(
            finalStrand = finalStrand,
            qScore = currentQScore,
            tm = BiophysicsEngine.calculateMeltingTemperature(finalStrand, salt),
            mfe = BiophysicsEngine.calculateMinimumFreeEnergy(finalStrand),
            steps = stepsList,
            isCollapsed = false
        )
    }
}
