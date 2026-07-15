## 1. SDK Setup

- [x] 1.1 Download GlyphSDK.aar from Nothing Developer Kit GitHub repo (https://github.com/Nothing-Developer-Programme/Glyph-Developer-Kit) and place in `app/libs/`
- [x] 1.2 Uncomment `implementation(files("libs/GlyphSDK.aar"))` in `app/build.gradle.kts` and verify build compiles
- [x] 1.3 Verify `GlyphManager`, `GlyphFrameBuilder`, `Common`, and `Glyph` classes are resolvable on the classpath

## 2. Device Detection

- [x] 2.1 Replace hardcoded `detectDevice()` in `GlyphManagerWrapper.kt` with `Common.is24111()` check
- [x] 2.2 Return `DeviceInfo` with `isSupported = true` and `model = "Nothing Phone (3a) Pro"` when `is24111()` returns true
- [x] 2.3 Return `DeviceInfo.UNKNOWN` when device is not supported

## 3. SDK Initialization

- [x] 3.1 In `GlyphManagerWrapper.init()`, call `GlyphManager.getInstance(context)` and store the instance
- [x] 3.2 Call `glyphManager.init(callback)` with a callback that transitions session state to `CONNECTED`
- [x] 3.3 Call `glyphManager.register(Glyph.DEVICE_24111)` in the `register()` method
- [x] 3.4 Add `withContext(Dispatchers.IO)` around blocking SDK calls

## 4. Session Management

- [x] 4.1 In `openSession()`, call `glyphManager.openSession()` and transition state to `SESSION_ACTIVE`
- [x] 4.2 In `closeSession()`, call `glyphManager.closeSession()` and transition state to `SESSION_CLOSED`
- [x] 4.3 In `cleanup()`, call `closeSession()` then `glyphManager.unInit()` and reset to `DISCONNECTED`

## 5. Toggle Channels

- [x] 5.1 In `toggleChannels()`, build a `GlyphFrame` via `glyphManager.getGlyphFrameBuilder()`
- [x] 5.2 Call `builder.buildChannel(index)` for each channel in the list
- [x] 5.3 Call `glyphManager.toggle(frame)` with the built frame
- [x] 5.4 Update internal `activeChannels` set to track toggled state

## 6. Animate Channels

- [x] 6.1 In `animateChannels()`, build a `GlyphFrame` with animation parameters
- [x] 6.2 Call `builder.buildPeriod(params.period)`, `buildCycles(params.cycles)`, `buildInterval(params.interval)`
- [x] 6.3 Call `glyphManager.animate(frame)` with the built frame

## 7. Display Progress

- [x] 7.1 In `displayProgress()`, build a `GlyphFrame` for Zone C channels
- [x] 7.2 Call `glyphManager.displayProgress(frame, progress, reverse)` with the frame and progress value

## 8. Turn Off

- [x] 8.1 In `turnOff()`, call `glyphManager.turnOff()` to turn off all Glyph channels
- [x] 8.2 Clear the internal `activeChannels` set after turnOff

## 9. Error Handling

- [x] 9.1 Wrap all SDK calls in try/catch blocks and map exceptions to `SdkResult.Error`
- [x] 9.2 Handle `GlyphManager` null returns or initialization failures gracefully
- [x] 9.3 Ensure `init()` returns `SdkResult.Error` if `GlyphManager.getInstance()` throws

## 10. Build & Verify

- [x] 10.1 Run `./gradlew assembleDebug` and verify successful build
- [ ] 10.2 Install on Nothing Phone (3a) Pro and verify SDK initialization succeeds
- [ ] 10.3 Test Toggle All button — physical Glyph LEDs SHALL turn on/off
- [ ] 10.4 Test Animate All button — physical Glyph LEDs SHALL animate
- [ ] 10.5 Test Turn Off button — all physical Glyph LEDs SHALL turn off
- [ ] 10.6 Test individual zone toggle on Controls screen
- [ ] 10.7 Test progress slider on Controls screen — Zone C ring SHALL show progress
