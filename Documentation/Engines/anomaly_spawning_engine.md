# Anomaly Spawning Engine Specification

The **Anomaly Spawning Engine** is a deterministic geospatial synthesis utility responsible for evaluating real-world map layers (building footprints, building heights, and road network topology) to place anomalies, classify morphology (**Spike** vs. **Sprawl**), and assign physical scale (**Spectral Classes M through O**).

- **Class Definition**: [AnomalySpawningEngine.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/AnomalySpawningEngine.kt)

---

## 1. Algorithmic Principles & Data Pipeline

The engine operates on a multi-stage deterministic pipeline converting raw vector geometry into structured `PoxAnomaly` records:

```
[Road Polylines + Building Polygons]
                 |
                 v
   [Spatial Hash Grid (20m)] -------------> [Road Intersection Graph & Degree]
                 |
                 v
[Building Footprint Shoelace Area] -------> [Local Building Coverage Ratio (phi)]
                 |
                 v
   [Candidate Centroid Scorer] -----------> [Ranked Spatial Hotspots]
                 |
                 v
   [Poisson-Disk Distance Filter] --------> [Non-Overlapping Spawn Points]
                 |
                 v
   [Morphology & Spectral Classifier] ----> [PoxAnomaly Model Records]
```

---

## 2. Mathematical Models & Classification

### A. Road Network Intersection Extraction
Road ways from OpenStreetMap are indexed using a spatial grid hash at $20\text{m}$ resolution:
$$\text{cell}_x = \left\lfloor \frac{\text{lng} \cdot 111000 \cdot \cos(\text{lat})}{20.0} \right\rfloor, \quad \text{cell}_y = \left\lfloor \frac{\text{lat} \cdot 111000}{20.0} \right\rfloor$$
Any grid cell containing vertices from 2 or more distinct road ways forms an intersection node whose connectivity degree $k_{\text{deg}}$ corresponds to the number of intersecting segments.

### B. Building Coverage Ratio & Shoelace Area
For each building polygon with vertices $(x_i, y_i)$ in local equirectangular meters:
$$\text{Area}_{\text{bldg}} = \frac{1}{2} \left| \sum_{i=0}^{N-1} (x_i y_{i+1} - x_{i+1} y_i) \right|$$

Within a local neighborhood window of probe radius $R_{\text{probe}} = 250\text{m}$, the Building Coverage Ratio (packing fraction $\phi_{\text{bldg}}$) is:
$$\phi_{\text{bldg}} = \frac{\sum_{i \in \text{Local}} \text{Area}_{\text{bldg}, i}}{\pi \cdot R_{\text{probe}}^2}$$

### C. Morphology Decision Rule (Spike vs. Sprawl)
Urban centers with dense structural packing or significant verticality force high aspect-ratio **Spikes** (tapering needles towering above buildings). Expansive low-profile or suburban sectors form broad **Sprawls** (spreading ground lobes with lower ceilings):

$$\text{Score}_{\text{spike}} = 1.5 \cdot \phi_{\text{bldg}} + 0.5 \cdot \min\left(2.0, \frac{\bar{h}_{\text{bldg}}}{25.0}\right)$$

$$\text{Morphology} = \begin{cases}
\text{SPIKE} & \text{if } \text{Score}_{\text{spike}} \ge 0.40 \text{ or } \bar{h}_{\text{bldg}} \ge 30.0\text{m} \\
\text{SPRAWL} & \text{otherwise}
\end{cases}$$

### D. Scale & Spectral Class Hierarchy
Physical scale is determined by transportation network connectivity degree ($k_{\text{deg}}$) and spatial clearance to structural obstacles ($d_{\text{clearance}}$):

$$\text{Rank}_{\text{composite}} = 0.6 \cdot \text{Score}_{\text{connectivity}} + 0.4 \cdot \min\left(2.0, \frac{d_{\text{clearance}}}{500.0}\right)$$

| Spectral Class | Morphology | Nominal Radius ($R$) | Summit Ceiling ($H_{\max}$) | Infrastructure Context |
| :--- | :--- | :--- | :--- | :--- |
| **Class M** | Spike / Sprawl | $10\text{m}\dots 100\text{m}$ | $3\text{m}\dots 50\text{m}$ | Alleyways, service corridors, localized dead-ends |
| **Class K** | Spike / Sprawl | $15\text{m}\dots 300\text{m}$ | $10\text{m}\dots 75\text{m}$ | Residential blocks, minor collector intersections |
| **Class G** | Spike / Sprawl | $30\text{m}\dots 500\text{m}$ | $75\text{m}\dots 200\text{m}$ | Secondary urban intersections, neighborhood nodes |
| **Class F** | Spike / Sprawl | $50\text{m}\dots 1000\text{m}$ | $150\text{m}\dots 500\text{m}$ | Multi-block intersections, district avenues |
| **Class A** | Spike / Sprawl | $100\text{m}\dots 1500\text{m}$ | $300\text{m}\dots 1000\text{m}$ | Major arterial crossways, township basins |
| **Class B** | Spike / Sprawl | $200\text{m}\dots 2500\text{m}$ | $800\text{m}\dots 1500\text{m}$ | Multi-lane highway interchanges, metro core |
| **Class O** | Spike / Sprawl | $600\text{m}\dots 4000\text{m}$ | $1200\text{m}\dots 3000\text{m}$ | Regional transportation hubs, metropolitan biospheres |

### E. Poisson-Disk Spatial Separation
To prevent clumping, candidate anomalies are evaluated sequentially against all accepted anomalies. A candidate is accepted if and only if:
$$d(A_{\text{cand}}, A_{\text{existing}}) \ge 1.2 \cdot (R_{\text{cand}} + R_{\text{existing}})$$

---

## 3. Integration Points

* **[ScannerViewModel.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/ScannerViewModel.kt)**: Receives streaming vector roads and buildings from `MapSectorManager` and updates `_rawAnomalies` via `AnomalySpawningEngine.generateAnomaliesForSector()`.
* **[MetaballMeshCompiler.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/scanner/map/MetaballMeshCompiler.kt)**: Consumes the generated `PoxAnomaly` records to compile 3D volumetric isosurfaces and targeting nodes.
