# Deployment Engine Specification

The **Deployment Engine** is a geodetic and celestial calculation utility responsible for spatial distance calculations, anomaly boundary modeling, synodic lunar resonance, and mission phase timings.

- **Class Definition**: [DeploymentEngine.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/DeploymentEngine.kt)

---

## 1. Features and Functionalities

### A. Geodetic Distance (Haversine Formula)
Calculates the spatial distance in feet between two geographic coordinate points (latitude/longitude):
$$d = 2 \cdot R_{\text{earth}} \cdot \arcsin\left(\sqrt{\sin^2\left(\frac{\Delta\text{lat}}{2}\right) + \cos(\text{lat}_1)\cos(\text{lat}_2)\sin^2\left(\frac{\Delta\text{lng}}{2}\right)}\right)$$
- **Earth Radius Constant**: $20,902,231.0$ feet.

### B. Lobed Non-Circular Anomaly Boundaries
Models the boundary radius $R(\theta)$ of an anomaly as a non-circular lobed shape to simulate variable containment forcefield geometries:
$$R(\theta) = R_0 \cdot (1.0 + \epsilon \cdot \cos(k \cdot \theta + \phi))$$
- $R_0$: Baseline radius ($0.5 \times \text{Diameter}$).
- $\epsilon$: Lobe amplitude fluctuation factor ($0.15, 0.20, \text{or } 0.25$).
- $k$: Lobe count ($3, 4, \text{or } 5$ lobes).
- $\phi$: Orientation offset phase.

### C. Synodic Lunar Resonance Modifiers
Determines base defense boosts or penalties based on the creature's faction alignment and the current synodic lunar phase:
- **Infection**: $+15$ during Waxing Crescent/Gibbous; $-10$ during Waning Crescent/Gibbous.
- **Mech**: $+15$ during First/Third Quarter; $-10$ during New/Full Moon.
- **Parasite**: $+15$ during Full Moon; $-10$ during New Moon.
- **Containment**: $+15$ during New Moon; $-10$ during Full Moon.

### D. Forcefield Stall Depth ($S$)
Calculates the penetration percentage reached within the anomaly's forcefield buffer:
$$S = \min\left(100.0, \max\left(0.0, \frac{\text{Defense}_{\text{eff}}}{R_{\text{anom}}} \cdot 100.0\right)\right)$$
- $R_{\text{anom}}$ scales dynamically with the current lunar age age-phase mod.

### E. Combined Wave Density
Calculates the aggregate wave interference density at any landing coordinate from all overlapping nearby anomalies:
$$\text{Density}_{\text{combined}} = \sum_{i} \text{Density}_{i} \cdot \cos(\omega \cdot d_i + \phi_i) \cdot e^{-\alpha \cdot d_i}$$
- Incorporates spatial frequency ($\omega = 0.02$) and spatial attenuation/damping ($\alpha = 0.002$).

### F. Transit Phase Timings
Computes the velocity and durations (in seconds) for each sequential stage of a deployment mission:
- **Travel Phase**: Time to transit from base to the anomaly boundary.
- **Descent Phase**: Time to descend through the lobed forcefield.
- **Harvest Phase**: Duration spent gathering biological material.
- **Ascent & Return Phases**: Timing required to return cargo to baseline orbit.

---

## 2. Integration and Usage

### Where it is used:
- [ScannerViewModel.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/ScannerViewModel.kt): To calculate geodetic distances, check boundary entry, calculate mission transit durations, and compute combined wave densities at active GPS coordinates.
- [ConflictEngine.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/ConflictEngine.kt): To resolve the effective defense attributes of creatures during combat based on active daily lunar configurations.
