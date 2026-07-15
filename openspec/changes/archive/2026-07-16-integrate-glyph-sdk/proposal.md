## Why

The app UI is fully functional on the Nothing Phone (3a) Pro — it connects, shows device info, toggles internal state, and displays "Glyph Active" — but the actual Glyph hardware never receives any commands. All SDK methods in `GlyphManagerWrapper.kt` are stubs with TODO comments. The Glyph SDK AAR has never been downloaded into `app/libs/`, and no real `GlyphManager` API calls exist in the codebase. The app must be wired to the real Nothing Glyph SDK to actually control the LED hardware.

## What Changes

- Download and include the official Nothing Glyph SDK AAR (`GlyphSDK.aar`) in `app/libs/`
- Uncomment and configure the AAR dependency in `app/build.gradle.kts`
- Replace all TODO stubs in `GlyphManagerWrapper.kt` with real SDK calls: `GlyphManager.getInstance()`, `init()`, `register()`, `openSession()`, `closeSession()`, `toggle()`, `animate()`, `displayProgress()`, `turnOff()`
- Implement device detection using `Common.is24111()` and `Glyph.DEVICE_24111`
- Build `GlyphFrame` objects via `GlyphFrameBuilder` for toggle, animate, and progress operations
- Add proper API key configuration (`"test"` for debug builds)
- Ensure session lifecycle is correctly managed (open before commands, close on cleanup)

## Capabilities

### New Capabilities

- `glyph-sdk-integration`: Full integration with the Nothing Glyph SDK — initialization, session management, device registration, and hardware control (toggle, animate, display progress, turn off)

### Modified Capabilities

_(none — this is purely adding real SDK calls to existing stubs)_

## Impact

- **Files modified**: `GlyphManagerWrapper.kt` (major rewrite), `app/build.gradle.kts` (uncomment AAR dep), `AndroidManifest.xml` (API key meta-data already present)
- **New file**: `app/libs/GlyphSDK.aar` (downloaded from GitHub)
- **Dependencies**: Nothing Glyph SDK (`com.nothing.ketchum`) AAR
- **Risk**: SDK API compatibility — the AAR version must match the target device's firmware. Debug builds use API key `"test"`.
