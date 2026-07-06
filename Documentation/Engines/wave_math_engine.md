# Wave Math Engine Specification

The **Wave Math Engine** is a mathematical and celestial utility responsible for orbital mechanics, geocentric coordinate projections, daily wave spectrum configurations, and solar-lunar alignment checks.

- **Class Definition**: [WaveMath.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/WaveMath.kt)

---

## 1. Features and Functionalities

### A. Heliocentric Planetary Orbits
Models the $(x,y)$ coordinate position of solar system bodies relative to the Sun:
- Orbital periods and radii are scaled proportionally (e.g. Earth = 1.0 AU, period = 365 days; Venus = 0.72 AU, period = 224 days).
- Computes position vector over elapsed time epochs:
  $$\lambda_{\text{orbit}} = \frac{2\pi \cdot (\text{Time} \pmod{\text{Period}})}{\text{Period}}$$
  $$x = R \cdot \cos(\lambda_{\text{orbit}})$$
  $$y = R \cdot \sin(\lambda_{\text{orbit}})$$

### B. Geocentric Coordinate Mapping & Hour Angles (HA)
Translates heliocentric vectors into geocentric longitude angles ($\lambda_{\text{geo}}$) as viewed from Earth:
- Sun Proxy Geocentric Longitude:
  $$\lambda_{\text{sun}} = \operatorname{atan2}(-y_{\text{earth}}, -x_{\text{earth}})$$
- Planetary Geocentric Longitude:
  $$\lambda_{\text{planet}} = \operatorname{atan2}(y_{\text{planet}} - y_{\text{earth}}, x_{\text{planet}} - x_{\text{earth}})$$
- Diurnal Hour Angle (HA) for local observers:
  $$\text{HA} = \left(\phi_{\text{rotation}} + \lambda_{\text{observer\_lng}} + \lambda_{\text{sun}} - \lambda_{\text{planet}}\right) \pmod{2\pi}$$
  Where $\phi_{\text{rotation}}$ represents the Earth's diurnal rotation phase over a 24-hour cycle.

### C. Astronomical Declination Projections
Projects geocentric ecliptic longitudes onto local declinations:
$$\delta = \arcsin(0.3978 \cdot \sin(\lambda_{\text{geo}}))$$

### D. Daily Wave Spectrums
Models the daily waveform oscillations of nucleotide siphoning energy:
- Generates a wave structure with an amplitude, frequency, and phase offset derived from the current solar calendar epoch.
- Predicts favored bases and siphoning multiplier spikes based on daily wave configuration peaks.

### E. Planetary Alignment Classifications
Calculates the angular separation between two planets to determine the alignment state:
$$\theta_{\text{separation}} = \min(|HA_1 - HA_2|, 2\pi - |HA_1 - HA_2|)$$
- **CONJUNCTION**: $\theta_{\text{separation}} < 30^\circ$ (Q-score bonus, reduced mutation).
- **OPPOSITION**: $\theta_{\text{separation}} \ge 150^\circ$ (Q-score penalty, double mutation).
- **QUADRATURE**: $75^\circ \le \theta_{\text{separation}} \le 105^\circ$ ($T_m$ shift, temperature window bounds).
- **NEUTRAL**: Baseline state.

---

## 2. Integration and Usage

### Where it is used:
- [ScannerViewModel.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/ScannerViewModel.kt): To compute daily nucleotide siphoning yields, and geocentric coordinate vectors.
- [ScannerScreen.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/ui/main/ScannerScreen.kt): In the OrreryCelestial panel to draw planet coordinates, orbit lines, and horizon arcs.
- [GraphicsMath.kt](file:///c:/Users/brent/Antigravity/GENPOX/pox-android/app/src/main/java/com/example/genpox/data/GraphicsMath.kt): To calculate orbital angles and projections for visual overlays.
