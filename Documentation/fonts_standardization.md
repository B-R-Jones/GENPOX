# Font Standardization Guidelines

To preserve the cyber-retro/sci-fi aesthetic of GENPOX, we standardize on two primary typography patterns across both web and native platforms.

---

## 1. Typography Hierarchy & Roles

| Platform | Font Family | Role & Usage | Visual Category |
| :--- | :--- | :--- | :--- |
| **React Web** | `'Share Tech Mono'` | DNA strings, numeric stats, console logs, terminal output, telemetry screens. | **Monospace** |
| **React Web** | `'Space Grotesk'` | Navigation tabs, main buttons, headers/titles, dialog options, standard text. | **Sans-Serif** |
| **Android** | `FontFamily.Monospace` | DNA strings, numeric stats, console logs, terminal output, telemetry screens. | **Monospace** |
| **Android** | System Sans-Serif | Navigation tabs, main buttons, headers/titles, dialog options, standard text. | **Sans-Serif** |

---

## 2. Platform Implementation Details

### React Web (Tailwind CSS v4)
Defined in `@theme` in [index.css](file:///c:/Users/brent/Antigravity/GENPOX/src/index.css):
```css
@theme {
  --font-mono: 'Share Tech Mono', ui-monospace, monospace;
  --font-sans: 'Space Grotesk', system-ui, sans-serif;
}
```

#### Usage Examples:
1. **Sans-Serif (Standard Interface)**:
   ```tsx
   <button className="font-sans text-sm font-bold uppercase tracking-wider">
     Initialize Reactor
   </button>
   ```
2. **Monospace (Genetic/Telemetry Data)**:
   ```tsx
   <div className="font-mono text-xs text-[#00FF41]">
     GEN: ATCGGCTA...
   </div>
   ```

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
