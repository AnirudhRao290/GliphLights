## Context

The Glyph Control app controls the Nothing Phone (3a) Pro's 36 Glyph LEDs across 3 zones (A: vertical bar, B: horizontal bar, C: camera ring). The existing SDK integration uses discrete `toggle()` and `animate()` commands — there is no continuous frame buffer. Each command builds a `GlyphFrame` with channel selections and brightness, then sends it to the SDK. The SDK is rate-limited to approximately 30 commands/second.

The goal is to add a real-time music visualizer that captures microphone audio, processes the signal, and maps amplitude to Glyph brightness and zone activation patterns.

## Goals / Non-Goals

**Goals:**
- Capture microphone audio at 44.1kHz mono PCM with low latency
- Process audio signals (RMS, peak, EMA smoothing, noise gate)
- Map audio amplitude to Glyph brightness and zone activation
- Provide 4 visualization modes: Pulse, Wave, Beat, Glow
- Deliver a dedicated UI with controls and real-time feedback
- Handle microphone permissions gracefully
- Optimize for battery and performance (coroutines, no allocations in audio loop)

**Non-Goals:**
- FFT/frequency analysis (amplitude-only visualization)
- Per-LED color control (SDK limitation — brightness only)
- Continuous frame buffer streaming (SDK limitation — discrete commands)
- Audio file playback visualization (microphone input only)
- Background visualization when app is not visible

## Decisions

### Decision 1: Audio Capture via AudioRecord on Dispatchers.IO

**Choice:** Use `AudioRecord` with a dedicated coroutine on `Dispatchers.IO`, reading into a pre-allocated circular buffer (4096 samples).

**Rationale:** `AudioRecord` is the only Android API that provides raw PCM access at low latency. `MediaRecorder` produces encoded output unsuitable for real-time analysis. The circular buffer prevents memory pressure and allows the processing loop to consume data independently of capture rate.

**Alternatives considered:** `Oboe` (native audio library) — rejected because it adds a native dependency and the project is pure Kotlin. `AudioRecord` with `VOICE_COMMUNICATION` audio source — considered but `MIC` source is simpler and sufficient.

### Decision 2: Signal Processing with EMA Smoothing

**Choice:** Calculate RMS amplitude per buffer, apply Exponential Moving Average (α=0.3) for smoothing, and use a configurable noise gate threshold.

**Rationale:** EMA provides smooth transitions without lag. RMS is more perceptually accurate than peak amplitude for volume representation. The noise gate prevents background noise from triggering visual activity.

**Alternatives considered:** FFT for frequency bands — rejected as overkill for amplitude-based visualization. Moving average — rejected because it introduces more lag than EMA.

### Decision 3: Glyph Updates via toggle() with Brightness

**Choice:** Map normalized amplitude (0.0–1.0) to Glyph brightness via `buildBrightness()`, and use `toggleChannels()` to activate the appropriate zone channels. Target 20–30 FPS update rate with a minimum 33ms interval between SDK commands.

**Rationale:** The SDK only supports discrete `toggle()` and `animate()` commands. There is no continuous frame buffer. Brightness mapping is the most direct way to represent amplitude. Rate limiting prevents SDK overload.

**Alternatives considered:** Using `animate()` with short periods — rejected because animate is designed for predefined breathing patterns, not real-time amplitude mapping. Sending commands as fast as possible — rejected because SDK rate limits would cause dropped commands.

### Decision 4: Visualization Modes as Strategy Pattern

**Choice:** Implement each mode (Pulse, Wave, Beat, Glow) as a `VisualizationMode` interface implementation. The visualizer engine calls `mode.render(amplitude, timestamp)` which returns a `GlyphCommand` (channels + brightness).

**Rationale:** Clean separation of concerns. Each mode is independently testable. New modes can be added without modifying the engine. The engine handles rate limiting and SDK communication uniformly.

### Decision 5: Single Screen with Compose StateFlow

**Choice:** One `MusicVisualizerScreen` composable driven by `MusicVisualizerViewModel` using `StateFlow`. Audio capture, processing, and Glyph updates run in ViewModel scope via `viewModelScope`.

**Rationale:** Follows the existing app architecture pattern. StateFlow provides reactive UI updates. ViewModel scope ensures cleanup on navigation.

## Risks / Trade-offs

- **[Risk] SDK rate limiting drops commands** → Mitigation: Enforce minimum 33ms interval between commands. Track actual FPS and display in UI. If commands are dropped, the visualizer gracefully degrades rather than crashing.
- **[Risk] Audio latency exceeds 80ms target** → Mitigation: Use `AudioRecord` with small buffer (4096 samples = ~93ms at 44.1kHz). Accept that Android audio latency has hardware floor. Display actual latency in UI for transparency.
- **[Risk] Battery drain from continuous audio capture** → Mitigation: Stop capture when visualizer is stopped. Use `WakeLock` only while active. Release all resources on screen exit.
- **[Trade-off] Amplitude-only vs frequency analysis** → Simpler implementation, lower CPU usage, but less visually rich than frequency-band mapping. Acceptable for v1.
- **[Trade-off] Zone-level activation vs per-LED** → SDK limitation. Wave mode activates zones sequentially (A→B→C) rather than animating individual LEDs. Less granular but the only supported approach.
