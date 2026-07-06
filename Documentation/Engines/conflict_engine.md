# Conflict Engine Specification

The **Conflict Engine** is the core turn-based simulation utility responsible for combat resolution, specimen hacking, and environmental survival/decay mechanics.

- **Class Definition**: [ConflictEngine.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/ConflictEngine.kt)

---

## 1. Features and Functionalities

### A. Combatant Initialization & Attribute Scaling
Initializes a combat state for a creature, scaling its attributes by its current telomeric integrity factor:
$$\text{Factor} = 0.25 + 0.75 \cdot \left(\frac{\text{Telomeres}}{100}\right)$$
$$\text{HP}_{\text{combat}} = \text{round}(\text{vitality} \cdot \text{Factor})$$
$$\text{Attack}_{\text{combat}} = \text{round}(\text{attack} \cdot \text{Factor})$$
$$\text{Defense}_{\text{combat}} = \text{DeploymentEngine.calculateEffectiveDefense}(\text{defenseBase}, \text{faction}, \text{phase})$$
- Defense incorporates the active daily lunar phase resonance (e.g. +15 defense during favored phases).
- Translates trailing gene blocks (indexes 64-80) into custom active movesets (`BIO_DRAIN_REPAIR`, `QUANTUM_ESCAPE_DEVIATION`, `ELECTROMAGNETIC_SHELL_DEFLECT`, etc.).

### B. Turn-Based Battle Loop
Executes turn-by-turn combat cycles between an attacker and a defender:
- **Turn Order**: Determined dynamically by speed attributes. The faster combatant acts first.
- **Actions**:
  1. **Evaded State**: Skips defense roll and evades damage.
  2. **Deflection State**: Reduces damage to zero.
  3. **Combat Skills**:
     - *BIO_DRAIN_REPAIR*: Deals damage and heals the attacker.
     - *QUANTUM_ESCAPE_DEVIATION*: Evades the next incoming attack.
     - *ELECTROMAGNETIC_SHELL_DEFLECT*: Deflects damage during this turn.
     - *MICRO_PHAGE_EXTRACTION*: Deals high damage and siphons payload.
  4. **Standard Strike**: Deals basic damage calculated by subtracting defense from attack, bounded to a minimum of 2 HP:
     $$\text{Damage} = \max(2, \text{Attack} - \text{Defense})$$

### C. Environmental Survival Simulation
Simulates the survival of deployed specimens against environmental hazards and telomere decay:
- **Decay Modifiers**: Solar and lunar configurations speed up or slow down telomere deterioration.
- **Harvest Rolls**: Specimens gather environmental gene segments over a series of ticks until their telomeres decay to zero or they are recalled.

### D. Specimen Hacking Runs
Resolves direct hacking and database infiltration mechanics:
- Hacker specimens attempt to siphon payloads from enemy vaults.
- High security thresholds spark direct security counter-combat runs.

---

## 2. Integration and Usage

### Where it is used:
- [MainViewModel.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/MainViewModel.kt): To run vault defenses, combat matches against rogue factions, and manage combat states.
- [ScannerViewModel.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/ScannerViewModel.kt): To resolve combats against anomaly cores during deployment and to tick environmental decay on active siphoning creatures.
