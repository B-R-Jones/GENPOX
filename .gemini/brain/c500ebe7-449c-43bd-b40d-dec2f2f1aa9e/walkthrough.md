# GENPOX Native Android Rewrite Walkthrough

This walkthrough documents the full native Jetpack Compose conversion of the cyberpunk game prototype `GENPOX` and highlights key components, compilation success, and local verification details.

## Changes Made

### 1. App Architecture & Scaffolding
- Built the native project in [pox-android/](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android) utilizing Jetpack Compose and modern Android architecture.
- Configured permissions for camera, locations, and network in the Android manifest, locking the display to **Portrait** mode.

### 2. Data & Settings Layer
- Defined data models in [Models.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/Models.kt) (`Creature`, `GeneSequence`, `NearbyUser`, `HarvestMission`).
- Set up a local [Database.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/Database.kt) with Room DAOs for offline storage of creatures, codons, and harvest missions.
- Implemented encrypted client-side storage for the Gemini API Key using Preferences DataStore in [PoxSettings.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/PoxSettings.kt).

### 3. Core Engine Translation
- Translated the audio engine into [PoxSynthManager.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/audio/PoxSynthManager.kt) to play custom procedural soundscapes, combinator ticks, and error buzzes using dynamic PCM buffers and `AudioTrack`.
- Translated the trigonometric visuals into [NodeCrystalCanvas.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/components/NodeCrystalCanvas.kt) which renders custom dynamic geometric nodes on Compose canvas.

### 4. UI Layouts & Core Machine
- Designed a cohesive cyberpunk terminal theme using custom monospace layouts, CRT scanlines, and glow indicators in [MainScreen.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/MainScreen.kt).
- Created views for the combinator compiler, codon splicer, forecasts, and vaults in [PoxScreens.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/PoxScreens.kt).
- Integrated local CameraX previews and ML Kit barcode scanning in [PoxCameraScanner.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/components/PoxCameraScanner.kt).
- Hooked all interfaces to [MainViewModel.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/MainViewModel.kt), which directly interacts with the Google AI SDK (`GenerativeModel`) using API keys input client-side.

---

## Verification & Testing

### 1. Build Verification
- Verified gradle compilation with JDK 21 (Android Studio bundled compiler).
- Built the debug APK successfully:
  ```powershell
  $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug
  ```
  > [!NOTE]
  > The output APK file is built and located under:
  > [app-debug.apk](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/build/outputs/apk/debug/app-debug.apk)

### 2. Unit Testing
- Created JVM unit tests in [MainScreenViewModelTest.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/test/java/com/example/genpox/ui/main/MainScreenViewModelTest.kt).
- Configured JUnit 4 with `UnconfinedTestDispatcher` on `Dispatchers.Main` to test the asynchronous sequence compile/save loops.
- Ran and passed all tests successfully:
  ```powershell
  $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat testDebugUnitTest
  ```
  ```text
  > Task :app:testDebugUnitTest
  BUILD SUCCESSFUL in 13s
  ```

### 3. Emulator Deployment Note
- System diagnostics (`systeminfo`) indicate that BIOS firmware virtualization is disabled on this machine:
  > **Hyper-V Requirements: Virtualization Enabled In Firmware: No**
- As a result, hardware acceleration (WHPX/AEHD) is unavailable, and the emulator (`medium_phone`) must run under software translation (QEMU TCG), which remains offline due to CPU performance limits.

### 4. Physical Device Verification (Pixel 10 Pro Fold)
- **Problem**: The app initially compiled but crashed immediately upon opening on the device because Room DAO annotations (`@Database`, `@Dao`, etc.) were not being processed under Kotlin's kapt/AGP 9.0 compiler setup, leading to `PoxDatabase_Impl does not exist` runtime exceptions.
- **Resolution**:
  1. Configured **Kotlin Symbol Processing (KSP)** using the independent `com.google.devtools.ksp` version `2.3.9` to match AGP 9.0's built-in Kotlin support.
  2. Removed legacy `kotlin-android` and `kapt` plugins from build configurations.
  3. Cleaned deprecated properties from `gradle.properties`.
  4. Upgraded the **Room database** library from `2.6.1` to `2.8.4` in [libs.versions.toml](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/gradle/libs.versions.toml) to fix a known KSP2 compiler exception (`unexpected jvm signature V` on DAO suspend functions returning Unit).
- **Execution & Deployment**:
  - Re-ran the clean build and compiled the debug APK successfully:
    ```powershell
    $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat clean assembleDebug
    ```
  - Installed the APK directly to the connected Pixel 10 Pro Fold:
    ```powershell
    adb -s 59091FDCGS00Y5 install -r app/build/outputs/apk/debug/app-debug.apk
    ```
  - Launched the app successfully via ADB activity manager:
    ```powershell
    adb -s 59091FDCGS00Y5 shell am start -n com.example.genpox/com.example.genpox.MainActivity
    ```
  - Verified via process monitoring that the application process runs stably, initializing the graphics environment (Vulkan) and UI (ViewRootImpl) without throwing any crashes or runtime errors.

### 5. Bio-Lab Dual-Reactor & Synodic Wave Synthesizer (Phase 8)
- **Mathematical Synthesizer Core**: Created [WaveMath.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/WaveMath.kt) containing:
  - Deterministic synodic lunar phase alignment age calculations (Reference New Moon epoch: May 17, 2026).
  - Wave config parameters & Week 3 suppression decay check.
  - Logarithmic chance curve calculation + Gaussian resonance peak boosts at 14% intervals + diurnal diurnal coupling adjustment.
  - Splicer generator for anomalous codons (`XZYW?!$%&@#` keys) and benefit translations (Vortex Phase-Strike, Supernova Reverb, etc.).
- **ViewModel Reactive State Flow**: Added reactor states, a 1-second interval coroutine coroutine heartbeat tick engine, standard nucleotide tally tracking, and standard codon depletion database methods in [MainViewModel.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/MainViewModel.kt).
- ** táctile UI Screens**: Refactored the old Combinator view in [PoxScreens.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/PoxScreens.kt) to display:
  - Dual sub-tabs (P.O.X. Reactor & Anomaly Engine) with dynamic emerald green vs twilight purple styling.
  - Cycle count progress bar, manual acceleration, and boost injection buttons.
  - Full-screen **Molecular Step-Search Overlay** providing phase-based dinucleotide grid selection filters.
  - **Batch Packet Log Overlay** showcasing historical codon batches, search queries, and diagnostic detail dialogs for passive perks.
  - **Decrypted Anomaly Vault** displaying custom passive perk breakdown lists.
- **Verification**:
  - Successfully built and ran the test suite:
    ```powershell
    $env:JAVA_HOME="C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat testDebugUnitTest
    ```
    (BUILD SUCCESSFUL, 100% tests passed)
  - Installed and launched the updated debug package to the Pixel 10 Pro Fold:
    ```powershell
    adb -s 59091FDCGS00Y5 install -r app/build/outputs/apk/debug/app-debug.apk
    adb -s 59091FDCGS00Y5 shell monkey -p com.example.genpox -c android.intent.category.LAUNCHER 1
    ```
    (Application started instantly, executing reactor ticks and rendering UI overlays smoothly with sound synthesis)### 6. Bio-Lab Console Layout Refinement (Phase 8.1)
- **Removal of Node Crystal Visualizer**: Completely removed the 3D-like `NodeCrystalCanvas` from the `CombinatorView`, adhering strictly to the original dashboard layout where this canvas does not belong on the Bio-Lab tab.
- **Terminology Realignment**:
  - Removed re-interpreted sci-fi labels like the `"Re-Sequencing Amplitude"` banner on the ticker, and the `"Synodic Lunar Detectors"` header.
  - Aligned all terminal labels with the React prototype console:
    - Header Row: `[ G.E.N. P.O.X. Tide Pool Reactor V2.4 ]` and `SYSTEMS ON` status.
    - Title: `Single-Node Cybernetic Synthesizer`.
    - Info grid: `Unique Gene IDs` and `Multi-Count Gene IDs` with `⬢` dot indicators.
    - Today's base-pair wave status: `TODAY'S BASE-PAIR WAVE` (Active: `${wave.pair} WAVE` / Dormant).
    - Tomorrow and Day After Tomorrow forecast grids.
  - Implemented raw, centered large monospace ticker displaying the `scrollingGene` inside a bordered box.
  - Re-anchored the progress bar, cycle tact, active booster timers, and buttons (`View Gene Synthesis Log` and `Manual Acceleration (-2s)`).
- **Verification**:
  - Built the refined debug build and ran unit tests with `testDebugUnitTest` passing successfully.
  - Deployed the refined APK onto the physical Pixel 10 Pro Fold device and verified visual accuracy.

### 7. Web App Terminology Alignment (Phase 9)
- **Recursive Directory Scan & Refactor**: Developed and executed a recursive Python script [rename_codons.py](file:///C:/Users/Brent/.gemini/antigravity-ide/brain/c500ebe7-449c-43bd-b40d-dec2f2f1aa9e/scratch/rename_codons.py) targeting all `.ts`, `.tsx`, `.txt`, `.html`, and `.json` files in the root and `src/` directories of the repository.
- **Global Replacement Rules**: Performed case-preserving substitutions (`codon`/`codons` ➔ `gene`/`genes`) across variable declarations, state parameters, type structures, log tags, comments, UI text, labels, and title headings in files such as:
  - [types.ts](file:///c:/Users/Brent/Antigravity/GENPOX/src/types.ts)
  - [genetic.ts](file:///c:/Users/Brent/Antigravity/GENPOX/src/utils/genetic.ts)
  - [wave.ts](file:///c:/Users/Brent/Antigravity/GENPOX/src/utils/wave.ts)
  - [PoxConsole.tsx](file:///c:/Users/Brent/Antigravity/GENPOX/src/components/PoxConsole.tsx)
  - [ForecastCalendar.tsx](file:///c:/Users/Brent/Antigravity/GENPOX/src/components/ForecastCalendar.tsx)
  - [LeafletScannerMap.tsx](file:///c:/Users/Brent/Antigravity/GENPOX/src/components/LeafletScannerMap.tsx)
  - Precompiled JS bundles: [splicer_compiled.txt](file:///c:/Users/Brent/Antigravity/GENPOX/splicer_compiled.txt) and [step_search_compiled.txt](file:///c:/Users/Brent/Antigravity/GENPOX/step_search_compiled.txt)
- **Zero Occurrence Validation**: Verified via recursive PowerShell search that **0 instances** of "codon" or "codons" (case-insensitive) remain in active source code, structures, or comments.
- **Android Compilation Integrity**: Re-verified the Kotlin-based compiler targets and successfully executed the Android local test suite and APK packaging pipelines, deploying the updated package directly to the connected Pixel 10 Pro Fold device.

### 8. Core/Cores Terminology Clean-Up (Phase 10)
- **Recursive Directory Scan & Refactor**: Developed and executed a recursive Python script [rename_cores.py](file:///C:/Users/Brent/.gemini/antigravity-ide/brain/c500ebe7-449c-43bd-b40d-dec2f2f1aa9e/scratch/rename_cores.py) targeting all `.ts`, `.tsx`, `.txt`, `.html`, `.json`, and `.kt` files in the root, `src/`, and `pox-android/app/src/` folders.
- **Import Statements Safety Check**: Ensured that the script explicitly skipped import blocks or package declarations that represent system libraries (e.g. `androidx.compose.animation.core.*`, `androidx.camera.core.*`, `androidx.core.content.ContextCompat`, `androidx.datastore.preferences.core.*`) to protect the compiler configuration.
- **Global Replacement Rules**: Swapped out references to `core`/`cores` with appropriate gameplay equivalents (`unit`, `gene`, `sequence`, `engine`, `node`, `system`, `mainframe`, `shell`):
  - [WaveMath.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/WaveMath.kt): `"Quantum Extraction Core"` ➔ `"Quantum Extraction Unit"`.
  - [MainScreen.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/MainScreen.kt): `"G.E.N.P.O.X. CORE"` ➔ `"G.E.N.P.O.X. MAIN"`.
  - [MainViewModel.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/MainViewModel.kt): Faction naming lists (`Chitin-Core` ➔ `Chitin-Shell`), offline compiler logging, AI engine flags, and lore string frequencies.
  - [PoxScreens.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/PoxScreens.kt): `"STANDARD GENE CORE"` ➔ `"STANDARD GENE SEQUENCE"`, `"CORE SEQUENCE"` ➔ `"GENE SEQUENCE"`, and info vault captions.
  - Web components: [genetic.ts](file:///c:/Users/Brent/Antigravity/GENPOX/src/utils/genetic.ts), [PoxConsole.tsx](file:///c:/Users/Brent/Antigravity/GENPOX/src/components/PoxConsole.tsx), [ForecastCalendar.tsx](file:///c:/Users/Brent/Antigravity/GENPOX/src/components/ForecastCalendar.tsx), compiled bundles, and the [scientific_formulas.md](file:///c:/Users/Brent/Antigravity/GENPOX/scientific_formulas.md) documentation.
- **Zero Occurrence Validation**: Verified via recursive search that **0 instances** of "core" or "cores" (case-insensitive) remain in active gameplay logic, texts, logs, or templates.
- **Deployment Verification**: Re-compiled and built the debug APK, successfully deployed and launched it on the connected physical Pixel 10 Pro Fold.

### 9. Anomaly Engine Layout Refinements (Phase 11)
- **Inactive Progress Loading Bar**: Configured the progress variable in [PoxScreens.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/PoxScreens.kt) to lock at `0f` when the `anomaly` subtab is selected and `anomalyEngineActive` is toggled `OFF`, preventing cycles from showing loading progression.
- **Anomaly Engine Active Switch**: Implemented a purple material `Switch` toggle directly bound to the `anomalyEngineActive` state, triggering `viewModel.setAnomalyEngineActive` when checked.
- **Resource and Rate Info Panel**:
  - Implemented the `RESOURCE COUNT` row displaying the current total standard nucleotide base count against the `250,000` base requirement. Bounded values show dynamically in **Red** with an `✕` indicator when below requirement, or in **Purple** when the requirement is fully met.
  - Added the `LOAD WARNING` row (`RESETS STABILITY & EMISSIONS TO 0` in orange/yellow).
  - Added the `CURRENT RATE` row (`-10,000 BASES / LOOP` in green).
- **Anomalous Discovery and Modifier Card**:
  - Implemented the `ANOMALOUS DISCOVERY` row showing the computed probability (e.g. `35.843%`) directly from `WaveMath.getAnomalyEngineSuccessChance`.
  - Added the `Spectrum Dial Coupling Modifier:` row showing the dynamic diurnal modifier (e.g. `+1.392%`) in cyan blue.
- **Verification**: Verified successfully via compiling, unit testing, packaging, and launching on the physical Pixel 10 Pro Fold device.

### 10. Bio-Lab Reactor Layout Refinement (Layout Swap)
- **Component Swap**: Swapped the vertical layout positions of the Anomaly/Gene Synthesis action buttons (View Log, Manual Acceleration) with the overlay trigger buttons (Molecular Step-Search, Batch Packet Log, Decrypted Vault) inside the Bio-Lab tab (`CombinatorView`).
- **Static Action Placement**: Placed the action buttons (View Log & Manual Acceleration) at the very bottom, outside the tab-switched Card Box container. This keeps their vertical position locked in the same screen coordinates when switching between the `P.O.X. Reactor` and `Anomaly Engine` subtabs.
- **Enclosed Navigation**: Moved the overlay trigger buttons (Molecular Step-Search, Batch Packet Log, Decrypted Vault) inside the Card Box container. Adjusted their container background color to `Color.Black.copy(alpha = 0.4f)` so they stand out distinctly.
- **Verification**: Built and compiled successfully, passed all unit tests, and verified the UI behavior and subtab switching responsiveness on the physical Pixel 10 Pro Fold.
