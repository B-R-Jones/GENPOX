package com.example.genpox.ui.scanner.map

/**
 * Evaluates smooth anomaly color gradients and pulses for real-time GPU rendering.
 */
object AnomalyPassRenderer {

    data class AnomalyColorRgb(
        val sr: Int,
        val sg: Int,
        val sb: Int,
        val fr: Int,
        val fg: Int,
        val fb: Int,
    )

    fun evaluateSmoothAnomalyColors(progress: Float, ambientPulse: Float): AnomalyColorRgb {
        val p = progress.coerceIn(0f, 0.9999f)
        val flashState = ((ambientPulse + 1f) * 0.5f).coerceIn(0f, 1f)

        // Palette 1: Inception (Electric Aqua / Cyan)
        val p1 = AnomalyColorRgb(0x22, 0xD3, 0xEE, 0x10, 0x66, 0x88)
        // Palette 2: Active Nexus (Cyber Neon Pink)
        val p2 = AnomalyColorRgb(0xCC, 0x1A, 0x44, 0xBB, 0x10, 0x25)
        // Palette 3: Destabilizing (Chromatic Amber / Magenta)
        val dSg = (0x20 + (0xAA - 0x20) * flashState).toInt()
        val dSb = (0x44 * (1f - flashState)).toInt()
        val dFg = (0x22 + (0x88 - 0x22) * flashState).toInt()
        val p3 = AnomalyColorRgb(0xFF, dSg, dSb, 0xEE, dFg, 0x15)
        // Palette 4: Void Collapse (Singularity Purple)
        val p4 = AnomalyColorRgb(0x79, 0x28, 0xCA, 0x25, 0x05, 0x45)

        fun blend(c1: AnomalyColorRgb, c2: AnomalyColorRgb, t: Float): AnomalyColorRgb {
            val f = t.coerceIn(0f, 1f)
            return AnomalyColorRgb(
                sr = (c1.sr + (c2.sr - c1.sr) * f).toInt(),
                sg = (c1.sg + (c2.sg - c1.sg) * f).toInt(),
                sb = (c1.sb + (c2.sb - c1.sb) * f).toInt(),
                fr = (c1.fr + (c2.fr - c1.fr) * f).toInt(),
                fg = (c1.fg + (c2.fg - c1.fg) * f).toInt(),
                fb = (c1.fb + (c2.fb - c1.fb) * f).toInt(),
            )
        }

        val blendWidth = 0.03f
        return when {
            p < 0.15f -> {
                if (p < blendWidth) {
                    blend(p4, p1, p / blendWidth)
                } else if (p > 0.15f - blendWidth) {
                    blend(p1, p2, (p - (0.15f - blendWidth)) / blendWidth)
                } else {
                    p1
                }
            }
            p < 0.70f -> {
                if (p > 0.70f - blendWidth) {
                    blend(p2, p3, (p - (0.70f - blendWidth)) / blendWidth)
                } else {
                    p2
                }
            }
            p < 0.90f -> {
                if (p > 0.90f - blendWidth) {
                    blend(p3, p4, (p - (0.90f - blendWidth)) / blendWidth)
                } else {
                    p3
                }
            }
            else -> {
                if (p > 1.0f - blendWidth) {
                    blend(p4, p1, (p - (1.0f - blendWidth)) / blendWidth)
                } else {
                    p4
                }
            }
        }
    }
}
