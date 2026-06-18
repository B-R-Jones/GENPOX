# G.E.N. Network: Creature Factions & Types

This documentation details the biological/cybernetic classifications of specimens compiled within the P.O.X. Emulator system. It details how factions and types are determined procedurally from DNA sequence composition, how they influence combat telemetry stats, how movesets are unlocked and calculated, and outlines the visual badge registry.

---

## 1. Faction Classifications & Deterministic Logic

Every creature compiled in the P.O.X. Reactor belongs to one of four core factions. Faction assignment is determined procedurally by calculating the highest base frequency count (A, G, T, C) in the first 64 characters of the DNA sequence:

$$\text{Faction} = \operatorname{argmax}(A_{\text{count}}, G_{\text{count}}, T_{\text{count}}, C_{\text{count}})$$

### ☣️ Infection
*   **Logical Trigger**: Adenine (`A`) count is the most frequent base in the sequence.
*   **Offline Type Designation**: `Acidic Virus Swarm`
*   **Standard Armament**: `Acid Spurt-Needle`
*   **Aesthetic Theme**: High-contrast Crimson/Vibrant Red (`#EF4444`)
*   **Functional Definition**: Overrunning hostile nodes with aggressive cellular corruption and high vitality reserves.

### 🤖 Mech
*   **Logical Trigger**: Guanine (`G`) count is the most frequent base in the sequence.
*   **Offline Type Designation**: `Heavy Metal Shell`
*   **Standard Armament**: `Titanium Piston Jaw`
*   **Aesthetic Theme**: Sky/Cyber Blue (`#60A5FA`)
*   **Functional Definition**: High-powered armor casings and mechanical force vectors optimized for heavy impact parameters.

### 🧬 Parasite
*   **Logical Trigger**: Thymine (`T`) count is the most frequent base in the sequence.
*   **Offline Type Designation**: `Neural Parasite`
*   **Standard Armament**: `Synapse Siphon`
*   **Aesthetic Theme**: Amethyst Purple (`#A855F7`)
*   **Functional Definition**: Siphoning host processes, hijacking network signals, and executing high-velocity evasive operations.

### 🛡️ Containment (Systems Guard)
*   **Logical Trigger**: Cytosine (`C`) count is the most frequent base (or is the fallback default).
*   **Offline Type Designation**: `Bio-Containment Unit`
*   **Standard Armament**: `Plasma Shield Mesh`
*   **Aesthetic Theme**: Terminal Green (`#00FF41`)
*   **Functional Definition**: Heavy firewall configurations, defensive shield shielding, and stabilizer/recovery protocols.

---

## 2. Creature Attributes & Stat Influence

A creature's base DNA composition directly shapes its physical attributes, combat telemetry, and stability:

### Core Combat Attributes
1.  **Vitality (HP)**: The creature's maximum integrity pool before systemic collapse.
2.  **Aggression (Attack)**: The offensive force multiplier applied during combat breaches.
3.  **Block Shells (Defense)**: The deflection rating reducing incoming breach payloads.
4.  **Speed Rate (Speed)**: Combat velocity determining initiative priority and evasion success.
5.  **Telomeres**: The genetic integrity percentage. Starts at $100\%$. Telomeres degrade over time from intensive operations (like generating QR transponders).

### Faction Stat Bias (Offline Compiler)
Factions apply specific scaling offsets based on base counts:

| Faction | Most Frequent Base | Primary Stat Buffed | Procedural Formula | Associated Badges |
| :--- | :--- | :--- | :--- | :--- |
| **Infection** | Adenine (A) | Vitality (HP) | $100 + (A_{\text{count}} \times 5)$ | `ALPHA GENE` (if HP $\ge 75$ scale equivalent) |
| **Mech** | Guanine (G) | Aggression (Attack) | $20 + (G_{\text{count}} \times 2)$ | `ALPHA GENE` (if Attack $\ge 75$) |
| **Containment** | Cytosine (C) | Block Shells (Defense) | $20 + (C_{\text{count}} \times 2)$ | `ALPHA GENE` (if Defense $\ge 75$) |
| **Parasite** | Thymine (T) | Speed Rate (Speed) | $20 + (T_{\text{count}} \times 2)$ | `ALPHA GENE` (if Speed $\ge 75$) |

> [!NOTE]
> In the AI Compilation Engine (Gemini API), these count-based offsets serve as baseline seeds. The AI model outputs stats in a balanced retro spectrum ($100-250$ for Vitality, $10-99$ for other attributes) matching the DNA composition.

### Telomere Stat Scaling
All combat stats are dynamically scaled in real-time by the current telomere length:

$$\text{Effective Stat} = \max\left(\text{MinBound}, \operatorname{round}\left(\text{Base Stat} \times \left(0.25 + 0.75 \times \frac{\text{Telomeres}}{100}\right)\right)\right)$$

*   **MinBound**: $10$ HP for Vitality, $5$ rating for Attack/Defense/Speed.
*   **Degraded State**: If $\text{Telomeres} < 100$, stats are drawn in alert red to show biological degradation.

### Attribute Wireframe Icon Specifications

Every combat attribute is displayed side-by-side with a retro wireframe vector icon in the Creature Detail Card:

| Attribute | Icon Component | Visual Shape / Description | Color Name | Hex Color |
| :--- | :--- | :--- | :--- | :--- |
| **Vitality (HP)** | `WireframeHeart` | Stylized anatomical/heraldic heart outline drawn with symmetric cubic curves meeting at a bottom apex. | Crimson Red | `#EF4444` |
| **Aggression (Attack)** | `WireframeClaws` | Three slanting curved paths representing scratch marks / claws. | Orange | `#F59E0B` |
| **Block Shells (Defense)** | `WireframeShield` | Classic gothic curved heater shield outline with a pointed bottom apex. | Sky Blue | `#60A5FA` |
| **Speed Rate (Speed)** | `WireframeLightning` | Jagged six-point lightning bolt outline slanting down-right. | Amber Yellow | `#FBBF24` |
| **Telomeres** | `WireframeDna` | Horizontal double-helix wave structure drawn with opposing sine waves and linking base-pair horizontal rungs. | Emerald Green | `#22C55E` |

---

## 3. Special Movesets & Gene Splicing

Special action movesets are locked by default and can **only** be unlocked by modifying the creature's sequence through gene extension (splicing additional 8-character blocks past the base 64 characters). Specimen moves are evaluated sequentially based on spliced segments:

### Move Slots & Unlocking Logic

1.  **Slot 1 (Characters 64 to 72)**:
    *   Evaluates the nucleotide composition of the first appended 8-character block.
    *   Count Adenine (`A`) + Guanine (`G`) vs Thymine (`T`) + Cytosine (`C`).
    *   **If $(A + G) \ge (T + C)$**: Unlocks **BIO-DRAIN REPAIR** (Healing).
    *   **If $(A + G) < (T + C)$**: Unlocks **QUANTUM ESCAPE DEVIATION** (Evasive).

2.  **Slot 2 (Characters 72 to 80)**:
    *   Only unlocks if sequence length is $\ge 80$ characters.
    *   Guarantees type variety by unlocking the alternative type to the move in Slot 1.
    *   **If Slot 1 is Healing**: Unlocks **ELECTROMAGNETIC SHELL DEFLECT** (Evasive).
    *   **If Slot 1 is Evasive**: Unlocks **MICRO-PHAGE EXTRACTION** (Healing).

### Unlocked Move Registry

| Move Name | Type | Combat Description | Trigger Badge |
| :--- | :--- | :--- | :--- |
| **BIO-DRAIN REPAIR** | Healing | Repairs circuitry by siphoning 35 HP from defender. | `MODIFIED` (extended sequence) |
| **QUANTUM ESCAPE DEVIATION** | Evasive | Evasive shift. Negates next attack damage in this round. | `MODIFIED` (extended sequence) |
| **ELECTROMAGNETIC SHELL DEFLECT** | Evasive | Hardened shield. Negates defender damage for this round. | `MODIFIED` (extended sequence) |
| **MICRO-PHAGE EXTRACTION** | Healing | Siphons 35 HP from opponent to mend system hardware. | `MODIFIED` (extended sequence) |

---

## 4. Visual Wireframe Badge Registry

Sequence tags are rendered as wordless retro visual badges to keep list layouts and cards clean. There are exactly 10 badges tracked by the registry:

### 1. `FAVORITE`
*   **Shape/Icon**: Five-pointed Star (Filled)
*   **Color**: Amber Gold (`#FFB300`)
*   **Meaning**: The specimen has been locked/favorited to protect it from deletion or quicken visual lookup.

### 2. `DEFENDER`
*   **Shape/Icon**: Heraldic Shield
*   **Color**: Sky Blue (`#60A5FA`)
*   **Meaning**: Designated as the active firewall node defending against auto-hack payloads.

### 3. `AUTO-HACKER`
*   **Shape/Icon**: Microchip / Gear
*   **Color**: Amber (`#FBBF24`)
*   **Meaning**: Assigned to run background hacking routines against linked network emulators.

### 4. `FULL COHERENCE`
*   **Shape/Icon**: Double Helix / DNA Wave (Pulsing)
*   **Color**: Cyber Green (`#00FF41`)
*   **Meaning**: Sequence matched the active target sequence perfectly upon creation/synthesis.

### 5. `ALPHA GENE`
*   **Shape/Icon**: Lightning Bolt
*   **Color**: Gold (`#FFB300`)
*   **Meaning**: High-tier attribute detected (possesses at least one base stat $\ge 75$).

### 6. `MODIFIED`
*   **Shape/Icon**: Four-pointed Sparkle
*   **Color**: Purple (`#C084FC`)
*   **Meaning**: Sequence has been extended past the standard 64 characters by splicing extra genes. Unlocks the creature's moveset slots.

### 7. `ORIGINAL`
*   **Shape/Icon**: Concentric Target Circles
*   **Color**: Cyber Green (`#00FF41`)
*   **Meaning**: Locally generated and synthesized unit.

### 8. `TRANSFER-ORIGIN`
*   **Shape/Icon**: Double Opposing Arrows
*   **Color**: Red (`#EF4444`)
*   **Meaning**: Specimen acquired from another emulator node via link transfer.

### 9. `NATURAL`
*   **Shape/Icon**: Organic Leaf
*   **Color**: Mint Green (`#10B981`)
*   **Meaning**: Full Coherence achieved via normal splicing/combinator synthesis.

### 10. `FORCED`
*   **Shape/Icon**: Anvil / Hammer
*   **Color**: Orange (`#F59E0B`)
*   **Meaning**: Full Coherence achieved via forced reactor compiler cycles.
