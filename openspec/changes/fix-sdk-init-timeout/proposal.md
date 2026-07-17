## Why

The Glyph SDK initialization has a critical hang bug: `serviceConnected.await()` in `GlyphManagerWrapper.init()` has no timeout. On Nothing Phone (3a) Pro, if the Glyph service connection callback never fires (service crash, slow startup, or any initialization issue), the coroutine hangs indefinitely. This keeps `isLoading = true` permanently, disabling the Start button and making the entire Glyph Editor screen unresponsive. The user sees buttons that don't respond to taps.

## What Changes

- Add timeout to `serviceConnected.await()` in `GlyphManagerWrapper.init()` (5 seconds)
- Add timeout to `glyphManager?.init(callback)` initialization
- Add timeout to `register()` SDK call
- Add timeout to `openSession()` SDK call
- Improve error messaging when timeouts occur
- Ensure `isLoading` is always reset on timeout or failure

## Capabilities

### New Capabilities

None - this is a bugfix to existing behavior.

### Modified Capabilities

- `glyph-editor-sdk-bridge`: Add timeout requirements to SDK initialization sequence. The spec currently assumes SDK calls complete promptly; we need to add explicit timeout constraints.

## Impact

**Affected Code:**
- `app/src/main/java/com/example/gliphlights/sdk/GlyphManagerWrapper.kt` — `init()`, `register()`, `openSession()` methods
- `app/src/main/java/com/example/gliphlights/viewmodel/GlyphEditorViewModel.kt` — `startSession()` error handling

**Dependencies:** No new dependencies. Uses `kotlinx.coroutines.withTimeout`.

**Risk:** Low — timeout failures produce clear error messages instead of silent hangs.
