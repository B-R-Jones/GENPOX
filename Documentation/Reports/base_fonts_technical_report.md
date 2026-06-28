# G.E.N. Network: Base Fonts Technical Specification Report

This report documents the architectural, mathematical, and graphical parameters governing the **Base Fonts** system in the GENPOX network. Base Fonts serve as localized, temporary atmospheric bursts of genetic material (Adenine, Guanine, Thymine, and Cytosine) that players gather on the tactical radar using proximity-based siphoning.

---

## 1. System Overview & Lifecycle

Base Fonts represent highly unstable genetic nodes that decay rapidly in the atmosphere. They are defined by the `PoxBaseFont` data class in [MainViewModel.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/MainViewModel.kt#L4054-L4073).

### Key Parameters:
* **Identification (`id`)**: A unique string with the prefix `"FNT-"` followed by a random 6-character uppercase alphanumeric hash (e.g., `FNT-A3E5D1`).
* **Base Type (`baseType`)**: Character representing the genetic nucleotide (`'A'`, `'G'`, `'T'`, or `'C'`), chosen with equal probability from `listOf('A', 'G', 'T', 'C')`.
* **Spawn Time (`spawnTime`)**: The epoch timestamp (in milliseconds) at which the node was materialized.
* **Duration (`durationMs`)**: The total lifespan of the font. While defaulted to `90,000L` (90 seconds), the reactor heartbeat dynamically overrides this value for each instance to be between **60 and 120 seconds**:
  $$\text{Lifespan} \in [60000\text{ ms}, 120000\text{ ms}]$$
* **Siphoning Radius (`maxRadiusFeet`)**: The maximum range in feet within which siphoning can occur. Statically set to **$500.0$ feet**.

---

## 2. Spawning Mechanics & Math

Spawning is governed by the reactor's heartbeat loop inside `startReactorHeartbeat` in [MainViewModel.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/MainViewModel.kt#L3025-L3104), which ticks exactly once every **$1000$ milliseconds** (1 second).

> [!WARNING]
> **Deterministic Network Synchronization Note**: Base Fonts are currently generated client-side using local randomness and system clocks. They are not deterministic and are unique to each player. A future update will migrate them to server-driven deterministic seeds to synchronize font materials, lifetimes, and coordinates across all players in the sector.

### Spawn Trigger
* If the active font count in the player's sector falls below **3**, the system generates new random fonts in a loop until the total active count reaches **5**:
  $$\text{Font Density} \in [3, 5]$$

### Location Parameters
Fonts are placed at a random distance and angle relative to the player's current coordinate:
* **Distance ($d_{\text{feet}}$)**: Placed between $150$ and $800$ feet from the player:
  $$d_{\text{feet}} = 150.0 + \text{Random}() \times 650.0$$
* **Angle ($\theta$)**: Placed in a random direction:
  $$\theta = \text{Random}() \times 2\pi$$
* **Coordinate Conversion**: The distance in feet is mapped to latitude and longitude shifts using the following formulas:
  $$\Delta Lat = \frac{d_{\text{feet}} \cdot \cos\theta}{364000.0}$$
  $$\Delta Lng = \frac{d_{\text{feet}} \cdot \sin\theta}{364000.0 \cdot \cos(\operatorname{rad}(PlayerLat))}$$
  *(Enforces $1\text{ degree Latitude} \approx 364,000\text{ feet}$, with longitude scale squashed by the cosine of the latitude to account for earth curvature).*

---

## 3. Proximity Siphoning & Gathering Math

Every second during the reactor heartbeat, the distance ($d$) between the player's coordinate and all active fonts is calculated:
$$d = \operatorname{calculateDistanceInFeet}(PlayerLat, PlayerLng, FontLat, FontLng)$$

### Siphoning Rate
If the distance $d \le 500.0\text{ feet}$, the font is flagged as the player's `activeSiphonFont`, and siphoning rate (units gathered per second) is calculated using a steep fourth-power decay curve to flatten yield at range:
$$\text{Gather Rate} = \max\left(0, \operatorname{round}\left(40.0 \times \left(1.0 - \frac{d}{500.0}\right)^{4.0}\right)\right)$$

### Yield Profile:
* **Epicenter ($0\text{ feet}$)**: Peak gather rate of **$40$ units/sec**.
* **Close Proximity ($100\text{ feet}$, ratio = 0.8)**: Gather rate of $\approx \operatorname{round}(40 \times 0.8^4) = \mathbf{16\text{ units/sec}}$.
* **Mid-Range ($250\text{ feet}$, ratio = 0.5)**: Gather rate of $\approx \operatorname{round}(40 \times 0.5^4) = \mathbf{3\text{ units/sec}}$.
* **Distal Range ($350\text{ feet}$, ratio = 0.3)**: Gather rate of $\approx \operatorname{round}(40 \times 0.3^4) = \mathbf{0\text{ units/sec}}$.
* **Boundary Edge ($500\text{ feet}$)**: Gather rate of **$0$ units/sec**.

### Stock Accumulation & Persistence
* Siphoned units are added directly to the player's core local variables (`rawStockA`, `rawStockG`, `rawStockT`, `rawStockC`) once per second.
* **Persistent Save**: The loop immediately invokes `repository.saveRawStocks(...)` to write updated stocks directly back to the local DataStore file, ensuring collected assets persist across sessions and are updated in the Combinator/Reactor tabs.
* **Logging**: Every 3 seconds of active siphoning, a diagnostic line is printed to the terminal console:
  `[SIPHON] Siphoned +$gatherRate $baseName units from Font.`

---

## 4. Visual Rendering Specifications

The 3D vector graphics are rendered inside `HolographicRadarScanner` in [ScannerScreen.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/ScannerScreen.kt) using custom 3D-to-2D projection formulas.

### 3D Projection Formula
Given a local coordinate $(x, y, z)$ on the dome relative to its projected map center $(fx, fy)$:
1. **Map Rotation**: Rotate $(x, y)$ on the horizontal plane by the map's current `rotationAngle` ($\alpha$):
   $$x_{\text{world}} = x \cos\alpha - y \sin\alpha$$
   $$y_{\text{world}} = x \sin\alpha + y \cos\alpha$$
2. **Camera Tilt-Squash**: Apply `tiltYScale` (tilt compression factor $s_y$) and project altitude $z$ using `sinTilt` ($s_t = \sqrt{1 - s_y^2}$):
   $$\Delta x = x_{\text{world}}$$
   $$\Delta y = y_{\text{world}} \cdot s_y - z \cdot s_t$$
3. **Screen Mapping**: The final projected screen position is:
    $$(fx + \Delta x, fy + \Delta y)$$
4. **Viewport Persistence**: No screen-space culling is applied to the base font epicenters. This ensures that tall volumetric spikes and cascading particles remain visible in the viewport even when the font's ground coordinates shift off-screen, with clipping handled at the radar frame boundary.

### Volumetric Heatmap Dome Geometry
* **Outer Base Radius ($R_{\text{outer}}$)**: The projected boundary of $500\text{ feet}$ in screen pixels:
  $$R_{\text{outer}} = \frac{500.0}{364000.0} \cdot \text{scale} \cdot \text{animScale}$$
* **Rounded Peak Slope**: The radius at height fraction $u \in [0.0, 1.0]$ uses a rounded hemisphere-like profile to ensure the tops of the spikes are rounded rather than sharp:
  $$r(u) = R_{\text{outer}} \cdot \sqrt{1.0 - u^2}$$
* **Volumetric Trajectory Mesh**: Formulated in 3D by modulating the rounded base radius at azimuth $\theta$:
  $$r_{\text{distorted}}(\theta) = r(u) \times \left(1.0 + \epsilon \cdot \cos(k \cdot \theta + \phi)\right)$$
* **Pulsing 3D Wireframe Rings**: 4 stacked irregular latitude ovals pulse continuously outward and downward from the peak ($u=1$) to the base ($u=0$) at a high frequency ($1.67\text{ Hz}$ / $600\text{ ms}$ period):
  $$\text{u}_{\text{ring}}(t) = 1.0 - \left(\frac{\text{rIdx} + (t \bmod 600)/600}{4} \bmod 1.0\right)$$
  The opacity fades in near the peak and fades out near the base.
* **Overlapping Ring Merging**: If base fonts overlap, their pulsing rings are clipped recursively against one another using Difference Clip operations:
  $$\text{RingPath}_i = \text{RingPath}_i \setminus \bigcup_{j \neq i} \text{RingPath}_j$$
  This merges the wireframe rings seamlessly at their intersections.
* **Faint Static Ground Boundary**: A static outline at $u=0$ defines the outer limits of the siphoning field. Overlapping boundaries are similarly merged using recursive Difference Clip operations.

### 3D Volumetric Heatmap Glow
* **Perspective Integration**: Rather than a flat 2D ground circle, the glow is built as a 3D volume by stacking 8 filled ovals vertically.
* **Slope Matching**: The radius of each level matches the rounded slope profile $r(u_{\text{glow}})$, shifted by $-z \cdot s_t$ vertically to map to the 3D map perspective.
* **Thermal Alpha Gradient**: The opacity decays linearly with height ($1.0 - u_{\text{glow}}$) and is normalized over the layer count.
* **Overlapping Glow Merging**: For overlapping font groups, the stacked ovals at each height level are added together into a unified clipping path. The individual radial glows are drawn clipped to this combined path, eliminating double-opacity overlaps.

### Cascading Peak Particles
* **Emission Source**: Particles originate from the 3D dome's peak:
  $$\text{Peak}_{\text{screen}} = (fx, fy - H_{\text{peak}} \cdot s_t)$$
* **Randomized Linear Trajectories**: 16 particle streaks cascade down from the peak ($u=1$) to the base ($u=0$) along straight lines down the rounded slope of the curve. For each streak $s$, a starting progress offset $p_{\text{offset}}$ determines the phase. The launch azimuth angle $\theta$ is randomized for each individual firing cycle using the cycle index:
  $$\text{cycleIndex}(t) = \lfloor \frac{t + p_{\text{offset}} \cdot 1200}{1200} \rfloor$$
  $$\theta(t) = \text{Random}(\text{seed}_{\text{streak}} + \text{cycleIndex}(t)) \cdot 2\pi$$
  The azimuth angle $\theta$ remains constant throughout that cycle (which lasts exactly 1200 ms). The height fraction and radius track the rounded dome:
  $$u = 1.0 - \text{progress}(t), \quad z_c = u \cdot H_{\text{peak}}$$
  $$r_c = R_{\text{outer}} \cdot \sqrt{1.0 - u^2}, \quad r_{\text{distorted}} = r_c \cdot (1.0 + \epsilon \cdot \cos(k \cdot \theta(t) + \phi))$$
* **Velocity Streak Stretch**: Each particle is drawn as a line representing its velocity vector, stretched over $20\%$ of the path length (from progress $p - 0.20$ to $p$).
* **Thinned Stroke & Intensified Phosphor Jitter**: Particles are thinned out to a base stroke width of `1.0.dp` on screen. A high-amplitude, high-frequency independent noise factor ($\pm 30\%$ variation) is applied to the alpha and stroke width of each particle to simulate energetic phosphor discharge.

---

## 5. Materialization & Safety Specifications

To enhance visual immersion while ensuring full user safety, the spawn and decay phases of Base Fonts are controlled via non-linear scaling and low-contrast analog noise modulation.

### Grow & Shrink Cycle
* **Dynamic Scaling (`animScale`)**: Base Font elements scale dynamically over their lifespan relative to their spawn and remaining time:
  $$\text{ScaleFactor} = \min\left(1.0, \frac{\text{AgeMs}}{3000.0}, \frac{\text{RemainingMs}}{3000.0}\right)$$
* **Spawn (First 3s)**: The dome and particles scale from $0.0$ to $1.0$ linearly.
* **Decay (Last 3s)**: The dome and particles collapse from $1.0$ to $0.0$.

### Safe Phosphor Hum Animation
* **Spawning/Despawning Opacity Fade**: During the first $1.0$ second and final $1.0$ second of its lifespan, a smooth alpha fade-in/fade-out is applied:
  $$\text{FadeAlpha} = \begin{cases}
  \frac{\text{AgeMs}}{1000.0} & \text{if AgeMs} < 1000 \\
  \frac{\text{RemainingMs}}{1000.0} & \text{if RemainingMs} < 1000 \\
  1.0 & \text{otherwise}
  \end{cases}$$
* **Low-Contrast Analog Jitter**: To simulate realistic vintage vacuum-tube or cathode-ray tube (CRT) phosphor stabilization without strobe/seizure hazards, a high-frequency low-contrast pseudo-random noise is superimposed:
  $$\text{NoiseVal} \in [-0.075, 0.075] \text{ updated at 30ms intervals}$$
  $$\text{FlickerMultiplier} = \operatorname{coerceIn}(\text{FadeAlpha} + \text{NoiseVal}, 0.0, 1.0)$$
* **Contrast Limit**: The maximum change in opacity (contrast delta) is constrained to **$15\%$ peak-to-peak**, which is well below the General Flash Threshold defined by Web Content Accessibility Guidelines (WCAG 2.1 SC 2.3.1). This prevents photosensitivity/epileptic triggers while retaining a distinct analog signal hum.

---

## 6. Animation Evolution & Pipeline Iterations

Throughout development, the visual style, spatial positioning, and trajectory math of Base Font animations evolved through three distinct phases:

### Phase 1: Spinning 3D Spheres (Deprecated)
* **Model**: A wireframe sphere composed of concentric equators and vertical longitude ellipses.
* **Movement**: Autonomous continuous rotation over time around the Z-axis, driven by elapsed frame milliseconds:
  $$\text{spin} = (t \bmod 4000) \times \frac{2\pi}{4000}$$
* **Particles**: 8 particle streaks uniformly distributed on a unit sphere using the golden ratio spiral, shooting radially outward.
* **Feedback**: Deemed visually cluttered and too abstract. The autonomous spinning distracted from target tracking.

### Phase 2: Volumetric Heatmap Dome & Static Ribs (Deprecated Particle Seeding)
* **Model**: Irregular 3D dome contour shape distorted horizontally using a multi-lobe cosine formula to match anomaly heatmaps.
* **Movement**: Removed the autonomous spinning/rotation. The dome and its 8 vertical ribs/struts are statically aligned to the font's coordinate frame (rotating only in sync with the map).
* **Particles**: 16 streaks cascade from the dome's peak down the curved slopes of the ribs. Launches were staggered using a deterministic time offset generator.
* **Trajectory Math**: Particle directions ($\theta$) were selected using a sequential seed addition:
  $$\theta = \text{Random}(\text{pSeed} + \text{cycleIndex}) \cdot 2\pi$$
* **Issue**: Because the streak seeds ($\text{pSeed}$) only differed by minor integers, their seed sequences overlapped completely. This caused all streaks to fire along the exact same few paths, appearing as "fixed positions" rather than unique, randomized directions.

### Phase 3: Prime Hash Seed Decorrelation (Active / Current)
* **Model**: Maintains the volumetric dome contour, rounded peaks, and static ribs from Phase 2.
* **Movement**: Static coordinates in the base font frame, rotating solely with the map zoom and viewport perspective.
* **Particles**: 16 thinned-out high-speed particle lines ($1.0\text{ dp}$ width, $1200\text{ ms}$ lifecycle) cascading down the rounded slope.
* **Trajectory Math**: Employs prime-based hash mixing to completely decorrelate coordinates and angles across different streaks ($s$) and cycle counts ($\text{cycleIndex}$):
  $$\text{streakSeed} = \text{hashCode} \oplus (s \cdot 104729)$$
  $$\text{fireSeed} = \text{hashCode} \oplus (s \cdot 104729) \oplus (\text{cycleIndex} \cdot 7919)$$
  $$\theta = \text{Random}(\text{fireSeed}) \cdot 2\pi$$
* **Result**: Particles now fire in completely independent, randomized directions in every cycle without repeating the same sequence of angles, creating a highly organic, dynamic signal discharge.
