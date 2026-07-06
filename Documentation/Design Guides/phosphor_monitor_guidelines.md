# GENPOX CRT Phosphor Monitor Shading Guidelines

This guide details the technical implementation and visual specifications for the cathode-ray tube (CRT) phosphor monitor emulation in the GENPOX HUD. It acts as a companion to the core wireframe rendering documentation.

---

## 1. Visual Aesthetics & Physical Theory

To deliver a premium, authentic retro sci-fi console aesthetic, the GENPOX interface simulates the physical properties of a vintage monochromatic cathode-ray tube display:

```
+---------------------------------------------+
|  [ Glass Curved Bezel Frame Boundary ]     |
|   +-------------------------------------+   |
|   |  \   Subtle Glass Glare Sheen   /   |   |
|   |   \____________________________/    |   |
|   |  [ Scanlines (Horizontal Grate) ]   |   |
|   |  =================================  |   |
|   |  [ Aperture Grille (Columns) ]      |   |
|   |  |   |   |   |   |   |   |   |   |  |   |
|   |  [ Color Matrix & Bleeding Bloom ]  |   |
|   |  (Excited phosphor glow halo)       |   |
|   |                                     |   |
|   +-------------------------------------+   |
|  [ Dark Edge Vignette Corner Bulge ]        |
+---------------------------------------------+
```

### Core CRT Effects Simulated
1. **Luminance color-matrix mapping**: Incoming RGB images are mapped to grayscale based on Rec. 709 luminance weights, then scaled and projected onto the active theme's monochromatic tint.
2. **Scanline Rasterization**: Emulation of the physical electron beam sweeps. Done using vertical repeating linear gradients with customizable density and opacity.
3. **Aperture Grille Mask**: Fine vertical grid-like columns representing phosphor stripe rows. Drawn using horizontal repeating linear gradients.
4. **Phosphor Glow / Bloom**: High-intensity phosphors excite the surrounding glass, creating light bleeding. Simulated via additive dual-layer drawing.
5. **Glass Curvature Bulge**: A radial darkening vignette is applied from the center outward to create the illusion of a bulbous, curved 3D glass tube face.
6. **Hum, Noise & Sync-Tear**: Subtle sync-slips and dynamic static particle ticks are animated based on continuous time to simulate analog signal decay.

---

## 2. Dynamic Color Coordination

The CRT tint maps directly onto the active GENPOX theme palettes. The math converts the input color's grayscale luminance $Y$ and scales it onto the target tint's channels:

$$Y = 0.2126 \cdot R + 0.7152 \cdot G + 0.0722 \cdot B$$

$$R_{out} = Y \cdot R_{theme}$$
$$G_{out} = Y \cdot G_{theme}$$
$$B_{out} = Y \cdot B_{theme}$$

### Supported Base Palette Coordinate Mapping:
- **CYBER-GREEN**: `#00FF66` (Classic green phosphor terminal)
- **CYBER-PURPLE**: `#A855F7` (Ultraviolet chemical assay)
- **CYBER-CYAN**: `#00E5FF` (Holographic projection feed)
- **CYBER-RED**: `#EF4444` (Emergency structural warning overlay)
- **ACTIVE REAGENT**: Shifting color derived dynamically from the slotted splicing catalyst (e.g. `REAGENT_BETAINE` orange).

---

## 3. Sobel Filter Vector Mode

To represent a vector-drawn CRT display (oscilloscope or wireframe vector arcade display), static image feeds can be run through a lightweight **Sobel edge detection filter**:

### Sobel Operators (3x3 kernels)

$$G_x = \begin{bmatrix} -1 & 0 & 1 \\ -2 & 0 & 2 \\ -1 & 0 & 1 \end{bmatrix} * A, \quad G_y = \begin{bmatrix} -1 & -2 & -1 \\ 0 & 0 & 0 \\ 1 & 2 & 1 \end{bmatrix} * A$$

$$G = \sqrt{G_x^2 + G_y^2}$$

Pixels where $G > \text{threshold}$ are rendered at full white-excitation level (which then blooms to the primary green/amber tint), while others remain dark, producing sharp, high-contrast cathode-line vector reconstructions.

---

## 4. Kotlin Jetpack Compose Usage API

The CRT emulation is packaged as a reusable Composable layout modifier and auxiliary functions:

### Modifier.phosphorMonitor(...)
```kotlin
fun Modifier.phosphorMonitor(
    baseColor: Color = CyberGreen,
    scanlineOpacity: Float = 0.25f,
    grilleOpacity: Float = 0.15f,
    bloomIntensity: Float = 0.3f,
    curvatureBezel: Float = 0.05f,
    jitterSpeed: Float = 0.5f,
    glitchIntensity: Float = 0.1f,
    timeMs: Long = 0L
): Modifier
```

### Code Example: Transforming standard Compose UI elements
```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .phosphorMonitor(
            baseColor = CyberTheme.purple,
            scanlineOpacity = 0.3f,
            grilleOpacity = 0.15f
        )
) {
    // Any standard composables here will look like they are rendered on a purple CRT terminal screen!
    Text("SYSTEM CALIBRATION OK", color = CyberTheme.purple)
}
```
