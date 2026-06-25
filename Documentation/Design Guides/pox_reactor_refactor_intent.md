# Branch Intent: POX Reactor Refactor

This branch is dedicated to refactoring the **P.O.X. Reactor (Standard Synthesis)** and the **Anomaly Engine (Unstable Fusion)** from a passive, background random number generator (RNG) process to a deep, active, and mathematically rigorous biochemical simulation.

## Core Technical Objectives

1. **Thermodynamic Modeling ($T_m$ and $\Delta G$)**:
   * Calculate melting temperatures ($T_m$) for synthesized 8-character DNA blocks based on sequence GC-content, salt concentration, and ambient temperature.
   * Model synthesis yield using a thermal tolerance curve centered around $T_m$.

2. **Base-Pair Targeting Symmetry (GC vs. AT Balance)**:
   * **High Temperature Regime**: Selects for thermally stable GC-rich sequences (favouring Mech and Containment factions).
   * **Low Temperature Regime ($15^\circ\text{C} - 30^\circ\text{C}$)**: Induces thermodynamic trapping where GC-rich sequences fold into rigid hairpins that stall polymerases, successfully selecting for open, single-stranded AT-rich sequences (favouring Infection and Parasite factions).
   * **Chemical Solutes**: Introduce **DMSO / Formamide** (GC-destabilizers to prevent hairpins) and **Netropsin** (AT-stabilizers to protect AT bonds under thermal stress) as active reagents.

3. **Polymerase Fidelity & Quality Metrics (Phred Scores)**:
   * Track synthesis reliability per base using Phred Quality Scores ($Q$).
   * Allow players to choose active enzymes in the chamber:
     * *Taq Polymerase*: Fast cycle speed, low fidelity (low $Q$-scores, high mutation discovery rates).
     * *Pfu Polymerase*: Slow cycle speed, proofreading active, high fidelity (high $Q$-scores, stable templates).

4. **Secondary Structure Folding (Minimum Free Energy - MFE)**:
   * Implement a simplified Nussinov dynamic programming algorithm to calculate the Minimum Free Energy (MFE) of single-stranded folding.
   * Apply MFE values to determine structural stability and assign splicing stalling penalties in the Splicer.

5. **Codon Adaptation Index (CAI)**:
   * Map each creature faction (Infection, Mech, Parasite, Containment) to a distinct codon usage bias table.
   * Calculate the CAI of compiled 64-character DNA to determine baseline combat stat scaling.

6. **Interactive Reactor Controls UI**:
   * Introduce dials/sliders for Temperature ($T_{\text{react}}$), Salt Concentration ($[Na^+]$), and Chemical Solute Injection.
   * Implement an interactive enzyme chamber selector and wave tuning frequency controls.
