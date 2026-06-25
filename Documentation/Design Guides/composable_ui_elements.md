# GENPOX Composable UI Elements Catalog

This document catalog lists and describes all Jetpack Compose `@Composable` UI elements across the GENPOX codebase. It serves as a central reference for developers to understand the available UI components, their location, and their roles within the application.

---

## 1. Application Architecture & Navigation

These components manage the application lifecycle, routing, and primary container frames.

| Composable Function | Source File | Description |
| :--- | :--- | :--- |
| `MainNavigation` | [Navigation.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/Navigation.kt#L15) | Core application routing handler. Configures the navigation backstack and directs user flow. |
| `MainScreen` | [MainScreen.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/MainScreen.kt#L33) | Main game dashboard. Houses the active tab content, top status header, sub-navigation tabs, and bottom interactive diagnostics logger terminal. |

---

## 2. Reusable UI Components & Custom Canvases

General-purpose UI components that provide consistent styling, canvas-drawn wireframes, and profiling overlays. Located in [ui/components/](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/components).

| Composable Function | Source File | Description |
| :--- | :--- | :--- |
| `QrCodeImage` | [QrCodeGenerator.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/components/QrCodeGenerator.kt#L36) | Renders a generated QR code from a string seed. Used for multiplayer sync and sharing genes. |
| `PoxTabFrame` | [PoxTabFrame.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/components/PoxTabFrame.kt#L29) | Standard sci-fi panel frame. Includes custom corner decorations, status fields, sub-navigation integration, and scroll state configurations. |
| `PoxUnifiedHoloNav` | [PoxHoloNav.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/components/PoxHoloNav.kt#L38) | Unified gesture-driven holo-navigation container supporting swipe-to-stack, tap-to-unstack, vertical row cycling, and bouncy spring physics. |
| `PoxHoloNav` | [PoxHoloNav.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/components/PoxHoloNav.kt#L206) | Standard sub-tab selector row. Refactored to delegate to `PoxUnifiedHoloNav` as a zero-overhead wrapper. |
| `PoxHoloButton` | [PoxHoloButton.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/components/PoxHoloButton.kt#L15) | Reusable standard 46.dp holographic navigation button styled with custom cyberglass borders and glow. |
| `PoxCameraScanner` | [PoxCameraScanner.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/components/PoxCameraScanner.kt#L35) | Integrates Android CameraX and ML Kit Barcode scanning to scan QR codes. |
| `NodeCrystalCanvas` | [NodeCrystalCanvas.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/components/NodeCrystalCanvas.kt#L40) | Dynamic canvas rendering holographic waveforms and dinucleotide base pairing frequencies. |
| `DualPaneConsoleFrame` | [DualPaneConsoleFrame.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/components/DualPaneConsoleFrame.kt#L20) | Multi-pane UI shell supporting primary/secondary screen partitions with cyberpunk borders. |
| `CreatureWireframeView` | [CreatureWireframeView.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/components/CreatureWireframeView.kt#L292) | Interactive 3D wireframe mesh generator that projects and rotates a creature model calculated deterministically from DNA. |
| `FrameTimeMonitor` | [MapProfiler.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/components/MapProfiler.kt#L131) | Uses Compose scheduler frames to monitor rendering loop times (FPS). |
| `ProfilerHUDOverlay` | [MapProfiler.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/components/MapProfiler.kt#L199) | HUD overlay showing active draw counts and rendering diagnostic details. |
| `ProfilingCanvas` | [MapProfiler.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/components/MapProfiler.kt#L334) | Custom canvas tracking low-level rendering calls (paths, arcs, text). |

---

## 3. Network and Multi-Peer Subviews

P2P multiplayer widgets and communication diagnostics tabs. Located in [NetworkScreen.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/NetworkScreen.kt).

| Composable Function | Description |
| :--- | :--- |
| `NetworkView` | Base container layout for the P2P connection deck. |
| `DiagnosticsContent` | Renders coordinates tracker, live signal latency stats, and connection logs. |
| `InboxContent` | Renders incoming lore emails, notifications, and decrypted messages. |
| `FriendsListContent` | List of registered multiplayer peer names, latency, and connection states. |
| `HoloGearIcon`, `HoloMailIcon`, `HoloFriendsIcon`, `HoloCautionIcon` | High-fidelity canvas-drawn holographic indicators. |

---

## 4. Telemetry Screens & Scientific Simulation Graphs

Scientific formulas and active planetary simulation charts. Located in [TelemetryScreen.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/TelemetryScreen.kt).

| Composable Function | Description |
| :--- | :--- |
| `TelemetryView` | Scrolling catalog of active planetary/cosmic physics models. |
| `TelemetryDetailScreen` | View detailed documentation and interactive simulation sliders for a chosen system. |
| `InteractiveWidgetRouter` | Routes a metric ID to its respective interactive simulation widget: |
| ↳ `LunarCycleSimulator` | Simulates lunar age, waxing/waning states, and shifts. |
| ↳ `MoonModifierGraph` | Plots active mutations over time as a line graph. |
| ↳ `MarkovTransitionMatrix` | Displays codon substitution probability tables. |
| ↳ `DeterministicHashFlowchart` | Diagram displaying deterministic PRNG seeding logic. |
| ↳ `SpectrumCouplingSineGraph` | Sine wave chart mapping hourly coupling cycles. |
| ↳ `LogarithmicSuccessGraph` | Graphs success rates of splicing operations based on logarithmic curves. |
| ↳ `DensityDragBarChart` | Charts genetic drag forces. |
| ↳ `PhenotypeGeneDecoder` | Custom input decoding DNA sequences into stats. |
| ↳ `WireframeGlobe` | Renders a revolving 3D globe showing global anomaly coordinate zones. |

---

## 5. Biological Lab, Splicing & Main Panels

Core gameplay loops, combinators, reactors, gene vaults, settings, and test benches.

| Composable Function | Source File | Description |
| :--- | :--- | :--- |
| `ReactorDashboardView` | [ReactorScreen.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/ReactorScreen.kt) | Controls gene incubation cycles, replication flows, and stream configurations. |
| `AnomalyDashboardView` | [AnomalyScreen.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/AnomalyScreen.kt) | Renders the GPS mapping system scanner, active anomaly nodes, and deployed missions. |
| `StepSearchView` | [StepSearchScreen.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/StepSearchScreen.kt) | Diagnostic codon analysis view displaying matching base sequences. |
| `BatchPacketLogView` | [CombinatorScreen.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/CombinatorScreen.kt) | Displays live incoming diagnostic streams. |
| `AnomalyVaultView` | [AnomalyScreen.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/AnomalyScreen.kt) | Catalog of discovered anomaly nodes and their genetic makeup. |
| `CombinatorView` | [CombinatorScreen.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/CombinatorScreen.kt) | DNA combinator screen for merging genome pools. |
| `SplicerView` | [SplicerScreen.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/SplicerScreen.kt) | DNA splicer screen containing detailed genomes selectors, slot frames, and results. |
| `SplicerLeftPanel` / `SplicerSlotCell` / `SplicerRightPanel` | [SplicerScreen.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/SplicerScreen.kt) | Splicer sub-panels, selectors, and controls. |
| `VaultView` | [VaultScreen.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/VaultScreen.kt) | Local player sequence vault. |
| `ScannerView` | [ScannerScreen.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/ScannerScreen.kt) | Displays the radar overlay map scanner. |
| `ForecastView` | [ForecastScreen.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/ForecastScreen.kt) | Climate forecast simulation details. |
| `InventoryView` | [InventoryScreen.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/InventoryScreen.kt) | Grid showing collected biological materials and assets. |
| `PaletteSettingsView` / `SettingsView` | [SettingsScreen.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/SettingsScreen.kt) | Configurations and color settings views. |
| `HolographicRadarScanner` | [ScannerScreen.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/ScannerScreen.kt) | Renders a rotating radar sweeps canvas over the background road lines. |
| `ZoomScrollCircle` / `RotationScrollCircle` | [ScannerScreen.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/ScannerScreen.kt) | Interactive rotary dials for controlling zoom and map rotation. |
| `SingleDnaDotGrid` / `DnaComparisonGrid` | [SharedDnaWidgets.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/SharedDnaWidgets.kt) | Colorful base-pair visual grids. |
| `HolographicHelixReactor` | [ReactorScreen.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/ReactorScreen.kt) | Rotating DNA helix projection canvas. |
| `GeneticResonanceScope` | [SplicerScreen.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/SplicerScreen.kt) | Oscillating wave canvas showing codon alignment. |
| `BioLabTestView` / `SplicerTestView` | [ReactorScreen.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/ReactorScreen.kt) / [SplicerScreen.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/SplicerScreen.kt) | Testing sandboxes for verifying codon math results. |
| `GeneDetailsPopup` / `StepSearchGeneDetailsPopup` / `VaultGeneDetailsPopup` | [GeneDetailsPopups.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/GeneDetailsPopups.kt) | Detailed stats and details card modals. |
| `WireframeHeart` (and other wireframes) | [WireframeIcons.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/WireframeIcons.kt) | Glowing cyberpunk canvas badges representing creature stats, factions, and genetic origin traits. |

