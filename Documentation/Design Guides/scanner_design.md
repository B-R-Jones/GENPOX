# G.E.N. Network: Scanner (Locator & Radar) Design Document

This document outlines the conceptual design, mathematical models, orbital physics, and interface systems of the **Scanner Tab** (internally referred to as the `ScannerView`). The Scanner acts as the geographical search radar and tactical transponder of the GENPOX network, allowing players to detect local anomalies, analyze gravitational wave interference, and dispatch compiled specimens on high-risk harvest sweeps.

---

## 1. Overview of the Scanner

The Scanner interface serves as the primary gateway for acquiring cosmic genes and anomaly elements. It operates in three main states:
1. **Nearby Frequency Locator (`Nearby List` sub-tab)**: A telemetry auditing pane listing the 5 closest anomalies detected in the immediate sector.
2. **Tactical Radar (`Tactical Radar` sub-tab)**: A graphical, CRT-styled vector scan representing geographical terrain, nearby anomalies, and traveling harvesters.
3. **Locked Anomaly Details (Target Focus)**: Engaged when a player targets a specific anomaly. Exposes detailed coordinate telemetry, atmospheric density, and dispatch mission monitoring controls.

Additionally, the Scanner incorporates the **Camera Barcode Transponder Overlay**, enabling barcode scanner scans to capture external specimens.

---

## 2. Interface Sub-Tabs & Mechanics

### Nearby Frequency Locator
Lists the five closest anomalies with metadata rows containing their faction type, name, coordinates, distance, and quick decryption links.

### Tactical Radar View
An interactive, animated coordinate workspace rendered inside a custom cyberglass deck:
*   **Concentric Scope Rings (Deprecated)**: The three concentric scope rings have been deprecated and removed to avoid visual distortion under 2.5D camera tilt/rotation and to optimize drawing performance by eliminating redundant `drawCircle` rasterization calls.
*   **CRT Glitch Simulation**: Renders random tracking glitch bars, horizontal line tears, and phosphor noise snow.
*   **Geographic Frustum Culling & LOD**:
    *   *Geographic Culling*: Before projection, road segments and building polygons are geographically checked against a rotation-invariant culling circle centered at `localMapCenterLat/localMapCenterLng`. To handle 2.5D perspective angling/tilting (where the Y-axis is squashed by `tiltYScale` down to `0.10f`), the culling radius expands along the tilt dimension by dividing the screen bounds radius by `tiltYScale`. This prevents distant buildings/roads from disappearing while culling over 80% of off-screen elements.
    *   *Simplified LOD rendering*: When the camera is angled down (no 3D height extrusion, i.e., `finalExtX == 0f && finalExtY == 0f`) or a building's screen footprint is small (`maxDim < 12 dp`), the renderer bypasses the heavy 3D walls/roof loops. It falls back to a flat 2D rendering path, reducing GPU draw calls from ~20+ down to 3 per building (1 mask fill, 1 holographic fill, 1 stroke outline).
*   **HUD Zoom Controller**: A vertical slider along the right side of the screen allows scaling the range:
    $$\text{Zoom Multiplier} \in \{4.00x, 2.00x, 1.00x, 0.50x, 0.25x\}$$

---

## 3. Locked Anomaly telemetry

Selecting an anomaly locks the transponder onto its frequency, drawing a detailed diagnostics log.

### Telemetry Parameters
*   **Classification**: Faction alignment represented by color-coded signatures:
    *   *Infection*: Red ($\texttt{0xFFEF4444}$)
    *   *Mech*: Yellow/Gold ($\texttt{0xFFFBBF24}$)
    *   *Parasite*: Purple ($\texttt{0xFFA855F7}$)
    *   *Containment & Others*: Cyan ($\texttt{0xFF22D3EE}$)
*   **Coordinates**: Latitude and Longitude coordinates.
*   **Distance**: Geographical separation from player coordinates in feet.
*   **Environment Density**: Atmospheric turbulence factor scaled to a percentage.
*   **Gene Carrier**: The target 8-character cosmic/anomalous gene block carried by the anomaly.
*   **Estimated Accuracy**: The yield likelihood of a successful harvest:
    $$\text{Yield Chance} = \max\left(0, \operatorname{round}\left(100 - \frac{\text{Distance}}{R_{\text{boundary}}} \times 100\right)\right)$$

---

## 4. Dispatch Physics & Transit Math

Launching a specimen to extract a gene block initiates the **Harvest Dispatch Sequence**, which simulates geographical transit and coordinate penetration.

### 1. Orbital Lunar Calibration
The Moon's phase modulates atmospheric resistance and biological decay (mutation rates):
$$\operatorname{scale}_{\text{lunar}} = \frac{1.0 - \cos\left(2\pi \times \frac{\text{lunarAge}}{29.53059}\right)}{2}$$
$$\operatorname{mod}_{\text{resistance}} = 0.7 + 0.6 \times \operatorname{scale}_{\text{lunar}}$$
$$\operatorname{mod}_{\text{mutation}} = 0.5 + 1.0 \times \operatorname{scale}_{\text{lunar}}$$

### 2. Scaffold Penetration (Stalled Depth)
As the dispatched specimen approaches the anomaly, it encounters a forcefield boundary. The creature's effective defense ($D_{\text{eff}}$) determines how deep it penetrates the anomaly before stalling.
*   **Active Field Resistance ($R_{\text{anom}}$)**:
    $$R_{\text{base}} = 0.1 \times R_{\text{boundary}}$$
    $$R_{\text{anom}} = R_{\text{base}} \times \operatorname{mod}_{\text{resistance}}$$
*   **Effective Defense ($D_{\text{eff}}$)**: Includes the synodic resonance modifier between the specimen's faction and the current moon phase:
    $$D_{\text{eff}} = D_{\text{base}} + \operatorname{mod}_{\text{resonance}}$$
*   **Stalled Depth ($Depth_{\text{stalled}}$)**: The percentage of the field boundary successfully bypassed:
    $$Depth_{\text{stalled}} = \min\left(100.0, \max\left(0.0, \frac{D_{\text{eff}}}{R_{\text{anom}}} \times 100\right)\right)$$
*   **Dispatch Distance ($d_{\text{dispatch}}$)**: The final resting distance from the anomaly's epicenter:
    $$d_{\text{dispatch}} = R_{\text{boundary}} \times \left(1.0 - \frac{Depth_{\text{stalled}}}{100.0}\right)$$

### 3. Wave Density & Descent Friction
The density of overlapping anomaly fields creates electromagnetic drag, reducing the creature's descent speed:
*   **Overlapping Density ($D_{\text{combined}}$)**:
    $$D_{\text{combined}} = \sum_{\text{anom } j} \operatorname{density}_j \times \cos(0.02 \cdot d_j + \phi_j) \cdot e^{-0.002 d_j}$$
    Where $d_j$ is the distance to anomaly $j$, and $\phi_j$ is a static phase shift derived from the anomaly's ID hash.
*   **Effective Density ($D_{\text{eff\_density}}$)**: Takes into account the lunar phase shift and coherence shields:
    $$D_{\text{eff\_density}} = \left(D_{\text{combined}} + 0.2 \cdot (\operatorname{scale}_{\text{lunar}} - 0.5)\right). \text{coerceIn}(-0.33, 0.33)$$
    *   *Coherence Shield Benefit*: If the creature possesses a Coherence Shield, it is immune to positive density drag: if $D_{\text{eff\_density}} > 0.0 \implies D_{\text{eff\_density}} = 0.0$.
*   **Descent Speed ($V_{\text{descent}}$)**:
    $$V_{\text{descent}} = V_{\text{travel}} \times (1.0 - D_{\text{eff\_density}})$$

### 4. Duration Calculations
A dispatch mission is split into 5 chronological phases:
1.  **Travel ($t_{\text{travel}}$)**: Transit to the outer field boundary:
    $$V_{\text{travel}} = \text{Speed}_{\text{creature}} \times 13.5$$
    $$d_{\text{travel}} = \max(0.0, \text{Distance}_{\text{anomaly}} - R_{\text{boundary}})$$
    $$t_{\text{travel}} = \max\left(1, \operatorname{round}\left(\frac{d_{\text{travel}}}{V_{\text{travel}}}\right)\right)$$
2.  **Descent ($t_{\text{descent}}$)**: Penetrating the field to the dispatch point:
    $$d_{\text{descent}} = R_{\text{boundary}} \times \frac{Depth_{\text{stalled}}}{100.0}$$
    $$t_{\text{descent}} = \max\left(1, \operatorname{round}\left(\frac{d_{\text{descent}}}{V_{\text{descent}}}\right)\right)$$
3.  **Harvest ($t_{\text{harvest}}$)**: Extracting the target gene block. Fixed duration:
    $$t_{\text{harvest}} = 60\text{ seconds}$$
4.  **Ascent ($t_{\text{ascent}}$)**: Exiting the field boundary:
    $$t_{\text{ascent}} = t_{\text{descent}}$$
5.  **Return ($t_{\text{return}}$)**: Transit back to player coordinates:
    $$t_{\text{return}} = t_{\text{travel}}$$

The total mission duration is:
$$t_{\text{total}} = t_{\text{travel}} + t_{\text{descent}} + t_{\text{harvest}} + t_{\text{ascent}} + t_{\text{return}}$$

---

## 5. Active Mission Dashboard & Telemetry Logs

When a specimen is in transit, the Locked Anomaly pane transforms into the **Mission Control Dashboard**, exposing two sub-views:

### Sequence Scan Pane
Displays real-time mission telemetry:
*   **Progress Gauge**: Measures completion ratio of the overall duration.
*   **Mission Phase Indicator**: Tracks active phase status:
    $$\text{Phase} \in \{\text{"TRAVEL"}, \text{"DESCENT"}, \text{"HARVEST"}, \text{"ASCENT"}, \text{"RETURN"}\}$$
*   **Harvester Tracking**: Activating **🛰️ TRACK HARVESTER** anchors the Tactical Radar viewport on the traveling creature instead of the player, displaying a dotted vector trail and target box labeled `TRK_LOCK`.
*   **DNA Mutation Monitor**: Displays the `DnaComparisonGrid` mapping the creature's original DNA against its current sequence in real-time, showing cosmic mutations occurring in transit.
*   **Recall Button**: Triggers `RECALL SEQUENCE & BANK Stockpile` once the return phase is completed, logging harvested blocks into the inventory.

### Telemetry Logs Pane
An animated console log showing real-time event packets generated by the probe:
*   *Warning/Stall/Failures*: Styled in red to alert the player to field resistance.
*   *Milestones/Successes*: Styled in cyber-green to report successful telemetry capture.

---

## 6. The Mutation Well

During field penetration, radiation from the anomaly's reactor core degrades the harvester's genetic code, triggering mutations.

### Mutation Frequency Formula
The time interval $\Delta t_{\text{mutation}}$ (seconds) between mutations is calculated based on environmental radiation levels:
$$\Delta t_{\text{mutation}} = \operatorname{round}\left( \frac{\frac{480.0 \times 2^{-\frac{Depth_{\text{stalled}}}{25.0}}}{\operatorname{mod}_{\text{mutation}}}}{16.0} \right)$$
*(Enforces a minimum interval of 1 second).*

Anomalous radiation forces a random nucleotide swap at a random sequence index every $\Delta t_{\text{mutation}}$ seconds, mutating the creature's genome.

---

## See Also
*   [Master Design & Font Standards](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/master_design_standards.md)
*   [Gen-Vault Design Document](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/gen_vault_design.md)
*   [Bio-Lab Design Document](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/bio_lab_design.md)
*   [Wireframe Animation Guidelines](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/wireframe_animation_guidelines.md)
