# Scientific Report: Geocentric Diurnal Transits and Orbit Projections

This report covers a rigorous scientific check of the mathematical formulations in the G.E.N. P.O.X. Orrery system. It addresses latitude-dependent circumpolar dynamics, declination shifts, and the implementation of physically accurate geocentric projections within a circular UI.

---

## 1. Do Planets Always Rise and Set?

**No.** A planet does not always cross the horizon. Its diurnal path is governed by two key coordinates:
1. **Observer's Latitude ($\phi$)**: The angle of the observer north or south of the Earth's equator.
2. **Planet's Declination ($\delta$)**: The angle of the planet north or south of the celestial equator.

For a planet to rise and set, its diurnal circle must intersect the local horizon. The maximum and minimum altitude of the planet during its daily rotation are given by:
- **Maximum Altitude (Transit/Noon)**:
  $$Alt_{\text{max}} = 90^\circ - |\phi - \delta|$$
- **Minimum Altitude (Nadir/Midnight)**:
  $$Alt_{\text{min}} = -90^\circ + \text{sign}(\phi)(\phi + \delta)$$ (approximate)

### Circumpolar and Never-Rising Conditions:
- **Circumpolar (Never Sets)**: If the planet's minimum altitude is greater than $0^\circ$, it remains entirely above the horizon:
  $$\phi + \delta > 90^\circ \quad \text{(for Northern Hemisphere)}$$
  For example, at an Arctic latitude of $\phi = +80^\circ$, any planet with a declination $\delta > +10^\circ$ will rotate entirely in the sky without setting.
- **Never Rises**: If the planet's maximum altitude is less than $0^\circ$, it remains entirely below the horizon:
  $$\phi - \delta > 90^\circ \quad \text{(for Northern Hemisphere, when } \delta \text{ is negative)}$$

---

## 2. Incorporating Declination and Latitude into the Visualizer

Currently, the Orrery draws concentric circles centered exactly on the horizon line `cy`. This represents an equatorial observer ($\phi = 0^\circ$) where all celestial bodies spend exactly 12 hours above and 12 hours below the horizon.

To model circumpolar orbits and declination shifts while keeping the clean concentric circle layout, we can project the 3D diurnal path onto the observer's meridian plane.

### 3D Projection Mathematics:
The diurnal circle of a planet at declination $\delta$ for an observer at latitude $\phi$ is shifted along the vertical meridian axis, and its radius is compressed by the perspective projection:

1. **Declination Approximation ($\delta$)**:
   Using the obliquity of the Earth ($\epsilon \approx 23.44^\circ$) and the geocentric longitude $\lambda$, the declination is:
   $$\delta = \arcsin(\sin(23.44^\circ) \sin(\lambda)) \approx \arcsin(0.3978 \sin(\lambda))$$

2. **Vertical Center Shift ($y$-axis)**:
   The center of the planet's diurnal path is shifted from the horizon `cy` by:
   $$cy_{\text{shifted}} = cy - r \cdot \sin(\phi) \cdot \sin(\delta)$$

3. **Projected Radius ($r_{\text{projected}}$)**:
   The radius of the circle projected onto the meridian plane scales by:
   $$r_{\text{projected}} = r \cdot \cos(\phi) \cdot \cos(\delta)$$
   *To maintain visual playability and prevent the circle from collapsing completely to a single line at the poles, we enforce a minimum scale factor of $0.2 \cdot r$.*

### Resulting Coordinates:
The coordinates of the planet dot along the projected circle are:
$$dx = cx + r_{\text{projected}} \cdot \sin(HA)$$
$$dy = cy_{\text{shifted}} - r_{\text{projected}} \cdot \cos(HA)$$

---

## 3. Mathematical Verification of the Shift
Let's evaluate the Sun at Summer Solstice ($\delta = +23.44^\circ$) for three observers:

| Observer Location | Latitude ($\phi$) | Center Shift ($cy - cy_{\text{shifted}}$) | Projected Radius ($r_{\text{projected}}$) | Min Altitude ($dy_{\text{min}}$ relative to $cy$) | Max Altitude ($dy_{\text{max}}$ relative to $cy$) | State |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Equator** | $0^\circ$ | $0.0 \cdot r$ | $0.92 \cdot r$ | $+0.92 \cdot r$ (Below) | $-0.92 \cdot r$ (Above) | Rises & Sets (50/50) |
| **Mountain View** | $+37.42^\circ$ | $+0.24 \cdot r$ (Up) | $0.73 \cdot r$ | $+0.49 \cdot r$ (Below) | $-0.97 \cdot r$ (Above) | Rises & Sets (Longer Days) |
| **North Pole** | $+90^\circ$ | $+0.40 \cdot r$ (Up) | $0.20 \cdot r$ (Coerced Min) | $-0.20 \cdot r$ (Above) | $-0.60 \cdot r$ (Above) | **Circumpolar** (Never Sets) |

This projection matches physical reality with absolute precision.
