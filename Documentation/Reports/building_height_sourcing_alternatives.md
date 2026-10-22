# Building Height Sourcing & 3D Extrusion Alternatives

This document outlines current and prospective methods for determining 3D building height data within the **GenPox Tactical Radar Visualizer**, comparing primary datasets, heuristic estimation algorithms, and future fine-tuning strategies.

---

## 1. Primary Data Sources & Attributes

| Source | Attribute Name | Unit | Coverage | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **FEMA USA Structures** | `HEIGHT` | Meters | Variable (~30–40% populated) | Explicit height derived from LiDAR point clouds. |
| **FEMA USA Structures** | `SQFEET` / `SQMETERS` | Sq Ft / Sq M | ~98% populated | Footprint area; used to estimate building volume. |
| **USGS National Map** | N/A (Points) | N/A | Landmark features | Public structures (schools, hospitals, fire stations). |
| **OpenStreetMap** | `height`, `building:levels` | Meters / Stories | ~15–20% populated | User-contributed tags when available. |

---

## 2. Implemented Height Determination Cascade

When rendering building structures, height values are resolved using the following order of precedence:

1. **Explicit Source Height (`HEIGHT`)**:
   - If the GIS feature explicitly provides a positive `HEIGHT` attribute, that value is used directly in meters.

2. **Footprint Area Heuristics (`SQFEET` / `SQMETERS`)**:
   - If `HEIGHT` is null or zero, building height is estimated based on footprint surface area:
     - **Large Structures** ($>4,500\text{ sq ft}$ / $>418\text{ m}^2$): **$12.0\text{ meters}$** (~3–4 stories).
     - **Medium Structures** ($2,500–4,500\text{ sq ft}$ / $232–418\text{ m}^2$): **$7.0\text{ meters}$** (~2 stories).
     - **Standard Residential** ($<2,500\text{ sq ft}$ / $<232\text{ m}^2$): **$4.5\text{ meters}$** (~1–1.5 stories).

3. **Deterministic Hash Fallback**:
   - For features lacking geometry attributes, a deterministic pseudo-random height between **$3.5\text{m}$ and $9.5\text{m}$** is assigned based on geographic coordinate hashing.

---

## 3. Alternative & Future Enhancement Strategies

If further visual fidelity or height precision is desired in future updates, the following alternative pipelines can be integrated:

### Option A: USGS 3DEP 1-Meter Digital Elevation Models (DEM/DSM)
- **Mechanism**: Query USGS 3D Elevation Program (3DEP) WCS/REST endpoints to fetch Digital Surface Models (DSM) minus Digital Terrain Models (DTM) at building bounding box centers.
- **Pros**: Sub-meter physical building height accuracy across the entire United States.
- **Cons**: Requires additional asynchronous raster tile queries per sector cell.

### Option B: Machine-Learning / Shadow Geometry Extraction
- **Mechanism**: Infer building story counts using building classification models (e.g. ORNL USA Structures ML classification) based on primary occupancy type (`PRIM_OCC`).
- **Pros**: Zero additional network overhead; leverages existing FEMA `PRIM_OCC` codes (e.g., Commercial, Industrial, Residential).

### Option C: Shader-Based Procedural Heights
- **Mechanism**: Apply procedural height noise maps based on urban density grids, extruding central business districts higher than suburban perimeters.
- **Pros**: Visually striking cyberpunk aesthetic ideal for tactical radar displays.
