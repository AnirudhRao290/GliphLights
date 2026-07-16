## Context

The Glyph Editor was implemented in the `glyph-editor-core` change but has multiple critical bugs at runtime. The editor uses a Compose Canvas to render 36 nodes across 3 arc regions, with gesture-based painting (tap/drag to toggle nodes), pinch-to-zoom, and SDK bridge for hardware output. The core issues are:

1. **Layout mismatch**: `GlyphMapView` creates a viewport-sized `GlyphNodeLayout` for rendering, but `GestureSampler` uses the ViewModel's hardcoded 400x600 layout. Hit-testing targets wrong positions.
2. **Gesture conflict**: `pointerInteropFilter` (returns true for all touch events) and `transformable` (Compose gesture modifier) fight over event consumption. `transformable` still receives events and modifies `graphicsLayer` translation.
3. **Session lifecycle**: `clearAll()` and `sendToDevice()` call SDK methods that require an active session, but no session is opened automatically. The user must manually tap "Start" first.
4. **Startup lag**: Layout created twice (ViewModel default + viewport), 36 `Animatable` instances initialized eagerly.

## Goals / Non-Goals

**Goals:**
- Nodes can be selected by tapping and painting with drag gestures
- Zoom/pan works without interfering with node painting
- Clear and Send work reliably without session errors
- Editor starts without noticeable lag
- Touch coordinates correctly map to node positions at any zoom level

**Non-Goals:**
- Redesigning the editor UI or adding new features
- Changing the SDK integration layer (GlyphManagerWrapper)
- Modifying the node layout algorithm or region positions
- Performance optimization beyond startup lag fix

## Decisions

### Decision 1: Unified gesture handling via `pointerInput` + `detectTapGestures`

**Choice**: Replace `pointerInteropFilter` + `transformable` with a single `pointerInput` modifier using `detectTapGestures` and custom `PointerInputScope` processing.

**Rationale**: `pointerInteropFilter` bypasses Compose's gesture system, causing conflicts with `transformable`. Using `pointerInput` keeps all gesture processing within Compose's unified pointer event system. Custom processing allows simultaneous tap/drag and zoom/pan without conflict.

**Alternative considered**: Keep `pointerInteropFilter` but disable `transformable` and implement zoom/pan manually. Rejected because `pointerInteropFilter` still bypasses Compose gestures, making future gesture additions harder.

### Decision 2: Layout created once in GlyphMapView, passed to ViewModel via callback

**Choice**: `GlyphMapView` creates the layout in `BoxWithConstraints` and calls `onLayoutCreated(layout)` to pass it to the ViewModel. ViewModel stores it and uses it for gesture processing.

**Rationale**: Eliminates the 400x600 default layout. Both rendering and gesture hit-testing use the same layout instance. The layout is created once when viewport dimensions are known.

**Alternative considered**: Pass viewport dimensions to ViewModel and create layout there. Rejected because it requires an extra recomposition cycle and the ViewModel shouldn't know about viewport dimensions.

### Decision 3: Auto-start session on editor open

**Choice**: `GlyphEditorScreen` calls `viewModel.startSession()` in a `LaunchedEffect(Unit)` on first composition. If session is already active (re-entering screen), skip.

**Rationale**: Eliminates the need for the user to manually tap "Start" before using the editor. The session is ready before any gestures occur.

**Alternative considered**: Gate all SDK commands behind session state check and show a "Start Session" button. Rejected because it adds friction — the editor should just work.

### Decision 4: Lazy animation initialization

**Choice**: Create `Animatable` instances lazily in `PreviewRenderer` using `remember` with `nodes` as key, rather than creating all 36 upfront.

**Rationale**: Reduces startup cost. Animatables are created once and reused across recompositions.

## Risks / Trade-offs

- **[Risk] Auto-start session may fail on non-Nothing devices** → Mitigation: Check device support before auto-starting, show error state if init fails
- **[Risk] Unified pointerInput may have subtle gesture conflicts** → Mitigation: Test tap vs drag vs zoom/pan thoroughly, use gesture cancellation logic
- **[Trade-off] Auto-start adds SDK init time to editor open** → Acceptable: Init is fast (~100ms) and happens in background
