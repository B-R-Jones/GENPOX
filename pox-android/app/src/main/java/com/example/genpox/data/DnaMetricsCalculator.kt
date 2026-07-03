package com.example.genpox.data

object DnaMetricsCalculator {

    data class GeneInventoryMetrics(
        val geneSequenceStrings: List<String> = emptyList(),
        val uniqueGenesSize: Int = 0,
        val multiCountGenesSize: Int = 0,
        val anomalousGenesList: List<GeneSequence> = emptyList(),
        val grandTotalStandardNucleotides: Long = 0L,
        val countA: Int = 0,
        val countG: Int = 0,
        val countT: Int = 0,
        val countC: Int = 0
    )

    fun calculateInventoryMetrics(list: List<GeneSequence>): GeneInventoryMetrics {
        var countA = 0
        var countG = 0
        var countT = 0
        var countC = 0
        var grandTotal = 0L
        var multiCount = 0
        val strings = ArrayList<String>(list.size)
        val anomalousList = ArrayList<GeneSequence>()

        for (i in list.indices) {
            val gene = list[i]
            val seq = gene.sequence
            strings.add(seq)
            if (gene.count > 1) {
                multiCount++
            }

            val isAnomalous = WaveMath.isAnomalousGene(seq)
            if (isAnomalous) {
                anomalousList.add(gene)
            } else {
                if (gene.count > 0) {
                    grandTotal += seq.length
                    for (cIdx in 0 until seq.length) {
                        when (seq[cIdx]) {
                            'A', 'a' -> countA += gene.count
                            'G', 'g' -> countG += gene.count
                            'T', 't' -> countT += gene.count
                            'C', 'c' -> countC += gene.count
                        }
                    }
                }
            }
        }

        return GeneInventoryMetrics(
            geneSequenceStrings = strings,
            uniqueGenesSize = list.size,
            multiCountGenesSize = multiCount,
            anomalousGenesList = anomalousList,
            grandTotalStandardNucleotides = grandTotal,
            countA = countA,
            countG = countG,
            countT = countT,
            countC = countC
        )
    }
}
