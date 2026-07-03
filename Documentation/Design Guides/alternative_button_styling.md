# Alternative Button & Border Design Standards (Network Tab Aesthetic)

This document describes the alternative "Network Tab" aesthetic for buttons, borders, and interactive containers, contrasted against the default "Cyberglass" style defined in [master_design_standards.md](file:///c:/Users/brent/Antigravity/GENPOX/Documentation/Design%20Guides/master_design_standards.md).

---

## 1. Aesthetic Comparison Overview

| Element | Master Cyberglass Design | Alternative Network Tab Design |
| :--- | :--- | :--- |
| **Borders** | Translucent thin borders (35% opacity) with double-layered corner ticks (8.dp lines). | Solid, opaque thin borders (100% opacity, 1.dp) with no corner ticks. |
| **Buttons** | Custom double-layered `Box` containers using the `.cyberglass()` modifier. | Native Material3 `Button` component or solid thin-bordered custom `Box` components. |
| **Fills/Backgrounds** | Semi-transparent dark panels (`Color.Black` or `CyberPanel` copy with alpha). | Solid flat fills (e.g. `CyberGreen`, `CyberCyan`, `Color.Red`, `Color(0xFFA855F7)`) or pure transparent/black. |
| **Corner Radius** | Sharper corners (often `2.dp` or custom corner ticks). | Standardized rounded corners (`RoundedCornerShape(4.dp)`). |
| **Text Contrast** | Neon-colored text matching the border color on a dark background. | Opaque black text (`Color.Black`) on solid color buttons, or neon/solid colors on transparent buttons. |

---

## 2. Alternative Button Implementation Guide

Instead of custom Box wrappers with complex drawscopes, standard interactive elements are constructed using Compose's native `Button` or standardized thin-bordered clickable `Box` elements.

### A. Solid Action Buttons
For main actions (e.g. Enable, dispatching missions, or starting activities):
* **Background**: Solid primary color (e.g., `CyberGreen`, `CyberCyan`, `Color.Red`, `Color(0xFFA855F7)`).
* **Text**: `Color.Black`, bold, sans-serif, uppercase.
* **Shape**: `RoundedCornerShape(4.dp)`.
* **Example Code**:
  ```kotlin
  Button(
      onClick = { /* Action */ },
      modifier = Modifier.height(32.dp),
      colors = ButtonDefaults.buttonColors(
          containerColor = CyberGreen,
          contentColor = Color.Black
      ),
      shape = RoundedCornerShape(4.dp),
      contentPadding = PaddingValues(horizontal = 8.dp)
  ) {
      Text(
          text = "PING GATEWAY",
          color = Color.Black,
          fontSize = 8.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Default
      )
  }
  ```

### B. Outline Utility Buttons
For secondary actions, reset toggles, or back buttons:
* **Background**: `Color.Transparent` or `Color.Black`.
* **Border**: `BorderStroke(1.dp, ActiveColor)` (e.g., `Color.Yellow`, `Color.Red`, `CyberBorder`).
* **Text**: Active color (e.g., `Color.Yellow`, `Color.Red`), bold, sans-serif, uppercase.
* **Shape**: `RoundedCornerShape(4.dp)`.
* **Example Code**:
  ```kotlin
  Button(
      onClick = { /* Action */ },
      modifier = Modifier.height(32.dp),
      colors = ButtonDefaults.buttonColors(
          containerColor = Color.Transparent,
          contentColor = Color.Yellow
      ),
      border = BorderStroke(1.dp, Color.Yellow),
      shape = RoundedCornerShape(4.dp)
  ) {
      Text(
          text = "RESET PORT",
          fontSize = 8.sp,
          fontWeight = FontWeight.Bold,
          fontFamily = FontFamily.Default
      )
  }
  ```

### C. State Toggle Buttons
For buttons with active/inactive binary states (e.g. Favorite, Defender, Auto-Hacker toggles):
* **Active State**: Solid filled background (e.g. `Color(0xFF3B82F6)` for Defender) with high-contrast text (`Color.Black`).
* **Inactive State**: `Color.Transparent` background, solid border (`BorderStroke(1.dp, CyberBorder)`), and muted text (`Color.Gray`).
* **Shape**: `RoundedCornerShape(4.dp)`.
* **Example Code**:
  ```kotlin
  Button(
      onClick = { /* Toggle */ },
      modifier = Modifier.height(38.dp),
      colors = ButtonDefaults.buttonColors(
          containerColor = if (isActive) ActiveColor else Color.Transparent,
          contentColor = if (isActive) Color.Black else Color.Gray
      ),
      border = if (isActive) null else BorderStroke(1.dp, CyberBorder),
      shape = RoundedCornerShape(4.dp)
  ) {
      Text(
          text = if (isActive) "ACTIVE" else "INACTIVE",
          fontSize = 8.5.sp,
          fontWeight = FontWeight.Bold
      )
  }
  ```

---

## 3. Alternative Border & Panel Guide

Interactive and static panels use a simpler, cleaner bounding box design:
* **Border Stroke**: Solid `1.dp` thickness (using `CyberBorder`, `CyberGreen`, etc.) with `RoundedCornerShape(4.dp)`. No opacity reduction (`copy(alpha = ...)`) is applied to the stroke color.
* **Corner Radius**: Enforced `RoundedCornerShape(4.dp)` instead of `RoundedCornerShape(2.dp)` to soften the UI grid.
* **Background Fills**: Clean `Color.Black` or `CyberPanel` backgrounds.
* **Example Code**:
  ```kotlin
  Box(
      modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, CyberBorder, RoundedCornerShape(4.dp))
          .background(Color.Black)
          .padding(8.dp)
  ) {
      // Panel Content
  }
  ```
