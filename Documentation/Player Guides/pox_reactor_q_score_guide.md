# P.O.X. Reactor: Transcription Q-Score & Specimen Vitality Guide

This player guide explains the biophysical mechanics of the **Phred Quality Score (Q-Score)** in the G.E.N.P.O.X. reactor network, how it is calculated during synthesis, and its vital role in creature construction.

---

## 1. What is the Q-Score?

The **Phred Quality Score (Q-score)** is a standard molecular biology metric representing transcription fidelity (accuracy) during gene compilation. A higher Q-score corresponds to a lower probability of incorrect base-pairing during the stepped transcription process.

The formula relating Q-score to error probability ($P$) is:
$$Q = -10 \log_{10}(P)$$

### Q-Score Reference Table
| Q-Score | Base Accuracy | Mutation/Error Probability |
| :--- | :--- | :--- |
| **Q40** | **99.99%** | `1 in 10,000` (Max accuracy) |
| **Q30** | **99.9%** | `1 in 1,000` |
| **Q20** | **99.0%** | `1 in 100` |
| **Q10** | **90.0%** | `1 in 10` |
| **Q5** | **68.4%** | `1 in 3` (Critical collapse boundary) |

---

## 2. How is Q-Score Calculated?

During transcription, each of the 8 steps calculates a step-specific Q-score based on three biophysical inputs:

### A. Base Polymerase Fidelity ($Q_{\text{base}}$)
The active polymerase enzyme sets the baseline accuracy range:
*   **Taq**: $Q_{\text{base}} \in [15.0, 25.0]$ (highly error-prone, fast)
*   **Tth**: $Q_{\text{base}} \in [25.0, 33.0]$ (moderate accuracy, heat-tolerant)
*   **Pfu**: $Q_{\text{base}} \in [35.0, 40.0]$ (extremely high accuracy, proofreading active)

### B. Temperature Deviance ($\eta$)
If the reactor temperature ($T_{\text{react}}$) deviates from the sequence's ideal melting temperature ($T_m$), the enzyme experiences biophysical stress, reducing accuracy:
$$\eta = \exp\left( - \frac{(T_{\text{react}} - T_m)^2}{2 \sigma^2} \right)$$
*   **$\sigma$ (Thermal Tolerance)**: Pfu has a very narrow range ($\sigma = 5.0$), making its Q-score crash if temperature deviates even slightly. Taq ($\sigma = 15.0$) and Tth ($\sigma = 25.0$) are much more forgiving.

### C. Stoichiometric Inlet Deviation (Penalty)
If the raw feedstock inlet sliders are misaligned compared to the target sequence base distribution, a stoichiometric penalty is subtracted:
$$\text{Penalty} = \text{Deviation} \times 15.0 \quad (\text{Capped at } 15.0)$$

### Combined Step Formula
The Q-score for a step is resolved as:
$$Q_{\text{step}} = (Q_{\text{base}} \times \eta - \text{Penalty}) \quad \text{clamped to } [5.0, 40.0]$$
The final Q-score of the compiled gene block is the cumulative running average of all 8 steps.

---

## 3. How Q-Score Affects Creature Stats

When you construct a creature (via the **Splicer** or **Forced Construction**) or upgrade it by welding new genes, the average Q-score of all sequence blocks ($\text{Avg } Q$) directly determines the specimen's starting health and lifespan:

### A. Starting Telomeres (Stability/Lifespan)
Telomeres represent the specimen's active life percentage ($[0 - 100\%]$). The starting telomere value is resolved as:
$$\text{Starting Telomeres} = (\text{Avg } Q \times 2.5) \quad \text{clamped to } [10, 100]$$
*   **High Q-Score ($Q40$)**: Specimen starts with **100% Telomeres** (maximum survival lifespan).
*   **Low Q-Score ($Q10$)**: Specimen starts with only **25% Telomeres** (highly unstable, rapid decay).

### B. Scaled Vitality (Max HP)
While the base vitality is determined by the A-base density in the creature's sequence, the final HP is scaled by its compilation quality:
$$\text{Scaled Vitality} = \text{Base Vitality} \times \left(0.5 + 0.5 \times \frac{\text{Avg } Q}{40.0}\right)$$
*   **Q40 Average**: Specimen retains **100% of its potential HP**.
*   **Q20 Average**: Specimen HP is scaled to **75% of its potential**.
*   **Q5 Average (Collapsed)**: Specimen HP is scaled down to **56%**.

---

## 4. Tips for Maximizing Q-Scores

1.  **Use Pfu for Clones**: Pfu is slower (32 seconds) and expensive, but its proofreading completely ignores inlet crosstalk, guaranteeing near-perfect Q40 accuracy *if* temperature is aligned.
2.  **Phase-Lock the Wave**: Ensure your reactor temperature slider is adjusted so the environment wave merges into a CyberGreen line with the target sequence wave. This guarantees $T_{\text{react}} \approx T_m$, keeping the yield factor $\eta \approx 1.0$.
3.  **Balance Inlets for Taq/Tth**: When utilizing Taq or Tth, you must match the raw base inlet sliders to the proportion of A, G, T, C bases in your target sequence. Failure to do so incurs the stoichiometric penalty and triggers crosstalk mutations.
