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
    *   **Lower Bound (GC Hairpin Stalling)**: $T_{\text{stall}} = 20^\circ\text{C} + [Na^+] \times 40^\circ\text{C}$
    *   **Upper Bound (AT Denaturation)**: $T_{\text{denature}} = 65^\circ\text{C} + [Na^+] \times 50^\circ\text{C}$
*   **What it Controls**: **Reaction Survival**.
*   **Tuning Goal**: Keep the chamber temperature between these two limits, or inject protective chemical solutes.
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
1.  Set **Salt** to `0.01 M` (Lowering salt decreases backbone stability; $T_{\text{denature}} = 65.5^\circ\text{C}$).
2.  Set **Temperature** to `70°C`.
3.  Fill feedstock stocks and tap **✕ INITIATE SYNTHESIS**.
4.  *Observation*: The reaction will collapse catastrophically around Step 4. The log will report: `CRITICAL COLLAPSE: Polymerase halted ... due to unstabilized denaturation`.

#### Run A2: High Salt Stabilization (Expected: Pass)
1.  Increase **Salt** to `0.30 M` (Raising salt stabilizes AT bonds; $T_{\text{denature}} = 80.0^\circ\text{C}$).
2.  Maintain **Temperature** at `70°C`.
3.  Tap **✕ INITIATE SYNTHESIS**.
4.  *Observation*: The synthesis compiles successfully. Salt stabilization prevented denaturation in the hot chamber.

#### Run A3: Netropsin Buffer Stabilization (Expected: Pass)
1.  Lower **Salt** back to `0.01 M` ($T_{\text{denature}} = 65.5^\circ\text{C}$).
2.  Inject **Netropsin** solute.
3.  Maintain **Temperature** at `70°C`.
4.  Tap **✕ INITIATE SYNTHESIS**.
5.  *Observation*: The synthesis compiles successfully. Netropsin protected the AT-rich sequence despite low salt.

---

### TEST CASE B: The Cold Run (GC Hairpin Stalling)
*   **Target Sequence**: `GCGCGCGC` (High GC content, Nussinov MFE $\le -5.0$)
*   **Ideal Temperature ($T_m$)**: $32^\circ\text{C}$ (at $0.05\text{ M}$ salt)
*   **Initial Setup**: Polymerase = Taq, Chem Suite = None.

#### Run B1: High Salt Collapse (Expected: Fail)
1.  Set **Salt** to `0.45 M` (Raising salt stabilizes secondary folds; $T_{\text{stall}} = 38.0^\circ\text{C}$).
2.  Set **Temperature** to `35°C`.
3.  Tap **✕ INITIATE SYNTHESIS**.
4.  *Observation*: The reaction collapses at Step 4. The log will report: `CRITICAL COLLAPSE: Polymerase halted ... due to unstabilized stalling`.

#### Run B2: Low Salt Prevention (Expected: Pass)
1.  Lower **Salt** to `0.05 M` (Lowering salt destabilizes the hairpin; $T_{\text{stall}} = 22.0^\circ\text{C}$).
2.  Maintain **Temperature** at `35°C`.
3.  Tap **✕ INITIATE SYNTHESIS**.
4.  *Observation*: The synthesis compiles successfully. Destabilizing the secondary structures via low salt prevented stalling.

#### Run B3: DMSO Buffer Stabilization (Expected: Pass)
1.  Raise **Salt** back to `0.45 M` ($T_{\text{stall}} = 38.0^\circ\text{C}$).
2.  Inject **DMSO** solute.
3.  Maintain **Temperature** at `35°C`.
4.  Tap **✕ INITIATE SYNTHESIS**.
5.  *Observation*: The synthesis compiles successfully. DMSO destabilized the GC folds and allowed transcription in the cold, high-salt chamber.
