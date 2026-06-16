# P.O.X. Genome Sequence Tags

This file tracks all available creature sequence tags, their definitions, logical triggers, and behavior within the GENPOX emulator platforms (Web App and Native Android App).

---

## Tag Registry & Definitions

### 1. `FAVORITE`
* **Definition**: Indicates the creature has been marked/starred for quick access or protection.
* **Trigger Logic**: Evaluated dynamically as `true` if `item.isFavorite == true`.
* **Platform Notes**: Present in both Web and Android apps.

### 2. `DEFENDER`
* **Definition**: Designated as the active defender node in network battles.
* **Trigger Logic**: Evaluated dynamically as `true` if the creature's ID matches the global `defenderCreatureId`.
* **Platform Notes**: Displayed as a blue shield badge on both Web and Android platforms.

### 3. `AUTO-HACKER`
* **Definition**: Designated to perform automated network breaches or background hacks.
* **Trigger Logic**: Evaluated dynamically as `true` if `item.isAutoHacker == true`.
* **Platform Notes**: Present in both Web and Android apps.

### 4. `FULL COHERENCE`
* **Definition**: The creature's sequence matches the G.E.N. Network target sequence at construction time.
* **Trigger Logic**: Evaluated exactly once at creation time. Checks if the first 64 characters of the creature sequence match the active target sequence. Set to `true` permanently if they match.
* **Platform Notes**: Displayed as a pulsing green DNA helix badge on both Web and Android.

### 5. `ALPHA GENE`
* **Definition**: High-tier specimen possessing at least one dominant biological attribute.
* **Trigger Logic**: Evaluated dynamically as `true` if any base stat (`vitality`, `attack`, `defense`, or `speed`) is greater than or equal to `75`.
* **Platform Notes**: Present in both Web and Android apps.

### 6. `MODIFIED`
* **Definition**: Genetically extended sequence.
* **Trigger Logic**: Evaluated dynamically as `true` if `item.appendedGenes` is not empty (sequence length exceeds 64 characters).
* **Platform Notes**: Present in both Web and Android apps.

### 7. `ORIGINAL`
* **Definition**: Locally synthesized specimen.
* **Trigger Logic**: Evaluated dynamically as `true` if the creature's origin metadata is `"Created"`.
* **Platform Notes**: Present in both Web and Android apps.

### 8. `TRANSFER-ORIGIN`
* **Definition**: A specimen acquired from another player/emulator.
* **Trigger Logic**: Evaluated dynamically as `true` if the creature's origin metadata starts with `"Traded"`.
* **Platform Notes**: Present in both Web and Android apps.

### 9. `NATURAL`
* **Definition**: A specimen with Full Coherence that was synthesized naturally (splicer/combinator).
* **Trigger Logic**: Evaluated exactly once at creation time if Full Coherence is achieved and forced construction was not used.
* **Platform Notes**: Displayed as a mint leaf badge.

### 10. `FORCED`
* **Definition**: A specimen with Full Coherence that was compiled via forced reactor cycles.
* **Trigger Logic**: Evaluated exactly once at creation time if Full Coherence is achieved and forced construction was used.
* **Platform Notes**: Displayed as an orange hammer badge.

---

## Wireframe Badge Design Specifications

To deliver a premium, wordless cyber-registry aesthetic, all sequence tags are rendered as wordless visual badges mapping to clean vector wireframes:

| Tag ID | Wireframe Shape / Icon | Color Name | Color Hex | Platform Asset / Component |
| :--- | :--- | :--- | :--- | :--- |
| **FAVORITE** | Five-pointed Star (Filled) | Gold / Amber | `#FFB300` | Web: Lucide `<Star className="fill-yellow-400" />` <br> Android: `WireframeStar(filled=true)` |
| **DEFENDER** | Heraldic Shield | Sky Blue | `#60A5FA` | Web: Lucide `<Shield />` <br> Android: `WireframeShield()` |
| **AUTO-HACKER** | Microchip / Gear | Amber | `#FBBF24` | Web: Lucide `<Cpu />` <br> Android: `WireframeGear()` |
| **FULL COHERENCE** | Double Helix / DNA Wave | Cyber Green | `#00FF41` | Web: Lucide `<Dna className="animate-pulse" />` <br> Android: `WireframeDna()` (Pulsing) |
| **ALPHA GENE** | Lightning Bolt | Gold | `#FFB300` | Web: Lucide `<Zap />` <br> Android: `WireframeLightning()` |
| **MODIFIED** | Four-pointed Sparkle | Purple | `#C084FC` | Web: Lucide `<Sparkles />` <br> Android: `WireframeSparkle()` |
| **ORIGINAL** | Concentric Target Circles | Emerald Green | `#00FF41` | Web: Lucide `<Compass />` <br> Android: `WireframeOriginal()` |
| **TRANSFER-ORIGIN** | Double Opposing Arrows | Red | `#EF4444` | Web: Lucide `<RefreshCw />` <br> Android: `WireframeTransfer()` |
| **NATURAL** | Organic Leaf | Mint Green | `#10B981` | Web: Lucide `<Leaf />` <br> Android: `WireframeNatural()` |
| **FORCED** | Anvil / Hammer | Orange | `#F59E0B` | Web: Lucide `<Hammer />` <br> Android: `WireframeForced()` |
