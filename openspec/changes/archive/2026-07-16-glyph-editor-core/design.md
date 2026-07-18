## Context

The Nothing Phone (3a) Pro has 36 Glyph LED segments across 3 arc-shaped regions (A: 11 nodes vertical arc, B: 5 nodes diagonal arc, C: 20 nodes camera ring arc). The existing app controls these via preset toggles and animations. The Glyph SDK exposes binary ON/OFF per channel — no brightness, no fade, no continuous frame buffer.

The goal is to add an Interactive Glyph Editor that lets users visually paint and animate the Glyph nodes. This change covers the core architecture and GlyphPaint mode. PathBuilder and brush presets are handled in subsequent changes.

## Goals / Non-Goals

**Goals:**
- Render a pixel-accurate Glyph node map matching the physical phone layout (arc positions)
- Each of the 36 nodes is individually selectable and toggleable
- Drag painting across nodes with deduplication and haptic feedback
- Pinch-to-zoom and pan on the Glyph map
- Software preview renderer at 60 FPS (Compose Canvas)
- SDK bridge that converts node states to SDK commands at 30 FPS
- SDK-agnostic architecture — editor never imports Glyph SDK classes

**Non-Goals:**
- PathBuilder mode (separate change)
- Brush presets (separate change)
- Animation sequencing (separate change)
- Brightness/fade effects (SDK limitation — simulated in future changes)
- Per-LED color control (SDK limitation)

## Decisions

### Decision 1: Node Layout as Parametric Arc Positions

**Choice:** Define node positions using parametric equations for circular arcs, computed relative to a center point and radius. Each region (A, B, C) has its own center, radius, start angle, and sweep angle.

**Rationale:** The physical Glyph segments follow curved paths around the camera module. Straight-line positioning would look wrong. Parametric arcs produce accurate visual placement that matches the hardware.

**Alternatives considered:**
- Hardcoded pixel coordinates — rejected because they break on different screen sizes/densities
- SVG path file — rejected because it adds a rendering dependency and makes hit-testing harder
- Percentage-based layout — considered but arc math is cleaner and more precise

### Decision 2: GestureSampler as Dedicated Touch Processor

**Choice:** Extract all touch handling into a `GestureSampler` class that emits `GestureEvent` sealed classes. The editor ViewModel processes events, not raw touch coordinates.

**Rationale:** Separates gesture recognition from business logic. Makes it easy to add new gesture types (long-press, multi-finger) without changing the editor logic. Testable in isolation.

**Alternatives considered:**
- Compose `detectTapGestures` / `detectDragGestures` — considered but these are composable-level and don't give low-level pointer tracking needed for deduplication
- Raw `PointerInputScope` in the composable — works but mixes gesture logic with rendering

### Decision 3: Graph Model for Node Connectivity

**Choice:** Each region is modeled as a linear chain graph: `A1 ↔ A2 ↔ ... ↔ A11`. Nodes know their neighbors. Path traversal uses graph adjacency.

**Rationale:** The Path Builder (future change) needs to find nearest nodes and smooth paths. A graph model supports shortest-path, ripple propagation, and adjacency queries natively.

**Alternatives considered:**
- Flat list with index arithmetic — works for linear chains but doesn't generalize to cross-region paths
- Adjacency matrix — overkill for 36 nodes with simple linear connectivity

### Decision 4: AnimationModel as Shared Data Contract

**Choice:** An immutable `AnimationModel` data class holds `List<NodeState>` where each `NodeState` contains channel index, on/off state, and timestamp. Both `PreviewRenderer` and `GlyphSdkRenderer` consume the same model.

**Rationale:** Single source of truth. Preview shows exactly what the SDK will receive. Easy to serialize for save/load (future). Decouples rendering from SDK.

**Alternatives considered:**
- Shared mutable state — rejected because it creates race conditions between preview (60 FPS) and SDK (30 FPS)
- Event stream — considered but model is simpler and snapshot-friendly

### Decision 5: Glow Animation via Compose Animatable

**Choice:** Each node's glow is an `Animatable<Float>` driven by Compose's animation system. Toggle ON animates 0→1 in 150ms. Toggle OFF animates 1→0 in 100ms.

**Rationale:** Runs at Compose's native frame rate (60 FPS). No manual coroutine management. Smooth and battery-efficient.

**Alternatives considered:**
- Manual `LaunchedEffect` with `delay()` — more control but harder to coordinate
- `updateTransition` — considered but per-node `Animatable` is simpler for independent nodes

### Decision 6: SDK Bridge with Command Coalescing

**Choice:** `GlyphSdkRenderer` collects the current set of active channels, builds a `GlyphFrame`, and calls `toggle()`. If called faster than 33ms, it coalesces — only the latest state is sent.

**Rationale:** The SDK rate-limits to ~30 FPS. Sending commands faster causes dropped frames. Coalescing ensures the most recent state is always what gets sent.

**Alternatives considered:**
- Queue-based with fixed interval — more predictable but adds latency
- Fire-and-forget — simple but wastes SDK calls

## Risks / Trade-offs

- **[Risk] Arc positions don't match physical hardware** → Mitigation: Use the user-provided layout spec exactly. Test on device. Positions are configurable constants.
- **[Risk] Touch hit-testing accuracy on small nodes** → Mitigation: Use generous hit areas (1.5x node radius). Visual feedback confirms selection.
- **[Risk] SDK toggle coalescing drops intermediate states** → Mitigation: For GlyphPaint mode, intermediate states don't matter (user sees final state). For PathBuilder (future), coalescing is acceptable because animations are time-based.
- **[Trade-off] No true brightness/fade** → Accepted. The editor architecture is ready for brightness when the SDK adds it. Current implementation uses binary ON/OFF.
- **[Trade-off] Preview is more accurate than hardware** → Accepted. Preview shows ideal glow animations. Hardware shows binary toggle. This is honest and the architecture supports both.
