# GENPOX: UI & Interaction Small Ideas

This living document tracks potential animation updates, graphic ideas, and visual polish notes for the `GENPOX` console interfaces.

---

## 1. Spinning Vector Graphics Inspiration
The rotating double-helix DNA graphic (rendered dynamically using trigonometric sine/cosine canvas draw commands) on the Splicing screen adds premium visual weight. Below are other UI views that could benefit from similar styled custom canvas animations:

*   **Reactor Thermal Lock (Bio-Lab Tab)**:
    *   *Idea*: A circular, rotating freeze graphic indicating when reactor coils are frozen (`isForcedLoopActive`). It could draw concentric dashed frost rings spinning in opposite directions, collapsing inward as the countdown ticks down.
*   **Decrypted Anomaly Engine (Anomaly Subtab)**:
    *   *Idea*: A pulsing, spiral particle vortex when anomalous scanning is active. It could render particle lines representingSynodic wave alignments curving into a central gravity point, accelerating when modifiers/reserves are high.
*   **Transceiver Map Scanner (Map Tab)**:
    *   *Idea*: An active radar sweep overlay on the Leaflet/Google Map views that uses semi-transparent sweeping arcs of HSL-tailored colors. It would pulse when a nearby nucleotide node or deep thermal anomaly enters scanning range.
*   **Data Vault Decryption Loader (Vault Tab)**:
    *   *Idea*: A rotating hexagonal cyber-shell loader that unlocks layer-by-layer (spinning out outer shells) as security clearances are resolved during cataloging.
*   **NTP Sync status (Main Header)**:
    *   *Idea*: Replacing the simple blinking green dot with a micro-spinning gear or signal wave vector animation next to `NTP_SYNC`.

---

## 2. Atmospheric / Oscillation Cycles Discussion
*   **Concept**: Synodic oscillation wave patterns or shifting atmospheric cycles that dynamically impact localized environmental variables like scan accuracy, mutation frequencies, and well density fluctuations over time.
*   **Discussion Points**:
    - How do these cycles synchronize with the standard 29.53-day lunar age?
    - Should the frequency of these cycles trigger on exact calendar dates, system uptime, or active reactor temperature parameters?
    - Visual display design: rendering oscillating sine waves or shifting atmospheric particle densities directly inside the target scanning dashboard.
