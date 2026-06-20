# G.E.N. Network: Splicer (Morphogenesis) Design Document

This document outlines the conceptual design, gameplay mechanics, mathematical formulas, and compiler logic of the **Splicer Tab** (internally referred to as the `Splicer`). The Splicer acts as the assembly chamber of the GENPOX network, allowing players to carefully align synthesized gene blocks to construct custom hybrid specimens, or trigger brute-force reactor cycles to forge forced genomes.

---

## 1. Overview of the Splicer

The Splicer view is structured as two key operational sections that display responsively (side-by-side in landscape/wide-screen displays, or vertically stacked in portrait phone layouts):
1.  **Splicing Morphogenesis Section (Primary)**: A visual assembly grid featuring 8 slots where players align 8-character standard gene blocks to construct a 64-character DNA sequence matching a specified **Target Sequence**.
2.  **Archive Inventory Deck (Secondary)**: An auditing interface displaying available stock matching the selected slot's sequence requirements, supporting filters and manual assignment.

The Splicer is supported by the **Emergency Forced Compile Terminal** for automating assembly when matching genes are missing from inventory stock.

---

## 2. Standard Splicing & Morphogenesis

Standard splicing is a high-stability genetic synthesis method where players recruit matching gene blocks from their inventory.

### The Slot Alignment Grid
The target genome consists of exactly **64 base-pairs** split into **8 distinct segments** of 8 nucleotides each:
$$\text{Target Genome} = S_1 S_2 S_3 S_4 S_5 S_6 S_7 S_8$$
*   **Slot 1**: Chars 0 to 7 ($S_1$)
*   **Slot 2**: Chars 8 to 15 ($S_2$)
*   ...
*   **Slot 8**: Chars 56 to 63 ($S_8$)

### Recruitment & Assignment
*   **Manual Assignment**: The player selects an empty slot $i$ and chooses a matching gene block from their inventory. The assigned block **must match** the required segment sequence $S_i$ exactly. Upon assignment, the gene count is decremented from inventory.
*   **Ejection**: If a player ejects a block from slot $i$, the block is returned to the archive stockpile.
*   **Autofill (Auto-Slot)**: Automatically audits the player's stock and assigns matching blocks to all currently empty slots, reducing manual interface steps.

### Morphogenesis Splicing Phase
Once all 8 slots are filled with matching standard genes, the player initiates specimen assembly:
1.  **Thermal Cycle**: The progress indicator ticks from $10\%$ to $100\%$ in increments of $10\%$ every **200 milliseconds** (taking 2.0 seconds total).
2.  **Appreciation Hold**: The system pauses for **1.5 seconds** at $100\%$ progress to display the fully aligned double-helix configuration.
3.  **Compilation**: The 64-character DNA sequence is compiled offline to calculate the creature's final stats (Vitality, Attack, Defense, Speed), faction alignment, movesets, and lore.
4.  **Metadata Badging**: The assembled creature is saved with origin `"Spliced Gene"`, `isFullCoherence = true`, and `coherenceType = "Natural"`.

---

## 3. Emergency Forced Compile Terminal (Forced Construction)

When players lack the exact matching gene blocks to fill the 8 slots, they can resort to **Forced Construction**. This process forces compiler loops to fuse mismatched standard genes together using unstable nucleotide replacements.

### Cycle Timers & Reactor Lock
*   **Reactor Freeze**: Engaging Forced Construction locks the standard P.O.X. Reactor (`_isReactorFrozen = true`), halting standard daily synthesis.
*   **Forced Loop**: The compile runs on an **8-second sequence loop** (each second corresponds to compiling one of the 8 slots).

### Forced Splicing Math & Probability
The baseline success of fusing a nucleotide in the compiler is modulated by orbital gravity and local compiler calibration.

#### 1. Baseline Failure Chance ($F$)
*   **Standard Base Failure**: $F = 37.5\%$
*   **Lunar Orbit Influence**:
    *   *New Moon*: Adds a $+3.75\%$ failure debuff ($F = 41.25\%$)
    *   *Full Moon*: Subtracts a $-3.75\%$ failure buff ($F = 33.75\%$)

#### 2. Slot Scaffold Resolution Rules
For each slot $i$ (from 0 to 7), the compiler checks the slot's scaffold alignment:
1.  **Pre-Aligned Manual Gene**: If a matching gene was manually aligned in the slot by the player, the compiler utilizes it directly. Success is guaranteed ($100\%$ chance, cost: $0$ extra genes).
2.  **Match Stock Recruited**: If the player does not have a gene aligned but a perfect matching block exists in inventory, the compiler recruits and deletes it. Success is guaranteed ($100\%$ chance, cost: $1$ matching gene).
3.  **Unstable Base Recruited**: If no perfect match exists, the compiler recruits any standard gene block from inventory. Success is probabilistic (cost: $1$ standard gene).
4.  **Void Scaffold**: If stockpile is completely empty, the compiler operates on a blank grid. Success is probabilistic (cost: $0$ genes).

#### 3. Base-Pair Synthesis & Sacrifice Loop
For each character index $j$ (from 0 to 7) in slot $i$'s segment, the compiler attempts to fuse the expected base:
*   A random roll (0-100) is compared against the active $FailureChance$ ($F$):
    *   **Successful Fusion**: If the scaffold character matches the expected character *or* the random $roll \ge F$, the base is successfully bound.
    *   **Failed Fusion (Sacrifice)**: If the roll fails, the compiler attempts to stabilize the base-pair by consuming backup material from inventory:
        1.  *Targeted Sacrifice*: Search the stockpile for any gene containing the expected character at position $j$, delete it, and force the replacement.
        2.  *Backup Sacrifice*: If no targeted gene exists, delete any random standard gene from stockpile to override the failure.
        3.  *Calibration Step*: Every sacrifice recalibrates the compiler, reducing the failure chance for subsequent attempts:
            $$F_{\text{new}} = \max(0.0, F_{\text{old}} - 3.25\%)$$
        4.  *Depletion Failure*: If a sacrifice is required but the stockpile is completely empty, the splicing protocol aborts immediately with a **depleted stockpile error**.

### Compilation Resolution
*   **Successful forced compilation** produces a creature with name suffix `[FORCED]`, origin `"Forced Synthesis"`, and `coherenceType = "Forced"`.
*   **Forced Loop Cascade**: If the loop cascade toggle is active, the system automatically saves the creature, generates a new random target sequence, waits 1.0 second, and initiates the next forced cycle.

---

## See Also
*   [Master Design & Font Standards](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/master_design_standards.md)
*   [Wireframe Animation Guidelines](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/wireframe_animation_guidelines.md)
*   [Bio-Lab Design Document](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/bio_lab_design.md)
*   [Gen-Vault Design Document](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/gen_vault_design.md)
*   [Scanner Design Document](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/scanner_design.md)
