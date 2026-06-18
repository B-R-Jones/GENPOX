# Dual Pane Console Template Typography & Text Guide

This document defines all text-requiring properties of the `DualPaneConsoleFrame` Jetpack Compose component in **GENPOX** (located at [DualPaneConsoleFrame.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/components/DualPaneConsoleFrame.kt)). It provides standard layout rules and formatting guidelines.

---

## 1. Component Signature

```kotlin
@Composable
fun DualPaneConsoleFrame(
    modifier: Modifier = Modifier,
    theme: String = "green", // "green" or "purple"
    flavorTitle: String,
    statusText: String? = null,
    statusColor: Color = CyberGreen,
    statusClickable: (() -> Unit)? = null,
    primaryTitle: String? = null,
    primaryContent: @Composable ColumnScope.() -> Unit,
    secondaryTitle: String? = null,
    secondaryContent: (@Composable ColumnScope.() -> Unit)? = null
)
```

---

## 2. Text Elements and Formatting Rules

### A. `flavorTitle` (Required)
- **Role**: Serves as the main console sub-header on the top-left of the view.
- **Formatting Guidelines**:
  - Encapsulated in square brackets: `[ TEXT HERE ]`.
  - Automatically converted to uppercase inside the template (`flavorTitle.uppercase()`).
  - Font styling uses standard system Sans-Serif (`FontFamily.Default`), `9.sp` font size, and `FontWeight.Bold` to denote administrative/console framework headings.
- **Examples**:
  - `"[ G.E.N. P.O.X. TIDE POOL REACTOR V2.4 ]"`
  - `"[ G.E.N. P.O.X. E-MERGE SEQUENCER V1.7 ]"`

### B. `statusText` (Optional)
- **Role**: Displays system telemetry status on the top-right of the view.
- **Formatting Guidelines**:
  - Usually 1-3 words representing active status.
  - Automatically converted to uppercase inside the template (`statusText.uppercase()`).
  - Rendered in a status-specific color (`statusColor`) at `9.sp` font size with bold weight and system Sans-Serif font.
  - Can optionally accept a click handler (`statusClickable`) to toggle systems.
- **Examples**:
  - `"SYSTEMS ON"` (Green/Active)
  - `"SYSTEMS OFF"` (Red/Disabled)
  - `"TEST MODE ACTIVE"` (Green/GreenDim)

### C. `primaryTitle` (Optional)
- **Role**: Bold heading text displayed inside the top of the **Primary Pane Card**.
- **Formatting Guidelines**:
  - Automatically capitalized inside the template (`primaryTitle.uppercase()`).
  - Rendered in solid white (`Color.White`), `bodyMedium` Typography style, with bold system Sans-Serif.
- **Examples**:
  - `"SINGLE-NODE CYBERNETIC SYNTHESIZER"`
  - `"GENETIC SPLICING MATRIX"`

### D. `secondaryTitle` (Optional)
- **Role**: Bold heading text displayed inside the top of the **Secondary Pane Card**.
- **Formatting Guidelines**:
  - Only rendered if `secondaryContent` is provided.
  - Automatically capitalized inside the template (`secondaryTitle.uppercase()`).
  - Rendered in solid white (`Color.White`), `bodyMedium` Typography style, with bold system Sans-Serif.
- **Examples**:
  - `"SPLICER STOCK GENE LEDGER"`
  - `"TARGET ALIGNMENT BUFFER"`

---

## 3. Typography Hierarchy in Content Block Slots

When writing custom views passed into `primaryContent` or `secondaryContent`, enforce these fonts from [fonts_standardization.md](file:///c:/Users/brent/Antigravity/GENPOX/fonts_standardization.md):

1. **Standard Interface Labels & Instructions**:
   - Use default sans-serif font family (`FontFamily.Default`) and uppercase styling.
   ```kotlin
   Text(
       text = "DESCRIPTION/INSTRUCTION TEXT GOES HERE",
       style = MaterialTheme.typography.bodySmall,
       fontFamily = FontFamily.Default
   )
   ```
2. **Genetic Data & Telemetry Stats**:
   - Use monospace (`FontFamily.Monospace`) for sequences, base numbers, counts, and raw buffers.
   ```kotlin
   Text(
       text = "GEN: ATCGGCTA...",
       style = MaterialTheme.typography.bodySmall,
       fontFamily = FontFamily.Monospace
   )
   ```
