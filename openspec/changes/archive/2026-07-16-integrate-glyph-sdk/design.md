## Context

The Glyph Control app has a complete UI layer (Dashboard, Controls, Settings screens), a ViewModel layer, and a repository layer — but the SDK wrapper (`GlyphManagerWrapper.kt`) contains only stubs. The `app/libs/` directory is empty (no AAR). The Nothing Glyph SDK is distributed as an AAR from [GitHub](https://github.com/Nothing-Developer-Programme/Glyph-Developer-Kit) and provides `GlyphManager`, `GlyphFrameBuilder`, `Common`, and `Glyph` classes.

Current flow: UI → ViewModel → Repository → GlyphManagerWrapper (stub) → nothing happens.
Target flow: UI → ViewModel → Repository → GlyphManagerWrapper → GlyphManager SDK → hardware LEDs.

## Goals / Non-Goals

**Goals:**
- Replace all stub methods with real SDK calls that control the physical Glyph LEDs
- Correctly manage the SDK lifecycle: `init()` → `register()` → `openSession()` → commands → `closeSession()` → `unInit()`
- Support toggle, animate, displayProgress, and turnOff operations on the real hardware
- Use `"test"` API key for debug builds so the app works without a production key
- Detect device using `Common.is24111()` for Nothing Phone (3a) Pro

**Non-Goals:**
- Music visualization, notification effects, charging effects (not supported by SDK)
- Brightness control (not available in SDK)
- Supporting devices beyond Nothing Phone (3a) Pro (model `is24111`)
- Publishing a signed release build

## Decisions

### 1. AAR as local file dependency

**Decision**: Download `GlyphSDK.aar` into `app/libs/` and reference via `implementation(files("libs/GlyphSDK.aar"))`.

**Rationale**: The SDK is not published to Maven Central. The official docs instruct manual AAR placement. This is the standard approach for Nothing SDK integration.

**Alternative considered**: Publishing to a private Maven repo — unnecessary complexity for a single-device app.

### 2. Keep existing GlyphManagerWrapper abstraction

**Decision**: Replace stubs inside the existing `GlyphManagerWrapper` class rather than removing it.

**Rationale**: The wrapper already defines a clean contract (`init`, `register`, `openSession`, `toggleChannels`, etc.) used by `GlyphRepositoryImpl` and ViewModels. Keeping it avoids cascading changes across the architecture. The wrapper becomes the single integration point with the SDK.

### 3. GlyphFrameBuilder pattern for commands

**Decision**: Build `GlyphFrame` objects via `GlyphManager.getGlyphFrameBuilder()` for each command type.

**Rationale**: The SDK requires frames to be constructed for toggle/animate/progress. The builder pattern allows adding channels individually (`buildChannel(index)`) and setting animation parameters (`buildPeriod`, `buildCycles`, `buildInterval`).

### 4. Synchronous SDK calls dispatched via coroutine scope

**Decision**: The GlyphManager SDK calls are synchronous/blocking. Wrap them in `withContext(Dispatchers.IO)` inside the already-suspending wrapper methods.

**Rationale**: The wrapper methods are already `suspend fun` from the existing interface. Dispatching blocking SDK calls to IO prevents UI thread stalls.

### 5. Error handling: catch SDK exceptions, map to SdkResult.Error

**Decision**: Wrap all SDK calls in try/catch, map exceptions to `SdkResult.Error(exception, message)`.

**Rationale**: The existing codebase already uses `SdkResult` for error propagation. This preserves the error-handling contract with ViewModels.

## Risks / Trade-offs

- **[SDK version mismatch]** → The AAR must match the target device firmware. Mitigation: Download the latest AAR from the official repo.
- **[API key in source]** → The `"test"` key is hardcoded for debug. Mitigation: Already handled via `AndroidManifest.xml` meta-data; production key would be injected at build time.
- **[No runtime fallback if SDK unavailable]** → If the AAR is missing or incompatible, `GlyphManager.getInstance()` may throw. Mitigation: Wrap in try/catch in `init()`, surface error via `SdkResult.Error`.
