# GENPOX

GENPOX is a native Android location-based genetic research & hacking game with a cyber-retro/sci-fi aesthetic. Using the device's GPS and custom overlays, players scan, splice, combine, and hack creature genomes, dispatching drones on coordinates to harvest genetic materials, and upgrading reactors using specialized algorithms.

---

## 🚀 Key Features

*   **Reactor Engine & Resonance**: Manage synthesis operations, helical reactors, and transcription speed using advanced diagnostic instruments and automated triggers.
*   **Gene Splicer & Combinator**: Combine genetic sequences (8-character nodes) into longer, more coherent genomes to optimize attributes (Vitality, Attack, Defense, Speed, Telomeres).
*   **Anomaly Engine & Vault**: Analyze decryption feeds, anomaly triggers, and manage cataloged genetic samples in the secure vault.
*   **GPS Radar Navigation**: Track and explore pre-cached road and building structures derived from OpenStreetMap data. Simulate multi-user network links and trade offers.
*   **Telemetry Scanner**: Interactive camera tool powered by CameraX and ML Kit Barcode Scanning to capture raw visual data feeds.
*   **Synth Soundscapes**: Built-in sound synthesis subsystem driving retro sci-fi audio indicators.

---

## 📂 Repository Structure

*   📁 [pox-android](file:///c:/Users/brent/Antigravity/GENPOX/pox-android) - The core Android project module.
    *   📁 `app/src/main/java/com/example/genpox` - Kotlin implementation of screens, view models, database, and background services.
    *   📁 `app/src/main/assets` - Pre-cached mapping structures (e.g., `pre_cached_roads.json`).
*   📁 [Documentation](file:///c:/Users/brent/Antigravity/GENPOX/Documentation) - Detailed design documents, rules, templates, and reference materials.
    *   📄 [master_design_standards.md](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/master_design_standards.md) - Design aesthetics, colors (CyberGreen, Cyan, Purple), and strict visual layouts.
    *   📄 [creature_types.md](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/creature_types.md) - Genome configurations and creature profiles.
    *   📄 [creature_handling.md](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/creature_handling.md) - Dispatch protocols and coherence levels.
    *   📄 [splicer_design.md](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/splicer_design.md) - Splicer layout & workflow rules.
*   📁 [scratch](file:///c:/Users/brent/Antigravity/GENPOX/scratch) - Local developer scripts and temporary scratchpads.

---

## 🛠️ Getting Started

### Prerequisites

*   **Java Development Kit (JDK)**: JDK 17 or higher.
*   **Android SDK**: Command Line Tools or Android Studio with API level 36 support.
*   **Node.js**: Required to execute helper scripts.

### Build and Run

1.  **Configure Environment**:
    Create a `pox-android/local.properties` file with your SDK paths and API keys:
    ```properties
    sdk.dir=/path/to/your/Android/sdk
    MAPS_API_KEY=your_google_maps_api_key
    ```
2.  **Compile Android App**:
    From the `pox-android` directory, run:
    ```bash
    ./gradlew assembleDebug
    ```
3.  **Run Tests**:
    ```bash
    ./gradlew test
    ```

### Developer Scripts

#### Pre-Caching Road Maps
To fetch road geometries for location rendering, use the pre-cache script:
1.  Navigate to the `pox-android` directory.
2.  Configure target latitude/longitude coordinates in [download_roads.js](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/download_roads.js).
3.  Execute the downloader:
    ```bash
    node download_roads.js
    ```
    This fetches map geometries from OpenStreetMap's Overpass API and saves them directly to the app's assets folder.

---

## 🎨 Theme & Aesthetic Guidelines

All interface components must adhere strictly to the guidelines defined in the [Master Design Standards](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/master_design_standards.md):
*   **Typography**: DNA sequences, telemetry feeds, and numbers must use `Monospace`. Main headers and buttons must use uppercase `Sans-Serif`.
*   **Color Palette**: Primary theme features **CyberGreen** (`#00FF66`), sub-headers feature **CyberGreenDim** (`#00993C`), and special states use Purple (`#A855F7`), Cyan (`#22D3EE`), and Yellow (`#FBBF24`).
*   **Cyberglass Frame**: Solid or dark background panels with 1.dp borders at 35% opacity and optional subtle, low-intensity glows. No heavy neon filters are permitted.
