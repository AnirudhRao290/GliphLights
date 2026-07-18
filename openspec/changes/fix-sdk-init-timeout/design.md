## Context

The Glyph SDK initialization in `GlyphManagerWrapper` uses `CompletableDeferred.await()` without a timeout. On Nothing Phone (3a) Pro, if the Glyph service connection callback never fires (service crash, slow startup, or initialization issue), the coroutine hangs indefinitely. This keeps `isLoading = true` in the ViewModel, permanently disabling the Start button and making the editor unresponsive.

Current code path:
1. `startSession()` → `glyphRepository.initialize()` → `glyphManager.init(callback)` → `serviceConnected.await()` (hangs)

## Goals / Non-Goals

**Goals:**
- Add timeouts to all SDK initialization calls to prevent indefinite hangs
- Ensure `isLoading` is always reset on timeout or failure
- Provide clear error messages when timeouts occur
- Maintain existing functionality when SDK connects normally

**Non-Goals:**
- Changing the SDK initialization sequence
- Adding retry logic (future enhancement)
- Modifying the preview renderer or gesture handling

## Decisions

### Decision 1: Use `withTimeout` from kotlinx.coroutines

**Choice:** Wrap SDK calls with `withTimeout(5000L)` (5 seconds)

**Rationale:**
- 5 seconds is generous enough for normal service connection
- `withTimeout` throws `TimeoutCancellationException` which is caught by existing try-catch
- No new dependencies needed — `kotlinx.coroutines` is already included

**Alternative considered:** Custom timeout with `delay()` + `CompletableDeferred` — rejected as more complex with no benefit.

### Decision 2: Apply timeout at wrapper level, not ViewModel

**Choice:** Add timeouts in `GlyphManagerWrapper.init()`, `register()`, `openSession()`

**Rationale:**
- Keeps timeout logic centralized in the SDK wrapper
- ViewModel already handles `SdkResult.Error` — timeout produces same error path
- Easier to adjust timeout values in one place

### Decision 3: Keep existing error message format

**Choice:** Timeout errors use same `SdkResult.Error` pattern with descriptive messages

**Rationale:**
- ViewModel error handling already works with this pattern
- Snackbar display already implemented
- Consistent user experience

## Risks / Trade-offs

- **[Risk]** 5-second timeout may be too long for poor UX → **Mitigation:** Can reduce to 3s if needed; 5s is safe default
- **[Risk]** Timeout on fast device unnecessarily delays → **Mitigation:** 5s is acceptable; real connection typically <1s
- **[Trade-off]** Adds `withTimeout` wrapper complexity → **Benefit:** Prevents indefinite hangs that break the entire UI
