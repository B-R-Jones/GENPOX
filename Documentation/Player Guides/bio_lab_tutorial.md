# Bio-Lab Operations: Stepped Synthesis Tutorial

This document serves as an interactive tutorial skeleton designed to be translated into the game's guided tutorial flows. It walks the player step-by-step through the core and advanced biophysical mechanics of the P.O.X. Reactor.

---

## Part 1: Beginner Synthesis (Core Operations)

### Step 1.1: Target Sequence Programming
* **Objective**: Define the target genetic template.
* **Concept**: Before synthesizing, you must program the exact 8-base-pair sequence you wish to construct. This sequence determines the creature stats (e.g., base vitality is scaled by 'A' count).
* **Tutorial Action**:
  1. Locate the **Target Dials** at the top of the Bio-Lab Reactor screen.
  2. Rotate the dials until they read: `A A A A A A A A`.
  3. Notice that the HUD updates to show an ideal melting temperature ($T_m$) of $16^\circ\text{C}$ and a Minimum Free Energy ($MFE$) of $0.0\text{ kcal/mol}$ (indicating no secondary structure folding risks).

---

### Step 1.2: Thermodynamic Wave Phase-Locking
* **Objective**: Align the chamber temperature with the sequence's ideal melting point.
* **Concept**: The synthesis chamber's temperature must align with the target sequence's ideal melting temperature ($T_m$). When misaligned, the environmental synodic waves are out of phase, decreasing transcription quality (Q-score).
* **Tutorial Action**:
  1. Locate the **Chamber Temperature** slider.
  2. Slide the temperature until it matches the target $T_m$ ($16^\circ\text{C}$).
  3. Observe the dynamic environment wave overlay: the blue/cyan wave and the red target wave will merge into a single glowing **CyberGreen** line.
  4. Your temperature deviation ($\eta$) is now maximized at $1.0$, securing a high baseline Q-score.

---

### Step 1.3: Feedstock Cost Verification
* **Objective**: Check feedstock reserves prior to ignition.
* **Concept**: Synthesis consumes raw base nucleotides (A, G, T, C) depending on the target sequence and selected enzyme. Under standard conditions:
  - **Taq** consumes **25 units** of each base present in the sequence.
  - Synthesis also requires a base fee of **Bio-Waste** (Taq = $0$, Tth = $0$, Pfu = $150$ units).
* **Tutorial Action**:
  1. Inspect the feedstock inventory indicators at the bottom left of the Reactor panel.
  2. Confirm you have at least $25 \times 8 = 200$ units of **A** (Adenine) feedstock.
  3. Verify that the **Bio-Waste** level meets the requirement ($0$ for Taq).

---

### Step 1.4: Initiating Stepped Transcription
* **Objective**: Manually run the synthesis cycle to completion.
* **Concept**: Once programmed and locked, synthesis runs as a sequence of 8 discrete transcription steps. Selecting Taq Polymerase yields a fast **8-second cycle** (1 second per step).
* **Tutorial Action**:
  1. Tap the **INITIATE SYNTHESIS** button.
  2. Watch the progress bar increment from Step 1 to Step 8.
  3. Upon completion, note the synthesis success audio cue and the addition of a new `AAAAAAAA` standard gene block to your Gen-Vault inventory.

---

### Step 1.5: The Thermal Safety Envelope (Avoiding Collapses)
* **Objective**: Learn how leaving safe thermodynamic boundaries causes catastrophic reaction collapses.
* **Concept**: The polymerase enzyme cannot transcribe if the chamber is too hot (**AT Denaturation**) or too cold (**GC Hairpin Stalling**). Leaving these bounds without protective solutes aborts the cycle, destroying all consumed feedstock and generating **+8 units of Bio-Waste**.
  - **Lower Bound (Stall)**: $T_{\text{stall}} = 28^\circ\text{C} + [Na^+] \times 30^\circ\text{C}$
  - **Upper Bound (Denature)**: $T_{\text{denature}} = 22^\circ\text{C} + [Na^+] \times 30^\circ\text{C}$
* **Tutorial Action**:
  1. Program a GC-rich target sequence: `G G G G G G G G`.
  2. Set **Salt Concentration** slider to its maximum of `0.15 M`. (This shifts $T_{\text{stall}}$ up to $32.5^\circ\text{C}$).
  3. Set **Chamber Temperature** to `30°C` (which is cold, falling below the $32.5^\circ\text{C}$ stall threshold).
  4. Ensure no chemical solutes are active.
  5. Tap **INITIATE SYNTHESIS**.
  6. *Observation*: At Step 4, the reaction collapses. The logs report: `CRITICAL COLLAPSE: Polymerase halted due to unstabilized stalling`. Note that the feedstock is lost, and the waste counter has increased.

---

## Part 2: Advanced Synthesis (Maximizing Q-Score)

### Step 2.1: Polymerase Selection & Baseline Fidelity
* **Objective**: Leverage high-fidelity enzymes to secure superior Phred Q-scores.
* **Concept**: The active polymerase dictates both baseline transcription accuracy ($Q_{\text{base}}$) and speed:
  - **Taq**: $Q_{\text{base}} \in [15.0, 25.0]$ (Fast, 8s cycle)
  - **Tth**: $Q_{\text{base}} \in [25.0, 33.0]$ (Medium, 16s cycle)
  - **Pfu**: $Q_{\text{base}} \in [35.0, 40.0]$ (Slow, 32s cycle, proofreading active)
  A higher final average Q-score directly boosts creature stats: Starting Telomeres (lifespan) $= Q \times 2.5$, and Max HP $= \text{Base HP} \times (0.5 + 0.5 \times \frac{Q}{40.0})$.
* **Tutorial Action**:
  1. Locate the **Enzyme Chamber Selector** row.
  2. Select **Pfu Polymerase**. (Note the cost changes: Pfu consumes $50$ of each sequence base and $150$ Bio-Waste).
  3. Pfu's built-in proofreading mechanism is now active, raising baseline accuracy to a minimum of $Q35$.

---

### Step 2.2: Stoichiometric Inlet Alignment
* **Objective**: Match raw feedstock inlets to the target sequence distribution.
* **Concept**: All polymerases suffer from a Q-score quality penalty (up to **$-15.0$**) if the raw base feed rate does not match the target strand's nucleotide ratios. Additionally, standard polymerases (Taq and Tth) will suffer from physical sequence errors (crosstalk mutations) when inlets are unbalanced.
  *Note: While Pfu's proofreading prevents physical sequence mutations, you still need to balance the inlets to avoid the Q-score quality penalty.*
* **Tutorial Action**:
  1. Swap to **Tth Polymerase**.
  2. Program the target sequence: `A A A A G G G G`. (50% Adenine, 50% Guanine).
  3. Locate the **4-Channel Feedstock Inlet Bias** dial and slider.
  4. Use the spinnable dial to select **Inlet A** and set its slider to `50%`. Then select **Inlet G** and set its slider to `50%` (ensure $I_T$ and $I_C$ are at `0%`).
  5. The stoichiometric deviation is now `0.0`, eliminating the deviance penalty.

---

### Step 2.3: Chemical Solute Injections
* **Objective**: Override thermodynamic failure points using chemical solutes.
* **Concept**: If an extreme sequence (e.g., highly AT-dense or GC-dense) forces you to run the chamber at high or low temperatures that exceed the safety boundaries, you can inject solutes to stabilize the reaction:
  - **DMSO**: Prevents GC Hairpin Stalling in cold environments.
  - **Netropsin**: Prevents AT Denaturation in hot environments.
  - *Solute Cost*: Injecting a solute costs **100 units** of each raw base.
* **Tutorial Action**:
  1. Program a highly AT-dense sequence: `A A A A A A A T`.
  2. Set **Salt Concentration** to `0.01 M` ($T_{\text{denature}} = 22.3^\circ\text{C}$).
  3. Adjust the temperature to `25°C` (violating the safety upper bound).
  4. Locate the **Chemical Solute Injection** selector and toggle **Netropsin**.
  5. Tap **INITIATE SYNTHESIS**.
  6. *Observation*: The synthesis compiles successfully. The Netropsin buffer bound to the AT minor grooves and prevented thermal denaturation, allowing synthesis to complete despite the unsafe thermal level.

---

### Step 2.4: Compiling a Prime Specimen
* **Objective**: Combine high Q-score gene blocks to construct a high-lifespan, high-HP creature.
* **Concept**: In the Splicer, welding gene blocks with an average Q-score of $40.0$ yields a specimen with **100% starting telomeres** (maximum survival lifespan) and **100% scaled Max HP**.
* **Tutorial Action**:
  1. Open the **Splicer** tab.
  2. Select your newly synthesized $Q40$ standard gene blocks.
  3. Assemble the 8 blocks into a full 64-character genome.
  4. Tap **CONSTRUCT SPECIMEN** to finalize the creature creation.
