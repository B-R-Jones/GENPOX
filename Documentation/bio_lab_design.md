# G.E.N. Network: Bio-Lab (Combinator) Design Document

This document outlines the conceptual design, gameplay mechanics, mathematical formulas, and interface components of the **Bio-Lab Tab** (internally referred to as the `Combinator`). The Bio-Lab serves as the core genetic refinery and synthesis hub in the GENPOX emulator, allowing players to generate standard nucleotides, harness celestial influences, and initiate unstable fusions to forge powerful anomalous genes.

---

## 1. Overview of the Bio-Lab

The Bio-Lab is divided into two primary sub-tabs representing distinct levels of genetic synthesis and engineering:
1.  **Tide Pool Reactor (`pox` sub-tab)**: A stable synthesizer that continuously harvests standard, biological gene sequences under the influence of global lunar forces.
2.  **Genetic Anomaly Harmonizer (`anomaly` sub-tab)**: An unstable, high-energy fusion engine that consumes massive quantities of standard nucleotides to create anomalous, rule-breaking genetic sequences.

These systems are supported by a suite of retro-sci-fi visual diagnostics, including the rotating *Holographic Helix Reactor*, *Node Crystal Canvas*, *Resonance Scope*, and the *Molecular Step-Search Directory* for precise genetic auditing.

---

## 2. The Tide Pool Reactor (Standard Synthesis)

The Tide Pool Reactor is an automated single-node cybernetic biophysical synthesizer. It compiles raw nucleotides (`A`, `G`, `T`, `C`) from the player's raw stockpile into sequences of 8 base-pairs to expand the player's stock.

### Reactor Feedstock Requirement
Unlike previous passive configurations, standard synthesis is resource-dependent. Compiling an 8-character block consumes the corresponding quantity of raw Adenine (`A`), Guanine (`G`), Thymine (`T`), and Cytosine (`C`) bases from the raw stockpile. 
*   **Acquiring Feedstocks**: Players acquire new gene blocks through mission payouts or scanner battle drops. Unwanted standard or anomalous blocks can be deconstructed (recycled) in the Vault to extract their raw A, G, T, C bases.
*   **Depletion Substitution**: If a cycle completes but the raw stock for a required base is depleted, the reactor automatically substitutes a random available base. This increases the mutation rate and lowers the sequence's Phred Quality Score ($Q$).

### Biophysical Synthesis Variables & Tuning
Players can optimize the synthesis yield and sequence characteristics by adjusting the reactor's environment parameters:
*   **Temperature ($T_{\text{react}}$)**:
    *   *High Temperature ($> 75^\circ\text{C}$)*: GC-rich sequences survive. AT-rich denature and fail unless stabilized by **Netropsin**.
    *   *Low Temperature ($15^\circ\text{C} - 30^\circ\text{C}$)*: AT-rich sequences survive. GC-rich templates form rigid internal hairpins (thermodynamic trapping) and stall unless stabilized by **DMSO / Formamide**.
*   **Salt Concentration ($[Na^+]$)**: Ranges from $10\text{ mM}$ to $500\text{ mM}$. Modulates the melting temperature $T_m$ and shifts wave frequencies.
*   **Enzyme Selection**:
    *   *Taq Polymerase*: Fast standard cycle (**8 seconds**). Low fidelity ($Q$-scores of $15-25$, high mutation rate).
    *   *Pfu Polymerase*: Slow standard cycle (**24 seconds**). Proofreading active, high fidelity ($Q$-scores of $35-40$, perfect copy rate).
    *   *Tth Polymerase*: Standard cycle (**16 seconds**). Highly heat-resistant.
*   **Inlet Feed Ratios**: Players adjust channels to feed excess bases into the reactor, skewing wave transition probabilities to target specific sequences.

### Synthesis Yield Math
The yield coefficient ($\eta$) maps the temperature tolerance curve around a block's melting temperature ($T_m$):
$$\eta = \exp\left( - \frac{(T_{\text{react}} - T_m)^2}{2 \sigma^2} \right)$$
Where $T_m$ is salt-corrected:
$$T_m = 2 \cdot (N_A + N_T) + 4 \cdot (N_G + N_C) - 16.6 \log_{10}([Na^+]) - \frac{675}{\text{Length}}$$
And $\sigma$ is the thermal tolerance of the active polymerase. If yield $\eta < 0.5$, synthesis fails or mutates heavily.

### Daily Base-Pair Waves
Synthesis rates are not uniform; they fluctuate according to daily environmental frequencies synchronized with the synodic lunar cycle. Each UTC calendar day features an active **Base-Pair Wave** composed of a **Primary** and a **Secondary** nucleotide (e.g., an `AG` wave).

#### Synodic Lunar Math
The active wave is deterministically generated based on the days elapsed since the standard reference New Moon:
*   **Reference New Moon ($T_0$)**: May 17, 2026, 01:54:00 UTC.
*   **Lunar Month Period ($L$)**: $29.53059\text{ days}$.
*   **Days Elapsed ($D$)**:
    $$D = \frac{\text{Current Time (ms)} - T_0}{86,400,000}$$
*   **Lunar Age ($A$)**:
    $$A = (D \bmod L + L) \bmod L$$

#### Moon Phase Classifications
The lunar age maps directly to one of the following phases:
*   **New Moon**: $A < 1.0$ or $A > 28.53059$
*   **Waxing Crescent**: $1.0 \le A < 6.38$
*   **First Quarter**: $6.38 \le A < 8.38$
*   **Waxing Gibbous**: $8.38 \le A < 13.77$
*   **Full Moon**: $13.77 \le A < 15.77$
*   **Waning Gibbous**: $15.77 \le A < 21.15$
*   **Third Quarter**: $21.15 \le A < 23.15$
*   **Waning Crescent**: $23.15 \le A \le 28.53059$

#### Wave Multiplier Formulas
The gravitational pull of the moon modulates synthesis weights via the moon's angular phase:
*   **Moon Angle ($\theta$)**:
    $$\theta = \frac{2\pi \cdot A}{L} - \frac{\pi}{2}$$
*   **Moon Modifier ($\delta$)**:
    $$\delta = 0.0125 \cdot \sin(\theta)$$
*   **Primary Wave Multiplier ($m_1$)**:
    $$m_1 = 1.125 + \delta$$
*   **Secondary Wave Multiplier ($m_2$)**:
    $$m_2 = 1.625 + \delta$$

#### Wave Suppression (Dormant Congested Decay)
During a random week of the calendar month, systemic decay can suppress the daily wave. If a deterministic hash of the date yields a value where $(Hash \bmod 100) < 50$, the wave enters a **Dormant State**. Under suppression, all nucleotide bases generate with equal probability ($25\%$), and no multipliers are active.

#### Wave-Weighted Gene Generation
When compiling an 8-character block under an active wave (with Primary base $B_1$ and Secondary base $B_2$), the probability of selecting each base at index $i$ depends on the preceding character:
1.  Initialize standard weights for bases `["A", "G", "T", "C"]` to $1.0$.
2.  If the preceding base in the current block (at index $i-1$) matches the Primary base ($B_1$), the weight of the Secondary base ($B_2$) is multiplied by $m_2$.
3.  Otherwise, the weight of the Primary base ($B_1$) is multiplied by $m_1$.
4.  A weighted random selection determines the nucleotide at index $i$.

---

## 3. The Genetic Anomaly Harmonizer (Anomaly Engine)

The Genetic Anomaly Harmonizer is an unstable fusion chamber. Instead of harvesting standard genes, it attempts to synthesize highly volatile **Anomalous Genes** containing non-standard, alien characters: `XZYW?!$%&@#`.

### Resource Cost & Consumption Mechanics
The Anomaly Engine requires massive genetic feedstock to stabilize its energy field:
*   **Synthesis Loop**: Ticks every **16 seconds** (or **8 seconds** if boosted, modulated by polymerase selection).
*   **Material Cost**: Consumes exactly **10,000 raw nucleotides** per loop. This cost is split evenly ($2,500$ of each base: A, G, T, C) or adjusted dynamically based on the composition of the target anomalous sequence.
*   **Threshold Shutdown**: The engine requires a minimum reserve of **250,000 raw nucleotides** (total across all bases) to operate. If raw stocks fall below this limit, the engine automatically shuts down.

### Fusion Success Probability Math
The probability of a fusion resolving successfully ($FinalChance$) is mathematically scaled based on the player's nucleotide stockpile, temporal fluctuations, and natural resonance peaks.

#### 1. Base Logarithmic Scaling ($BaseChance$)
Below $10,000$ nucleotides, the success chance is hard-locked to $1.0\%$. At the $250,000$ threshold, it reaches a base of $100.0\%$. In between, the progression scales logarithmically:
$$t = \frac{\ln(\text{Stockpile}) - \ln(10,000)}{\ln(250,000) - \ln(10,000)}$$
$$BaseChance = 1.0 + 99.0 \cdot t$$

#### 2. Resonance Peak Boosts ($PeakBoost$)
The fusion chamber experiences quantum harmonics that spike at multiples of $14\%$ base success rate ($P \in \{14, 28, 42, 56, 70, 84, 98\}$). If $BaseChance$ is close to a peak $P$, a bell-curve boost is added:
*   **Distance to Peak ($d$)**: $d = |BaseChance - P|$
*   **Peak Boost ($PB$)**: If $d < 5.0$, then:
    $$PB = \max_P \left( 6.5 \cdot \exp\left(-\left(\frac{d}{1.8}\right)^2\right) \right)$$
    *(Otherwise, $PB = 0.0$)*

#### 3. Spectrum Wave Coupling ($S$) & Harmonic Modifier
Temporal waves fluctuating throughout the calendar day affect fusion stability.
*   **Day Fraction ($f_{\text{day}}$)**: The current elapsed fraction of the 24-hour day ($0.0 \le f_{\text{day}} < 1.0$).
*   **Coupling Value ($S$)**: Fluctuates sinusoidally across a 6-hour cycle (4 peaks per day):
    $$S = 80.0 + 12.375 \cdot \sin(f_{\text{day}} \cdot 8\pi)$$
*   **Harmonic Modifier ($HM$)**: Modulates success rates positively or negatively based on deviation from the baseline ($80.0$):
    $$HM = (S - 80.0) \times 0.25$$

#### 4. Final Fusion Success Chance ($FinalChance$)
The absolute success chance is the summation of these factors, clamped between $1.0\%$ and $100.0\%$:
$$FinalChance = \operatorname{clamp}(BaseChance + PeakBoost + HM, 1.0\%, 100.0\%)$$

### Fusion Resolution
*   **Success**: A fully anomalous 8-character gene block containing characters randomly selected from `XZYW?!$%&@#` is synthesized and added to the stockpile.
*   **Decomposition (Decay)**: The fusion fails. The 10,000 standard nucleotides decompose into useless bio-matter, resulting in a net resource loss.

---

## 4. Anomalous Genetic Benefits & Gameplay Effects

When successfully synthesized, anomalous genes grant creatures powerful battlefield perks depending on the specific characters present in the 8-character sequence.

### Nomenclature & Identification
Anomalous genes are dynamically named based on their first two characters ($s_0$ and $s_1$):

| Character | Prefix ($s_0$) | Suffix ($s_1$) |
| :--- | :--- | :--- |
| **X** | Vortex | Phase-Strike |
| **Z** | Zero-Point | Mirror-Shield |
| **Y** | Quantum | Reverb |
| **W** | Tachyon | Extraction-Unit |
| **?** | Shrouded | Siphon |
| **!** | Overdrive | Anomaly |
| **$** | Bio-Organic | Resonance |
| **%** | Plasma | Helix |
| **&** | Eldritch | Well |
| **@** | Temporal | Pulse |
| **#** | Cosmic | Matrix |
| *Fallback* | Prime | Weld |

*Example: An anomalous gene starting with `Z?` is designated as a **"Zero-Point Siphon"**.*

### Raw Power Calculation
The middle characters of the sequence ($s_2, s_3, s_4, s_5$) determine the potency of the benefit. Each character maps to a numeric value:
*   `X`, `Z`, `Y`, `W` $\rightarrow 3$
*   `?`, `!` $\rightarrow 4$
*   `$`, `%` $\rightarrow 5$
*   `&`, `@`, `#` $\rightarrow 6$
*   *Other* $\rightarrow 1$

$$RawPower = \sum_{i=2}^5 \operatorname{val}(s_i) \quad (\text{Range: } 4 \text{ to } 24)$$

### Combat Benefit Classes
The combat effect type is determined by the summation hash of the first two characters:
$$EffectIndex = (\operatorname{code}(s_0) + \operatorname{code}(s_1)) \bmod 6$$

| Index | Benefit ID | Combat Effect | Scaling Formula |
| :--- | :--- | :--- | :--- |
| **0** | `DOUBLE_STRIKE` | Outgoing damage multiplier. | $\text{Damage} = (1.2 + 0.04 \cdot RawPower)\text{x}$ |
| **1** | `SELF_DESTRUCT` | Deals flat damage to opponent upon system defeat. | $\text{Damage} = 40.0 + 8.0 \cdot RawPower$ |
| **2** | `HARVEST_BOOST` | Chance to harvest $+1$ extra gene on a winning match. | $\text{Chance} = \min(30\% + 3\% \cdot RawPower, 100\%)$ |
| **3** | `HEALTH_REGEN` | Health restored upon executing an attack. | $\text{HP restored} = 4.0 + RawPower$ |
| **4** | `PHASE_SHIFT` | Flat evasion chance to entirely bypass incoming attacks. | $\text{Evasion} = (10\% + 1.5\% \cdot RawPower)$ |
| **5** | `COHERENCE_SHIELD`| Immunity to positive density drag during anomaly runs. | Active (Magnitude = $1.0$) |

### Trigger Conditions
The conditions under which a combat benefit activates are governed by the last two characters of the sequence:
$$TriggerIndex = (\operatorname{code}(s_6) + \operatorname{code}(s_7)) \bmod 8$$

*   **0**: Always active in combat.
*   **1**: Only active during Dark moon phases (New Moon, Crescent).
*   **2**: Only active during Light moon phases (Full Moon, Gibbous).
*   **3**: Only active when Vitality is below $40\%$.
*   **4**: Only active when Vitality is above $70\%$.
*   **5**: Only active during the first 3 turns of combat.
*   **6**: Only active after turn 6 of combat.
*   **7**: Only active when local Spectrum Wave Coupling is above $82\%$.

---

## 5. Diagnostic Overlays & Interfaces

To assist in filtering, tracking, and monitoring genetic development, the Bio-Lab contains three retro diagnostics overlays.

### Molecular Step-Search Directory
A stepwise search mechanism that allows players to incrementally search the gene bank by selecting target prefixes. This prevents visual overload and mimics classic data search systems:
*   **Standard Mode**: The search progresses in 4 steps of **2 base-pairs** each (`1-2bp` $\rightarrow$ `3-4bp` $\rightarrow$ `5-6bp` $\rightarrow$ `7-8bp`).
*   **Anomalous Mode**: The search progresses character-by-character in 8 steps of **1 base-pair** each.
*   *Interaction*: Players can select characters, see the count of matching genes in real-time, undo the last step, or clear filters.

### Discovered Packets Log
Displays a rolling buffer of the last 50 compiled genetic telemetry packets.
*   **Standard Packets (`PKT-XXXXXX`)**: Show standard 8-gene batches.
*   **Anomaly Packets (`ANM-XXXXXX`)**: Track individual fusion runs, indicating whether they resulted in an anomalous gene or decayed into waste.

### Visual Chamber Renderers
*   **Holographic Helix Reactor**: A rotating 2D/3D projection of a double-helix.
*   **Node Crystal Canvas**: A multi-dimensional grid showing crystal coordinates. The nodes are dynamically positioned using dimensions derived from the active day's wave configuration multipliers ($m_1$ and $m_2$), visualizing the gravitational pull on the matrix.
*   **Resonance Scope Chamber**: A simulated progress oscilloscope that draws animated soundwaves matching the exact cycle completion percentage.
*   **Transcription Decoder Ticker**: A scrolling text line that rotates at 110ms cycles. During standard synthesis, it displays genetic letters; during anomalous runs, it scrambles into code symbols based on current time hashes.

---

## See Also
*   [Master Design & Font Standards](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/master_design_standards.md)
*   [Wireframe Animation Guidelines](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/wireframe_animation_guidelines.md)
*   [Creature Factions & Types](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/creature_types.md)
*   [Gen-Vault Design Document](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/gen_vault_design.md)
*   [Scanner Design Document](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/scanner_design.md)
