## 1. Add Timeout to Service Connection

- [x] 1.1 Wrap `serviceConnected.await()` in `GlyphManagerWrapper.init()` with `withTimeout(5000L)`
- [x] 1.2 Add `TimeoutCancellationException` import to `GlyphManagerWrapper.kt`
- [x] 1.3 Update error message for timeout: "Glyph service connection timed out"

## 2. Add Timeout to Register Call

- [x] 2.1 Wrap `glyphManager?.register()` call in `register()` with `withTimeout(5000L)`
- [x] 2.2 Update error message for timeout: "Glyph registration timed out"

## 3. Add Timeout to Open Session Call

- [x] 3.1 Wrap `glyphManager?.openSession()` call in `openSession()` with `withTimeout(5000L)`
- [x] 3.2 Update error message for timeout: "Glyph session open timed out"

## 4. Verify Error Handling

- [x] 4.1 Verify `startSession()` in ViewModel catches timeout errors and resets `isLoading`
- [x] 4.2 Verify Snackbar displays timeout error messages
- [x] 4.3 Test that Start button is re-enabled after timeout

## 5. Build and Test

- [x] 5.1 Run `./gradlew assembleDebug` to verify compilation
- [x] 5.2 Verify no import errors or warnings
