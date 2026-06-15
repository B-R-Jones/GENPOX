# GENPOX Developer & Debug Functions Reference

This document tracks **explicit developer-mode, debug-only, and override functions** in the GENPOX codebase.

*(Note: The "Forced Construction", "Reactor Freezing", and "Auto-Loop" compilation sequences are legitimate, risky core gameplay mechanics and are not developer-only tools.)*

---

## 1. Developer State Variables

Developer overrides assist in testing, accelerating scanning operations, and seeding inventory.

### Android Application (Kotlin)

State flow tracked in [MainViewModel.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/MainViewModel.kt):

| State Variable | Location | Description & Effect |
| :--- | :--- | :--- |
| `devForceAnomaly` | [MainViewModel.kt:L120-122](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/MainViewModel.kt#L120-L122) | **GNPX Mode**: StateFlow toggled via header badge. When active, anomalous fusions succeed with 100% chance and do not consume standard nucleotides. Also displays the developer inject button. |

### React Web App (TypeScript)

State variables declared in [PoxConsole.tsx](file:///c:/Users/Brent/Antigravity/GENPOX/src/components/PoxConsole.tsx):

| State Variable | Location | Description & Effect |
| :--- | :--- | :--- |
| `devForceAnomaly` | [PoxConsole.tsx:L1226-1235](file:///c:/Users/Brent/Antigravity/GENPOX/src/components/PoxConsole.tsx#L1226-L1235) | **GNPX Mode**: Toggled via the header GNPX button. When active, it bypasses standard nucleotide/gene consumption and guarantees 100% success on anomalous fusion events. It also expands the sync-net scan radius from 60ft to 120ft. |
| `isDevSpeedActive` | [PoxConsole.tsx:L1573](file:///c:/Users/Brent/Antigravity/GENPOX/src/components/PoxConsole.tsx#L1573) | **60x Overclock**: Accelerates idle synthesis scans from 1s real-time ticks to a 60x fast-forward multiplier. This state can only be toggled when `devForceAnomaly` is enabled. |

---

## 2. Developer UI Controls

Developer functions are exposed via dedicated UI elements in the game interfaces.

### Android Application (Kotlin)

UI elements declared in [MainScreen.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/MainScreen.kt) and [PoxScreens.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/PoxScreens.kt):

| UI Control / Button | Location | Action |
| :--- | :--- | :--- |
| **GNPX Badge / Button** | [MainScreen.kt:L135-157](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/MainScreen.kt#L135-L157) | Rendered in the main header bar next to `NTP_SYNC`. Toggles `devForceAnomaly` (lights up high-contrast orange when active). |
| **DEV: INJECT 10K GENES Button** | [PoxScreens.kt:L921-940](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/PoxScreens.kt#L921-L940) | Appears on the **P.O.X. Reactor** page under the acceleration button when GNPX Mode is active. Automatically generates 10,000 random 8-base genes and inserts them into inventory in bulk. |
| **DEV: INJECT MISSING GENES Button** | [PoxScreens.kt:L2434-2454](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/PoxScreens.kt#L2434-2454) | Appears in the **Splicer** view when GNPX Mode is active. Automatically adds all 8-base target sequence segments missing from stock to the inventory. |

### React Web App (TypeScript)

UI elements declared in [PoxConsole.tsx](file:///c:/Users/Brent/Antigravity/GENPOX/src/components/PoxConsole.tsx):

| UI Control / Button | Location | Action |
| :--- | :--- | :--- |
| **GNPX Badge / Button** | [PoxConsole.tsx:L4996-5010](file:///c:/Users/Brent/Antigravity/GENPOX/src/components/PoxConsole.tsx#L4996-5010) | Rendered in the console header. Controls `devForceAnomaly`. Toggling this badge serves as the primary entryway into developer mode. |
| **⚡ DEV (60x) Button** | [PoxConsole.tsx:L8556-8573](file:///c:/Users/Brent/Antigravity/GENPOX/src/components/PoxConsole.tsx#L8556-8573) & [L11091-11108](file:///c:/Users/Brent/Antigravity/GENPOX/src/components/PoxConsole.tsx#L11091-L11108) | Rendered in the mobile and desktop anomaly specification details. Toggles the `isDevSpeedActive` multiplier (requires `devForceAnomaly` to be active). |
| **DEV: INJECT MISSING GENES Button** | [PoxConsole.tsx:L6416-6423](file:///c:/Users/Brent/Antigravity/GENPOX/src/components/PoxConsole.tsx#L6416-L6423) | Appears under the auto slot controls on the **Splicer** panel when GNPX Mode is active. Identical target gene injection action. |

---

## 3. Dev Trigger Logic / Functions

### Android Application (Kotlin)

Backend functions supporting developer tools in [MainViewModel.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/MainViewModel.kt):

| Function | Location | Description |
| :--- | :--- | :--- |
| `toggleDevForceAnomaly()` | [MainViewModel.kt:L124-128](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/MainViewModel.kt#L124-L128) | Toggles the active/inactive developer state flow. |
| `addDevGenes()` | [MainViewModel.kt:L130-153](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/MainViewModel.kt#L130-L153) | Runs on `Dispatchers.IO`. Generates 10,000 random 8-base gene combinations, merges counts, and batch-inserts them into Room using the repository's bulk transaction. |
| `devInjectMissingTargetGenes()` | [MainViewModel.kt:L857-L890](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/MainViewModel.kt#L857-L890) | Extracts required 8-base gene blocks from the target sequence, checks active stock, and bulk-inserts missing segments into the database. |

### React Web App (TypeScript)

Component functions supporting developer tools in [PoxConsole.tsx](file:///c:/Users/Brent/Antigravity/GENPOX/src/components/PoxConsole.tsx):

| Function | Location | Description |
| :--- | :--- | :--- |
| `handleInjectMissingTargetGenes()` | [PoxConsole.tsx:L2661-L2691](file:///c:/Users/Brent/Antigravity/GENPOX/src/components/PoxConsole.tsx#L2661-L2691) | Scans target sequence segments, checks current stock sequences, and inserts missing required genes into the active user state. |
