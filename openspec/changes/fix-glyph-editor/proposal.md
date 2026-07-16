## Why

The Glyph Editor has multiple critical bugs that make it unusable: nodes can't be selected (layout mismatch between gesture and rendering), touch gestures drag the entire UI instead of painting nodes (gesture modifier conflict), Clear/Send buttons fail with "session is not active" errors, and the editor lags heavily on startup. These must be fixed before the editor can be used.

## What Changes

- Fix layout mismatch: gesture hit-testing uses a different node layout than the renderer, so taps/drags never hit the right nodes
- Fix gesture conflict: `pointerInteropFilter` and `transformable` fight over touch events, causing the whole UI to pan when the user tries to paint
- Fix session lifecycle: auto-start session on editor open, gate SDK commands behind session state, prevent "session not active" errors
- Fix startup performance: eliminate duplicate layout creation, defer animation initialization
- Fix coordinate transformation: ensure touch-to-node mapping accounts for zoom/pan offset correctly

## Capabilities

### New Capabilities

_(none — this is a bug fix, not new features)_

### Modified Capabilities

- `glyph-editor-gestures`: Fix gesture handling to use unified pointer input (remove `transformable` conflict, fix coordinate mapping)
- `glyph-editor-preview`: Fix layout sharing so renderer and gesture sampler use the same node positions
- `glyph-editor-sdk-bridge`: Fix session lifecycle — auto-start session, gate commands, prevent errors

## Impact

- Files modified: `GlyphMapView.kt`, `GlyphEditorViewModel.kt`, `GlyphEditorScreen.kt`, `GestureSampler.kt`
- No API changes, no new dependencies
- No breaking changes to existing functionality
