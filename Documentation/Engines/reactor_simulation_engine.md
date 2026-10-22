# Reactor Simulation Engine Specification

The **Reactor Simulation Engine** is a specialized biophysical simulation utility that manages the step-by-step transcription, chemical buffer reactions, and thermodynamic collapse conditions of DNA synthesis runs.

- **Class Definition**: [ReactorSimulationEngine.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/ReactorSimulationEngine.kt)

---

## 1. Features and Functionalities

### A. Stepped Transcription Simulation
Simulates the sequential, base-by-base transcription of an 8-base target sequence over time:
- Employs a non-blocking coroutine delay loop (`delay(stepIntervalMs)`) to pace the assembly.
- Triggers a callback (`onStep`) after transcribing each nucleotide, sending back the growing sequence, real-time quality metrics, and thermodynamic values.

### B. Real-Time Biophysical Filters
At each step of transcription, the engine runs a suite of biophysical checks to determine if the reaction continues or collapses:

1. **Inlet Deprivation**:
   - *Check*: Asserts that the feedstock inlet flow ratio for the base currently being transcribed is greater than $5\%$ ($0.05$).
   - *Failure*: Halts transcription immediately, collapsing the reaction.

2. **GC Hairpin Stalling**:
   - *Check*: Triggers if the reactor temperature is below the salt-stabilized stall threshold:
     $$T_{\text{react}} < 28^\circ\text{C} + \left(30^\circ\text{C} \cdot [\text{Salt}]\right)$$
     AND the growing sequence's Minimum Free Energy (MFE) is $\le -5.0$ kcal/mol.
   - *Mitigation*: The presence of the chemical solute **DMSO** prevents stalling.
   - *Failure*: Catastrophic reaction collapse.

3. **AT Denaturation**:
   - *Check*: Triggers if sequence length is $\ge 4$, GC-content is $< 40\%$, and temperature is above the denaturation threshold:
     $$T_{\text{react}} > 22^\circ\text{C} + \left(30^\circ\text{C} \cdot [\text{Salt}]\right)$$
   - *Mitigation*: The presence of the chemical solute **Netropsin** protects AT bonds.
   - *Failure*: Catastrophic reaction collapse.

4. **Enzyme Thermal Limits**:
   - *Taq Polymerase*: Denatures if temperature exceeds $45.0^\circ\text{C}$.
   - *Pfu Polymerase*: Denatures if temperature exceeds $40.0^\circ\text{C}$.
   - *Tth Polymerase*: Inactivates if temperature falls below $25.0^\circ\text{C}$.

### C. Celestial Well Siphoning (Anomalous Bases)
When transcribing under a specific host planet at its associated step (e.g. Venus at step 2), the engine rolls for environmental mutations:
- If the Phred Q-score is sufficiently high, there is a chance ($15\%$ to $40\%$ depending on planetary alignment) of incorporating an anomalous base (`X`, `Z`, `Y`, or `W`) into the strand.

### D. Reagent & Enzyme Costs
The engine deterministically calculates feedstock base consumption prior to reaction ignition (Bio-Waste is not required for enzyme programming):

1. **Polymerase Enzyme Costs (`calculatePolymeraseCost`)**:
   - **Taq Polymerase**: Free ($0$ cost across all bases).
   - **Tth Polymerase**:
     - *Base Cost*: **100 units from all bases** ($A, G, T, C$).
     - *Multiplier Additionals*: Multiplies each occurrence of a base in the target sequence by **100 units** ($N_{\text{base}} \times 100$).
     - *Formula*: $\text{Cost}(B) = 100 + \text{count}(B) \times 100$.
     - *Example Sequence (`AATGCGTG`)*:
       - $A: 100 + 2 \times 100 = 300$
       - $C: 100 + 1 \times 100 = 200$
       - $G: 100 + 3 \times 100 = 400$
       - $T: 100 + 2 \times 100 = 300$
   - **Pfu Polymerase**:
     - *Base Cost*: **200 units from all bases** ($A, G, T, C$).
     - *Multiplier Additionals*: Multiplies each occurrence of a base in the target sequence by **200 units** ($N_{\text{base}} \times 200$).
     - *Formula*: $\text{Cost}(B) = 200 + \text{count}(B) \times 200$.
     - *Example Sequence (`AATGCGTG`)*:
       - $A: 200 + 2 \times 200 = 600$
       - $C: 200 + 1 \times 200 = 400$
       - $G: 200 + 3 \times 200 = 800$
       - $T: 200 + 2 \times 200 = 600$
   - *Bio-Waste Cost*: **0** (Bio-waste is not consumed for enzyme programming).

2. **Chemical Solute Cofactor Costs (`calculateSoluteCost`)**:
   - **None**: Free ($0$ cost).
   - **DMSO**: Requires **100 units of each base** ($A, G, T, C$) per synthesis run.
   - **Netropsin**: Requires **100 units of each base** ($A, G, T, C$) per synthesis run.

---

## 2. Integration and Usage

### Where it is used:
- [ReactorViewModel.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/ReactorViewModel.kt): Inside the `initiateStandardSynthesis` coroutine block to execute the transcription runs.
