# GENPOX: Antigravity Workspace Master Design Instruction & Architecture Rulebook

Target Application: G.E.N. P.O.X. (Native Android / pox-android)
Target Stack: Kotlin 2.3.x, Jetpack Compose (BOM 2026.03.01), Coroutines 1.10.2, Android 14+ (Target SDK 36, Min SDK 24)
Scope: Strict workspace enforcement rules for Antigravity AI agents across all code modifications, refactors, and feature generation.

---

## 1. System Architecture & Dependency Management

### 1.1. MVVM & Clean Layer Separation
- Strict Layer Isolation: The codebase must adhere to three strict layers: UI (Jetpack Compose), Domain (UseCases, Entities, Engines), and Data (Room DB, Repositories, Network, DataSources).
- Prohibition of UI Hardware Leaks: Composable functions must never directly access Android system services (e.g., LocationManager, BluetoothManager, SensorManager, CameraManager). All hardware interactions must be encapsulated in dedicated DataSource interfaces in the Data layer and exposed via StateFlow streams.
- Prohibition of ViewModel Nesting: ViewModels must never manually instantiate other ViewModels (e.g., `val reactor = ReactorViewModel(...)`). Every ViewModel must be independently registered with `@HiltViewModel` and injected at navigation boundaries via `hiltViewModel()`.

### 1.2. Dependency Injection Standards (Google Hilt)
- Zero Global Singletons: Global state objects (e.g., `PoxApplication.repository`) are strictly prohibited.
- Constructor Injection: All ViewModels, Repositories, DataSources, and Simulation Engines must use constructor injection (`@Inject constructor(...)`).
- Coroutine Dispatcher Injection: Hardcoding `Dispatchers.IO`, `Dispatchers.Default`, or `Dispatchers.Main` inside business logic or ViewModels is forbidden. All classes performing asynchronous work must receive an injected `DispatchersProvider` interface to guarantee deterministic unit testing.

---

## 2. Concurrency, Threading & Resource Safety

### 2.1. Coroutine Lifecycle & Scoping Rules
- Supervised Scopes: Standalone coroutine scopes (e.g., in background engines or managers) must always include a `SupervisorJob()` (e.g., `CoroutineScope(SupervisorJob() + dispatchers.default)`). Unsupervised scopes that risk cascading cancellation are strictly forbidden.
- ViewModel Lifecycle Scopes: ViewModels must execute all coroutines within `viewModelScope`.
- Cancellation Cooperative Code: All iterative simulation loops (e.g., stepped DNA transcription, haversine distance scans, wave calculations) must remain cooperative with cancellation by invoking `ensureActive()` or checking `isActive` on each iteration.

### 2.2. Audio Engine & Native Hardware Lifecycle
- Zero Per-Sound AudioTrack Allocations: Allocating a new `AudioTrack` instance in `MODE_STATIC` per sound effect trigger is strictly prohibited due to Android's 32-track system limit.
- Streaming Audio Engine Architecture: Procedural audio generation must use a single persistent streaming `AudioTrack` in `MODE_STREAM` with a non-blocking ring buffer. Short transient UI sound effects must utilize a reusable `SoundPool` instance.
- Explicit Lifecycle Cleanup: All native resources, camera sessions, audio tracks, and broadcast receivers must implement explicit `close()`, `release()`, or `teardown()` methods bound to lifecycle cleanup events.

---

## 3. Jetpack Compose & UI Performance Rules

### 3.1. Zero Heap Allocations in 60fps/120fps Draw Scopes
- Canvas Draw Scope Ban: Instantiating heap objects inside `Canvas`, `DrawScope`, or `onDrawWithContent` is strictly forbidden. This includes `Path()`, `Paint()`, `Matrix()`, `UUID.randomUUID()`, temporary `List`, and string formatting.
- Pre-allocation Pattern: All `Path` instances, geometric bounds, and projection arrays must be remembered outside the draw scope (e.g., `val path = remember { Path() }`) and reset before drawing via `path.reset()`.
- Trigonometric Pre-computation: Complex 3D/2D projection math (such as orbital Keplerian projections or creature wireframes) must be pre-calculated inside a `remember(key)` geometry model, keeping frame drawing zero-allocation.

### 3.2. Lifecycle-Aware State Collection
- Mandatory collectAsStateWithLifecycle: Collecting flows in Composables using standard `collectAsState()` is strictly prohibited. All flows must be collected using `collectAsStateWithLifecycle()` from `androidx.lifecycle.compose` to stop emissions and save battery when screens are in the background.
- Immutable State Containers: All state classes exposed to Compose must be annotated with `@Immutable` or `@Stable` to prevent unnecessary recompositions.

### 3.3. Elimination of Global Mutable Themes
- Prohibition of `object CyberTheme` Mutations: Storing `mutableStateOf` variables on global static `object` singletons is strictly prohibited.
- CompositionLocal Theme Architecture: Theme palettes must be structured as immutable `@Immutable data class CyberColors(...)` and provided down the hierarchy via `CompositionLocalProvider(LocalCyberColors provides ...)`.

---

## 4. Android 14+ / Modern Platform Compatibility (Target SDK 36)

### 4.1. Strict Non-SDK Hidden Reflection Ban
- Zero Hidden API Reflection: Calling Java reflection on private or hidden Android framework fields/methods (e.g., `CursorWindow.sCursorWindowSize`) is strictly prohibited. Code violating ART runtime restrictions will fail CI review immediately.
- Room Data Handling: If large datasets require chunking, utilize Room pagination, SQLite cursors with explicit windowing, or streaming `Flow<List<T>>` queries.

### 4.2. Safe Database Evolution & Type Safety
- No Destructive Database Fallbacks: Using `.fallbackToDestructiveMigration()` on production databases is forbidden. All Room schema updates must include explicit `Migration(from, to)` classes.
- Automated Schema Verification: Every schema version change must have an accompanying Room `MigrationTest` to verify that player game saves are never wiped.
- Domain Value Classes: Primitives with semantic meaning must be wrapped in `@JvmInline value class` (e.g., `DnaSequence`, `GeneBlock`, `SectorId`) to prevent stringly-typed bugs. Tab routing must use strongly-typed `sealed interface MainTab`.

---

## 5. Visual Aesthetics, Typography & HUD Design Standards

### 5.1. Typography Hierarchy
- Monospace (`FontFamily.Monospace`): Enforced for all DNA sequences (8-char and 64-char), numerical stats, Phred Q-scores, terminal outputs, console streams, and telemetry readouts.
- Sans-Serif (`FontFamily.Default` / Space Grotesk): Enforced for navigation tabs, action buttons, dialogs, form labels, and section headings.

### 5.2. UI Formatting & Emoji Policy
- Strict Emoji Ban: Emojis are strictly banned from all user-facing UI text, headers, buttons, logs, dialogs, and popups.
- Text Capitalization: All button labels, navigation tab titles, and main console headers must be uppercase.
- No Enclosing Brackets in General Labels: Avoid unnecessary decorative bracket wrapping (`[ ]`) or symbols in headers unless explicitly defining a terminal flavor tag.

### 5.3. Standard Palette Definitions
- CyberGreen: `0xFF00FF66` (Primary active highlights, positive status)
- CyberGreenDim: `0xFF00993C` (Sub-headers, secondary labels, dimmed indicators)
- CyberPurple: `0xFFA855F7` (Anomaly Engine theme, anomalous mutations)
- CyberCyan: `0xFF00E5FF` (Celestial Orrery, telemetry grids, GPS tracking)
- CyberRed: `0xFFEF4444` (Emergency warnings, critical collapse, dismiss buttons)
- CyberPanel: `0xFF09120B` (Background console panel surface)
- CyberBorder: `0xFF0D2513` (Default bounding frame outline)

---
*Rulebook active for all Antigravity code generation, refactoring, and review workflows.*