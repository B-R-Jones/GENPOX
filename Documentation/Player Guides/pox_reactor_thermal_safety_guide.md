# P.O.X. Reactor: Thermal Safety & Tuning Guide

Understanding the relationship between **Ideal Temperature** and the **Safe Temperature Range** is the key to operating the P.O.X. Reactor successfully. Misalignment drops compilation quality, while leaving the safe boundary leads to catastrophic reaction collapse.

---

## 1. Safe Temperature vs. Ideal Temperature

### Ideal Temperature ($T_m$)
*   **Definition**: The Wallace-Breslauer melting temperature of the final target sequence.
*   **What it Controls**: **Copy Quality (Phred Q-Score)**.
*   **Tuning Goal**: Adjust the chamber temperature ($T_{\text{react}}$) to align exactly with $T_m$ (represented in the UI by the environmental wave phase-locking and merging into a green line).
*   **Consequence of Deviation**: If the chamber temperature is too hot or too cold compared to the ideal $T_m$, the synthesized bases will suffer a quality penalty (lower Q-score), increasing the chance of random mutations. The reaction, however, will not collapse.

### Safe Temperature Range
*   **Definition**: The thermodynamic safety window inside which the polymerase enzyme can actively compile the DNA strand. This window is governed by two salt-dependent limits:
    *   **Lower Bound (GC Hairpin Stalling)**: $T_{\text{stall}} = 28^\circ\text{C} + [Na^+] \times 30^\circ\text{C}$
    *   **Upper Bound (AT Denaturation)**: $T_{\text{denature}} = 22^\circ\text{C} + [Na^+] \times 30^\circ\text{C}$
*   **What it Controls**: **Reaction Survival**.
*   **UI Display (Context-Aware Safe Range)**: The UI dynamically calculates and displays the actual safe range for the active sequence under current chemical solute conditions:
    *   **AT-dense sequences** (denaturation risk): displays `15°C - T_denature` (e.g., `15°C - 23°C` at standard salt).
    *   **GC-dense sequences** (stalling risk): displays `T_stall - 50°C` (e.g., `29°C - 50°C` at standard salt).
    *   **Balanced/Buffered sequences**: displays the full `15°C - 50°C` range.
*   **Tuning Goal**: Keep the chamber temperature inside the displayed dynamic Safe Range. The salt slider range is optimized between **$0.01\text{ M}$ and $0.15\text{ M}$** to cover this active biophysical safety envelope.
*   **Synthesis Speed Multiplier**: Increasing salt concentration speeds up polymerase transcription binding. The step duration is scaled by:
    $$\text{saltSpeedMultiplier} = 1.0 + ([Na^+] - 0.05) \times 2.0$$
    This yields up to a **$20\%$ speed boost** at $0.15\text{ M}$ salt compared to standard baseline speed.
*   **Consequence of Deviation**: If the chamber temperature crosses either threshold without the appropriate protective solute, the reaction suffers a **Catastrophic Collapse**. The run aborts, the strand is destroyed, and the consumed feedstock is converted into Bio-Waste.

---

## 2. Chemical Solute Protection

When a target sequence's ideal temperature falls outside the safe boundaries, you must use chemical solutes as buffers:
*   **DMSO (Dimethyl Sulfoxide)**: Prevents GC Hairpin Stalling in cold chambers. Binds to GC pairs to prevent secondary folding.
*   **Netropsin**: Prevents AT Denaturation in hot chambers. Binds to the minor groove of AT-rich strands to stabilize them against thermal melting.

---

## 3. Step-by-Step Test Scenarios

Use the following hot and cold runs to test the reactor's biophysical responses.

### TEST CASE A: The Hot Run (AT Denaturation)
*   **Target Sequence**: `AAAAAAAT` (High AT content)
*   **Ideal Temperature ($T_m$)**: $16^\circ\text{C}$ (at $0.05\text{ M}$ salt)
*   **Initial Setup**: Polymerase = Taq, Chem Suite = None.

#### Run A1: Low Salt Collapse (Expected: Fail)
1.  Set **Salt** to `0.01 M` (Lowering salt decreases backbone stability; $T_{\text{denature}} = 22.3^\circ\text{C}$).
2.  Set **Temperature** to `25°C`.
3.  Fill feedstock stocks and tap **INITIATE SYNTHESIS**.
4.  *Observation*: The reaction will collapse catastrophically around Step 4. The log will report: `CRITICAL COLLAPSE: Polymerase halted ... due to unstabilized denaturation`.

#### Run A2: High Salt Stabilization (Expected: Pass)
1.  Increase **Salt** to `0.15 M` (Raising salt stabilizes AT bonds; $T_{\text{denature}} = 26.5^\circ\text{C}$).
2.  Maintain **Temperature** at `25°C`.
3.  Tap **INITIATE SYNTHESIS**.
4.  *Observation*: The synthesis compiles successfully. Salt stabilization prevented denaturation in the hot chamber.

#### Run A3: Netropsin Buffer Stabilization (Expected: Pass)
1.  Lower **Salt** back to `0.01 M` ($T_{\text{denature}} = 22.3^\circ\text{C}$).
2.  Inject **Netropsin** solute.
3.  Maintain **Temperature** at `25°C`.
4.  Tap **INITIATE SYNTHESIS**.
5.  *Observation*: The synthesis compiles successfully. Netropsin protected the AT-rich sequence despite low salt.

---

### TEST CASE B: The Cold Run (GC Hairpin Stalling)
*   **Target Sequence**: `GCGCGCGC` (High GC content, Nussinov MFE $\le -5.0$)
*   **Ideal Temperature ($T_m$)**: $32^\circ\text{C}$ (at $0.05\text{ M}$ salt)
*   **Initial Setup**: Polymerase = Taq, Chem Suite = None.

#### Run B1: High Salt Collapse (Expected: Fail)
1.  Set **Salt** to `0.15 M` (Raising salt stabilizes secondary folds; $T_{\text{stall}} = 32.5^\circ\text{C}$).
2.  Set **Temperature** to `30°C`.
3.  Tap **INITIATE SYNTHESIS**.
4.  *Observation*: The reaction collapses at Step 4. The log will report: `CRITICAL COLLAPSE: Polymerase halted ... due to unstabilized stalling`.

#### Run B2: Low Salt Prevention (Expected: Pass)
1.  Lower **Salt** to `0.05 M` (Lowering salt destabilizes the hairpin; $T_{\text{stall}} = 29.5^\circ\text{C}$).
2.  Set **Temperature** to `35°C`.
3.  Tap **INITIATE SYNTHESIS**.
4.  *Observation*: The synthesis compiles successfully. Destabilizing the secondary structures via low salt prevented stalling.

#### Run B3: DMSO Buffer Stabilization (Expected: Pass)
1.  Raise **Salt** back to `0.15 M` ($T_{\text{stall}} = 32.5^\circ\text{C}$).
2.  Inject **DMSO** solute.
3.  Set **Temperature** to `30°C`.
4.  Tap **INITIATE SYNTHESIS**.
5.  *Observation*: The synthesis compiles successfully. DMSO destabilized the GC folds and allowed transcription in the cold, high-salt chamber.

---

### TEST CASE C: Mixed Sequence Tuning (The GCAATAGC Dilemma)
*   **Target Sequence**: `GCAATAGC` (Balanced GC/AT content: 4 GC, 4 AT)
*   **Ideal Temperature ($T_m$)**: $24.0^\circ\text{C}$ (at $0.05\text{ M}$ salt)
*   **Initial Setup**: Polymerase = Pfu.

This sequence presents a unique thermodynamic challenge. During transcription:
1.  At **Step 6** (`GCAATA`), GC content drops to $33.3\%$ (making it vulnerable to **AT Denaturation** if the chamber is too hot).
2.  At **Step 8** (`GCAATAGC`), Nussinov folding energy reaches $-6.0\text{ kcal/mol}$ (making it vulnerable to **GC Hairpin Stalling** if the chamber is too cold).

Because $T_{\text{stall}}$ is always $6^\circ\text{C}$ higher than $T_{\text{denature}}$, they form mutually exclusive temperature bounds. Without a protective solute, the reaction is mathematically guaranteed to collapse.

#### Run C1: Hot Run with DMSO (Expected: Fail)
1.  Set **Salt** to `0.15 M` ($T_{\text{denature}} = 26.5^\circ\text{C}$).
2.  Inject **DMSO** solute.
3.  Set **Temperature** to `31.9°C`.
4.  Tap **INITIATE SYNTHESIS**.
5.  *Observation*: The reaction collapses at **Step 6**. Since the temperature ($31.9^\circ\text{C}$) exceeds $T_{\text{denature}}$ ($26.5^\circ\text{C}$), the AT bonds denature. DMSO only prevents stalling and does not protect against denaturation.

#### Run C2: Low Temp Run with DMSO (Expected: Fail)
1.  Lower **Salt** to `0.05 M` ($T_{\text{denature}} = 23.5^\circ\text{C}$).
2.  Inject **DMSO** solute.
3.  Set **Temperature** to `23.9°C`.
4.  Tap **INITIATE SYNTHESIS**.
5.  *Observation*: The reaction still collapses at **Step 6**. Despite lowering the temperature to $23.9^\circ\text{C}$, it is still above the low-salt denaturation limit of $23.5^\circ\text{C}$.

#### Run C3: Hot Run with Netropsin (Expected: Fail)
1.  Raise **Salt** back to `0.15 M` ($T_{\text{stall}} = 32.5^\circ\text{C}$).
2.  Inject **Netropsin** solute.
3.  Set **Temperature** to `31.9°C`.
4.  Tap **INITIATE SYNTHESIS**.
5.  *Observation*: Netropsin protects the AT bonds, allowing the strand to successfully clear Step 6. However, the reaction collapses at **Step 8**. Since the temperature ($31.9^\circ\text{C}$) is below $T_{\text{stall}}$ ($32.5^\circ\text{C}$), the final strand folds into a hairpin ($MFE = -6.0\text{ kcal/mol}$) and stalls because Netropsin does not prevent GC folding.

#### Run C4: Successful Tuning Strategy (Expected: Pass)
To compile this sequence, you must choose one of two strategies to align the safe ranges:

*   **Strategy A (The Netropsin Route)**:
    1.  Lower **Salt** to `0.05 M` ($T_{\text{stall}} = 29.5^\circ\text{C}$).
    2.  Inject **Netropsin** solute (protects AT denaturation at Step 6).
    3.  Set **Temperature** to `30.0°C` (above $T_{\text{stall}}$ to prevent hairpin stalling at Step 8).
    4.  *Result*: Pass.
*   **Strategy B (The DMSO Route)**:
    1.  Raise **Salt** to `0.15 M` ($T_{\text{denature}} = 26.5^\circ\text{C}$).
    2.  Inject **DMSO** solute (prevents GC hairpin stalling at Step 8).
    3.  Set **Temperature** to `25.0°C` (below $T_{\text{denature}}$ to prevent AT denaturation at Step 6).
    4.  *Result*: Pass.

