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

---

## 3. Dev Trigger Logic / Functions

### Android Application (Kotlin)

Backend functions supporting developer tools in [MainViewModel.kt](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/MainViewModel.kt):

| Function | Location | Description |
| :--- | :--- | :--- |
| `toggleDevForceAnomaly()` | [MainViewModel.kt:L124-128](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/MainViewModel.kt#L124-L128) | Toggles the active/inactive developer state flow. |
| `addDevGenes()` | [MainViewModel.kt:L130-153](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/MainViewModel.kt#L130-L153) | Runs on `Dispatchers.IO`. Generates 10,000 random 8-base gene combinations, merges counts, and batch-inserts them into Room using the repository's bulk transaction. |
| `devInjectMissingTargetGenes()` | [MainViewModel.kt:L857-L890](file:///c:/Users/Brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/MainViewModel.kt#L857-L890) | Extracts required 8-base gene blocks from the target sequence, checks active stock, and bulk-inserts missing segments into the database. |
