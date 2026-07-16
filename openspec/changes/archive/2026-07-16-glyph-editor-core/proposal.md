## Why

The Glyph Control app currently provides only preset animations and manual zone toggles. Users want creative control over the Glyph Interface — the ability to paint individual nodes and draw custom light paths. An Interactive Glyph Editor transforms the phone into a creative canvas where users design their own Glyph patterns, unlocking the full potential of the 36-node LED array.

## What Changes

- Add an Interactive Glyph Editor screen with two editing modes: GlyphPaint and PathBuilder
- Add an SVG/vector representation of the Nothing Phone (3a) Pro Glyph layout with 36 individually addressable nodes across 3 arc regions (A: 11 nodes, B: 5 nodes, C: 20 nodes)
- Add GlyphPaint mode: tap to toggle nodes, drag to paint across nodes, haptic feedback, pinch-to-zoom, pan
- Add gesture deduplication to prevent re-toggling nodes during drag
- Add a graph model representing node connectivity within each region
- Add a software preview renderer that displays animation state on the Glyph map at 60 FPS
- Add a GlyphSdkRenderer that converts animation models to SDK toggle/turnOff commands (rate-limited to 30 FPS)
- Add an AnimationModel data layer shared between preview and SDK renderers
- Add an editor entry point to the main navigation

**SDK Limitations (adapted, not modified):**
- The Glyph SDK only supports binary ON/OFF per channel — no brightness, no fade
- Fade effects are simulated via rapid channel toggling (timing-based ON/OFF sequences)
- The editor architecture is SDK-agnostic; the GlyphSdkRenderer is the only SDK touchpoint

## Capabilities

### New Capabilities
- `glyph-node-layout`: Node layout model for the Nothing Phone (3a) Pro — 36 nodes across 3 arc regions with positions, connectivity graph, and SDK channel mapping
- `glyph-editor-gestures`: Gesture handling for the editor — tap, drag paint, deduplication, haptic feedback, pinch-to-zoom, pan
- `glyph-editor-preview`: Software preview renderer — renders node state and glow animations on the Glyph map at 60 FPS, independent of SDK
- `glyph-editor-sdk-bridge`: SDK integration bridge — converts AnimationModel objects to SDK toggle/turnOff commands with rate limiting

### Modified Capabilities
- `glyph-sdk-integration`: Add `turnOffChannels(channels)` convenience method for the editor to turn off specific channels without toggling

## Impact

- **New packages:** `editor/`, `editor/model/`, `editor/gesture/`, `editor/render/`, `editor/sdk/`
- **New files:** GlyphMapView, GlyphNode, GlyphNodeLayout, GestureSampler, AnimationModel, PreviewRenderer, GlyphSdkRenderer, GlyphEditorViewModel, GlyphEditorScreen
- **Modified:** GlyphRepository (add turnOffChannels), Navigation (add editor route), Screen.kt (add Editor entry)
- **Dependencies:** None new — uses Android's AudioRecord for haptics, Compose Canvas for rendering
- **Permissions:** None new
