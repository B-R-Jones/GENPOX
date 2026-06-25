# G.E.N. Network: Creature Handling Guidelines & Rules

This document outlines both the design principles and technical implementation rules for compiling, welding, upgrading, and dispatching biological specimens (creatures) across the GENPOX network.

---

## 1. Core Biological Parameters

Every creature is derived from a base 64-character DNA sequence composed of standard nucleotides (`A`, `G`, `T`, `C`).

### Stat Resolution Formulas
Creature stats scale according to nucleotide density inside their active sequence:
- **Vitality**: Base $100 + (A \times 5) + (\text{Length} / 2)$
- **Attack**: resolved as $15 + (G \times 3)$, capped at $99$.
- **Defense**: resolved as $15 + (T \times 3)$, capped at $99$.
- **Speed**: resolved as $15 + (C \times 3)$, capped at $99$.

### Faction Alignment
Faction alignment is determined by the dominant nucleotide frequency:
*   Dominant `A` $\implies$ **Containment** (Cyan signature)
*   Dominant `G` $\implies$ **Infection** (Red signature)
*   Dominant `T` $\implies$ **Mech** (Yellow/Gold signature)
*   Dominant `C` $\implies$ **Parasite** (Purple signature)

---

## 2. Technical Lifecycle & State Management

### Compilation & Splicing
- New creatures are compiled by consuming base sequences stored in the player inventory.
- Appending new 8-character gene blocks to an existing creature permanently updates its sequence, increasing stats and potentially unlocking hybrid moves (e.g. at lengths $\ge 72$ and $\ge 80$).

### Telomere Decay
- Performing high-risk operations (such as harvesting anomalies or undergoing unstable reactor loops) causes telomere degradation.
- Telomeres represent the specimen's active life percentage ($[0 - 100\%]$).
- When a creature's telomere count reaches $0\%$, a **Chromosomal Failure** is logged:
  - The specimen is permanently deleted from the database.
  - $50\%$ of its genetic sequence blocks (shuffled, rounded up) are returned to the player's stock.
  - The remaining blocks are lost forever.

---

## 3. Mission Dispatch & Radar HUD Rules

### targeting Navigation Rule
- Dispatching a creature automatically anchors the satellite transceiver target tracking system (`trackedMissionId`) on that creature's mission.
- This immediately resets current target selections (`selectedAnomalyId = null`), sets the active map view sub-tab to `"radar"`, and transitions the screen to the tactical radar view.

### Layering Priorities on Radar Canvas
- Anomaly epicenter hover labels (composed of Compose layouts) are layered under active creature wireframes.
- Active harvesting probes, target lock indicators, and `TRK_LOCK` labels are drawn on a foreground Canvas block to prevent labels from occluding the target status.

### Visual Styling & Visibility
- Wireframe creatures, locks, and associated native texts are drawn with double size parameters on the radar canvas (minimum scale of $0.28$ and a standard HUD size of $20\text{ dp}$).
- A drop-shadow layer drawn at an offset of $1.5\text{ dp}$ in translucent black (`0xCC000000`) is rendered behind each vector and text element to maximize CRT phosphor contrast.

---

## See Also
- [Master Design & Font Standards](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/fonts_standardization.md)
- [Scanner Locator & Radar Design](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/scanner_design.md)
