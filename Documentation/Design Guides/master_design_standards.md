# Master Design & Font Standards

To preserve the cyber-retro/sci-fi aesthetic of GENPOX, we standardize on two primary typography patterns.

---

## 1. Typography Hierarchy & Roles

| Platform | Font Family | Role & Usage | Visual Category |
| :--- | :--- | :--- | :--- |
| **Android** | `FontFamily.Monospace` | DNA strings, numeric stats, console logs, terminal output, telemetry screens. | **Monospace** |
| **Android** | System Sans-Serif | Navigation tabs, main buttons, headers/titles, dialog options, standard text. | **Sans-Serif** |

---

## 2. Platform Implementation Details

### Native Android (Jetpack Compose)
Defined as Material3 Typography in [Type.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/theme/Type.kt):
```kotlin
val Typography = Typography(
    bodyLarge = TextStyle(fontFamily = FontFamily.Monospace, ...),
    bodyMedium = TextStyle(fontFamily = FontFamily.Monospace, ...),
    bodySmall = TextStyle(fontFamily = FontFamily.Monospace, ...),
    titleLarge = TextStyle(fontFamily = FontFamily.Monospace, ...),
    titleMedium = TextStyle(fontFamily = FontFamily.Monospace, ...),
    labelSmall = TextStyle(fontFamily = FontFamily.Monospace, ...)
)
```

#### Usage Examples:
1. **Sans-Serif (Standard Interface)**:
   *Use default system font (by omitting fontFamily parameter or explicitly passing FontFamily.Default)*:
   ```kotlin
   Text(
       text = "INITIALIZE REACTOR",
       style = MaterialTheme.typography.bodyMedium,
       fontFamily = FontFamily.Default, // Or default sans-serif
       fontWeight = FontWeight.Bold
   )
   ```
2. **Monospace (Genetic/Telemetry Data)**:
   *Use standard Monospace font family (or the Typography preset styles)*:
   ```kotlin
   Text(
       text = "GEN: ATCGGCTA...",
       style = MaterialTheme.typography.bodySmall,
       fontFamily = FontFamily.Monospace
   )
   ```

---

## 3. Typography Standardization Rules

1. **Keep DNA Sequences Monospace**: All representations of 64-char genomic sequences, splicing buffers, or base pairs **must** be rendered in `Monospace` (Share Tech Mono / `FontFamily.Monospace`) to prevent variable character widths from causing visual misalignment.
2. **Numbers & Stat Values**: To align stats properly in listings and bars, represent all numeric attributes (Vitality, Aggression, Defense, Speed, Telomeres) with `Monospace` fonts.
3. **Buttons, Dialogs & Nav Headers**: Use `Sans-Serif` (Space Grotesk) to improve readability of standard menu commands, button triggers, and dialog overlays. All text inside these components **must** be capitalized completely (rendered in uppercase).
4. **Logs & Decryption Feeds**: Command terminals, decryption feeds, and siphoning logs **must** use `Monospace`.
5. **Form & Overlay Headers**: Form headers, panel headers, and popup overlay headers **must** use Sans-Serif (`FontFamily.Default` / Space Grotesk), `9.sp` (or equivalent `text-[9px]`) font size, the tab's secondary dim primary color (e.g., `activeColorDim`, `CyberGreenDim`, or dim purple/red), and be written in uppercase without any enclosing brackets `[` `]` or extraneous characters like leading bullet points (`●`).
6. **General Syntax Constraints & Emoji Ban**: Emojis are strictly banned from all user-facing text, labels, buttons, headers, logs, and popups to preserve the sci-fi retro terminal aesthetic. No leading emojis, bullet points, or symbols in text labels/headers. No containing brackets `[` `]` or braces `{}`. Keep texts simple, clean, and uppercase.

---

## 4. Thematic Color Definitions

We maintain a consistent sci-fi retro color system:

| Color Name | Color HEX (Compose) | Context & Usage |
| :--- | :--- | :--- |
| **CyberGreen** | `0xFF00FF66` | Primary active green theme, button highlights, positive notifications. |
| **CyberGreenDim** | `0xFF00993C` | Secondary dim green, labels, sub-headers, text overlays. |
| **Purple** | `0xFFA855F7` | Anomaly Engine theme, anomalous gene triggers. |
| **PurpleDim** | `0xFF701A75` | Anomaly Engine sub-headers, secondary dim labels. |
| **Cyan** | `0xFF22D3EE` | Celestial & telemetry status, special radar grids. |
| **Yellow** | `0xFFFBBF24` | Warnings, intermediate/selection states, Undo/Reset actions. |
| **Red** | `0xFFEF4444` | System failure states, close/dismiss buttons. |
| **CyberPanel** | `0xFF09120B` | Background panel color. |
| **CyberBorder** | `0xFF0D2513` | Default border/frame outline color. |

---

## 5. Visual Rendering: Phosphor vs. Holographic

GENPOX consoles alternate between two rendering models:
1.  **Phosphor Visuals**: Emulate old CRT green monochrome terminals.
    *   Utilizes primarily **CyberGreen** and **CyberGreenDim**.
    *   Ensure all text maintains legibility with high contrast ratios on dark frames.
2.  **Holographic Visuals**: Represent high-tech overlays.
    *   Utilizes a variety of colors (Cyan, Purple, Yellow, Red) depending on the active node/engine.
    *   **Rule**: Holographic visuals must **not** emit excessive or heavy glowing filters to avoid being mistaken for basic neon styling. Glows must be subtle and thin (`glowColor = borderColor.copy(alpha = 0.15f)`).

---

## 6. Button & Frame Layout Styles

### The Cyberglass Frame Aesthetic
All interactive containers, panels, and buttons utilize the double-layered **Cyberglass** style:
*   A solid or semi-transparent dark background (`Color.Black` or `CyberPanel`).
*   A thin 1.dp border with 35% opacity.
*   **Double-Layer Corner Ticks**: 8.dp corner lines drawn in the four outer corners of the box layout to create a premium retro wireframe border.

### Standard Button Layouts
Standard controls must use custom double-layered cyberglass box structures (instead of stock buttons or flat clickable text):

1.  **Close Button**:
    *   **Style**: Red border (`borderColor = Color.Red`, background `Color.Transparent`).
    *   **Text**: `"✕ CLOSE"` (in default Sans-Serif, 8.sp, bold, red color).
    *   **Padding**: `horizontal = 8.dp, vertical = 4.dp`.
    *   **Placement**: Must be positioned as the top-rightmost element within the header row.
2.  **Clear / Reset Button**:
    *   **Style**: Yellow border (`borderColor = Color.Yellow`, background `Color.Transparent`).
    *   **Text**: `"✕ RESET"` or `"✕ CLEAR ALL"` (in default Sans-Serif, 8.sp, bold, yellow color).
    *   **Padding**: `horizontal = 8.dp, vertical = 4.dp`.
3.  **Undo / Back Button**:
    *   **Style**: Yellow border (`borderColor = Color.Yellow`, background `Color.Transparent`).
    *   **Text**: `"↶ UNDO"` or `"✕ BACK"` (in default Sans-Serif, 8.sp, bold, yellow color).
    *   **Padding**: `horizontal = 8.dp, vertical = 4.dp`.
4.  **Frame Navigation Buttons**:
    *   **Style**: Small, square holographic buttons floating in corners of active screens.
    *   **Size**: `46.dp` x `46.dp`.
    *   **Border & Glow**: Styled using `.cyberglass(...)` modifier. Mapped to active color when selected (e.g. `CyberGreen`, purple, or cyan), and dimmed secondary color when inactive (e.g. `CyberGreenDim.copy(alpha = 0.4f)`). Thin glow (`glowColor = borderColor.copy(alpha = 0.15f)`) is active only when selected.
    *   **Content**: Contains either a 24.dp custom wireframe Canvas vector icon (such as a target reticle) or exactly three horizontal lines of question marks/symbols/text stacked vertically in `FontFamily.Monospace`, 7.sp, bold, uppercase.
    *   **Feedback**: Trigger arcade chime sound beep upon click.
5.  Unified Holo-Nav Container & Gestures (PoxUnifiedHoloNav):
    *   **Structure**: Combines single-row sub-tabs and multi-row action decks into a single, high-fidelity gesture-driven container using standard `46.dp` holographic square buttons.
    *   **Horizontal Swipe-to-Stack**:
        *   Swiping right slides the buttons of all rows together and shifts them offscreen by `64.dp`. With the container padded `22.dp` from the screen boundary, this leaves a tiny `4.dp` sliver of the stacked handle visible.
        *   Swiping left pulls the deck back out to its normal, unstacked state.
    *   **Tap-to-Unstack Option**: To prevent accidental triggers of the Android system back gesture while swiping left from the edge, a single tap/click on the top stacked button handle instantly restores/unstacks the deck. Standard button actions are disabled when stacked, and only the topmost button is click-enabled to trigger unstacking.
    *   **Z-Depth Cycle Gestures (Multi-Row)**: For multi-row configurations, swiping vertically (up/down) cycles which row is in the foreground. Inactive rows are scaled down (`0.85f`), offset vertically by `-24.dp`, and have their clicks disabled to prevent accidental hits.
    *   **Stack Row Fading**: During horizontal stacking, inactive rows fade out completely (`alpha = 0f`) to keep the collapsed edge handle clean. Buttons in the active row that sit underneath the top stacked handle fade slightly (`alpha = 0.5f` factor) to emphasize the card-deck layout.
    *   **Spring & Bounce Physics**:
        *   Transitions for stacking, unstacking, and row cycling use spring physics for an active, mechanical feel.
        *   *Vertical Attraction Bounce*: The inactive row performs a periodic vertical bounce (rising by `-8.dp` and returning via low-stiffness spring every 6 seconds) when unstacked.
        *   *Horizontal Offscreen Bounce*: The stacked handle performs an infinite horizontal bounce (oscillating left by `-8.dp` and returning) when collapsed to indicate it is draggable.
    *   **Feedback**: Trigger combinator click/tick sounds on stacking, unstacking, and cycling.

---

## 7. Wireframe Animations

For details on custom 2D/3D wireframe animations, coordinate projection, and rendering optimizations in Compose, see the companion document:
*   [Wireframe Animation Guidelines](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/wireframe_animation_guidelines.md)

## 8. Holographic Zoom Scroll Circle & Morphing Test Animation

To enhance scanner map manipulation, the radar zoom is represented by a compact circular dial that collapses into a small magnifying glass icon when not in use.

### Scroll Circle Design Details
*   **Track & Styling**: A solid, thin Cyber Green (`0xFF00FF66`) circular track of size `90.dp`. The active portion is highlighted, and a drag/tap-sensitive radial line thumb slides clockwise from the bottom start angle (`90°`).
*   **Anti-Wrap boundary guard**: Strictly checks angle thresholds around `90°` to prevent the slider value from wrapping/jumping directly between min (`0f`) and max (`4f`) zoom levels.
*   **Central Readout**: Displays the zoom factor (e.g., `2.00x`) inside a `54.dp` central translucent black overlay disc in Cyber Green.
*   **Stationary Bottom Navigation**: The main Radar and List tab navigation buttons at the bottom-right of the viewport remain completely stationary. The zoom control is laid out as a sibling directly inside the viewport `Box`, decoupling its size and position changes from the bottom buttons.

### Morphing Expanding Test Animation
*   **Triggers**: When the radar sub-tab is selected, a small green circular button containing a wireframe magnifying glass (with a handle connected to the inside of the lens) is placed directly above the radar navigation button.
*   **Behavior**:
    *   Tapping the magnifying glass button initiates a seamless expansion transition (`expansionFraction` from `0f` to `1f` using a 350ms transition) morphing it into the full `90.dp` circular dial.
    *   Tapping the central zoom readout disc collapses it back into the small magnifying glass button.
    *   **Morphing Variables**: The size morphs from `46.dp` to `90.dp`, the center overlay size scales from `46.dp` to `54.dp`, the background opacity increases, the magnifying glass vector fades out, and the zoom dial/text fades in.
    *   **Layout Translation**: The padding shifts dynamically based on `expansionFraction`: `bottom` remains stationary at `70.dp` (maintaining a constant `8.dp` gap above the buttons), and `end` interpolates from `70.dp` (centered directly over the Radar button) to `21.dp` (perfectly centered above the combined span of both the Radar and List buttons).

---

## 9. Padding & Layout Spacing Standards

To ensure pixel-perfect alignment and visual consistency across all view tabs:

1. **Vertical Spacing Between Main Sibling Elements**:
   - The standardized vertical spacing between sibling widgets (e.g. counts section, forecast wave card, forecast grid row, visualizer, scope card, logs) within a primary layout Column **must** be exactly `10.dp`.
   - In Native Android (Jetpack Compose), this is enforced using `Arrangement.spacedBy(10.dp)` on the scrollable content Column inside `PoxTabFrame`.
   - Manual `Spacer(modifier = Modifier.height(...))` dividers must **not** be inserted between standard sibling widgets inside these Columns to prevent spacing drift.

2. **Inner Container Padding (Cyberglass Containers)**:
   - **Primary Counts Panel**: The main numeric status grid or row at the top of a frame **must** use an inner padding of `vertical = 10.dp, horizontal = 8.dp` within its cyberglass wrapper.
   - **Wide Status Card (e.g., active wave status)**: Uses an inner padding of `8.dp` on all sides.
   - **Grid Forecast Cards (e.g., tomorrow/day after columns)**: Uses an inner padding of `6.dp` on all sides to prevent text clipping and wrapping on narrow devices.

3. **Subtabs & Subframe Structure**:
   - Subtabs (e.g., Step Search, Synthesis Lists, logs) and subframes **must** render inline inside the content slot of their parent tab frame (e.g., `PoxTabFrame`), rather than as overlays/dialogs.
   - The primary tab header (containing the screen title, status indicator, and context description) and the bottom navigation subtabs **must** remain fully visible and active at all times.
   - To accommodate scrollable listings or tables within a subtab (such as `LazyColumn` or `LazyVerticalGrid`), set `isScrollable = false` on the parent frame (e.g., `PoxTabFrame`) to disable outer column scrolling and constrain the height, allowing inner list components to use `.weight(1f)` without nested scrolling exceptions.
   - **Navigation Placement**: No tab or subtab navigation buttons at the top of forms. All tab navigations must be handled via the lower tab grid. All subtab navigations must be handled via our dedicated composable for holo-nav buttons/controls.
   - **No bottom flavor or info text**: To maximize content area and prevent UI clutter, we must never take up space at the bottom of a frame with flavor text, info text, or any other kind of passive status text. Only active functional indicators or countdowns (such as synthesis progress or active boost remaining) are permitted at the bottom.

4. **No Scrolling on Main Frames (Banned Solution)**:
   - Using scrolling as a solution to resolve crowded user interfaces on main screens or tabs is strictly banned.
   - Main frames and tab screens must be designed layout-wise to be completely usable, visible, and functional without requiring the player to scroll to access any interactive controls (such as sliders, selectors, or action buttons). All controls must fit comfortably within the viewport.
