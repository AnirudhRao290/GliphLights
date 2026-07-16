## 1. Node Layout Model

- [x] 1.1 Create `GlyphNode` data class with id, region, sdkIndex, position, state fields
- [x] 1.2 Create `GlyphRegion` enum (A, B, C) with channel ranges and arc parameters (center, radius, startAngle, sweepAngle)
- [x] 1.3 Create `GlyphNodeLayout` class that computes 36 node positions using parametric arc equations
- [x] 1.4 Implement responsive layout that scales positions relative to viewport size
- [x] 1.5 Create adjacency graph — each node knows its neighbors within its region

## 2. AnimationModel

- [x] 2.1 Create `NodeState` data class (channelIndex, isOn, timestamp)
- [x] 2.2 Create `AnimationModel` immutable data class containing `List<NodeState>`
- [x] 2.3 Add factory methods to create model from current node states

## 3. GestureSampler

- [x] 3.1 Create `GestureEvent` sealed class (Tap, DragStart, DragEnter, DragEnd, Pan, Zoom)
- [x] 3.2 Create `GestureSampler` class that processes raw pointer events into GestureEvents
- [x] 3.3 Implement tap detection with hit-testing against node positions
- [x] 3.4 Implement drag painting with per-pointer last-hit tracking for deduplication
- [x] 3.5 Implement haptic feedback trigger on first entry to each new node during drag
- [x] 3.6 Implement pinch-to-zoom gesture detection
- [x] 3.7 Implement pan gesture detection (two-finger drag)

## 4. Preview Renderer

- [x] 4.1 Create `PreviewRenderer` composable that renders all 36 nodes on a Canvas
- [x] 4.2 Render nodes at arc-computed positions with region color coding
- [x] 4.3 Add per-node `Animatable<Float>` for glow intensity (150ms on, 100ms off)
- [x] 4.4 Render node visual states: inactive (dim), active (glowing), hit-target (highlighted)
- [x] 4.5 Wire glow animations to AnimationModel state changes

## 5. GlyphMapView

- [x] 5.1 Create `GlyphMapView` composable combining Canvas, GestureSampler, and PreviewRenderer
- [x] 5.2 Apply `TransformableState` for pinch-to-zoom and pan
- [x] 5.3 Connect gesture events to node state updates
- [x] 5.4 Ensure 60 FPS rendering (no blocking operations on composition thread)

## 6. SDK Bridge

- [x] 6.1 Create `GlyphSdkRenderer` class that converts AnimationModel to SDK commands
- [x] 6.2 Implement `buildFrame()` — builds GlyphFrame from active channels
- [x] 6.3 Implement rate limiting with 33ms minimum interval and command coalescing
- [x] 6.4 Implement turnOff handling when no channels are active

## 7. Repository Integration

- [x] 7.1 Add `turnOffChannels(channels)` method to `GlyphRepository` interface
- [x] 7.2 Implement `turnOffChannels()` in `GlyphRepositoryImpl`
- [x] 7.3 Implement `turnOffChannels()` in `GlyphManagerWrapper`

## 8. ViewModel

- [x] 8.1 Create `GlyphEditorViewModel` with Hilt injection
- [x] 8.2 Hold GlyphNodeLayout, AnimationModel, and node states as StateFlow
- [x] 8.3 Process GestureEvents — toggle nodes, update AnimationModel
- [x] 8.4 Collect AnimationModel changes and forward to GlyphSdkRenderer
- [x] 8.5 Implement start/stop — start opens SDK session and begins rendering, stop turns off and closes

## 9. Editor Screen

- [x] 9.1 Create `GlyphEditorScreen` composable with Material 3 styling
- [x] 9.2 Embed GlyphMapView with full-screen Canvas
- [x] 9.3 Add top bar with Back button and title
- [x] 9.4 Add bottom controls (Clear All, Send to Device buttons)
- [x] 9.5 Show active node count indicator

## 10. Navigation & Integration

- [x] 10.1 Add `Editor` route to Screen.kt
- [x] 10.2 Add Editor entry to bottom navigation or dashboard
- [x] 10.3 Add composable route to NavHost in MainActivity
- [x] 10.4 Add Hilt bindings if needed for new dependencies

## 11. Spec Sync

- [x] 11.1 Sync delta specs to main specs
