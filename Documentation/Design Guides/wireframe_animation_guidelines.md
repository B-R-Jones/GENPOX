# Guidelines for Efficient Wireframe Animations in Compose Canvas

This companion document outlines the core optimization and design patterns established for rendering 2D/3D wireframe visuals in GENPOX console screens (e.g. `CreatureWireframeView`, `NodeCrystalCanvas`, `WireframeGlobe`).

---

## 1. Geometry Pre-computation
Do **not** parse raw text/DNA or recompute vertex positions from scratch on every rendering frame inside the draw loop (`DrawScope`).
*   **The Pattern**: Instantiate a static geometry builder (e.g., `CreatureGeometry`) inside a `remember(key)` block.
*   **Benefits**: Offloads string slicing, trigonometry constants calculation, and array structure initialization to recomposition boundaries, leaving the rendering loop cheap.

```kotlin
// Correct: Geometry instantiation remembered outside the draw loop
val geometry = remember(dna) { CreatureGeometry(dna) }

Canvas(modifier = modifier) {
    // Perform only projection and drawing here
}
```

---

## 2. Zero-Allocation Draw Loops
Avoid allocating any heap objects (like lists, vectors, or formatting strings) inside the `DrawScope` block. Drawing happens at 60+ FPS, and object allocation inside the frame loop causes frequent garbage collection pauses (micro-stuttering).
*   **The Pattern**: Pre-allocate lists/offsets, or reuse primitive arrays for projections.
*   **Avoid**: Calling list builders, string formats, or `Color` instantiations inside the `Canvas` drawing scope.

---

## 3. Depth Sorting & Projection Fade
To give 2D line drawings a 3D perspective effect:
1.  Apply Y-rotation (spin) and X-rotation (tilt) transformations to raw coordinates.
2.  Maintain the transformed Z-coordinate (depth) for each projected vertex.
3.  Calculate a depth percentage (`depthPct`) using the maximum model boundaries.
4.  Scale the line color's `alpha` and `strokeWidth` proportionally:
    *   **Foreground lines**: Higher alpha (up to 1.0f), thicker stroke.
    *   **Background lines**: Lower alpha (down to 0.18f), thinner stroke.

```kotlin
val depthPct = ((avgZ / maxRadius).coerceIn(-1.0, 1.0) + 1.0) / 2.0
val alpha = (0.18f + 0.82f * depthPct).toFloat()
val stroke = (0.8f + 1.2f * depthPct).toFloat() * density
```

---

## 4. Density-Aware Scaling
Canvas stroke widths, ticks, and coordinates are drawn in pixels, not device-independent pixels (dp).
*   Always extract Compose's `density` value (`densityVal = density` inside `Canvas` or `LocalDensity.current` outer context).
*   Multiply stroke widths and sizes by the density factor to ensure crisp lines on high-DPI screens.

---

## 5. Infinite Transitions
Use Compose `rememberInfiniteTransition` to animate rotations, breathing loops, and scan line sweeps.
*   Use `LinearEasing` for continuous rotations.
*   Specify distinct duration cycles (e.g., 18s for global spin, 4s for breathing, 1.5s for CRT static flickering) to create a complex, non-repetitive aesthetic.

---

## 6. Gyroscopic Multi-Axis Layering
To make wireframe structures look alive and premium:
*   Add a secondary internal element (e.g., core energy reactor) rotating around multiple axes (Y and Z) at a faster speed than the outer skeleton.
*   This layered movement gives depth and premium mechanical weight with extremely low CPU/GPU cost.

---

## 7. UI Gesture-Driven Layer Swap Animations
For interactive z-layered layouts (such as the overlapping holo-nav stack), combine pointer input detection with spring-based scale, opacity, and offset transitions to simulate tactile physical depth:
*   **Swipe-to-Cycle Detection**: Track vertical drag displacements using Compose `pointerInput(Unit) { detectDragGestures(...) }`. Toggling the layer state requires exceeding a threshold (e.g., `50` pixels) in either vertical direction (up or down).
*   **Tactile Spring Physics**: Animate depth shifts (Scale `1.0f` to `0.85f`, Alpha `1.0f` to `0.6f`, and vertical translation from `0.dp` to `-24.dp`) using a spring transition with `DampingRatioLowBouncy` and `StiffnessLow`.
*   **Idle Attractor Bounce**: To signal layered depth without user interaction, run a periodic `LaunchedEffect` that executes a subtle vertical bounce (e.g., `-8.dp` offset with a medium spring return) on the **back row only** every 6 seconds.

