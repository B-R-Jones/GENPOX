# Genetics Engine Specification

The **Genetics Engine** is a core mathematical and biophysical utility responsible for simulating DNA thermodynamics, sequencing quality metrics, and genomic adaptation indexes.

- **Class Definition**: [GeneticsEngine.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/GeneticsEngine.kt)

---

## 1. Features and Functionalities

### A. Wallace-Breslauer Melting Temperature ($T_m$)
Calculates the melting temperature of a DNA strand in Celsius, taking into account the monovalent salt concentration of the reactor:
$$T_m = 2 \cdot N_{\text{AT}} + 4 \cdot N_{\text{GC}} - 16.6 \cdot \log_{10}\left(\frac{0.05}{[\text{Salt}]}\right)$$
- **Wallace Formulation**: Provides a baseline calculation where AT pairs contribute $2^\circ\text{C}$ and GC pairs contribute $4^\circ\text{C}$.
- **Salt Correction Factor**: Shifts the $T_m$ up or down depending on monovalent ion concentration ($[\text{Salt}]$) to simulate thermodynamic screening.

### B. Nussinov Dynamic Programming ($MFE$)
Determines the Minimum Free Energy (MFE) in kcal/mol of a single-stranded sequence to estimate its secondary structure folding (hairpin formation):
- **Base Pair Weights**: $\text{G-C} = -3.0$ kcal/mol, $\text{A-T} = -2.0$ kcal/mol, and anomalous pairings (e.g., $\text{X-Y} = -4.0$ kcal/mol).
- **DP Recurrence**:
  $$dp[i][j] = \min \begin{cases} 
    dp[i+1][j] \\
    dp[i][j-1] \\
    dp[i+1][j-1] + \text{Energy}(seq[i], seq[j]) & \text{if pairable} \\
    \min_{i < mid < j} \left( dp[i][mid] + dp[mid+1][j] \right)
  \end{cases}$$
- **Loop Constraint**: Imposes a minimum hairpin loop size of 3 bases (computes width $k \ge 4$).

### C. Phred Quality Scores ($Q$)
Simulates Phred-scale quality scores representing the error probability of nucleotide replication based on the fidelity of the polymerase enzyme:
- **Taq Polymerase**: Low fidelity, returns $Q \in [15.0, 25.0]$.
- **Tth Polymerase**: Medium fidelity, returns $Q \in [25.0, 33.0]$.
- **Pfu Polymerase**: High fidelity (active proofreading), returns $Q \in [35.0, 40.0]$.
- **Error Probability Relation**:
  $$P(\text{error}) = 10^{-\frac{Q}{10}}$$

### D. Codon Adaptation Index ($CAI$)
Evaluates the translation efficiency of a 64-character compiled creature genome against a target faction's codon preference bias:
- **Faction Biases**: Infection favors $A$, Mech favors $G$, Parasite favors $T$, and Containment favors $C$.
- **Codon Weights**: Scaled linearly based on the count of preferred bases inside a triplet ($N_{\text{preferred}}$):
  $$w = 0.2 + 0.8 \cdot \frac{N_{\text{preferred}}}{3}$$
- **Geometric Mean Index**:
  $$CAI = \exp\left(\frac{1}{L} \sum_{i=1}^{L} \ln(w_i)\right)$$

---

## 2. Integration and Usage

### Where it is used:
- [ReactorViewModel.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/ReactorViewModel.kt): To compute thermodynamic metrics when generating anomalous genes and inserting new standard gene blocks in the database.
- [ReactorSimulationEngine.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/ReactorSimulationEngine.kt): To dynamically evaluate the $T_m$, $MFE$, and $Q$ of the growing strand at each step of transcription.
- [SplicerViewModel.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/SplicerViewModel.kt): To calculate the CAI score of spliced creature genomes, which scales their base attributes (vitality, attack, defense, speed).
