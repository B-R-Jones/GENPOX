# P.O.X. Forced Construction Log Texts Reference

Below is the complete inventory of all text patterns, warnings, metrics, and event logs emitted during the **Emergency Forced Compilation / Auto-Loop Cascade** cycles.

---

## 1. Sequence Initialization (Start of 8.0s Cycle)

- **Initialization Header**:
  ```text
  [INIT] >> FORCING TARGET SEQUENCE COMPILATION...
  ```

- **Loop Mode Info message** (Only when auto-loop is active):
  ```text
  [STATUS: FREEZE ACTIVE] >> TARGET SEQUENCE UNDERGOING FORCED SEQUENCING.
  [REASON] >> SYSTEMS RUNNING UNDER ABNORMAL STRESS.
  ```

- **Single Mode Warning message** (Only when auto-loop is inactive / single cycle):
  ```text
  [WARNING] >> G.E.N. NETWORK TAGS ALL FORCED SEQUENCES!
  [WARNING] >> P.O.X. REACTOR LOCKED FOR SEQUENCING.
  ```

- **Lunar Modifier Status** (Dynamic depending on the daily phase):
  ```text
  [LUNAR STATUS] >> Phase: [Phase Name] | Effective Debuff Mod: [Modifier Text]
  ```
  *Values for `Modifier Text`:*
  - `None / Baseline`
  - `New Moon (+3.75% Debuff on construction)`
  - `Full Moon (-3.75% Buff on construction)`

- **Initial Failure Calculations**:
  ```text
  [CALCULATION] >> Base: [Base Pos] | Stock: [Stock Count] | Fail Chance: [Calculated %]
  ```
  *(Example: [CALCULATION] >> Base: 64 | Stock: 10452 | Fail Chance: 41.25%)*

---

## 2. Chronological Splicing & Scaffold Processing (Ticking 1.0s to 8.0s)

For each elapsed second, the reactor logs calibration progress alongside slot scaffolding events.

- **Calibration Progress Tick**:
  ```text
  [SEQUENCE PROGRESS] >> [Progress %]% Complete
  ```
  *(Example: [SEQUENCE PROGRESS] >> 37% Complete)*

- **Slot Processing Event**:
  ```text
  Slot #[Slot Number] processing using scaffold: [Scaffold Type]
  ```
  *Values for `Scaffold Type`:*
  - `PRE-ALIGNED MANUAL GENE` (Manually placed sequence in slot)
  - `MATCH STOCK RECRUITED` (Exact sequence match found in stock inventory)
  - `ANY GENE UNSTABLE BASE [[First 4 Bases]...]` (No exact match; consumed random gene block from stock)
  - `VOID SYNTHESIS SCAFFOLD` (No gene blocks remaining in stockpile)

---

## 3. Base Append Failure & Gene Attrition Logs

During slot assembly, if a base append fails (due to random fail chance roll or scaffold char mismatch), the system logs the corrective depletion action at the corresponding position.

- **Standard Pool Sacrifice**:
  ```text
    ➔ FAILED APPEND (pos [Base Pos]). Sacrificed gene [Gene Sequence] (depleting pool)
  ```
  *(Example:  ➔ FAILED APPEND (pos 4). Sacrificed gene AAGGCCTT (depleting pool))*

- **Backup Pool Sacrifice**:
  ```text
    ➔ FAILED APPEND (pos [Base Pos]). Sacrificed backup gene [Gene Sequence] to guarantee placement
  ```

- **Splicing Protocol Depletion Abort** (Fails sequencing if stockpile runs empty):
  ```text
    ➔ [FATAL] >> SPLICING PROTOCOL ABORTED: Nucleotide stockpile fully depleted.
  ```

---

## 4. Sequence Completion & Tab Redirection (End of 8.0s Cycle)

Once calibration finishes, the result is output to the global terminal system.

- **Successful Loop Iteration**:
  ```text
  [FORCED LOOP] Specimen "[Creature Name] [FORCED]" assembled successfully! Continuing loop iteration...
  ```

- **Loop Deactivation**:
  ```text
  FORCED LOOP ENDED. Reactor is now available for normal sequencing.
  ```

- **Single Construction Complete**:
  ```text
  FORCED CONSTRUCTION COMPLETED. Produced sequence: "[Creature Name] [FORCED]"!
  ```
