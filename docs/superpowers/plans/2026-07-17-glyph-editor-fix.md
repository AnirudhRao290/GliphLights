# Glyph Editor End-to-End Fix Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the Glyph Editor so session lifecycle (Stop/Start) works, remove the redundant Send button, and ensure node taps send commands to hardware reliably.

**Architecture:** The editor has three critical bugs: (1) the Send button is redundant because `updateModel()` already calls `sdkRenderer.render()` on every node toggle, (2) Stop/Start doesn't work because the singleton `GlyphManagerWrapper` session state conflicts between Dashboard and Editor, and (3) the renderer scope lifecycle doesn't align with the session lifecycle. This plan removes the Send button, fixes the session lifecycle, and ensures node taps always reach hardware.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt, Coroutines, Nothing Glyph SDK

## Global Constraints

- Nothing Phone (3a) Pro only (`Common.is24111()`)
- Glyph SDK AAR bundled in `libs/`
- Min SDK from build.gradle.kts
- Kotlin coroutines with `withTimeout(5000L)` for SDK calls

## Issues Found (End-to-End Trace)

### Issue 1: Send button is redundant
- `GlyphEditorViewModel.updateModel()` (line 100-101) already calls `sdkRenderer.render(currentModel)` when session is active
- Every node toggle immediately sends to hardware
- `sendToDevice()` does the exact same thing: `sdkRenderer.render(currentModel)`
- The Send button adds zero functionality

### Issue 2: Stop/Start cycle broken
- `DashboardViewModel.initializeSdk()` runs on `init` and opens a session
- `DashboardViewModel.onCleared()` calls `closeSession()` which closes the shared session
- When user navigates Dashboard → Editor → Back → Editor, the session state is corrupted
- `AppLifecycleObserver` also opens/closes sessions on foreground/background transitions
- Multiple components fight over the singleton `GlyphManagerWrapper` session

### Issue 3: Renderer scope lifecycle mismatch
- `sdkRenderer.start()` creates a `CoroutineScope`
- `sdkRenderer.stop()` cancels it and nulls it
- If session is active but renderer wasn't started (e.g., session opened by Dashboard), all renders fail with "scope is null"
- Already partially fixed by adding `startRenderer()` to LaunchedEffect, but the root cause is the lifecycle mismatch

### Issue 4: toggleChannels always sends ALL active channels
- `GlyphManagerWrapper.toggleChannels()` builds a frame with ALL `newActiveChannels`, not just the changed ones
- This is correct behavior (the Glyph SDK expects full state), but means the wrapper's `activeChannels` set must always be accurate
- If any command fails silently, the wrapper's state diverges from hardware

---

## File Structure

| File | Action | Purpose |
|------|--------|---------|
| `app/.../ui/screens/GlyphEditorScreen.kt` | Modify | Remove Send button, fix bottom bar |
| `app/.../viewmodel/GlyphEditorViewModel.kt` | Modify | Remove `sendToDevice()`, fix session lifecycle |
| `app/.../editor/sdk/GlyphSdkRenderer.kt` | Modify | Fix scope lifecycle, add logging |
| `app/.../viewmodel/DashboardViewModel.kt` | Modify | Remove auto-init (let Editor manage its own session) |
| `app/.../AppLifecycleObserver.kt` | Modify | Remove session open/close (conflicts with Editor) |

---

### Task 1: Remove Send button from Editor UI

**Files:**
- Modify: `app/src/main/java/com/example/gliphlights/ui/screens/GlyphEditorScreen.kt:86-96` (bottom bar params)
- Modify: `app/src/main/java/com/example/gliphlights/ui/screens/GlyphEditorScreen.kt:168-182` (Send button)

**Interfaces:**
- Consumes: `EditorBottomBar` composable
- Produces: Bottom bar with only Clear and Start/Stop buttons

- [ ] **Step 1: Remove Send button from EditorBottomBar**

Remove the `onSendToDevice` parameter and the Send button from `EditorBottomBar`. The bottom bar should only have Clear and Start/Stop.

In `GlyphEditorScreen.kt`, change the `EditorBottomBar` call (around line 86) to remove `onSendToDevice = viewModel::sendToDevice`:

```kotlin
EditorBottomBar(
    activeCount = uiState.activeCount,
    isSessionActive = uiState.isSessionActive,
    isLoading = uiState.isLoading,
    onStartSession = viewModel::startSession,
    onStopSession = viewModel::stopSession,
    onClearAll = viewModel::clearAll
)
```

- [ ] **Step 2: Update EditorBottomBar signature and remove Send button**

Replace the entire `EditorBottomBar` composable (lines 116-186) with:

```kotlin
@Composable
private fun EditorBottomBar(
    activeCount: Int,
    isSessionActive: Boolean,
    isLoading: Boolean,
    onStartSession: () -> Unit,
    onStopSession: () -> Unit,
    onClearAll: () -> Unit
) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Active nodes: $activeCount / 36",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
            ) {
                androidx.compose.material3.OutlinedButton(
                    onClick = onClearAll,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.height(18.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Clear")
                }

                androidx.compose.material3.Button(
                    onClick = if (isSessionActive) onStopSession else onStartSession,
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(text = if (isSessionActive) "Stop" else "Start")
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Remove unused imports**

Remove `Icons.Default.Send` import if no longer used.

- [ ] **Step 4: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/gliphlights/ui/screens/GlyphEditorScreen.kt
git commit -m "fix: remove redundant Send button from editor - node taps already send to hardware"
```

---

### Task 2: Remove sendToDevice() from ViewModel

**Files:**
- Modify: `app/src/main/java/com/example/gliphlights/viewmodel/GlyphEditorViewModel.kt:175-186`

**Interfaces:**
- Consumes: None (removing a method)
- Produces: Cleaner ViewModel with no dead code

- [ ] **Step 1: Remove sendToDevice() method**

Delete the `sendToDevice()` method (lines 175-186) from `GlyphEditorViewModel.kt`:

```kotlin
// DELETE THIS:
fun sendToDevice() {
    val channels = currentModel.activeChannels
    Log.d(TAG, "sendToDevice: isSessionActive=${_uiState.value.isSessionActive}, channels=$channels")
    if (_uiState.value.isSessionActive) {
        sdkRenderer.render(currentModel)
    } else {
        Log.w(TAG, "sendToDevice: session not active, showing error")
        _uiState.update {
            it.copy(errorMessage = "Start a session first to send to device")
        }
    }
}
```

- [ ] **Step 2: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/gliphlights/viewmodel/GlyphEditorViewModel.kt
git commit -m "fix: remove sendToDevice() - node toggles already send via updateModel()"
```

---

### Task 3: Fix Stop/Start session lifecycle

**Files:**
- Modify: `app/src/main/java/com/example/gliphlights/viewmodel/GlyphEditorViewModel.kt:157-163` (stopSession)
- Modify: `app/src/main/java/com/example/gliphlights/viewmodel/GlyphEditorViewModel.kt:109-155` (startSession)

**Interfaces:**
- Consumes: `GlyphRepository` (closeSession, turnOff, initialize, register, openSession)
- Produces: Reliable Stop/Start cycle

- [ ] **Step 1: Fix stopSession() to fully clean up**

Replace `stopSession()` (lines 157-163) with:

```kotlin
fun stopSession() {
    viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        sdkRenderer.stop()
        glyphRepository.turnOff()
        glyphRepository.closeSession()
        _uiState.update { it.copy(isLoading = false, isSessionActive = false) }
    }
}
```

- [ ] **Step 2: Fix startSession() to handle re-initialization**

The current `startSession()` skips if `isLoading` is true. After Stop, the session is closed but `isSessionActive` becomes false. The `LaunchedEffect` should auto-retry. But we need to make sure `startSession()` can re-initialize from a clean state.

The current code already handles this correctly - it calls `init()` → `register()` → `openSession()` → `sdkRenderer.start()`. The only issue is the `isLoading` guard. After Stop sets `isLoading = false`, Start should work.

No change needed to `startSession()` logic, but verify the `isLoading` state is properly reset in `stopSession()`.

- [ ] **Step 3: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/example/gliphlights/viewmodel/GlyphEditorViewModel.kt
git commit -m "fix: proper Stop/Start lifecycle - fully clean up session on stop"
```

---

### Task 4: Fix Dashboard/Editor session conflict

**Files:**
- Modify: `app/src/main/java/com/example/gliphlights/viewmodel/DashboardViewModel.kt:36-61` (initializeSdk)
- Modify: `app/src/main/java/com/example/gliphlights/viewmodel/DashboardViewModel.kt:117-122` (onCleared)

**Interfaces:**
- Consumes: `GlyphRepository`
- Produces: Dashboard doesn't interfere with Editor session

**Problem:** `DashboardViewModel` calls `initializeSdk()` on `init` (opens a session) and `closeSession()` on `onCleared()`. This conflicts with the Editor's session management because both use the same singleton `GlyphManagerWrapper`.

- [ ] **Step 1: Remove auto-init from DashboardViewModel**

The Dashboard should only READ the glyph state, not manage sessions. Remove `initializeSdk()` call from `init` and the `initializeSdk()` method itself.

In `DashboardViewModel.kt`, change the `init` block:

```kotlin
init {
    observeGlyphState()
}
```

Remove the `initializeSdk()` method entirely (lines 36-61).

- [ ] **Step 2: Remove session cleanup from onCleared**

The Dashboard shouldn't close sessions it didn't open. Change `onCleared()`:

```kotlin
override fun onCleared() {
    super.onCleared()
    // Don't close session here - the Editor manages its own session lifecycle
}
```

- [ ] **Step 3: Remove unused imports**

Remove any imports that are no longer needed after removing `initializeSdk()`.

- [ ] **Step 4: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/gliphlights/viewmodel/DashboardViewModel.kt
git commit -m "fix: remove session management from Dashboard - only Editor manages sessions"
```

---

### Task 5: Remove AppLifecycleObserver session conflicts

**Files:**
- Modify: `app/src/main/java/com/example/gliphlights/AppLifecycleObserver.kt`

**Interfaces:**
- Consumes: `GlyphRepository`
- Produces: No session open/close on foreground/background

**Problem:** `AppLifecycleObserver` opens a session on `onStart` (foreground) and closes on `onStop` (background). This conflicts with the Editor's session management.

- [ ] **Step 1: Remove session management from lifecycle observer**

The lifecycle observer should only observe, not manage sessions. Remove the `openSession()` and `closeSession()` calls.

In `AppLifecycleObserver.kt`, replace the `onStart` and `onStop` methods:

```kotlin
@OnLifecycleEvent(Lifecycle.Event.ON_START)
fun onEnterForeground() {
    // Don't auto-open sessions - let the Editor manage its own lifecycle
}

@OnLifecycleEvent(Lifecycle.Event.ON_STOP)
fun onEnterBackground() {
    // Don't auto-close sessions - let the Editor manage its own lifecycle
}
```

- [ ] **Step 2: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/gliphlights/AppLifecycleObserver.kt
git commit -m "fix: remove session management from lifecycle observer - conflicts with Editor"
```

---

### Task 6: Fix renderer scope lifecycle alignment

**Files:**
- Modify: `app/src/main/java/com/example/gliphlights/editor/sdk/GlyphSdkRenderer.kt`

**Interfaces:**
- Consumes: `GlyphRepository`
- Produces: Renderer scope always matches session state

**Problem:** The renderer's `scope` can be null while the session is active, causing all renders to silently fail. The `start()`/`stop()` lifecycle must align with the session.

- [ ] **Step 1: Make renderer self-healing**

If `render()` is called and `scope` is null, automatically start the scope instead of dropping the frame.

Replace the `render()` method in `GlyphSdkRenderer.kt`:

```kotlin
fun render(model: AnimationModel) {
    val currentScope = scope ?: run {
        Log.w(TAG, "render: scope was null, auto-starting renderer")
        start()
        scope ?: return
    }
    val channels = model.activeChannels
    Log.d(TAG, "render: channels=$channels")
    val now = System.currentTimeMillis()
    val elapsed = now - lastCommandTime

    if (elapsed >= MIN_FRAME_INTERVAL_MS) {
        lastCommandTime = now
        sendCommand(model)
    } else {
        pendingModel = model
        coalesceJob?.cancel()
        coalesceJob = currentScope.launch {
            val delayMs = MIN_FRAME_INTERVAL_MS - elapsed
            kotlinx.coroutines.delay(delayMs)
            val latest = pendingModel ?: return@launch
            pendingModel = null
            lastCommandTime = System.currentTimeMillis()
            sendCommand(latest)
        }
    }
}
```

- [ ] **Step 2: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/gliphlights/editor/sdk/GlyphSdkRenderer.kt
git commit -m "fix: renderer auto-starts scope if null - prevents silent frame drops"
```

---

### Task 7: End-to-end verification

**Files:** None (testing only)

- [ ] **Step 1: Build final APK**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Manual test checklist**

Test on Nothing Phone (3a) Pro:

1. **Cold start**: Open app → Dashboard → tap "Open Editor"
   - Session should auto-start (LaunchedEffect calls startSession)
   - Loading spinner shows on Start button
   - After session connects, button changes to "Stop"
   - Tap nodes → they should light up on hardware immediately

2. **Stop/Start cycle**: Tap "Stop" → then "Stop" should change to "Start"
   - Hardware LEDs should turn off
   - Tap "Start" → session re-initializes
   - Tap nodes → they should light up again

3. **Back navigation**: Tap Back → tap "Open Editor" again
   - Session should start fresh
   - Nodes should work

4. **Clear button**: Tap some nodes → tap "Clear"
   - All nodes should turn off on hardware
   - Active count should reset to 0

5. **Rapid toggling**: Tap nodes rapidly
   - Should not crash
   - Hardware should keep up (33ms frame rate limit)

- [ ] **Step 3: Commit final state**

```bash
git add -A
git commit -m "fix: complete Glyph Editor lifecycle fix - remove Send, fix Stop/Start, resolve session conflicts"
```
