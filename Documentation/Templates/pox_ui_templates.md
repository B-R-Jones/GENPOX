# GENPOX UI Templates Guide

This guide details the usage of reusable UI components for layout framing and sub-tab navigation in the GENPOX application. These templates enforce strict visual alignment, cyber themes, sound synchronization, and performance optimization (preventing redundant scroll nesting).

---

## 1. Tab Framing: `PoxTabFrame`

`PoxTabFrame` is the primary container for tabs in the application HUD. It handles standard header metadata, cyberglass wireframe framing, colors, status text actions, and sub-tab pagination.

### Import
```kotlin
import com.example.genpox.ui.components.PoxTabFrame
```

### Usage for a Single Default Tab
For simple tabs that do not require sub-tabs:
```kotlin
PoxTabFrame(
    flavorTitle = "G.E.N. P.O.X. CORE DATABASE V1.0",
    statusText = "ONLINE",
    statusColor = CyberGreen,
    headerTitle = "DATABASE ENTRIES",
    descriptionText = "Inspect containment and genetic database registries."
) {
    // Custom content layouts (e.g. lists, grids)
    Text("Content slots in here nicely...")
}
```

---

## 2. Sub-Tab Framing & Navigation

When a screen requires sub-tabs, `PoxTabFrame` works in tandem with `PoxHoloNav` to provide automatic floating navigation buttons in the bottom-right corner.

### Import
```kotlin
import com.example.genpox.ui.components.PoxSubTab
```

### Usage with Sub-Tabs
Configure `PoxSubTab` objects and pass them into the frame:
```kotlin
@Composable
fun MyCustomTabView(viewModel: MainViewModel) {
    var activeSubTab by remember { mutableStateOf("home") }

    val subTabs = listOf(
        PoxSubTab(
            id = "home",
            tag = "HOME",
            icon = { color -> IconHome(color) }
        ),
        PoxSubTab(
            id = "settings",
            tag = "SETTING",
            icon = { color -> IconSettings(color) }
        )
    )

    PoxTabFrame(
        flavorTitle = "SYSTEM SUB-CHANNELS",
        headerTitle = "CUSTOM CONTROL DECK",
        descriptionText = "Switch between controls using the holo-nav buttons.",
        subTabs = subTabs,
        activeSubTab = activeSubTab,
        onSubTabClick = { id, tag ->
            activeSubTab = id
            // Optionally log or perform actions on tab swap
        },
        viewModel = viewModel,
        isScrollable = false // Critical: Disable scroll to prevent nesting scroll conflicts!
    ) {
        when (activeSubTab) {
            "home" -> HomePanel()
            "settings" -> SettingsPanel()
        }
    }
}
```

> [!IMPORTANT]
> **Scroll Nesting Prevention**:
> If your screen or any sub-tab content has its own scrollable list (e.g., `LazyColumn`, `verticalScroll(rememberScrollState())`), you **MUST** pass `isScrollable = false` to `PoxTabFrame`. Nested scrollable containers in Compose are disallowed and can cause runtime crashes or severe performance degradation.

---

## 3. Sub-Component: `PoxHoloNav`

`PoxHoloNav` is the layout builder for the floating button deck. It is called internally by `PoxTabFrame` if sub-tabs are configured, but can also be used independently by custom screens.

### Design Standards:
- **Dimensions**: Buttons are strictly `46.dp` x `46.dp`.
- **Spacing**: Arranged horizontally with `spacedBy(8.dp)` and positioned at the bottom-right via `Alignment.BottomEnd` with `16.dp` padding.
- **Audio Feedback**: Automatically triggers the standard cybercombinator tick audio when clicked.
- **Visuals**: Displays active/inactive cyberglass border/glow styling based on the active tab state.
