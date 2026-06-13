# Implementation Plan: Eliminate 'core' and 'cores' References Across GENPOX

This plan outlines the refactoring steps to replace all occurrences of `core` and `cores` (case-insensitive) across the Android application (`pox-android/app/src/`), the web application (`src/` and compiled bundles), and the markdown documentation files.

## Rationale and Constraints
- The gameplay loop does not expose any "cores" concept to the player.
- **Critical Compilation Constraint**: Android system library paths and Compose imports (e.g., `androidx.compose.animation.core.*`, `androidx.camera.core.*`, `androidx.core.content.ContextCompat`, `androidx.datastore.preferences.core.*`) must **NOT** be touched, as they are part of the platform APIs and will break compilation.

## Proposed Changes

We will systematically rename the term "core" in standard code files, texts, and comments:

### 1. Android Codebase (`pox-android/app/src/`)

#### [MODIFY] [WaveMath.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/WaveMath.kt)
- Rename `"Quantum Extraction Core"` to `"Quantum Extraction Unit"`.

#### [MODIFY] [MainScreen.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/MainScreen.kt)
- Rename `"G.E.N.P.O.X. CORE"` to `"G.E.N.P.O.X. MAIN"`.

#### [MODIFY] [MainViewModel.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/MainViewModel.kt)
- Rename `"Core Anomaly"` ➔ `"Anomaly"`.
- Rename `"compilation core"` ➔ `"compilation system"`.
- Rename `"offline compiler core"` ➔ `"offline compiler engine"`.
- Rename `"AI Core"` ➔ `"AI Engine"`.
- Rename `"Chitin-Core"` ➔ `"Chitin-Shell"`.
- Rename `"Core power normalized"` ➔ `"Reactor power normalized"`.
- Rename `"Core sequence frequencies"` ➔ `"Gene sequence frequencies"`.

#### [MODIFY] [PoxScreens.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/PoxScreens.kt)
- Rename `"No matching standard or anomalous gene cores found"` ➔ `"No matching standard or anomalous genes found"`.
- Rename `"COSMIC GENE CORES"` ➔ `"COSMIC GENES"`.
- Rename `"STANDARD GENE CORE"` ➔ `"STANDARD GENE SEQUENCE"`.
- Rename `"ANOMALOUS CORE"` ➔ `"ANOMALOUS GENE SEQUENCE"`.
- Rename `"NUCLEOTIDE CORE"` ➔ `"NUCLEOTIDE SEQUENCE"`.
- Rename `"CORE SEQUENCE"` ➔ `"GENE SEQUENCE"`.

---

### 2. Web Application Codebase (`src/`)

#### [MODIFY] [genetic.ts](file:///c:/Users/Brent/Antigravity/GENPOX/src/utils/genetic.ts)
- Rename list entry `"Core"` ➔ `"Shell"` in the name generator matrix.
- Rename comments referencing `"representing core"`.

#### [MODIFY] [PoxConsole.tsx](file:///c:/Users/Brent/Antigravity/GENPOX/src/components/PoxConsole.tsx)
- Rename comments, status messages, UI headers, logs, and dialog strings:
  - `"Core points adjusted"` ➔ `"Points adjusted"`
  - `"Core distance"` ➔ `"Node distance"`
  - `"Sync Core"` ➔ `"Sync Unit"`
  - `"Core power normalized"` ➔ `"Reactor power normalized"`
  - `"Spliced Core"` ➔ `"Spliced Gene"`
  - `"defender core"` ➔ `"defender unit"`
  - `"System Core Down"` ➔ `"System Offline"`
  - `"AUTOTRONIC MORPHOGENESIS CORE"` ➔ `"AUTOTRONIC MORPHOGENESIS ENGINE"`
  - `"NEW CORES"` ➔ `"NEW GENES"`

#### [MODIFY] [ForecastCalendar.tsx](file:///c:/Users/Brent/Antigravity/GENPOX/src/components/ForecastCalendar.tsx)
- Rename `"core base pair"` ➔ `"base-pair"`.
- Rename `"core biological combination"` ➔ `"biological combination"`.

#### [MODIFY] [splicer_compiled.txt](file:///c:/Users/Brent/Antigravity/GENPOX/splicer_compiled.txt) and [step_search_compiled.txt](file:///c:/Users/Brent/Antigravity/GENPOX/step_search_compiled.txt)
- Apply the same replacements to clean precompiled bundles.

---

### 3. Documentation Files

#### [MODIFY] [scientific_formulas.md](file:///c:/Users/Brent/Antigravity/GENPOX/scientific_formulas.md)
- Replace references containing "Core" with the updated terminology (e.g. `Decryption Core` ➔ `Decryption Deck`, `AI Core` ➔ `AI Engine`).

## Refactoring Strategy

We will use a Python script `rename_cores.py` located in the scratch directory to perform these replacements safely:
1. It will iterate over target files.
2. It will skip any line that starts with `import` or contains standard framework classes (like `androidx.compose.animation.core.*`, `androidx.camera.core.*`, `androidx.core.*`, `androidx.datastore.*`).
3. For all other lines, it will perform exact word or substring replacements representing core components.

## Verification Plan

### Automated Checks
1. Compile the Android application and verify that tests pass:
   ```powershell
   $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat testDebugUnitTest
   ```
2. Verify that the search for the word `core` or `cores` (excluding framework libraries) returns zero matches.

### Manual Verification
- Deploy to the Pixel 10 Pro Fold and verify that the interface displays the corrected terms without crashes.
