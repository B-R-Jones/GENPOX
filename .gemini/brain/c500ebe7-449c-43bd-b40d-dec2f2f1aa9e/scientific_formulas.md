# GENPOX: Scientific and Mathematical Formulas Documentation

This document outlines the unit scientific and mathematical formulas used across the `GENPOX` engine (both Android native and web console platforms), describing their mathematical definitions, parameter mappings, and active gameplay loops.

---

## 1. Lunar Synodic Cycle & Phase Engine

### Mathematical Formula
The synodic month tracks the average duration between consecutive new moons (approximately $29.53059$ days).
Given a current timestamp in milliseconds ($t$) and the reference epoch for a known New Moon ($t_{ref} = \text{May 17, 2026, 01:54:00 UTC}$):

$$d_{diff} = \frac{t - t_{ref}}{86,400,000}$$

$$Age_{lunar} = (d_{diff} \bmod 29.53059 + 29.53059) \bmod 29.53059$$

### Phase Classification
The synodic cycle is partitioned into 8 primary phases:

| Lunar Age Range (Days) | Moon Phase Name |
| :--- | :--- |
| $[0, 1.0)$ or $(28.53059, 29.53059]$ | New Moon (Apex) |
| $[1.0, 6.38)$ | Waxing Crescent |
| $[6.38, 8.38)$ | First Quarter |
| $[8.38, 13.77)$ | Waxing Gibbous |
| $[13.77, 15.77)$ | Full Moon (Apex) |
| $[15.77, 21.15)$ | Waning Gibbous |
| $[21.15, 23.15)$ | Third Quarter |
| $[23.15, 28.53059]$ | Waning Crescent |

### Gameplay Loop Impact
- **Wave Multipliers**: Directly affects the base-pair wave catalysts multipliers.
- **Lunar Influence State**: Activates the lunar influence window (when $Age_{lunar} < 8.0$ days), scaling spawn rates and encounter factors.
- **Visual rendering**: Feeds synodic coordinates into the scanning maps and canvas visualizations.

---

## 2. Daily Wave Selection & Suppression Engine

### Mathematical Formula
To generate deterministic daily base-pair waves, the system hashes the date string `YYYY-MM-DD` using a JS-compatible signed 32-bit FNV-like polynomial rolling hash:

$$h_0 = 0$$

$$h_{i} = \left((h_{i-1} \cdot 32) - h_{i-1} + \text{code}(char_i)\right) \pmod{2^{32}}$$

$$Hash = \left| \text{toInt32}(h_n) \right|$$

#### Base-Pair Selection:
- Primary Base Catalyst Index:
  $$Idx_{primary} = Hash \bmod 4$$
- Secondary Base Catalyst Index:
  $$Idx_{secondary} = (Hash + 1 + (Hash \bmod 3)) \bmod 4$$
- Nucleobase mapping: $[0 \rightarrow \text{A}, 1 \rightarrow \text{G}, 2 \rightarrow \text{T}, 3 \rightarrow \text{C}]$

#### Week 3 Suppression Check:
For calendar days $D \in [15, 21]$ (representing Week 3 of the month):

$$\text{isSuppressed} = (Hash \bmod 100) < 50$$

### Gameplay Loop Impact
- **Daily Wave Activity**: Determines the active molecular pair of the day (e.g. `GA WAVE` or `AT WAVE`).
- **Suppression State**: Suppresses active waves in Week 3 on 50% of dates, triggering `DORMANT (CONGESTED DECAY)` cycles with standard flat probabilities.

---

## 3. Moon Phase Multiplier Modulation

### Mathematical Formula
Base-pair spawn multipliers decay and swell smoothly based on the moon's position in its synodic orbit. The angle is calculated as:

$$\theta_{moon} = \frac{2 \pi \cdot Age_{lunar}}{29.53059} - \frac{\pi}{2}$$

The multiplier adjustment modifier is:

$$\Delta_{moon} = 0.0125 \cdot \sin(\theta_{moon})$$

The dynamic multipliers applied to the base-pairs are:

$$M_{primary} = 1.125 + \Delta_{moon}$$

$$M_{secondary} = 1.625 + \Delta_{moon}$$

### Gameplay Loop Impact
- **Splicing Probability Boost**: Scales raw harvesting weights. During the Full Moon peak, multipliers are maximized; during the New Moon trough, they decay.

---

## 4. Weighted Markov-Chain Gene Generator

### Mathematical Formula
Standard gene sequences are generated in blocks of 8 nucleobases. A first-order Markov chain is utilized to bias the sequence creation towards the active daily wave pair ($b_1 \rightarrow b_2$):

For each index $i \in [0, 7]$ of the block:
1. Initialize the transition weights:
   $$W(A) = 1.0, \quad W(G) = 1.0, \quad W(T) = 1.0, \quad W(C) = 1.0$$
2. Adjust transition weights depending on the previous base:
   - If $char_{i-1} = b_1$ (primary catalyst):
     $$W(b_2) = M_{secondary}$$
   - Otherwise:
     $$W(b_1) = M_{primary}$$
3. Perform a weighted roulette-wheel selection using the summed weights.

### Gameplay Loop Impact
- **Bio-Lab Splicer**: Generates biased gene blocks from the active reactor wave. Splicing actions yield higher frequencies of wave sequence strings for target matching.

---

## 5. Spectrum Wave Coupling (Diurnal Energy Level)

### Mathematical Formula
Fluctuations in global atmospheric network energy follow a diurnal cycle with a frequency of 4 oscillations per day (6-hour cycles). Given the fraction of the day elapsed ($F_{day} \in [0, 1)$):

$$\text{SinusoidalFluctuation} = 12.375 \cdot \sin(F_{day} \cdot 8\pi)$$

$$\text{Coupling} = \text{Clamp}(80.0 + \text{SinusoidalFluctuation}, 0.0, 100.0)$$

### Gameplay Loop Impact
- **Anomaly Engine Unit**: Modulates the harmonic synthesis success chance of anomalous genes based on the exact second the player interacts with the reactor console.

---

The Anomaly Engine consumes standard genes to forge anomalous sequences. The success rate ($P_{success}$) incorporates logarithmic base scaling (using unique gene sequence counts to establish a natural upper bound), Gaussian resonance peaks, and harmonic diurnal wave coupling.

### 1. Base Logarithmic Scaling
Given the player's grand total of cataloged **unique** standard nucleotides ($N_{total}$, calculated as $8 \times \text{number of unique standard gene sequences}$):
- If $N_{total} \le 10,000 \Rightarrow P_{base} = 1.0\%$
- If $N_{total} \ge 250,000 \Rightarrow P_{base} = 100.0\%$
- For $10,000 < N_{total} < 250,000$:

$$t_{scale} = \frac{\ln(N_{total}) - \ln(10,000)}{\ln(250,000) - \ln(10,000)}$$

$$P_{base} = 1.0 + 99.0 \cdot t_{scale}$$

### 2. Gaussian Resonance Peaks
To reward players targeting specific base probability milestones, the engine introduces Gaussian resonance boosts centered around multiples of $14\%$ ($P_{target} \in \{14, 28, 42, 56, 70, 84, 98\}$):

$$Boost_i = 6.5 \cdot \exp\left( - \left(\frac{P_{base} - P_{target, i}}{1.8}\right)^2 \right)$$

$$Boost_{max} = \max_i(Boost_i)$$

### 3. Harmonic Diurnal Modulation
The current diurnal coupling value modifies the final success rate:

$$Mod_{harmonic} = (\text{Coupling} - 80.0) \cdot 0.25$$

### 4. Final Success Chance

$$P_{success} = \text{Clamp}(P_{base} + Boost_{max} + Mod_{harmonic}, 1.0, 100.0)$$

### Gameplay Loop Impact
- **Anomaly Synthesizer**: Determines the probability that standard gene conversion successfully spawns an anomalous gene block containing rare symbols (`XZYW?!$%&@#`).

---

## 7. Procedural Creature Stat Allocation

### Mathematical Formula
Bot specimens are constructed from 64-character DNA sequences. Their statistics and faction alignments are deterministically resolved by counting the occurrences of each nucleobase: $N_A$, $N_G$, $N_T$, and $N_C$.

#### Faction Allocation:
Dominant base count dictates the faction:

$$\text{Faction} = \begin{cases} 
\text{Infection} & \text{if } N_A = \max(N_A, N_G, N_T, N_C) \\
\text{Mech} & \text{if } N_G = \max(N_A, N_G, N_T, N_C) \\
\text{Parasite} & \text{if } N_T = \max(N_A, N_G, N_T, N_C) \\
\text{Containment} & \text{otherwise} 
\end{cases}$$

#### Stat Formulas:
- **HP (Vitality)**:
  $$Vitality = 100 + (N_A \cdot 5)$$
- **Attack Rating**:
  $$Attack = 20 + (N_G \cdot 2)$$
- **Defense Rating**:
  $$Defense = 20 + (N_C \cdot 2)$$
- **Speed Rating**:
  $$Speed = 20 + (N_T \cdot 2)$$

### Gameplay Loop Impact
- **Creature Splicer & Combinator**: When a 64-character sequence is compiled into a bot specimen, this algorithm deterministically calculates combat stats, faction traits, lore, and visual representations.

---

## 8. Procedural Audio Synthesizer Oscillators

The native synthesizer uses math functions to construct waveforms directly into 16-bit mono PCM stream buffers.

### Waveform Math
Given a current phase angle $\phi$ (incremented by $\Delta\phi = \frac{2\pi \cdot f}{SampleRate}$ on each sample):

- **Sine Wave**:
  $$y(t) = \sin(\phi)$$
- **Triangle Wave**:
  $$y(t) = \begin{cases} 
  -1.0 + \frac{4.0 \cdot \phi_{norm}}{2\pi} & \text{if } \phi_{norm} < \pi \\ 
  3.0 - \frac{4.0 \cdot \phi_{norm}}{2\pi} & \text{otherwise} 
  \end{cases}$$
  where $\phi_{norm} = \phi \bmod 2\pi$.
- **Sawtooth Wave**:
  $$y(t) = -1.0 + 2.0 \cdot \left(\frac{\phi \bmod 2\pi}{2\pi}\right)$$
- **Square Wave**:
  $$y(t) = \begin{cases} 
  1.0 & \text{if } \sin(\phi) \ge 0 \\ 
  -1.0 & \text{otherwise} 
  \end{cases}$$

### Envelopes & Sweeps
- **Decay Envelope**: Linear decay multiplier:
  $$Env(t) = 1.0 - \frac{i}{Samples_{total}}$$
- **Frequency Sweep**: Logarithmic frequency slide:
  $$f(t) = f_{start} \cdot \left(\frac{f_{end}}{f_{start}}\right)^{\frac{i}{Samples_{total}}}$$

### Gameplay Loop Impact
- **Console Audio Feedback**: Drives the procedurally generated sounds, including the combinator tick, compiler alerts, success bells, reject buzzers, and combat trade signals.
