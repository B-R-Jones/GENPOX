# G.E.N. Network: Gen-Vault (Library) Design Document

This document outlines the conceptual design, interface console, filtering algorithms, combat stats scaling, and custom actions of the **Gen-Vault Tab** (internally referred to as the `VaultView`). The Gen-Vault acts as the central genetic containment database and telemetry archive of the GENPOX network, allowing players to audit, filter, harvest, extend, and deploy their synthesized P.O.X. specimens.

---

## 1. Overview of the Gen-Vault

The Gen-Vault contains all compiled and spliced genomic specimens synthesized by the player. The interface operates in two primary visual states:
1. **Secure Gen-Vault Library (Default Registry)**: A telemetry and registry summary console. It displays the database version, operational status, matched selections tracker, and a collapsible filter module. In this state, players can trigger the sequence directory search.
2. **Creature Detail Card (Active Focus)**: Opened when a specific specimen is selected from the registry directory. It displays structural wireframes, genetic transponder coupling, combat attributes, movesets, gene salvage modules, extension options, and system roles.

If the vault has zero specimens (e.g., before any splicing or scan collection), the interface displays a fallback offline message:
$$\text{Status} = \text{OFFLINE}$$
$$\text{Display: } \texttt{"GEN-VAULT DATABANKS OFFLINE. SEED GENETIC HOSTS IN COMBINATOR."}$$

---

## 2. Exploration Filters Console

To navigate large datasets of compiled specimens, the Gen-Vault integrates a collapsible **Filter Console** featuring dropdown selectors and quick-reset mechanics.

### Selection Telemetry Tracker
The header displays real-time matching telemetry comparing active matched records against the total database size:
$$\text{Active Ratio} = \frac{N_{\text{matched}}}{N_{\text{total}}}$$

### Sorting and Classification Rules
The dropdown controls execute the following sorting and filtering priorities on the creature list:

#### 1. Sorting Rules
*   **Name (A ➔ Z / Z ➔ A)**: Sorts alphabetically by name string.
*   **Type (A ➔ Z / Z ➔ A)**: Sorts alphabetically by creature classification type.
*   **Faction (A ➔ Z)**: Sorts alphabetically by faction alignment name.
*   **Vitality (High ➔ Low)**: Sorts in descending order of the creature's Vitality stat.
*   **Attack (High ➔ Low)**: Sorts in descending order of the creature's Aggression (Attack) stat.
*   **Defense (High ➔ Low)**: Sorts in descending order of the creature's Block Shells (Defense) stat.
*   **Speed (High ➔ Low)**: Sorts in descending order of the creature's Speed Rate (Speed) stat.
*   **Tag Density**: Sorts by the number of active tags assigned to the creature (descending order).

#### 2. Classifiers
*   **Faction Filter**: Filters by exact faction string matches:
    $$\text{Faction} \in \{\text{"Infection"}, \text{"Mech"}, \text{"Parasite"}, \text{"Containment"}\}$$
*   **Creature Type Filter**: Filters by unique types extracted dynamically from the database.
*   **Creature Tag Filter**: Filters based on active status tags:
    $$\text{Tags} \in \{\text{"Favorite"}, \text{"Defender"}, \text{"Auto-Hacker"}, \text{"Full Coherence"}, \text{"Natural"}, \text{"Forced"}, \text{"Alpha Gene"}, \text{"Modified"}, \text{"Original"}, \text{"Transfer Origin"}\}$$

#### 3. Clear Filters Action
The reset trigger resets all filters to standard values:
$$\text{libSortBy} \leftarrow \text{"name-asc"}$$
$$\text{libFilterFaction} \leftarrow \text{"ALL"}$$
$$\text{libFilterType} \leftarrow \text{"ALL"}$$
$$\text{libFilterTag} \leftarrow \text{"ALL"}$$

---

## 3. The Creature Detail Card

When a sequence is decrypted from the directory, the detail card displays comprehensive bio-telemetry.

### Identity & Factions
Each card displays the specimen's unique ID, capitalized name, and creature type, alongside faction color badging:
*   **Infection**: Red ($\texttt{0xFFEF4444}$)
*   **Mech**: Blue ($\texttt{0xFF60A5FA}$)
*   **Parasite**: Purple ($\texttt{0xFFA855F7}$)
*   **Containment & Others**: Green ($\texttt{0xFF00FF41}$)

### Visual Reconstruction Sub-Panels
*   **Bio-Physical Recon**: A rotating 2D/3D wireframe render of the specimen based on its DNA sequence. Selecting this launches the fullscreen **3D Holo-Struct Recon** transponder overlay.
*   **Genetic QR Coupling**: Displays a transponder matrix representing the creature's base64 data. Selecting this launches the transponder transception modal to share specimen custody with other emulators. Due to scanner radiation, it degrades the creature's telomeres by $15\%$:
    $$\text{Telomeres}_{\text{new}} = \max(0\%, \text{Telomeres}_{\text{old}} - 15\%)$$

### Armament & Lore
Contains the designated weapon structure (Primary Weapon) and background archival lore describing the creature's biological context.

---

## 4. Combat Telemetry & Telomere Scaling

A specimen's combat performance is strictly regulated by its genetic health. As telomeres degrade, combat attributes suffer a linear penalty.

### Telomere Scaling Formula
The effective combat stats are calculated using a scale factor $F_{\text{scale}}$ derived from the remaining Telomere percentage:
$$F_{\text{scale}} = 0.25 + 0.75 \times \frac{\text{Telomeres}}{100}$$

For base stats (Vitality $V$, Attack $A$, Defense $D$, Speed $S$), effective values are computed as:
$$\text{Vitality}_{\text{eff}} = \max(10, \operatorname{round}(V_{\text{base}} \times F_{\text{scale}}))$$
$$\text{Attack}_{\text{eff}} = \max(5, \operatorname{round}(A_{\text{base}} \times F_{\text{scale}}))$$
$$\text{Defense}_{\text{eff}} = \max(5, \operatorname{round}(D_{\text{base}} \times F_{\text{scale}}))$$
$$\text{Speed}_{\text{eff}} = \max(5, \operatorname{round}(S_{\text{base}} \times F_{\text{scale}}))$$

### Visual Degradation Warning
*   **Healthy ($>65\%$)**: Renders the telomere bar in Green ($\texttt{0xFF22C55E}$).
*   **Unstable ($30\% \text{ to } 65\%$)**: Renders the telomere bar in Orange ($\texttt{0xFFF59E0B}$).
*   **Degraded ($\le 30\%$)**: Renders the telomere bar in Red ($\texttt{0xFFEF4444}$).
*   When a creature is degraded ($\text{Telomeres} < 100$), its stats turn Red, and the base attributes are displayed in parentheses next to the effective stats: e.g., $\texttt{12 HP (40)}$.

---

## 5. Genomic Sequence & Mutation Grid

### Sequence Segments Grid
The 64-character DNA string is visually parsed into 8 blocks of 8 characters:
$$\text{Sequence} = B_1 B_2 B_3 B_4 B_5 B_6 B_7 B_8$$
*   **Anomalous Blocks**: If a block represents an anomalous gene, it is styled with purple borders and backgrounds.
*   **Coherence Check**: Natural/Standard blocks are color-coded based on block indexing to aid structural readability.

### Mutation Comparison
If a specimen has undergone active mutations (`isMutated = true`), the card displays the `DnaComparisonGrid` mapping the original nucleotide configuration directly against the mutated sequence to visualize delta changes.

---

## 6. Action Triggers & System Roles

The footer of the detail card provides utility triggers to assign the specimen to system-level routines or salvage its genomic structure.

### Role Assignments
*   **Defender**: Assigns the specimen as the active node firewall defender. Only one active defender can be designated across the system.
*   **Auto-Hacker**: Enrolls the specimen into background terminal decryption sweeps.
*   **Favorite**: Favorites/pins the specimen within the containment vault.
*   **Audio Emit**: Triggers synthesis of acoustic sound waves based on the creature's DNA sequence.

### Gene Harvesting Matrix (Incineration)
If a player requires a specific gene block for splicing but lacks inventory stock, they can harvest a segment from a compiled creature:
1. The player selects exactly one of the 8 blocks ($B_i$) of the creature's sequence.
2. If the block is not in the stockpile, the interface displays a yellow **"NEW"** badge.
3. Engaging **Harvest Incineration** extracts the selected gene block, increments its count in the archive inventory, and **purges the creature permanently** from the databank.

### Gene Sequence Extension Lab
Allows appending up to two additional 8-character gene blocks to the sequence, increasing the maximum genome capacity to 80 characters ($10$ blocks total).
*   **Alignment Suffix Rule**: The next appended gene must start with the reversed string of the current sequence's final 4 characters:
    $$\text{Prefix}_{\text{next}} = \operatorname{reverse}(\text{Sequence}[-4:])$$
*   **Step-by-Step Search**: Players search matching stock interactively in stages ($1\text{-}2\text{bp}$, $3\text{-}4\text{bp}$, $5\text{-}6\text{bp}$, $7\text{-}8\text{bp}$).
*   **Anomalous Extensions**: Appending anomalous gene blocks is allowed but flags the specimen's containment telemetry.

### Anomaly Transmission Link (Dispatch)
If the player locks an active anomaly in the Scanner view, they can dispatch the specimen:
*   The dispatch mission runs in the background.
*   The total duration is scaled inversely by the creature's effective Speed stat.
*   Upon completion, the player can recall the mission to bank the harvested genetic material.

---

## See Also
*   [Master Design & Font Standards](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/master_design_standards.md)
*   [Splicer Design Document](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/splicer_design.md)
*   [Bio-Lab Design Document](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/bio_lab_design.md)
*   [Wireframe Animation Guidelines](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/wireframe_animation_guidelines.md)
*   [Scanner Design Document](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/scanner_design.md)
