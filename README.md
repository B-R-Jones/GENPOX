# G.E.N.P.O.X. (pronounced 'genpox')

G.E.N.P.O.X. is a native Android location-based genetic research and database hacking simulation game. Set in a cyber-retro, low-fi sci-fi terminal interface, players physically scan genetic barcodes, splice complex creature genomes, deploy automated harvesters on geographic anomalies, and hack peer mainframe nodes.

All interfaces are built with a terminal-inspired HUD aesthetic, featuring custom cyberglass borders, CRT scanline flickering, and multi-axis rotating vector wireframes.

---

## ☣️ Creator Manifesto: A Note on AI Use

> **G.E.N. P.O.X. is my dream. Not Gemini's, not any LLMs, not any AIs.** The way it looks, the way it feels, the way it plays is all pulled directly from over 35 years of sci-fi nerd osmosis. What you see on the screen is what I see in my head. It is not a vision forced by AI. It is not an asset shoveled in from someone else. It's my direction to the perfect sous-chef. This soup, this fuckin' cybersoup, is mine.
>
> G.E.N. P.O.X. is built on the idea of taking powerful corporate technology meant for domination and turning it into something more. Not something good, but something chaotic. Something that breaks intended use on the rocks of bastardly action.
>
> The world is changing all around us, and none of us can stop it. What we can do is ride the wave and bend it, break it, make it work for us, even if it - and they - don't want to.
>
> **Get splicing, Agent.**
>
> P.S. No AI had any part in the statement above.

---

## ⚙️ Core Engines & Gameplay Simulations

G.E.N.P.O.X. is driven under the hood by 6 specialized mathematical and physical engines that shape the gameplay and run real-time simulations:

### 1. Genetics Engine
The biophysical core of genetic research. It models DNA thermodynamics and sequence adaptation index metrics.
- **Melting Temperature ($T_m$)**: Evaluates how temperature and salt screening affect DNA stability using a salt-corrected Wallace-Breslauer formula.
- **Minimum Free Energy ($MFE$)**: Simulates single-stranded folding loops using Nussinov's dynamic programming algorithm to detect GC hairpin structures.
- **Phred Quality Scores ($Q$)**: Models base-replication accuracy depending on the active polymerase (Taq, Tth, Pfu).
- **Codon Adaptation Index ($CAI$)**: Evaluates genetic translation weights against faction-specific codon preferences (Infection, Mech, Parasite, Containment).
- **Source**: [GeneticsEngine.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/GeneticsEngine.kt) | [Tech Spec](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/Engines/genetics_engine.md)

### 2. Conflict Engine
Governs turn-based tactical combat, spec-ops infiltration, and specimen decay.
- **Telomere Stat Degradation**: Real-time degradation of creature HP, attack, and speed based on their telomeric decay ($0-100\%$).
- **Action Resolver**: Simulates battle phases, dodging states, and special movesets (e.g. *BIO_DRAIN_REPAIR* siphons health; *QUANTUM_ESCAPE_DEVIATION* evades damage).
- **Survival Simulation**: Resolves environmental hazards and telomere decay rates for specimens in the field.
- **Source**: [ConflictEngine.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/ConflictEngine.kt) | [Tech Spec](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/Engines/conflict_engine.md)

### 3. Deployment Engine
Handles geodetic mapping, coordinates checking, and transit kinetics.
- **Geodetic Distance**: Uses the Haversine formula to compute exact distances (in feet) between coordinates.
- **Lobed Boundary Radii**: Models anomalies with non-circular, multi-lobed forcefields ($R(\theta)$) containing $3$, $4$, or $5$ lobes.
- **Forcefield Stall Depth**: Calculates how far a harvester penetrates the containment field before stalling based on its defense and active lunar resistance.
- **Combined Wave Density**: Models spatial wave interference from nearby overlapping anomalies using damped sine equations.
- **Source**: [DeploymentEngine.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/DeploymentEngine.kt) | [Tech Spec](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/Engines/deployment_engine.md)

### 4. Reactor Simulation Engine
Runs step-by-step transcription checks during the synthesis of nucleotide blocks.
- **Stepped Assembly**: Delays and transcribes sequences base-by-base, checking for biophysical collapse states.
- **Biophysical Collapse Filters**: Halts synthesis if it suffers from feedstock *Inlet Deprivation*, stabilized *GC Hairpin Stalling* (mitigated by DMSO), or *AT Denaturation* (mitigated by Netropsin).
- **Source**: [ReactorSimulationEngine.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/ReactorSimulationEngine.kt) | [Tech Spec](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/Engines/reactor_simulation_engine.md)

### 5. Wave Math Engine
Aligns siphoning loops with real-world solar system coordinate projections.
- **Heliocentric Orbits**: Calculates planetary orbits using orbital periods and Keplerian distances.
- **Geocentric Projections**: Translates heliocentric vectors into geocentric longitudes and declinations as viewed from Earth.
- **Local Diurnal Hour Angles**: Accounts for Earth's rotation to map planetary bodies onto the dome coordinates of the observer.
- **Synodic Alignments**: Classifies planet configurations into Conjunctions, Oppositions, and Quadratures.
- **Source**: [WaveMath.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/WaveMath.kt) | [Tech Spec](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/Engines/wave_math_engine.md)

### 6. P.O.X. Network Manager
Coordinates ad-hoc peer-to-peer (P2P) connections over local radios.
- **Nearby Connections**: Leverages Google Nearby Connections to establish ad-hoc clusters.
- **Hydra Packet Protocol**: Custom binary serialization of trade offers, specimen files, database hacking intrusions, and mainframe security audits.
- **Source**: [PoxNetworkManager.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/network/PoxNetworkManager.kt) | [Tech Spec](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/Engines/pox_network_manager.md)

---

## 🎨 Visual Aesthetics & Animation Framework

To achieve a tactile, futuristic terminal aesthetic, G.E.N.P.O.X. utilizes a custom graphics framework built directly on Jetpack Compose Canvas layers.

### 1. Retro CRT & Wireframe Aesthetics
- **Phosphor Monitor Effect**: UI layers are colored in single-wavelength cybernetic palettes: CyberCyan (`0xFF06B6D4`), CyberGreen (`0xFF10B981`), CyberAmber (`0xF59E0B`), or Crimson Warning (`0xEF4444`).
- **Cyberglass Materials**: Containers utilize semi-transparent backdrops and custom vector double borders, creating a glowing glassmorphic overlay.
- **CRT Scanlines & Glitches**: Simulates signal interference and flickering loops by varying opacity, scale offsets, and linear shifts over time.

### 2. Math-Driven Rendering
All visual geometries are calculated procedurally rather than using raster images:
- **Rotating Double Helix**: Calculated in [GraphicsMath.calculateHelixStrand](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/GraphicsMath.kt) using sinusoidal wave functions.
- **3D Wireframe blueprint**: Calculated via 3D rotation matrix coordinates projected onto 2D viewport coordinates in [GraphicsMath.project3DBlueprintPoint](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/GraphicsMath.kt).
- **Diurnal Orrery Transit Panel**: Projects celestial declinations, geocentric longitude offsets, and local diurnal hour angles onto a shifted horizon arc in [GraphicsMath.projectOrreryPlanet](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/GraphicsMath.kt).

### 3. Tactile Spring Physics & Layer Swap Animations
Interactive transitions use spring physical models to feel responsive and alive:
- **Infinite Transitions**: Utilizes `rememberInfiniteTransition` with distinct duration cycles (e.g. 18 seconds for global map rotation, 4 seconds for pulsing breath effects, 1.5 seconds for glitch jitters) to build an organic, non-repetitive UI.
- **Swipe-to-Cycle Layer Stack**: Vertical drag coordinates on the radar overlay are captured using Compose `pointerInput(Unit) { detectDragGestures(...) }`. Swipes exceeding 50 pixels toggle the active z-layered screen depth.
- **Tactile Springs**: Transition parameters (scale, alpha, and vertical translation) are computed using spring equations (`DampingRatioLowBouncy`, `StiffnessLow`) to create visual weight and bounce.

---

## 🛠️ Architectural Choice: Why Kotlin?

Rather than developing G.E.N.P.O.X. inside a commercial game engine (e.g. Unity, Godot, or Unreal), we opted to build the application as a **fully native Kotlin Android App** using **Jetpack Compose**. The primary reasons are:

### 1. Battery & Memory Efficiency in the Field
Location-based games require GPS hardware and background radios to run for hours. Commercial game engines loop a heavy, dedicated C++ rendering thread that constantly repaints the entire screen ($30$–$60$ frames per second), draining battery rapidly. 
By utilizing Jetpack Compose's state-driven recomposition, G.E.N.P.O.X. only draws vectors on the Canvas when states actually change. This reduces GPU overhead to near zero when the screen is static, preserving the device's battery life in the field.

### 2. Low-Overhead Native API Integration
Integrating camera overlays, hardware sensors, and ML engines inside a 3rd party game engine is complex and introduces JNI/interop lag. Native Kotlin allows:
- **Direct CameraX Bindings**: Low-overhead frame analysis on the CPU/GPU.
- **Google Play Services & Maps SDK**: Native map integration that doesn't need wrapper plugins or third-party mapping packages.
- **ML Kit Barcode Scanner**: Direct, lightning-fast execution of Google's barcode analysis pipeline.

### 3. Background Services & Persistent Engines
A core part of the gameplay loop involves creatures traversing anomalies in the background. Traditional game engines are paused or killed by the Android OS when minimized. Native Kotlin handles:
- **WorkManager & Foreground Services**: Safely processes background timers and triggers notifications even when the player exits the application.
- **Room Database**: Native SQLite integration for efficient inventory management and transaction safety.
- **DataStore Preferences**: Fast, async storage for application state.

---

## 📂 Repository Structure

- 📁 [pox-android](file:///c:/Users/brent/Antigravity/GENPOX/pox-android) - The core Android project module.
  - 📁 `app/src/main/java/com/example/genpox` - Kotlin implementation of screens, view models, database, and background services.
    - 📁 [audio](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/audio) - Procedural low-level audio engine (`PoxSynthManager.kt`).
    - 📁 [data](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data) - Core engines, database, models, and utility formulas.
      - 📁 [network](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/network) - P2P connection manager (`PoxNetworkManager.kt`).
    - 📁 [ui](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui) - Composable screens, navigation files, and UI layouts.
      - 📁 [components](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/components) - Custom drawing modules (holographic maps, wireframe vectors, canvas charts, and camera scanners).
  - 📁 `app/src/assets` - Pre-cached road maps and building geometries (`pre_cached_roads.json`).
- 📁 [Documentation](file:///c:/Users/brent/Antigravity/GENPOX/Documentation) - Design specs, developer guides, player manuals, and technical reports.
  - 📁 [Engines](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/Engines) - Technical and scientific specifications of core simulation engines.
  - 📁 [Celestial Bodies](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/Celestial%20Bodies) - Profiles of orbital bodies that influence siphoning rates.
  - 📁 [Design Guides](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/Design%20Guides) - System architecture and visual design specification sheets.
  - 📁 [Player Guides](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/Player%20Guides) - Walkthroughs for reactor control and genome manipulation.
  - 📁 [Reports](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/Reports) - Rendering audits and runtime validation logs.
- 📁 [scratch](file:///c:/Users/brent/Antigravity/GENPOX/scratch) - Development scripts and experimental scratchpads.

---

## 🛠️ Getting Started

### Prerequisites
- **Java Development Kit (JDK)**: JDK 17 or higher.
- **Android SDK**: Android Studio (Koala or newer recommended) or CLI command-line tools configured with SDK Platform support for API level 36.
- **Node.js**: Required to execute helper road-caching scripts.

### Build and Run

1. **Configure Environment**:
   Create a `pox-android/local.properties` file with your SDK paths and API keys:
   ```properties
   sdk.dir=/path/to/your/Android/sdk
   MAPS_API_KEY=your_google_maps_api_key
   ```
2. **Compile Android App**:
   From the `pox-android` directory, run:
   ```bash
   ./gradlew.bat assembleDebug
   ```
3. **Run Tests**:
   ```bash
   ./gradlew.bat testDebugUnitTest
   ```
